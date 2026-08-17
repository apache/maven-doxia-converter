/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.maven.doxia;

import javax.inject.Inject;
import javax.inject.Named;

import java.io.BufferedInputStream;
import java.io.CharArrayWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.Reader;
import java.io.StringReader;
import java.io.Writer;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Scanner;
import java.util.stream.Collectors;

import com.ibm.icu.text.CharsetDetector;
import com.ibm.icu.text.CharsetMatch;
import org.apache.commons.io.IOUtils;
import org.apache.commons.io.input.XmlStreamReader;
import org.apache.commons.io.output.XmlStreamWriter;
import org.apache.commons.lang3.StringUtils;
import org.apache.maven.doxia.macro.MacroExecutionException;
import org.apache.maven.doxia.macro.MacroExecutor;
import org.apache.maven.doxia.macro.MacroRequest;
import org.apache.maven.doxia.macro.manager.MacroNotFoundException;
import org.apache.maven.doxia.parser.ParseException;
import org.apache.maven.doxia.parser.Parser;
import org.apache.maven.doxia.sink.Sink;
import org.apache.maven.doxia.sink.SinkFactory;
import org.apache.maven.doxia.wrapper.InputFileWrapper;
import org.apache.maven.doxia.wrapper.InputReaderWrapper;
import org.apache.maven.doxia.wrapper.OutputFileWrapper;
import org.apache.maven.doxia.wrapper.OutputStreamWrapper;
import org.codehaus.plexus.util.FileUtils;
import org.codehaus.plexus.util.PathTool;
import org.codehaus.plexus.util.SelectorUtils;
import org.codehaus.plexus.util.xml.XmlUtil;
import org.codehaus.plexus.util.xml.pull.MXParser;
import org.codehaus.plexus.util.xml.pull.XmlPullParser;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static java.lang.String.format;

/**
 * Default implementation of <code>Converter</code>
 *
 * @author <a href="mailto:vincent.siveton@gmail.com">Vincent Siveton</a>
 */
@Named
public class DefaultConverter implements Converter {

    /** Filename suffix used for Doxia source files being preprocessed by Velocity */
    private static final String VELOCITY_TEMPLATE_EXTENSION = ".vm";

    /** Macro formatter for different Doxia formats.
     * @see <a href="https://maven.apache.org/doxia/macros/index.html">Doxia Macros</a>
     */
    enum MacroFormatter {
        APT("%{", "|", "=", "|", "}"),
        FML("<macro name=\"", "\"", "<param name=\"", "\" value=\" />", "</macro>"),
        XDOC("<macro name=\"", "\"", "<param name=\"", "\" value=\" />", "</macro>"),
        XHTML("<!-- MACRO{", "|", "=", "|", "} -->"),
        MARKDOWN("<!-- MACRO{", "|", "=", "|", "} -->");

        private final String prefix;
        private final String nameParameterDelimiter;
        private final String parameterNameValueDelimiter;
        private final String parameterDelimiter;
        private final String suffix;

        public static MacroFormatter forFormat(DoxiaFormat format) {
            switch (format) {
                case APT:
                    return APT;
                case FML:
                    return FML;
                case XDOC:
                    return XDOC;
                case XHTML:
                    return XHTML;
                case MARKDOWN:
                    return MARKDOWN;
                default:
                    throw new IllegalArgumentException("Unsupported Doxia format: " + format);
            }
        }

        MacroFormatter(
                String prefix,
                String nameParameterDelimiter,
                String parameterNameValueDelimiter,
                String parameterDelimiter,
                String suffix) {
            this.prefix = prefix;
            this.nameParameterDelimiter = nameParameterDelimiter;
            this.parameterNameValueDelimiter = parameterNameValueDelimiter;
            this.parameterDelimiter = parameterDelimiter;
            this.suffix = suffix;
        }

        /**
         * Formats a macro with the given name and parameters for this format.
         * @param name
         * @param parameters
         * @return the formatted macro string to be emitted in the output
         */
        public String format(String name, Map<String, Object> parameters) {
            StringBuilder macro = new StringBuilder();
            macro.append(prefix).append(name);
            String parameterString = parameters.entrySet().stream()
                    .map(e -> e.getKey() + parameterNameValueDelimiter + e.getValue())
                    .collect(Collectors.joining(parameterDelimiter));
            if (!parameters.isEmpty()) {
                macro.append(nameParameterDelimiter).append(parameterString);
            }
            macro.append(suffix);
            return macro.toString();
        }
    }

    /**
     * All supported Doxia formats (either only parser, only sink or both)
     */
    public enum DoxiaFormat {
        APT("apt", "apt", true, true),
        FML("fml", "fml", "faqs", true, false),
        XDOC("xdoc", "xml", "document", true, true),
        XHTML("xhtml", "html", "html", true, true),
        MARKDOWN("markdown", "md", true, true);

        /** Plexus role hint for Doxia sink/parser */
        private final String roleHint;

        private final String extension;
        /** The name of the first element in case this is an XML format, otherwise {@code null} */
        private final String firstElement;

        private final boolean hasParser;
        private final boolean hasSink;

        DoxiaFormat(String roleHint, String extension, boolean hasParser, boolean hasSink) {
            this(roleHint, extension, null, hasParser, hasSink);
        }

        DoxiaFormat(String roleHint, String extension, String firstElement, boolean hasParser, boolean hasSink) {
            this.roleHint = roleHint;
            this.extension = extension;
            this.firstElement = firstElement;
            this.hasParser = hasParser;
            this.hasSink = hasSink;
        }

        /**
         *
         * @return the primary extension used with this format
         */
        public String getExtension() {
            return extension;
        }

        public boolean hasParser() {
            return hasParser;
        }

        public boolean hasSink() {
            return hasSink;
        }

        /**
         *
         * @return {@code true} in case this format is XML based
         */
        public boolean isXml() {
            return firstElement != null;
        }

        /**
         * @param parsers all available parsers, keyed by role hint
         * @param macroFormatter a formatter for macros in the target format
         * @return an instance of <code>Parser</code> depending on the format which converts macros with the given {@link MacroFormatter}
         * @throws IllegalStateException if no Parser is registered for the given format.
         * @throws IllegalArgumentException if any parameter is null
         */
        public Parser getParser(Map<String, Parser> parsers, MacroFormatter macroFormatter) {
            if (!hasParser) {
                throw new IllegalStateException("The format " + this + " is not supported as parser!");
            }
            Objects.requireNonNull(parsers, "parsers is required");
            Parser parser = parsers.get(roleHint);
            if (parser == null) {
                throw new IllegalStateException("No Parser registered for format " + this + " (role hint \"" + roleHint
                        + "\"); is the according Doxia module on the classpath?");
            }
            parser.setMacroExecutor(new MacroConverterExecutor(macroFormatter));
            return parser;
        }

        public static class MacroConverterExecutor implements MacroExecutor {
            private final MacroFormatter macroFormatter;

            public MacroConverterExecutor(MacroFormatter macroFormatter) {
                super();
                this.macroFormatter = macroFormatter;
            }

            @Override
            public void executeMacro(String id, MacroRequest request, Sink sink)
                    throws MacroExecutionException, MacroNotFoundException {
                // filter out internal parameters but keep original order
                Map<String, Object> parameters = request.getParameters().entrySet().stream()
                        .filter(e -> !MacroRequest.isInternalParameter(e.getKey()))
                        .collect(Collectors.toMap(
                                Map.Entry::getKey, Map.Entry::getValue, (e1, e2) -> e1, LinkedHashMap::new));

                // the format of macros differs between the parser implementations
                // (https://maven.apache.org/doxia/macros/index.html)
                String macro = macroFormatter.format(id, parameters);
                sink.rawText(macro);
            }
        }

        /**
         * @param sinkFactories all available sink factories, keyed by role hint
         * @return an instance of <code>SinkFactory</code> depending on the given format.
         * @throws IllegalStateException if no SinkFactory is registered for the given format.
         * @throws IllegalArgumentException if any parameter is null
         */
        public SinkFactory getSinkFactory(Map<String, SinkFactory> sinkFactories) {
            if (!hasSink) {
                throw new IllegalStateException("The format " + this + " is not supported as sink!");
            }
            Objects.requireNonNull(sinkFactories, "sinkFactories is required");

            SinkFactory sinkFactory = sinkFactories.get(roleHint);
            if (sinkFactory == null) {
                throw new IllegalStateException("No SinkFactory registered for format " + this + " (role hint \""
                        + roleHint + "\"); is the according Doxia module on the classpath?");
            }
            return sinkFactory;
        }

        /**
         * Auto detect Doxia format for the given file depending on:
         * <ul>
         * <li>the file name for TextMarkup based Doxia files</li>
         * <li>the file content for XMLMarkup based Doxia files</li>
         * </ul>
         *
         * @param f not null file
         * @return the detected encoding from f.
         * @throws IllegalArgumentException if f is not a file.
         * @throws UnsupportedOperationException if could not detect the Doxia format.
         */
        public static DoxiaFormat autoDetectFormat(File f) {
            if (!f.isFile()) {
                throw new IllegalArgumentException(
                        "The path '" + f.getAbsolutePath() + "' does not locate a file, could not detect format.");
            }

            for (DoxiaFormat format : EnumSet.allOf(DoxiaFormat.class)) {
                if (format.isXml()) {
                    // Handle XML files
                    String firstTag = getFirstTag(f);
                    if (firstTag == null) {
                        //noinspection UnnecessaryContinue
                        continue;
                    }
                    if (firstTag.equals(format.firstElement)) {
                        return format;
                    }
                } else {
                    if (hasFileExtensionIgnoreCase(f.getName(), format.getExtension())) {
                        return format;
                    }
                }
            }
            throw new UnsupportedOperationException(format(
                    "Could not detect the Doxia format for file: %s%nSpecify explicitly the Doxia format.",
                    f.getAbsolutePath()));
        }
    }

    private PostProcess postProcess = PostProcess.NONE;

    /** Flag to format the generated files, actually only for XML based sinks. */
    private boolean formatOutput;

    /** All Doxia parsers on the classpath, keyed by role hint */
    private final Map<String, Parser> parsers;

    /** All Doxia sink factories on the classpath, keyed by role hint */
    private final Map<String, SinkFactory> sinkFactories;

    @Inject
    public DefaultConverter(Map<String, Parser> parsers, Map<String, SinkFactory> sinkFactories) {
        this.parsers = parsers;
        this.sinkFactories = sinkFactories;
    }

    /** SLF4J logger */
    private static final Logger LOGGER = LoggerFactory.getLogger(DefaultConverter.class);

    /** Map of temporary output files to their final output files */
    private Map<Path, Path> outputRenameMap = new HashMap<>();

    /** {@inheritDoc} */
    @Override
    public void convert(InputFileWrapper input, OutputFileWrapper output)
            throws UnsupportedFormatException, ConverterException {
        Objects.requireNonNull(input, "input is required");
        Objects.requireNonNull(output, "output is required");

        outputRenameMap.clear();
        if (input.getFile().isFile()) {
            convert(input.getFile(), input.getEncoding(), input.getFormat(), output);
        } else {
            List<File> files;
            try {
                files = FileUtils.getFiles(
                        input.getFile(),
                        getFileNamePatterns(input.getFormat().getExtension(), !input.isExcludeVelocityTemplates()),
                        StringUtils.join(FileUtils.getDefaultExcludes(), ", "));
            } catch (IOException e) {
                throw new ConverterException("IOException: " + e.getMessage(), e);
            } catch (IllegalStateException e) {
                throw new ConverterException("IllegalStateException: " + e.getMessage(), e);
            }
            if (files.isEmpty()) {
                throw new ConverterException("ConverterException: No files with extension "
                        + input.getFormat().getExtension() + " found in directory " + input.getFile());
            }
            for (File f : files) {
                File relativeOutputDirectory =
                        new File(PathTool.getRelativeFilePath(input.getFile().getAbsolutePath(), f.getParent()));
                convert(f, input.getEncoding(), input.getFormat(), output, relativeOutputDirectory);
            }
        }
        try {
            postProcessAllFiles(output.getFormat());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new ConverterException("Error post processing all files: " + e.getMessage(), e);
        } catch (IOException e) {
            throw new ConverterException("Error post processing all files: " + e.getMessage(), e);
        }
    }

    static String getFileNamePatterns(String extension, boolean includeVelocityTemplates) {
        StringBuilder patterns = new StringBuilder("**/*." + extension);
        if (includeVelocityTemplates) {
            patterns.append(",");
            patterns.append("**/*.").append(extension).append(VELOCITY_TEMPLATE_EXTENSION);
        }
        return patterns.toString();
    }

    private void postProcessFile(File inputFile, File outputFile) throws IOException, InterruptedException {
        switch (postProcess) {
            case REMOVE_AFTER_CONVERSION:
                Files.delete(inputFile.toPath());
                LOGGER.info("Removed input file \"{}\" after successful conversion", inputFile);
                break;
            case GIT_MV_INPUT_TO_OUTPUT:
                // first move rename output file to tmp file name
                Path tmpOutputFile = outputFile.toPath().resolveSibling(outputFile.getName() + ".tmp");
                Files.move(outputFile.toPath(), tmpOutputFile);
                LOGGER.info(
                        "Renamed output file \"{}\" to temp name \"{}\"",
                        outputFile.getCanonicalPath(),
                        tmpOutputFile.getFileName());
                // rename all input files to have the proper extension (must be individually committed)
                executeCommand("git", "mv", inputFile.getCanonicalPath(), outputFile.getCanonicalPath());
                LOGGER.info(
                        "Moved input file \"{}\" to output file \"{}\" (keeping the old content)",
                        inputFile.getCanonicalPath(),
                        outputFile.getCanonicalPath());
                outputRenameMap.put(tmpOutputFile, outputFile.getCanonicalFile().toPath());
                break;
            default:
                break;
        }
    }

    private void postProcessAllFiles(DoxiaFormat outputFormat) throws IOException, InterruptedException {
        if (postProcess == PostProcess.GIT_MV_INPUT_TO_OUTPUT) {
            // first commit the move operation with original contents
            executeCommand(
                    "git",
                    "commit",
                    "-m",
                    String.format("Move to match target converter format %s with doxia-converter", outputFormat));
            for (Map.Entry<Path, Path> entry : outputRenameMap.entrySet()) {
                // move back the converted file to the original output name (i.e. overwrite its old content)
                Files.move(entry.getKey(), entry.getValue(), StandardCopyOption.REPLACE_EXISTING);
                LOGGER.info("Replaced output file \"{}\" with converted file \"{}\"", entry.getValue(), entry.getKey());
            }
        }
    }

    static void executeCommand(String... commandAndArgs) throws IOException, InterruptedException {
        Process process = Runtime.getRuntime().exec(commandAndArgs);
        int exitCode = process.waitFor();
        if (exitCode != 0) {
            logOutput(process.getInputStream(), "");
            logOutput(process.getErrorStream(), "Error: ");
            throw new IOException("Command " + String.join(" ", commandAndArgs) + " failed with exit code " + exitCode);
        }
    }

    static void logOutput(InputStream inputStream, String prefix) throws InterruptedException {
        Thread t = new Thread(() -> {
            Scanner scanner = new Scanner(inputStream, "UTF-8");
            while (scanner.hasNextLine()) {
                LOGGER.error("{}{}", prefix, scanner.nextLine());
            }
            scanner.close();
        });
        t.start();
        t.join();
    }

    /** {@inheritDoc} */
    @Override
    public void convert(InputReaderWrapper input, OutputStreamWrapper output)
            throws UnsupportedFormatException, ConverterException {
        Objects.requireNonNull(input, "input is required");
        Objects.requireNonNull(output, "output is required");

        Parser parser = input.getFormat().getParser(parsers, MacroFormatter.forFormat(output.getFormat()));
        LOGGER.debug("Parser used: {}", parser.getClass().getName());

        SinkFactory sinkFactory = output.getFormat().getSinkFactory(sinkFactories);

        Sink sink;
        try {
            sink = sinkFactory.createSink(output.getOutputStream(), output.getEncoding());
        } catch (IOException e) {
            throw new ConverterException("IOException: " + e.getMessage(), e);
        }
        try (Sink s = sink) {
            LOGGER.debug("Sink used: {}", sink.getClass().getName());
            parse(parser, input.getReader(), s);
        }
    }

    /** {@inheritDoc} */
    @Override
    public void setFormatOutput(boolean formatOutput) {
        this.formatOutput = formatOutput;
    }

    @Override
    public void setPostProcess(PostProcess postProcess) {
        this.postProcess = postProcess;
    }

    // ----------------------------------------------------------------------
    // Private methods
    // ----------------------------------------------------------------------

    /**
     * @param inputFile a not null existing file.
     * @param inputEncoding a not null supported encoding or {@link InputFileWrapper#AUTO_ENCODING}
     * @param parserFormat  a not null supported format or {@link InputFileWrapper#AUTO_FORMAT}
     * @param output not null OutputFileWrapper object
     * @throws ConverterException if any
     * @throws UnsupportedFormatException if any
     */
    private void convert(File inputFile, String inputEncoding, DoxiaFormat parserFormat, OutputFileWrapper output)
            throws ConverterException, UnsupportedFormatException {
        convert(inputFile, inputEncoding, parserFormat, output, null);
    }

    /**
     * @param inputFile a not null existing file.
     * @param inputEncoding a not null supported encoding or {@link InputFileWrapper#AUTO_ENCODING}
     * @param parserFormat  a not null supported format
     * @param output not null OutputFileWrapper object
     * @param relativeOutputDirectory the relative output directory (may be null, created if it does not exist yet)
     * @return the output file
     * @throws ConverterException if any
     * @throws UnsupportedFormatException if any
     */
    private File convert(
            File inputFile,
            String inputEncoding,
            DoxiaFormat parserFormat,
            OutputFileWrapper output,
            File relativeOutputDirectory)
            throws ConverterException, UnsupportedFormatException {
        File outputDirectoryOrFile = relativeOutputDirectory != null
                ? new File(output.getFile(), relativeOutputDirectory.getPath())
                : output.getFile();
        LOGGER.debug(
                "Parsing file from '{}' with the encoding '{}' to '{}' with the encoding '{}'",
                inputFile.getAbsolutePath(),
                inputEncoding,
                outputDirectoryOrFile.getAbsolutePath(),
                output.getEncoding());

        if (InputFileWrapper.AUTO_ENCODING.equals(inputEncoding)) {
            inputEncoding = autoDetectEncoding(inputFile);
            LOGGER.debug("Auto detected encoding: '{}'", inputEncoding);
        }

        boolean isVelocityTemplate = inputFile.getName().endsWith(VELOCITY_TEMPLATE_EXTENSION);
        Parser parser = parserFormat.getParser(parsers, MacroFormatter.forFormat(output.getFormat()));

        File outputFile;
        if (outputDirectoryOrFile.isDirectory()
                || !SelectorUtils.match("**.*", output.getFile().getName())
                || relativeOutputDirectory != null) {
            // assume it is a directory
            outputDirectoryOrFile.mkdirs();
            final String outputFileName;
            if (isVelocityTemplate) {
                outputFileName = FileUtils.removeExtension(inputFile
                                .getName()
                                .substring(0, inputFile.getName().length() - VELOCITY_TEMPLATE_EXTENSION.length()))
                        + "."
                        + output.getFormat().getExtension()
                        + VELOCITY_TEMPLATE_EXTENSION;
            } else {
                outputFileName = FileUtils.removeExtension(inputFile.getName()) + "."
                        + output.getFormat().getExtension();
            }
            outputFile = new File(outputDirectoryOrFile, outputFileName);
        } else {
            outputDirectoryOrFile.getParentFile().mkdirs();
            outputFile = output.getFile();
        }

        Reader reader;
        try {
            if (inputEncoding != null) {
                if (parser.getType() == Parser.XML_TYPE) {
                    reader = XmlStreamReader.builder().setFile(inputFile).get();
                } else {
                    reader = new InputStreamReader(Files.newInputStream(inputFile.toPath()), inputEncoding);
                }
            } else {
                reader = Files.newBufferedReader(inputFile.toPath());
            }
        } catch (IOException e) {
            throw new ConverterException("IOException: " + e.getMessage(), e);
        }

        // a *.vm source is only valid markup after Velocity has run, so hide the Velocity constructs
        // from the parser and put them back into the converted document afterwards
        final VelocityMasker velocityMasker;
        if (isVelocityTemplate) {
            velocityMasker = new VelocityMasker();
            try (Reader r = reader) {
                reader = new StringReader(velocityMasker.mask(IOUtils.toString(r)));
            } catch (IOException e) {
                throw new ConverterException("IOException: " + e.getMessage(), e);
            }
        } else {
            velocityMasker = null;
        }

        SinkFactory sinkFactory = output.getFormat().getSinkFactory(sinkFactories);

        final String outputEncoding;
        if (StringUtils.isEmpty(output.getEncoding()) || output.getEncoding().equals(OutputFileWrapper.AUTO_ENCODING)) {
            outputEncoding = inputEncoding;
        } else {
            outputEncoding = output.getEncoding();
        }

        Sink sink;
        try {
            OutputStream out = new FileOutputStream(outputFile);
            sink = sinkFactory.createSink(out, outputEncoding);
        } catch (IOException e) {
            throw new ConverterException("IOException: " + e.getMessage(), e);
        }

        LOGGER.debug("Sink used: {}", sink.getClass().getName());
        try (Sink s = sink) {
            parse(parser, reader, s);
        } catch (Exception e) {
            throw new ConverterException(
                    "Error converting file \"" + inputFile.getAbsolutePath() + "\": " + e.getMessage(), e);
        }
        if (velocityMasker != null) {
            restoreVelocityConstructs(velocityMasker, outputFile, outputEncoding);
        }
        if (formatOutput && output.getFormat().isXml()) {
            try (Reader r = XmlStreamReader.builder().setFile(outputFile).get();
                    Writer w = XmlStreamWriter.builder().setFile(outputFile).get()) {
                CharArrayWriter caw = new CharArrayWriter();
                XmlUtil.prettyFormat(r, caw);
                w.write(caw.toString());
            } catch (IOException e) {
                throw new ConverterException("IOException: " + e.getMessage(), e);
            }
        }
        LOGGER.info(
                "Successfully converted file \"{}\" to \"{}\"",
                inputFile.getAbsolutePath(),
                outputFile.getAbsolutePath());
        try {
            postProcessFile(inputFile, outputFile);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new ConverterException("Error post processing files: " + e.getMessage(), e);
        } catch (IOException e) {
            throw new ConverterException("Error post processing files: " + e.getMessage(), e);
        }
        return outputFile;
    }

    /**
     * Substitutes the Velocity constructs taken out of the source back into the converted document
     * and reports the two cases the substitution cannot make good by itself.
     *
     * @param velocityMasker the masker holding the constructs taken out of the source
     * @param outputFile the converted document
     * @param outputEncoding the encoding the document was written with
     * @throws ConverterException if the document cannot be read back or rewritten
     */
    private void restoreVelocityConstructs(VelocityMasker velocityMasker, File outputFile, String outputEncoding)
            throws ConverterException {
        Charset charset = Charset.forName(outputEncoding);
        String converted;
        try {
            converted = velocityMasker.unmask(new String(Files.readAllBytes(outputFile.toPath()), charset));
            Files.write(outputFile.toPath(), converted.getBytes(charset));
        } catch (IOException e) {
            throw new ConverterException("IOException: " + e.getMessage(), e);
        }
        for (String reference : velocityMasker.findNewReferences(converted)) {
            LOGGER.warn(
                    "\"{}\" was written literally in the source but is a live Velocity reference in \"{}\", "
                            + "so escape it there",
                    reference,
                    outputFile.getName());
        }
        for (String directive : velocityMasker.getMaskedDirectives()) {
            LOGGER.warn(
                    "Velocity directive \"{}\" was kept but the parser treated it as ordinary content, "
                            + "so check its placement in \"{}\"",
                    directive.trim(),
                    outputFile.getName());
        }
    }

    /**
     * @param parser not null
     * @param reader not null
     * @param sink not null
     * @throws ConverterException if any
     */
    private void parse(Parser parser, Reader reader, Sink sink) throws ConverterException {
        try (Reader r = reader) {
            parser.parse(r, sink);
        } catch (ParseException e) {
            throw new ConverterException(
                    "ParseException in line " + e.getLineNumber() + ", column " + e.getColumnNumber() + ": "
                            + e.getMessage(),
                    e);
        } catch (IOException e) {
            throw new ConverterException("IOException: " + e.getMessage(), e);
        }
    }

    /**
     * @param f not null file
     * @return the detected encoding for f or <code>null</code> if not able to detect it.
     * @throws IllegalArgumentException if f is not a file.
     * @throws UnsupportedOperationException if could not detect the file encoding.
     * @see XmlStreamReader#getEncoding() for xml files
     * @see CharsetDetector#detect() for text files
     */
    static String autoDetectEncoding(File f) {
        if (!f.isFile()) {
            throw new IllegalArgumentException(
                    "The file '" + f.getAbsolutePath() + "' is not a file, could not detect encoding.");
        }
        try {
            if (XmlUtil.isXml(f)) {
                try (XmlStreamReader reader =
                        XmlStreamReader.builder().setFile(f).get()) {
                    return reader.getEncoding();
                }
            }

            try (InputStream is = new BufferedInputStream(new FileInputStream(f))) {
                CharsetDetector detector = new CharsetDetector();
                detector.setText(is);
                CharsetMatch match = detector.detect();

                return match.getName().toUpperCase(Locale.ENGLISH);
            }
        } catch (IOException e) {
            // nop
        }
        throw new UnsupportedOperationException(format(
                "Could not detect the encoding for file: %s\n" + "Specify explicitly the encoding.",
                f.getAbsolutePath()));
    }

    /**
     * @param f not null
     * @param format could be null
     * @return <code>true</code> if the file extension matches
     */
    private static boolean hasFileExtensionIgnoreCase(String name, String extension) {
        Objects.requireNonNull(name, "name is required.");

        return extension.equals(FileUtils.getExtension(name.toLowerCase(Locale.ENGLISH)));
    }

    /**
     * @param xmlFile not null and should be a file.
     * @return the first tag name if found, <code>null</code> in other case.
     */
    private static String getFirstTag(File xmlFile) {
        if (xmlFile == null) {
            throw new IllegalArgumentException("xmlFile is required.");
        }
        if (!xmlFile.isFile()) {
            throw new IllegalArgumentException("The file '" + xmlFile.getAbsolutePath() + "' is not a file.");
        }

        try (Reader reader = XmlStreamReader.builder().setFile(xmlFile).get()) {
            XmlPullParser parser = new MXParser();
            parser.setInput(reader);
            int eventType = parser.getEventType();
            while (eventType != XmlPullParser.END_DOCUMENT) {
                if (eventType == XmlPullParser.START_TAG) {
                    return parser.getName();
                }
                eventType = parser.nextToken();
            }
        } catch (IOException | XmlPullParserException e) {
            return null;
        }

        return null;
    }
}

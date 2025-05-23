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

import javax.inject.Named;

import java.io.BufferedInputStream;
import java.io.CharArrayWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Reader;
import java.io.Writer;
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
import org.apache.commons.io.input.XmlStreamReader;
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
import org.codehaus.plexus.ContainerConfiguration;
import org.codehaus.plexus.DefaultContainerConfiguration;
import org.codehaus.plexus.DefaultPlexusContainer;
import org.codehaus.plexus.PlexusConstants;
import org.codehaus.plexus.PlexusContainer;
import org.codehaus.plexus.PlexusContainerException;
import org.codehaus.plexus.component.repository.exception.ComponentLookupException;
import org.codehaus.plexus.util.FileUtils;
import org.codehaus.plexus.util.PathTool;
import org.codehaus.plexus.util.ReaderFactory;
import org.codehaus.plexus.util.SelectorUtils;
import org.codehaus.plexus.util.WriterFactory;
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
        MARKDOWN("markdown", "md", false, true);

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
         * @param plexus not null
         * @param macroFormatter a formatter for macros in the target format
         * @return an instance of <code>Parser</code> depending on the format which converts macros with the given {@link MacroFormatter}
         * @throws ComponentLookupException if could not find the Parser for the given format.
         * @throws IllegalArgumentException if any parameter is null
         */
        public Parser getParser(PlexusContainer plexus, MacroFormatter macroFormatter) throws ComponentLookupException {
            if (!hasParser) {
                throw new IllegalStateException("The format " + this + " is not supported as parser!");
            }
            Objects.requireNonNull(plexus, "plexus is required");
            Parser parser = plexus.lookup(Parser.class, roleHint);
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
         * @param plexus not null
         * @return an instance of <code>SinkFactory</code> depending on the given format.
         * @throws ComponentLookupException if could not find the SinkFactory for the given format.
         * @throws IllegalArgumentException if any parameter is null
         */
        public SinkFactory getSinkFactory(PlexusContainer plexus) throws ComponentLookupException {
            if (!hasSink) {
                throw new IllegalStateException("The format " + this + " is not supported as sink!");
            }
            Objects.requireNonNull(plexus, "plexus is required");

            return plexus.lookup(SinkFactory.class, roleHint);
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

    /** Plexus container */
    private PlexusContainer plexus;

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

        try {
            startPlexusContainer();
        } catch (PlexusContainerException e) {
            throw new ConverterException("PlexusContainerException: " + e.getMessage(), e);
        }
        outputRenameMap.clear();
        try {
            if (input.getFile().isFile()) {
                convert(input.getFile(), input.getEncoding(), input.getFormat(), output);
            } else {
                List<File> files;
                try {
                    files = FileUtils.getFiles(
                            input.getFile(),
                            "**/*." + input.getFormat().getExtension(),
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
                    File relativeOutputDirectory = new File(
                            PathTool.getRelativeFilePath(input.getFile().getAbsolutePath(), f.getParent()));
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
        } finally {
            stopPlexusContainer();
        }
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

        try {
            startPlexusContainer();
        } catch (PlexusContainerException e) {
            throw new ConverterException("PlexusContainerException: " + e.getMessage(), e);
        }

        try {
            Parser parser;
            try {
                parser = input.getFormat().getParser(plexus, MacroFormatter.forFormat(output.getFormat()));
            } catch (ComponentLookupException e) {
                throw new ConverterException("ComponentLookupException: " + e.getMessage(), e);
            }
            LOGGER.debug("Parser used: {}", parser.getClass().getName());

            SinkFactory sinkFactory;
            try {
                sinkFactory = output.getFormat().getSinkFactory(plexus);
            } catch (ComponentLookupException e) {
                throw new ConverterException("ComponentLookupException: " + e.getMessage(), e);
            }

            Sink sink;
            try {
                sink = sinkFactory.createSink(output.getOutputStream(), output.getEncoding());
            } catch (IOException e) {
                throw new ConverterException("IOException: " + e.getMessage(), e);
            }
            LOGGER.debug("Sink used: {}", sink.getClass().getName());

            parse(parser, input.getReader(), sink);
        } finally {
            stopPlexusContainer();
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

        Parser parser;
        try {
            parser = parserFormat.getParser(plexus, MacroFormatter.forFormat(output.getFormat()));
        } catch (ComponentLookupException e) {
            throw new ConverterException("ComponentLookupException: " + e.getMessage(), e);
        }

        File outputFile;
        if (outputDirectoryOrFile.isDirectory()
                || !SelectorUtils.match("**.*", output.getFile().getName())
                || relativeOutputDirectory != null) {
            // assume it is a directory
            outputDirectoryOrFile.mkdirs();
            outputFile = new File(
                    outputDirectoryOrFile,
                    FileUtils.removeExtension(inputFile.getName()) + "."
                            + output.getFormat().getExtension());
        } else {
            outputDirectoryOrFile.getParentFile().mkdirs();
            outputFile = output.getFile();
        }

        Reader reader;
        try {
            if (inputEncoding != null) {
                if (parser.getType() == Parser.XML_TYPE) {
                    reader = ReaderFactory.newXmlReader(inputFile);
                } else {
                    reader = ReaderFactory.newReader(inputFile, inputEncoding);
                }
            } else {
                reader = ReaderFactory.newPlatformReader(inputFile);
            }
        } catch (IOException e) {
            throw new ConverterException("IOException: " + e.getMessage(), e);
        }

        SinkFactory sinkFactory;
        try {
            sinkFactory = output.getFormat().getSinkFactory(plexus);
        } catch (ComponentLookupException e) {
            throw new ConverterException("ComponentLookupException: " + e.getMessage(), e);
        }

        Sink sink;
        try {
            String outputEncoding;
            if (StringUtils.isEmpty(output.getEncoding())
                    || output.getEncoding().equals(OutputFileWrapper.AUTO_ENCODING)) {
                outputEncoding = inputEncoding;
            } else {
                outputEncoding = output.getEncoding();
            }

            OutputStream out = new FileOutputStream(outputFile);
            sink = sinkFactory.createSink(out, outputEncoding);
        } catch (IOException e) {
            throw new ConverterException("IOException: " + e.getMessage(), e);
        }

        LOGGER.debug("Sink used: {}", sink.getClass().getName());
        parse(parser, reader, sink);

        if (formatOutput && output.getFormat().isXml()) {
            try (Reader r = ReaderFactory.newXmlReader(outputFile);
                    Writer w = WriterFactory.newXmlWriter(outputFile)) {
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
     * @param parser not null
     * @param reader not null
     * @param sink not null
     * @throws ConverterException if any
     */
    private void parse(Parser parser, Reader reader, Sink sink) throws ConverterException {
        try (Reader r = reader) {
            parser.parse(r, sink);
        } catch (ParseException | IOException e) {
            throw new ConverterException("ParseException: " + e.getMessage(), e);
        } finally {
            sink.flush();
            sink.close();
        }
    }

    /**
     * Start the Plexus container.
     *
     * @throws PlexusContainerException if any
     */
    private void startPlexusContainer() throws PlexusContainerException {
        if (plexus != null) {
            return;
        }

        Map<Object, Object> context = new HashMap<>();
        context.put("basedir", new File("").getAbsolutePath());

        ContainerConfiguration containerConfiguration = new DefaultContainerConfiguration();
        containerConfiguration.setName("Doxia");
        containerConfiguration.setContext(context);
        containerConfiguration.setAutoWiring(true);
        containerConfiguration.setClassPathScanning(PlexusConstants.SCANNING_ON);

        plexus = new DefaultPlexusContainer(containerConfiguration);
    }

    /**
     * Stop the Plexus container.
     */
    private void stopPlexusContainer() {
        if (plexus == null) {
            return;
        }

        plexus.dispose();
        plexus = null;
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
                try (XmlStreamReader reader = new XmlStreamReader(f)) {
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

        try (Reader reader = ReaderFactory.newXmlReader(xmlFile)) {
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

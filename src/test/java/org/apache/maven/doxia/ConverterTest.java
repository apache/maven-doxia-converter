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

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.io.Reader;
import java.io.StringReader;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import org.apache.maven.doxia.DefaultConverter.DoxiaFormat;
import org.apache.maven.doxia.wrapper.InputFileWrapper;
import org.apache.maven.doxia.wrapper.InputReaderWrapper;
import org.apache.maven.doxia.wrapper.OutputFileWrapper;
import org.apache.maven.doxia.wrapper.OutputStreamWrapper;
import org.codehaus.plexus.util.FileUtils;
import org.codehaus.plexus.util.IOUtil;
import org.codehaus.plexus.util.ReaderFactory;
import org.codehaus.plexus.util.WriterFactory;
import org.eclipse.sisu.launch.InjectedTest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertLinesMatch;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * Tests Doxia converter.
 *
 * @author <a href="mailto:vincent.siveton@gmail.com">Vincent Siveton</a>
 */
class ConverterTest extends InjectedTest {
    @Inject
    private Converter converter;

    private boolean formatOutput;

    /** {@inheritDoc} */
    @BeforeEach
    public void setUp() throws Exception {
        super.setUp();
        formatOutput = Boolean.parseBoolean(System.getProperty("format", "false"));
    }

    /** {@inheritDoc} */
    @AfterEach
    public void tearDown() throws Exception {
        super.tearDown();
        converter = null;
    }

    /**
     * Input file / output dir
     *
     * @see Converter#convert(InputFileWrapper, OutputFileWrapper)
     * @throws Exception if any
     */
    @Test
    void fileConverterWithInputFileOutputDir() throws Exception {
        String in = getBasedir() + "/src/test/resources/unit/Doxia.htm";
        String out = getBasedir() + "/target/unit/";

        InputFileWrapper input = InputFileWrapper.valueOf(in, DoxiaFormat.XHTML, ReaderFactory.UTF_8);
        OutputFileWrapper output = OutputFileWrapper.valueOf(out, DoxiaFormat.APT, WriterFactory.UTF_8);

        converter.setFormatOutput(formatOutput);
        converter.convert(input, output);
        assertTrue(new File(out, "Doxia.apt").exists());
        assertTrue(new File(out, "Doxia.apt").length() != 0);

        FileUtils.deleteDirectory(new File(getBasedir() + "/target/unit/"));
    }

    /**
     * Input file / output dir
     *
     * @see Converter#convert(InputFileWrapper, OutputFileWrapper)
     * @throws Exception if any
     */
    @Test
    void fileConverterWithInputDirOutputDir() throws Exception {
        String in = getBasedir() + "/src/test/resources/unit/apt";
        String out = getBasedir() + "/target/unit/";

        InputFileWrapper input = InputFileWrapper.valueOf(in, DoxiaFormat.APT, ReaderFactory.UTF_8);
        OutputFileWrapper output = OutputFileWrapper.valueOf(out, DoxiaFormat.MARKDOWN, WriterFactory.UTF_8);

        converter.setFormatOutput(formatOutput);
        converter.convert(input, output);
        assertTrue(new File(out, "test.md").exists());
        assertTrue(new File(out, "test.md").length() != 0);
        assertTrue(new File(out, "child/test.md").exists());
        assertTrue(new File(out, "child/test.md").length() != 0);

        FileUtils.deleteDirectory(new File(getBasedir() + "/target/unit/"));
    }

    /**
     * Input file / output file
     *
     * @see Converter#convert(InputFileWrapper, OutputFileWrapper)
     * @throws Exception if any
     */
    @Test
    void fileConverterWithInputFileOutputFile() throws Exception {
        String in = getBasedir() + "/src/test/resources/unit/Doxia.htm";
        String out = getBasedir() + "/target/unit/Doxia.apt";

        InputFileWrapper input = InputFileWrapper.valueOf(in, DoxiaFormat.XHTML, ReaderFactory.UTF_8);
        OutputFileWrapper output = OutputFileWrapper.valueOf(out, DoxiaFormat.APT, WriterFactory.UTF_8);

        converter.setFormatOutput(formatOutput);
        converter.convert(input, output);
        assertTrue(new File(out).exists());
        assertTrue(new File(out).length() != 0);

        FileUtils.deleteDirectory(new File(getBasedir() + "/target/unit/"));
    }

    /**
     * Input apt file / output file
     *
     * @see Converter#convert(InputFileWrapper, OutputFileWrapper)
     * @throws Exception if any
     */
    @Test
    void aptFileConverter() throws Exception {
        String in = getBasedir() + "/src/test/resources/unit/apt/test.apt";
        String out = getBasedir() + "/target/unit/file/apt/test.apt.xhtml";

        InputFileWrapper input = InputFileWrapper.valueOf(in, DoxiaFormat.APT, ReaderFactory.UTF_8);
        OutputFileWrapper output = OutputFileWrapper.valueOf(out, DoxiaFormat.XHTML, WriterFactory.UTF_8);

        converter.setFormatOutput(formatOutput);
        converter.convert(input, output);
        assertTrue(new File(out).exists());
        assertTrue(new File(out).length() != 0);

        in = getBasedir() + "/target/unit/file/apt/test.apt.xhtml";
        out = getBasedir() + "/target/unit/file/apt/test.apt";

        input = InputFileWrapper.valueOf(in, DoxiaFormat.XHTML, ReaderFactory.UTF_8);
        output = OutputFileWrapper.valueOf(out, DoxiaFormat.APT, WriterFactory.UTF_8);

        converter.setFormatOutput(formatOutput);
        converter.convert(input, output);
        assertTrue(new File(out).exists());
        assertTrue(new File(out).length() != 0);
    }

    /**
     * Input fml dir / output dir
     *
     * @see Converter#convert(InputFileWrapper, OutputFileWrapper)
     * @throws Exception if any
     */
    @Test
    void fmlFileConverter() throws Exception {
        String in = getBasedir() + "/src/test/resources/unit/fml/test.fml";
        String out = getBasedir() + "/target/unit/file/fml/test.fml.xhtml";

        InputFileWrapper input = InputFileWrapper.valueOf(in, DoxiaFormat.FML, ReaderFactory.UTF_8);
        OutputFileWrapper output = OutputFileWrapper.valueOf(out, DoxiaFormat.XHTML, WriterFactory.UTF_8);

        converter.setFormatOutput(formatOutput);
        converter.convert(input, output);
        assertTrue(new File(out).exists());
        assertTrue(new File(out).length() != 0);

        // opposite conversion not supported
    }

    /**
     * Input xdoc file / output dir
     *
     * @see Converter#convert(InputFileWrapper, OutputFileWrapper)
     * @throws Exception if any
     */
    @Test
    void xdocFileConverter() throws Exception {
        String in = getBasedir() + "/src/test/resources/unit/xdoc/test.xml";
        String out = getBasedir() + "/target/unit/file/xdoc/test.xdoc.xhtml";

        InputFileWrapper input = InputFileWrapper.valueOf(in, DoxiaFormat.XDOC, ReaderFactory.UTF_8);
        OutputFileWrapper output = OutputFileWrapper.valueOf(out, DoxiaFormat.XHTML, WriterFactory.UTF_8);

        converter.setFormatOutput(formatOutput);
        converter.convert(input, output);
        assertTrue(new File(out).exists());
        assertTrue(new File(out).length() != 0);

        in = getBasedir() + "/target/unit/file/xdoc/test.xdoc.xhtml";
        out = getBasedir() + "/target/unit/file/xdoc/test.xdoc";

        input = InputFileWrapper.valueOf(in, DoxiaFormat.XHTML, ReaderFactory.UTF_8);
        output = OutputFileWrapper.valueOf(out, DoxiaFormat.XDOC, WriterFactory.UTF_8);

        converter.setFormatOutput(formatOutput);
        converter.convert(input, output);
        assertTrue(new File(out).exists());
        assertTrue(new File(out).length() != 0);
    }

    /**
     * Input xhtml file / output dir
     *
     * @see Converter#convert(InputFileWrapper, OutputFileWrapper)
     * @throws Exception if any
     */
    @Test
    void xhtmlFileConverter() throws Exception {
        String in = getBasedir() + "/src/test/resources/unit/xhtml/test.xhtml";
        String out = getBasedir() + "/target/unit/file/xhtml/test.xhtml.xhtml";

        InputFileWrapper input = InputFileWrapper.valueOf(in, DoxiaFormat.XHTML, ReaderFactory.UTF_8);
        OutputFileWrapper output = OutputFileWrapper.valueOf(out, DoxiaFormat.XHTML, WriterFactory.UTF_8);

        converter.setFormatOutput(formatOutput);
        converter.convert(input, output);
        assertTrue(new File(out).exists());
        assertTrue(new File(out).length() != 0);

        in = getBasedir() + "/target/unit/file/xhtml/test.xhtml.xhtml";
        out = getBasedir() + "/target/unit/file/xhtml/test.xhtml";

        input = InputFileWrapper.valueOf(in, DoxiaFormat.XHTML, ReaderFactory.UTF_8);
        output = OutputFileWrapper.valueOf(out, DoxiaFormat.XHTML, WriterFactory.UTF_8);

        converter.setFormatOutput(formatOutput);
        converter.convert(input, output);
        assertTrue(new File(out).exists());
        assertTrue(new File(out).length() != 0);
    }

    /**
     * Input apt reader / output writer
     *
     * @see Converter#convert(InputReaderWrapper, OutputStreamWrapper)
     * @throws Exception if any
     */
    @Test
    void aptWriterConverter() throws Exception {
        String in = getBasedir() + "/src/test/resources/unit/apt/test.apt";
        String out = getBasedir() + "/target/unit/writer/apt/test.apt.xhtml";

        File inFile = new File(in);
        File outFile = new File(out);
        outFile.getParentFile().mkdirs();

        try (OutputStream fo = new FileOutputStream(outFile)) {
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

            InputReaderWrapper input = InputReaderWrapper.valueOf(new FileReader(inFile), DoxiaFormat.APT);
            OutputStreamWrapper output = OutputStreamWrapper.valueOf(outputStream, DoxiaFormat.XHTML, "UTF-8");

            converter.setFormatOutput(formatOutput);
            converter.convert(input, output);

            IOUtil.copy(outputStream.toByteArray(), fo);
        }

        assertTrue(outFile.exists());
        assertTrue(outFile.length() != 0);
    }

    /**
     * Input xdoc (autodetect) reader / output writer
     *
     * @see Converter#convert(InputReaderWrapper, OutputStreamWrapper)
     * @throws Exception if any
     */
    @Test
    void autoDetectConverter() throws Exception {
        String in = getBasedir() + "/src/test/resources/unit/xdoc/test.xml";
        String out = getBasedir() + "/target/unit/writer/apt/test.xdoc.apt";

        File inFile = new File(in);
        File outFile = new File(out);
        outFile.getParentFile().mkdirs();

        try (FileWriter fw = new FileWriter(outFile)) {
            StringWriter writer = new StringWriter();

            InputFileWrapper input =
                    InputFileWrapper.valueOf(inFile.getAbsolutePath(), DoxiaFormat.autoDetectFormat(inFile));
            OutputFileWrapper output = OutputFileWrapper.valueOf(outFile.getAbsolutePath(), DoxiaFormat.XHTML);

            converter.setFormatOutput(formatOutput);
            converter.convert(input, output);

            IOUtil.copy(writer.toString(), fw);

            assertTrue(outFile.exists());
            assertTrue(outFile.length() != 0);
        }

        in = getBasedir() + "/src/test/resources/unit/apt/test.apt";
        out = getBasedir() + "/target/unit/writer/apt/test.apt.xhtml";

        inFile = new File(in);
        outFile = new File(out);
        outFile.getParentFile().mkdirs();

        try (FileWriter fw = new FileWriter(outFile)) {
            StringWriter writer = new StringWriter();

            InputFileWrapper input =
                    InputFileWrapper.valueOf(inFile.getAbsolutePath(), DoxiaFormat.autoDetectFormat(inFile));
            OutputFileWrapper output = OutputFileWrapper.valueOf(outFile.getAbsolutePath(), DoxiaFormat.XHTML);

            converter.setFormatOutput(formatOutput);
            converter.convert(input, output);

            IOUtil.copy(writer.toString(), fw);

            assertTrue(outFile.exists());
            assertTrue(outFile.length() != 0);
        }

        in = getBasedir() + "/src/test/resources/unit/apt/test.unknown";
        out = getBasedir() + "/target/unit/writer/apt/test.apt.xhtml";

        inFile = new File(in);
        outFile = new File(out);
        outFile.getParentFile().mkdirs();

        try (FileWriter fw = new FileWriter(outFile)) {
            InputFileWrapper input =
                    InputFileWrapper.valueOf(inFile.getAbsolutePath(), DoxiaFormat.autoDetectFormat(inFile));
            OutputFileWrapper output = OutputFileWrapper.valueOf(outFile.getAbsolutePath(), DoxiaFormat.XHTML);

            converter.setFormatOutput(formatOutput);
            converter.convert(input, output);

            fail();
        } catch (UnsupportedOperationException e) {
            assertTrue(true);
        }

        in = getBasedir() + "/src/test/resources/unit/xhtml/test.xhtml";
        out = getBasedir() + "/target/unit/writer/xhtml/test.html5.xhtml";

        inFile = new File(in);
        outFile = new File(out);
        outFile.getParentFile().mkdirs();

        try (FileWriter fw = new FileWriter(outFile)) {
            StringWriter writer = new StringWriter();

            InputFileWrapper input =
                    InputFileWrapper.valueOf(inFile.getAbsolutePath(), DoxiaFormat.autoDetectFormat(inFile));
            OutputFileWrapper output = OutputFileWrapper.valueOf(outFile.getAbsolutePath(), DoxiaFormat.XHTML);

            converter.setFormatOutput(formatOutput);
            converter.convert(input, output);

            IOUtil.copy(writer.toString(), fw);

            assertTrue(outFile.exists());
            assertTrue(outFile.length() != 0);
        }
    }

    private String autoDetectEncoding(File f) {
        return DefaultConverter.autoDetectEncoding(f);
    }

    private String autoDetectEncoding(String filename) {
        return autoDetectEncoding(new File(getBasedir() + "/src/test/resources/unit/" + filename));
    }

    /**
     * Test {@link DefaultConverter#autoDetectEncoding(File)}
     */
    @Test
    void autodetectEncoding() {
        assertEquals("ISO-8859-1", autoDetectEncoding("apt/test.apt"));
        assertEquals("UTF-8", autoDetectEncoding("fml/test.fml")); // plexus-utils should detect ISO-8859-1
        assertEquals("UTF-8", autoDetectEncoding("xhtml/test.xhtml"));
    }

    private DoxiaFormat autoDetectFormat(File f) {
        return DefaultConverter.DoxiaFormat.autoDetectFormat(f);
    }

    private DoxiaFormat autoDetectFormat(String filename) {
        return autoDetectFormat(new File(getBasedir() + "/src/test/resources/unit/" + filename));
    }

    /**
     * Test {@link DefaultConverter.DoxiaFormat#autoDetectFormat(File)}
     *
     */
    @Test
    void autodetectFormat() {
        assertEquals(DoxiaFormat.APT, autoDetectFormat("apt/test.apt"));

        try {
            autoDetectFormat("apt/test.unknown");
            fail();
        } catch (UnsupportedOperationException e) {
            assertTrue(true);
        }

        assertEquals(DoxiaFormat.FML, autoDetectFormat("fml/test.fml"));
        assertEquals(DoxiaFormat.XHTML, autoDetectFormat("xhtml/test.xhtml"));
    }

    @Test
    void testAptToMarkdownWithMacro() throws IOException, UnsupportedFormatException, ConverterException {
        Path in = new File(getBasedir() + "/src/test/resources/unit/apt/macro.apt").toPath();
        Path expectedOut = new File(getBasedir() + "//src/test/resources/unit/markdown/macro.md").toPath();

        try (ByteArrayOutputStream output = new ByteArrayOutputStream();
                Reader reader = Files.newBufferedReader(in, StandardCharsets.UTF_8); ) {
            converter.convert(
                    InputReaderWrapper.valueOf(reader, DoxiaFormat.APT),
                    OutputStreamWrapper.valueOf(output, DoxiaFormat.MARKDOWN, StandardCharsets.UTF_8.name()));

            try (BufferedReader outputReader =
                    new BufferedReader(new StringReader(new String(output.toByteArray(), StandardCharsets.UTF_8))); ) {
                assertLinesMatch(Files.readAllLines(expectedOut).stream(), outputReader.lines());
            }
        }
    }
}

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

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.OutputStream;
import java.io.StringWriter;

import org.apache.maven.doxia.DefaultConverter.DoxiaFormat;
import org.apache.maven.doxia.wrapper.InputFileWrapper;
import org.apache.maven.doxia.wrapper.InputReaderWrapper;
import org.apache.maven.doxia.wrapper.OutputFileWrapper;
import org.apache.maven.doxia.wrapper.OutputStreamWrapper;
import org.codehaus.plexus.PlexusTestCase;
import org.codehaus.plexus.util.FileUtils;
import org.codehaus.plexus.util.IOUtil;
import org.codehaus.plexus.util.ReaderFactory;
import org.codehaus.plexus.util.WriterFactory;

/**
 * Tests Doxia converter.
 *
 * @author <a href="mailto:vincent.siveton@gmail.com">Vincent Siveton</a>
 */
public class ConverterTest extends PlexusTestCase {
    private Converter converter;

    private boolean formatOutput;

    /** {@inheritDoc} */
    @Override
    protected void setUp() throws Exception {
        super.setUp();

        converter = new DefaultConverter();

        formatOutput = Boolean.parseBoolean(System.getProperty("format", "false"));
    }

    /** {@inheritDoc} */
    @Override
    protected void tearDown() throws Exception {
        super.tearDown();

        converter = null;
    }

    /**
     * Input file / output dir
     *
     * @see Converter#convert(InputFileWrapper, OutputFileWrapper)
     * @throws Exception if any
     */
    public void testFileConverterWithInputFileOutputDir() throws Exception {
        String in = getBasedir() + "/src/test/resources/unit/Doxia.htm";
        String out = getBasedir() + "/target/unit/";

        InputFileWrapper input = InputFileWrapper.valueOf(in, DoxiaFormat.XHTML5, ReaderFactory.UTF_8);
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
    public void testFileConverterWithInputDirOutputDir() throws Exception {
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
    public void testFileConverterWithInputFileOutputFile() throws Exception {
        String in = getBasedir() + "/src/test/resources/unit/Doxia.htm";
        String out = getBasedir() + "/target/unit/Doxia.apt";

        InputFileWrapper input = InputFileWrapper.valueOf(in, DoxiaFormat.XHTML5, ReaderFactory.UTF_8);
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
    public void testAptFileConverter() throws Exception {
        String in = getBasedir() + "/src/test/resources/unit/apt/test.apt";
        String out = getBasedir() + "/target/unit/file/apt/test.apt.xhtml";

        InputFileWrapper input = InputFileWrapper.valueOf(in, DoxiaFormat.APT, ReaderFactory.UTF_8);
        OutputFileWrapper output = OutputFileWrapper.valueOf(out, DoxiaFormat.XHTML5, WriterFactory.UTF_8);

        converter.setFormatOutput(formatOutput);
        converter.convert(input, output);
        assertTrue(new File(out).exists());
        assertTrue(new File(out).length() != 0);

        in = getBasedir() + "/target/unit/file/apt/test.apt.xhtml";
        out = getBasedir() + "/target/unit/file/apt/test.apt";

        input = InputFileWrapper.valueOf(in, DoxiaFormat.XHTML5, ReaderFactory.UTF_8);
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
    public void testFmlFileConverter() throws Exception {
        String in = getBasedir() + "/src/test/resources/unit/fml/test.fml";
        String out = getBasedir() + "/target/unit/file/fml/test.fml.xhtml";

        InputFileWrapper input = InputFileWrapper.valueOf(in, DoxiaFormat.FML, ReaderFactory.UTF_8);
        OutputFileWrapper output = OutputFileWrapper.valueOf(out, DoxiaFormat.XHTML5, WriterFactory.UTF_8);

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
    public void testXdocFileConverter() throws Exception {
        String in = getBasedir() + "/src/test/resources/unit/xdoc/test.xml";
        String out = getBasedir() + "/target/unit/file/xdoc/test.xdoc.xhtml";

        InputFileWrapper input = InputFileWrapper.valueOf(in, DoxiaFormat.XDOC, ReaderFactory.UTF_8);
        OutputFileWrapper output = OutputFileWrapper.valueOf(out, DoxiaFormat.XHTML5, WriterFactory.UTF_8);

        converter.setFormatOutput(formatOutput);
        converter.convert(input, output);
        assertTrue(new File(out).exists());
        assertTrue(new File(out).length() != 0);

        in = getBasedir() + "/target/unit/file/xdoc/test.xdoc.xhtml";
        out = getBasedir() + "/target/unit/file/xdoc/test.xdoc";

        input = InputFileWrapper.valueOf(in, DoxiaFormat.XHTML5, ReaderFactory.UTF_8);
        output = OutputFileWrapper.valueOf(out, DoxiaFormat.XDOC, WriterFactory.UTF_8);

        converter.setFormatOutput(formatOutput);
        converter.convert(input, output);
        assertTrue(new File(out).exists());
        assertTrue(new File(out).length() != 0);
    }

    /**
     * Input xhtml5 file / output dir
     *
     * @see Converter#convert(InputFileWrapper, OutputFileWrapper)
     * @throws Exception if any
     */
    public void testXhtml5FileConverter() throws Exception {
        String in = getBasedir() + "/src/test/resources/unit/xhtml/test.xhtml5";
        String out = getBasedir() + "/target/unit/file/xhtml/test.xhtml.xhtml5";

        InputFileWrapper input = InputFileWrapper.valueOf(in, DoxiaFormat.XHTML5, ReaderFactory.UTF_8);
        OutputFileWrapper output = OutputFileWrapper.valueOf(out, DoxiaFormat.XHTML5, WriterFactory.UTF_8);

        // output has missing section end tag (3 opening, but only 2 closing ones)
        converter.setFormatOutput(formatOutput);
        converter.convert(input, output);
        assertTrue(new File(out).exists());
        assertTrue(new File(out).length() != 0);

        in = getBasedir() + "/target/unit/file/xhtml/test.xhtml.xhtml5";
        out = getBasedir() + "/target/unit/file/xhtml/test.xhtml5";

        input = InputFileWrapper.valueOf(in, DoxiaFormat.XHTML5, ReaderFactory.UTF_8);
        output = OutputFileWrapper.valueOf(out, DoxiaFormat.XHTML5, WriterFactory.UTF_8);

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
    public void testAptWriterConverter() throws Exception {
        String in = getBasedir() + "/src/test/resources/unit/apt/test.apt";
        String from = "apt";
        String out = getBasedir() + "/target/unit/writer/apt/test.apt.xhtml";
        String to = "xhtml5";

        File inFile = new File(in);
        File outFile = new File(out);
        outFile.getParentFile().mkdirs();

        try (OutputStream fo = new FileOutputStream(outFile)) {
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

            InputReaderWrapper input = InputReaderWrapper.valueOf(new FileReader(inFile), from);
            OutputStreamWrapper output = OutputStreamWrapper.valueOf(outputStream, to, "UTF-8");

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
    public void testAutoDetectConverter() throws Exception {
        String in = getBasedir() + "/src/test/resources/unit/xdoc/test.xml";
        String out = getBasedir() + "/target/unit/writer/apt/test.xdoc.apt";

        File inFile = new File(in);
        File outFile = new File(out);
        outFile.getParentFile().mkdirs();

        try (FileWriter fw = new FileWriter(outFile)) {
            StringWriter writer = new StringWriter();

            InputFileWrapper input =
                    InputFileWrapper.valueOf(inFile.getAbsolutePath(), DoxiaFormat.autoDetectFormat(inFile));
            OutputFileWrapper output = OutputFileWrapper.valueOf(outFile.getAbsolutePath(), DoxiaFormat.XHTML5);

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
            OutputFileWrapper output = OutputFileWrapper.valueOf(outFile.getAbsolutePath(), DoxiaFormat.XHTML5);

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
            OutputFileWrapper output = OutputFileWrapper.valueOf(outFile.getAbsolutePath(), DoxiaFormat.XHTML5);

            converter.setFormatOutput(formatOutput);
            converter.convert(input, output);

            fail();
        } catch (UnsupportedOperationException e) {
            assertTrue(true);
        }

        in = getBasedir() + "/src/test/resources/unit/xhtml/test.xhtml5";
        out = getBasedir() + "/target/unit/writer/xhtml/test.html5.xhtml5";

        inFile = new File(in);
        outFile = new File(out);
        outFile.getParentFile().mkdirs();

        try (FileWriter fw = new FileWriter(outFile)) {
            StringWriter writer = new StringWriter();

            InputFileWrapper input =
                    InputFileWrapper.valueOf(inFile.getAbsolutePath(), DoxiaFormat.autoDetectFormat(inFile));
            OutputFileWrapper output = OutputFileWrapper.valueOf(outFile.getAbsolutePath(), DoxiaFormat.XHTML5);

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
    public void testAutodetectEncoding() {
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
    public void testAutodetectFormat() {
        assertEquals(autoDetectFormat("apt/test.apt"), DoxiaFormat.APT);

        try {
            autoDetectFormat("apt/test.unknown");
            fail();
        } catch (UnsupportedOperationException e) {
            assertTrue(true);
        }

        assertEquals(autoDetectFormat("fml/test.fml"), DoxiaFormat.FML);
        assertEquals(autoDetectFormat("xhtml/test.xhtml"), DoxiaFormat.XHTML5);
    }
}

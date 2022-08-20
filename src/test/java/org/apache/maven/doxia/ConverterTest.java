package org.apache.maven.doxia;

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

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.OutputStream;
import java.io.StringWriter;

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
public class ConverterTest
    extends PlexusTestCase
{
    private Converter converter;

    private boolean formatOutput;

    /** {@inheritDoc} */
    @Override
    protected void setUp()
        throws Exception
    {
        super.setUp();

        converter = new DefaultConverter();

        formatOutput = Boolean.parseBoolean( System.getProperty( "format", "false" ) );
    }

    /** {@inheritDoc} */
    @Override
    protected void tearDown()
        throws Exception
    {
        super.tearDown();

        converter = null;
    }

    /**
     * @see Converter#getInputFormats()
     */
    public void testGetInputFormats()
    {
        assertNotNull( converter.getInputFormats() );
    }

    /**
     * @see Converter#getOutputFormats()
     */
    public void testGetOutputFormats()
    {
        assertNotNull( converter.getOutputFormats() );
    }

    /**
     * Input file / output dir
     *
     * @see Converter#convert(InputFileWrapper, OutputFileWrapper)
     * @throws Exception if any
     */
    public void testFileConverterWithInputFileOutputDir()
        throws Exception
    {
        String in = getBasedir() + "/src/test/resources/unit/Doxia.htm";
        String from = "xhtml";
        String out = getBasedir() + "/target/unit/";
        String to = "apt";

        InputFileWrapper input =
            InputFileWrapper.valueOf( in, from, ReaderFactory.UTF_8, converter.getInputFormats() );
        OutputFileWrapper output =
            OutputFileWrapper.valueOf( out, to, WriterFactory.UTF_8, converter.getOutputFormats() );

        converter.setFormatOutput( formatOutput );
        converter.convert( input, output );
        assertTrue( new File( out, "Doxia.htm.apt" ).exists() );
        assertTrue( new File( out, "Doxia.htm.apt" ).length() != 0 );

        FileUtils.deleteDirectory( new File( getBasedir() + "/target/unit/" ) );
    }

    /**
     * Input file / output file
     *
     * @see Converter#convert(InputFileWrapper, OutputFileWrapper)
     * @throws Exception if any
     */
    public void testFileConverterWithInputFileOutputFile()
        throws Exception
    {
        String in = getBasedir() + "/src/test/resources/unit/Doxia.htm";
        String from = "xhtml";
        String out = getBasedir() + "/target/unit/Doxia.apt";
        String to = "apt";

        InputFileWrapper input =
            InputFileWrapper.valueOf( in, from, ReaderFactory.UTF_8, converter.getInputFormats() );
        OutputFileWrapper output =
            OutputFileWrapper.valueOf( out, to, WriterFactory.UTF_8, converter.getOutputFormats() );

        converter.setFormatOutput( formatOutput );
        converter.convert( input, output );
        assertTrue( new File( out ).exists() );
        assertTrue( new File( out ).length() != 0 );

        FileUtils.deleteDirectory( new File( getBasedir() + "/target/unit/" ) );
    }

    /**
     * Input apt file / output file
     *
     * @see Converter#convert(InputFileWrapper, OutputFileWrapper)
     * @throws Exception if any
     */
    public void testAptFileConverter()
        throws Exception
    {
        String in = getBasedir() + "/src/test/resources/unit/apt/test.apt";
        String from = "apt";
        String out = getBasedir() + "/target/unit/file/apt/test.apt.xhtml";
        String to = "xhtml";

        InputFileWrapper input =
            InputFileWrapper.valueOf( in, from, ReaderFactory.UTF_8, converter.getInputFormats() );
        OutputFileWrapper output =
            OutputFileWrapper.valueOf( out, to, WriterFactory.UTF_8, converter.getOutputFormats() );

        converter.setFormatOutput( formatOutput );
        converter.convert( input, output );
        assertTrue( new File( out ).exists() );
        assertTrue( new File( out ).length() != 0 );

        in = getBasedir() + "/target/unit/file/apt/test.apt.xhtml";
        from = "xhtml";
        out = getBasedir() + "/target/unit/file/apt/test.apt";
        to = "apt";

        input = InputFileWrapper.valueOf( in, from, ReaderFactory.UTF_8, converter.getInputFormats() );
        output = OutputFileWrapper.valueOf( out, to, WriterFactory.UTF_8, converter.getOutputFormats() );

        converter.setFormatOutput( formatOutput );
        converter.convert( input, output );
        assertTrue( new File( out ).exists() );
        assertTrue( new File( out ).length() != 0 );
    }

    /**
     * Input fml dir / output dir
     *
     * @see Converter#convert(InputFileWrapper, OutputFileWrapper)
     * @throws Exception if any
     */
    public void testFmlFileConverter()
        throws Exception
    {
        String in = getBasedir() + "/src/test/resources/unit/fml/test.fml";
        String from = "fml";
        String out = getBasedir() + "/target/unit/file/fml/test.fml.xhtml";
        String to = "xhtml";

        InputFileWrapper input =
            InputFileWrapper.valueOf( in, from, ReaderFactory.UTF_8, converter.getInputFormats() );
        OutputFileWrapper output =
            OutputFileWrapper.valueOf( out, to, WriterFactory.UTF_8, converter.getOutputFormats() );

        converter.setFormatOutput( formatOutput );
        converter.convert( input, output );
        assertTrue( new File( out ).exists() );
        assertTrue( new File( out ).length() != 0 );

        in = getBasedir() + "/target/unit/file/fml/test.fml.xhtml";
        from = "xhtml";
        out = getBasedir() + "/target/unit/file/fml/test.fml";
        to = "fml";

        try
        {
            input = InputFileWrapper.valueOf( in, from, ReaderFactory.UTF_8, converter.getInputFormats() );
            output = OutputFileWrapper.valueOf( out, to, WriterFactory.UTF_8, converter.getOutputFormats() );

            converter.setFormatOutput( formatOutput );
            converter.convert( input, output );

            fail();
        }
        catch ( UnsupportedFormatException e )
        {
            assertTrue( true );
        }
    }

    /**
     * Input twiki file / output file
     *
     * @see Converter#convert(InputFileWrapper, OutputFileWrapper)
     * @throws Exception if any
     */
    public void testTwikiFileConverter()
        throws Exception
    {
        String in = getBasedir() + "/src/test/resources/unit/twiki/test.twiki";
        String from = "twiki";
        String out = getBasedir() + "/target/unit/file/twiki/test.twiki.xhtml";
        String to = "xhtml";

        InputFileWrapper input =
            InputFileWrapper.valueOf( in, from, ReaderFactory.UTF_8, converter.getInputFormats() );
        OutputFileWrapper output =
            OutputFileWrapper.valueOf( out, to, WriterFactory.UTF_8, converter.getOutputFormats() );

        converter.setFormatOutput( formatOutput );
        converter.convert( input, output );
        assertTrue( new File( out ).exists() );
        assertTrue( new File( out ).length() != 0 );

        in = getBasedir() + "/target/unit/file/twiki/test.twiki.xhtml";
        from = "xhtml";
        out = getBasedir() + "/target/unit/file/twiki/test.twiki";
        to = "twiki";

        input = InputFileWrapper.valueOf( in, from, ReaderFactory.UTF_8, converter.getInputFormats() );
        output = OutputFileWrapper.valueOf( out, to, WriterFactory.UTF_8, converter.getOutputFormats() );

        converter.setFormatOutput( formatOutput );
        try
        {
            converter.convert( input, output );
        }
        catch ( ConverterException e )
        {
            // The TWiki parser is wrong for *  <pre>some text</pre>
            if ( !e.getMessage().contains( "Error validating the model" ) )
            {
                throw e;
            }
        }
    }

    /**
     * Input xdoc file / output dir
     *
     * @see Converter#convert(InputFileWrapper, OutputFileWrapper)
     * @throws Exception if any
     */
    public void testXdocFileConverter()
        throws Exception
    {
        String in = getBasedir() + "/src/test/resources/unit/xdoc/test.xml";
        String from = "xdoc";
        String out = getBasedir() + "/target/unit/file/xdoc/test.xdoc.xhtml";
        String to = "xhtml";

        InputFileWrapper input =
            InputFileWrapper.valueOf( in, from, ReaderFactory.UTF_8, converter.getInputFormats() );
        OutputFileWrapper output =
            OutputFileWrapper.valueOf( out, to, WriterFactory.UTF_8, converter.getOutputFormats() );

        converter.setFormatOutput( formatOutput );
        converter.convert( input, output );
        assertTrue( new File( out ).exists() );
        assertTrue( new File( out ).length() != 0 );

        in = getBasedir() + "/target/unit/file/xdoc/test.xdoc.xhtml";
        from = "xhtml";
        out = getBasedir() + "/target/unit/file/xdoc/test.xdoc";
        to = "xdoc";

        input = InputFileWrapper.valueOf( in, from, ReaderFactory.UTF_8, converter.getInputFormats() );
        output = OutputFileWrapper.valueOf( out, to, WriterFactory.UTF_8, converter.getOutputFormats() );

        converter.setFormatOutput( formatOutput );
        converter.convert( input, output );
        assertTrue( new File( out ).exists() );
        assertTrue( new File( out ).length() != 0 );
    }

    /**
     * Input xhtml file / output dir
     *
     * @see Converter#convert(InputFileWrapper, OutputFileWrapper)
     * @throws Exception if any
     */
    public void testXhtmlFileConverter()
        throws Exception
    {
        String in = getBasedir() + "/src/test/resources/unit/xhtml/test.xhtml";
        String from = "xhtml";
        String out = getBasedir() + "/target/unit/file/xhtml/test.xhtml.xhtml";
        String to = "xhtml";

        InputFileWrapper input =
            InputFileWrapper.valueOf( in, from, ReaderFactory.UTF_8, converter.getInputFormats() );
        OutputFileWrapper output =
            OutputFileWrapper.valueOf( out, to, WriterFactory.UTF_8, converter.getOutputFormats() );

        converter.setFormatOutput( formatOutput );
        converter.convert( input, output );
        assertTrue( new File( out ).exists() );
        assertTrue( new File( out ).length() != 0 );

        in = getBasedir() + "/target/unit/file/xhtml/test.xhtml.xhtml";
        from = "xhtml";
        out = getBasedir() + "/target/unit/file/xhtml/test.xhtml";
        to = "xhtml";

        input = InputFileWrapper.valueOf( in, from, ReaderFactory.UTF_8, converter.getInputFormats() );
        output = OutputFileWrapper.valueOf( out, to, WriterFactory.UTF_8, converter.getOutputFormats() );

        converter.setFormatOutput( formatOutput );
        converter.convert( input, output );
        assertTrue( new File( out ).exists() );
        assertTrue( new File( out ).length() != 0 );
    }
    /**
     * Input xhtml5 file / output dir
     *
     * @see Converter#convert(InputFileWrapper, OutputFileWrapper)
     * @throws Exception if any
     */
    public void testXhtml5FileConverter()
            throws Exception
    {
        String in = getBasedir() + "/src/test/resources/unit/xhtml/test.xhtml5";
        String from = "xhtml5";
        String out = getBasedir() + "/target/unit/file/xhtml/test.xhtml.xhtml5";
        String to = "xhtml5";

        InputFileWrapper input =
                InputFileWrapper.valueOf( in, from, ReaderFactory.UTF_8, converter.getInputFormats() );
        OutputFileWrapper output =
                OutputFileWrapper.valueOf( out, to, WriterFactory.UTF_8, converter.getOutputFormats() );

        converter.setFormatOutput( formatOutput );
        converter.convert( input, output );
        assertTrue( new File( out ).exists() );
        assertTrue( new File( out ).length() != 0 );

        in = getBasedir() + "/target/unit/file/xhtml/test.xhtml.xhtml5";
        from = "xhtml5";
        out = getBasedir() + "/target/unit/file/xhtml/test.xhtml5";
        to = "xhtml5";

        input = InputFileWrapper.valueOf( in, from, ReaderFactory.UTF_8, converter.getInputFormats() );
        output = OutputFileWrapper.valueOf( out, to, WriterFactory.UTF_8, converter.getOutputFormats() );

        converter.setFormatOutput( formatOutput );
        converter.convert( input, output );
        assertTrue( new File( out ).exists() );
        assertTrue( new File( out ).length() != 0 );
    }

    /**
     * Input apt reader / output writer
     *
     * @see Converter#convert(InputReaderWrapper, OutputStreamWrapper)
     * @throws Exception if any
     */
    public void testAptWriterConverter()
        throws Exception
    {
        String in = getBasedir() + "/src/test/resources/unit/apt/test.apt";
        String from = "apt";
        String out = getBasedir() + "/target/unit/writer/apt/test.apt.xhtml";
        String to = "xhtml";

        File inFile = new File( in );
        File outFile = new File( out );
        outFile.getParentFile().mkdirs();

        try ( OutputStream fo = new FileOutputStream( outFile ) )
        {
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

            InputReaderWrapper input = InputReaderWrapper.valueOf( new FileReader( inFile ), from,
                    converter.getInputFormats() );
            OutputStreamWrapper output = OutputStreamWrapper.valueOf( outputStream, to, "UTF-8",
                    converter.getOutputFormats() );

            converter.setFormatOutput( formatOutput );
            converter.convert( input, output );

            IOUtil.copy( outputStream.toByteArray(), fo );
        }

        assertTrue( outFile.exists() );
        assertTrue( outFile.length() != 0 );
    }

    /**
     * Input xdoc (autodetect) reader / output writer
     *
     * @see Converter#convert(InputReaderWrapper, OutputStreamWrapper)
     * @throws Exception if any
     */
    public void testAutoDetectConverter()
        throws Exception
    {
        String in = getBasedir() + "/src/test/resources/unit/xdoc/test.xml";
        String out = getBasedir() + "/target/unit/writer/apt/test.xdoc.apt";
        String to = "xhtml";

        File inFile = new File( in );
        File outFile = new File( out );
        outFile.getParentFile().mkdirs();

        try (FileWriter fw = new FileWriter( outFile ))
        {
            StringWriter writer = new StringWriter();

            InputFileWrapper input =
                InputFileWrapper.valueOf( inFile.getAbsolutePath(), null, converter.getInputFormats() );
            OutputFileWrapper output =
                OutputFileWrapper.valueOf( outFile.getAbsolutePath(), to, converter.getOutputFormats() );

            converter.setFormatOutput( formatOutput );
            converter.convert( input, output );

            IOUtil.copy( writer.toString(), fw );

            assertTrue( outFile.exists() );
            assertTrue( outFile.length() != 0 );
        }

        in = getBasedir() + "/src/test/resources/unit/apt/test.apt";
        out = getBasedir() + "/target/unit/writer/apt/test.apt.xhtml";
        to = "xhtml";

        inFile = new File( in );
        outFile = new File( out );
        outFile.getParentFile().mkdirs();

        try (FileWriter fw = new FileWriter( outFile ))
        {
            StringWriter writer = new StringWriter();

            InputFileWrapper input =
                InputFileWrapper.valueOf( inFile.getAbsolutePath(), null, converter.getInputFormats() );
            OutputFileWrapper output =
                OutputFileWrapper.valueOf( outFile.getAbsolutePath(), to, converter.getOutputFormats() );

            converter.setFormatOutput( formatOutput );
            converter.convert( input, output );

            IOUtil.copy( writer.toString(), fw );

            assertTrue( outFile.exists() );
            assertTrue( outFile.length() != 0 );
        }

        in = getBasedir() + "/src/test/resources/unit/apt/test.unknown";
        out = getBasedir() + "/target/unit/writer/apt/test.apt.xhtml";
        to = "xhtml";

        inFile = new File( in );
        outFile = new File( out );
        outFile.getParentFile().mkdirs();

        try (FileWriter fw = new FileWriter( outFile ))
        {
            InputFileWrapper input =
                InputFileWrapper.valueOf( inFile.getAbsolutePath(), null, converter.getInputFormats() );
            OutputFileWrapper output =
                OutputFileWrapper.valueOf( outFile.getAbsolutePath(), to, converter.getOutputFormats() );

            converter.setFormatOutput( formatOutput );
            converter.convert( input, output );

            fail();
        }
        catch ( UnsupportedOperationException e )
        {
            assertTrue( true );
        }

        in = getBasedir() + "/src/test/resources/unit/xhtml/test.xhtml5";
        out = getBasedir() + "/target/unit/writer/xhtml/test.html5.xhtml5";
        to = "xhtml5";

        inFile = new File( in );
        outFile = new File( out );
        outFile.getParentFile().mkdirs();

        try (FileWriter fw = new FileWriter( outFile ))
        {
            StringWriter writer = new StringWriter();

            InputFileWrapper input =
                    InputFileWrapper.valueOf( inFile.getAbsolutePath(), null, converter.getInputFormats() );
            OutputFileWrapper output =
                    OutputFileWrapper.valueOf( outFile.getAbsolutePath(), to, converter.getOutputFormats() );

            converter.setFormatOutput( formatOutput );
            converter.convert( input, output );

            IOUtil.copy( writer.toString(), fw );

            assertTrue( outFile.exists() );
            assertTrue( outFile.length() != 0 );
        }


    }

    private String autoDetectEncoding( File f )
    {
        return DefaultConverter.autoDetectEncoding( f );
    }

    private String autoDetectEncoding( String filename )
    {
        return autoDetectEncoding( new File( getBasedir() + "/src/test/resources/unit/" + filename ) );
    }

    /**
     * Test {@link DefaultConverter#autoDetectEncoding(File)}
     */
    public void testAutodetectEncoding()
    {
        assertEquals( "ISO-8859-1", autoDetectEncoding( "apt/test.apt" ) );
        assertEquals( "UTF-8", autoDetectEncoding( "fml/test.fml" ) ); // plexus-utils should detect ISO-8859-1
        assertEquals( "ISO-8859-1", autoDetectEncoding( "twiki/test.twiki" ) );
        assertEquals( "UTF-8", autoDetectEncoding( "xhtml/test.xhtml" ) );
    }

    private String autoDetectFormat( File f, String encoding )
    {
        return DefaultConverter.autoDetectFormat( f, encoding );
    }

    private String autoDetectFormat( String filename, String encoding )
    {
        return autoDetectFormat( new File( getBasedir() + "/src/test/resources/unit/" + filename ), encoding );
    }

    /**
     * Test {@link DefaultConverter#autoDetectFormat(File,String)}
     *
     */
    public void testAutodetectFormat()
    {
        assertEquals( autoDetectFormat( "apt/test.apt", "UTF-8" ), "apt" );

        try
        {
            autoDetectFormat( "apt/test.unknown", "UTF-8" );
            fail();
        }
        catch ( UnsupportedOperationException e )
        {
            assertTrue( true );
        }

        assertEquals( autoDetectFormat( "fml/test.fml", "UTF-8" ), "fml" );
        assertEquals( autoDetectFormat( "twiki/test.twiki", "UTF-8" ), "twiki" );
        assertEquals( autoDetectFormat( "xhtml/test.xhtml", "UTF-8" ), "xhtml" );
    }
}

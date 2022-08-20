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
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.maven.doxia.logging.Log;
import org.apache.maven.doxia.logging.SystemStreamLog;
import org.apache.maven.doxia.parser.ParseException;
import org.apache.maven.doxia.parser.Parser;
import org.apache.maven.doxia.sink.Sink;
import org.apache.maven.doxia.sink.SinkFactory;
import org.apache.maven.doxia.util.ConverterUtil;
import org.apache.maven.doxia.wrapper.InputFileWrapper;
import org.apache.maven.doxia.wrapper.InputReaderWrapper;
import org.apache.maven.doxia.wrapper.OutputFileWrapper;
import org.apache.maven.doxia.wrapper.OutputStreamWrapper;
import org.codehaus.plexus.ContainerConfiguration;
import org.codehaus.plexus.DefaultContainerConfiguration;
import org.codehaus.plexus.DefaultPlexusContainer;
import org.codehaus.plexus.PlexusContainer;
import org.codehaus.plexus.PlexusContainerException;
import org.codehaus.plexus.component.repository.exception.ComponentLookupException;
import org.codehaus.plexus.util.FileUtils;
import org.codehaus.plexus.util.ReaderFactory;
import org.codehaus.plexus.util.SelectorUtils;
import org.codehaus.plexus.util.StringUtils;
import org.codehaus.plexus.util.WriterFactory;
import org.apache.commons.io.input.XmlStreamReader;
import org.codehaus.plexus.util.xml.XmlUtil;
import org.codehaus.plexus.util.xml.pull.MXParser;
import org.codehaus.plexus.util.xml.pull.XmlPullParser;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;

import com.ibm.icu.text.CharsetDetector;
import com.ibm.icu.text.CharsetMatch;

import static java.lang.String.format;

/**
 * Default implementation of <code>Converter</code>
 *
 * @author <a href="mailto:vincent.siveton@gmail.com">Vincent Siveton</a>
 */
public class DefaultConverter
    implements Converter
{
    private static final String APT_PARSER = "apt";

    private static final String FML_PARSER = "fml";

    private static final String XDOC_PARSER = "xdoc";

    private static final String XHTML_PARSER = "xhtml";

    private static final String XHTML5_PARSER = "xhtml5";

    /** Supported input format, i.e. supported Doxia parser */
    public static final String[] SUPPORTED_FROM_FORMAT =
        { APT_PARSER, FML_PARSER, XDOC_PARSER, XHTML_PARSER,
                XHTML5_PARSER};

    private static final String APT_SINK = "apt";

    private static final String XDOC_SINK = "xdoc";

    private static final String XHTML_SINK = "xhtml";

    private static final String XHTML5_SINK = "xhtml5";

    /** Supported output format, i.e. supported Doxia Sink */
    public static final String[] SUPPORTED_TO_FORMAT = { APT_SINK, XDOC_SINK, XHTML_SINK, XHTML5_SINK };

    /** Flag to format the generated files, actually only for XML based sinks. */
    private boolean formatOutput;

    /** Plexus container */
    private PlexusContainer plexus;

    /** Doxia logger */
    private Log log;

    /** {@inheritDoc} */
    @Override
    public void enableLogging( Log log )
    {
        this.log = log;
    }

    /**
     * Returns a logger for this sink.
     * If no logger has been configured, a new SystemStreamLog is returned.
     *
     * @return Log
     */
    protected Log getLog()
    {
        if ( log == null )
        {
            log = new SystemStreamLog();
        }

        return log;
    }

    /** {@inheritDoc} */
    @Override
    public String[] getInputFormats()
    {
        return SUPPORTED_FROM_FORMAT;
    }

    /** {@inheritDoc} */
    @Override
    public String[] getOutputFormats()
    {
        return SUPPORTED_TO_FORMAT;
    }

    /** {@inheritDoc} */
    @Override
    public void convert( InputFileWrapper input, OutputFileWrapper output )
        throws UnsupportedFormatException, ConverterException
    {
        Objects.requireNonNull( input, "input is required" );
        Objects.requireNonNull( output, "output is required" );

        try
        {
            startPlexusContainer();
        }
        catch ( PlexusContainerException e )
        {
            throw new ConverterException( "PlexusContainerException: " + e.getMessage(), e );
        }

        try
        {
            if ( input.getFile().isFile() )
            {
                parse( input.getFile(), input.getEncoding(), input.getFormat(), output );
            }
            else
            {
                List<File> files;
                try
                {
                    files = FileUtils.getFiles( input.getFile(), "**/*." + input.getFormat(),
                                            StringUtils.join( FileUtils.getDefaultExcludes(), ", " ) );
                }
                catch ( IOException e )
                {
                    throw new ConverterException( "IOException: " + e.getMessage(), e );
                }
                catch ( IllegalStateException e )
                {
                    throw new ConverterException( "IllegalStateException: " + e.getMessage(), e );
                }

                for ( File f : files )
                {
                    parse( f, input.getEncoding(), input.getFormat(), output );
                }
            }
        }
        finally
        {
            stopPlexusContainer();
        }
    }

    /** {@inheritDoc} */
    @Override
    public void convert( InputReaderWrapper input, OutputStreamWrapper output )
        throws UnsupportedFormatException, ConverterException
    {
        Objects.requireNonNull( input, "input is required" );
        Objects.requireNonNull( output, "output is required" );

        try
        {
            startPlexusContainer();
        }
        catch ( PlexusContainerException e )
        {
            throw new ConverterException( "PlexusContainerException: " + e.getMessage(), e );
        }

        try
        {
            Parser parser;
            try
            {
                parser = ConverterUtil.getParser( plexus, input.getFormat(), SUPPORTED_FROM_FORMAT );
                parser.enableLogging( log );
            }
            catch ( ComponentLookupException e )
            {
                throw new ConverterException( "ComponentLookupException: " + e.getMessage(), e );
            }

            if ( getLog().isDebugEnabled() )
            {
                getLog().debug( "Parser used: " + parser.getClass().getName() );
            }

            SinkFactory sinkFactory;
            try
            {
                sinkFactory = ConverterUtil.getSinkFactory( plexus, output.getFormat(), SUPPORTED_TO_FORMAT );
            }
            catch ( ComponentLookupException e )
            {
                throw new ConverterException( "ComponentLookupException: " + e.getMessage(), e );
            }

            Sink sink;
            try
            {
                sink = sinkFactory.createSink( output.getOutputStream(), output.getEncoding() );
            }
            catch ( IOException e )
            {
                throw new ConverterException( "IOException: " + e.getMessage(), e );
            }
            sink.enableLogging( log );

            if ( getLog().isDebugEnabled() )
            {
                getLog().debug( "Sink used: " + sink.getClass().getName() );
            }

            parse( parser, input.getReader(), sink );
        }
        finally
        {
            stopPlexusContainer();
        }
    }

    /** {@inheritDoc} */
    @Override
    public void setFormatOutput( boolean formatOutput )
    {
        this.formatOutput = formatOutput;
    }

    // ----------------------------------------------------------------------
    // Private methods
    // ----------------------------------------------------------------------

    /**
     * @param inputFile a not null existing file.
     * @param inputEncoding a not null supported encoding or {@link InputFileWrapper#AUTO_ENCODING}
     * @param inputFormat  a not null supported format or {@link InputFileWrapper#AUTO_FORMAT}
     * @param output not null OutputFileWrapper object
     * @throws ConverterException if any
     * @throws UnsupportedFormatException if any
     */
    private void parse( File inputFile, String inputEncoding, String inputFormat, OutputFileWrapper output )
        throws ConverterException, UnsupportedFormatException
    {
        if ( getLog().isDebugEnabled() )
        {
            getLog().debug(
                            "Parsing file from '" + inputFile.getAbsolutePath() + "' with the encoding '"
                                + inputEncoding + "' to '" + output.getFile().getAbsolutePath()
                                + "' with the encoding '" + output.getEncoding() + "'" );
        }

        if ( InputFileWrapper.AUTO_ENCODING.equals( inputEncoding ) )
        {
            inputEncoding = autoDetectEncoding( inputFile );
            if ( getLog().isDebugEnabled() )
            {
                getLog().debug( "Auto detect encoding: " + inputEncoding );
            }
        }

        if ( InputFileWrapper.AUTO_FORMAT.equals( inputFormat ) )
        {
            inputFormat = autoDetectFormat( inputFile, inputEncoding );
            if ( getLog().isDebugEnabled() )
            {
                getLog().debug( "Auto detect input format: " + inputFormat );
            }
        }

        Parser parser;
        try
        {
            parser = ConverterUtil.getParser( plexus, inputFormat, SUPPORTED_FROM_FORMAT );
            parser.enableLogging( log );
        }
        catch ( ComponentLookupException e )
        {
            throw new ConverterException( "ComponentLookupException: " + e.getMessage(), e );
        }

        File outputFile;
        if ( output.getFile().isDirectory() )
        {
            outputFile = new File( output.getFile(), inputFile.getName() + "." + output.getFormat() );
        }
        else
        {
            if ( !SelectorUtils.match( "**.*", output.getFile().getName() ) )
            {
                // assume it is a directory
                output.getFile().mkdirs();
                outputFile = new File( output.getFile(), inputFile.getName() + "." + output.getFormat() );
            }
            else
            {
                output.getFile().getParentFile().mkdirs();
                outputFile = output.getFile();
            }
        }

        Reader reader;
        try
        {
            if ( inputEncoding != null )
            {
                if ( parser.getType() == Parser.XML_TYPE )
                {
                    reader = ReaderFactory.newXmlReader( inputFile );
                }
                else
                {
                    reader = ReaderFactory.newReader( inputFile, inputEncoding );
                }
            }
            else
            {
                reader = ReaderFactory.newPlatformReader( inputFile );
            }
        }
        catch ( IOException e )
        {
            throw new ConverterException( "IOException: " + e.getMessage(), e );
        }

        SinkFactory sinkFactory;
        try
        {
            sinkFactory = ConverterUtil.getSinkFactory( plexus, output.getFormat(), SUPPORTED_TO_FORMAT );
        }
        catch ( ComponentLookupException e )
        {
            throw new ConverterException( "ComponentLookupException: " + e.getMessage(), e );
        }

        Sink sink;
        try
        {
            String outputEncoding;
            if ( StringUtils.isEmpty( output.getEncoding() )
                || output.getEncoding().equals( OutputFileWrapper.AUTO_ENCODING ) )
            {
                outputEncoding = inputEncoding;
            }
            else
            {
                outputEncoding = output.getEncoding();
            }

            OutputStream out = new FileOutputStream( outputFile );
            sink = sinkFactory.createSink( out, outputEncoding );
        }
        catch ( IOException e )
        {
            throw new ConverterException( "IOException: " + e.getMessage(), e );
        }

        sink.enableLogging( log );

        if ( getLog().isDebugEnabled() )
        {
            getLog().debug( "Sink used: " + sink.getClass().getName() );
        }

        parse( parser, reader, sink );

        if ( formatOutput && ( XDOC_SINK.equals( output.getFormat() )
            || XHTML_SINK.equals( output.getFormat() ) || XHTML5_SINK.equals( output.getFormat() ) ) )
        {
            try ( Reader r = ReaderFactory.newXmlReader( outputFile );
                  Writer w = WriterFactory.newXmlWriter( outputFile ) )
            {
                CharArrayWriter caw = new CharArrayWriter();
                XmlUtil.prettyFormat( r, caw );
                w.write( caw.toString() );
            }
            catch ( IOException e )
            {
                throw new ConverterException( "IOException: " + e.getMessage(), e );
            }
        }
    }

    /**
     * @param parser not null
     * @param reader not null
     * @param sink not null
     * @throws ConverterException if any
     */
    private void parse( Parser parser, Reader reader, Sink sink )
        throws ConverterException
    {
        try ( Reader r = reader )
        {
            parser.parse( r, sink );
        }
        catch ( ParseException | IOException e )
        {
            throw new ConverterException( "ParseException: " + e.getMessage(), e );
        }
        finally
        {
            sink.flush();
            sink.close();
        }
    }

    /**
     * Start the Plexus container.
     *
     * @throws PlexusContainerException if any
     */
    private void startPlexusContainer()
        throws PlexusContainerException
    {
        if ( plexus != null )
        {
            return;
        }

        Map<Object, Object> context = new HashMap<>();
        context.put( "basedir", new File( "" ).getAbsolutePath() );

        ContainerConfiguration containerConfiguration = new DefaultContainerConfiguration();
        containerConfiguration.setName( "Doxia" );
        containerConfiguration.setContext( context );

        plexus = new DefaultPlexusContainer( containerConfiguration );
    }

    /**
     * Stop the Plexus container.
     */
    private void stopPlexusContainer()
    {
        if ( plexus == null )
        {
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
    static String autoDetectEncoding( File f )
    {
        if ( !f.isFile() )
        {
            throw new IllegalArgumentException( "The file '" + f.getAbsolutePath()
                + "' is not a file, could not detect encoding." );
        }
        try
        {
            if ( XmlUtil.isXml( f ) )
            {
                try ( XmlStreamReader reader = new XmlStreamReader( f ) )
                {
                    return reader.getEncoding();
                }
            }

            try ( InputStream is = new BufferedInputStream( new FileInputStream( f ) ) )
            {
                CharsetDetector detector = new CharsetDetector();
                detector.setText( is );
                CharsetMatch match = detector.detect();

                return match.getName().toUpperCase( Locale.ENGLISH );
            }
        }
        catch ( IOException e )
        {
            // nop
        }
        throw new UnsupportedOperationException( format( "Could not detect the encoding for file: %s\n"
                + "Specify explicitly the encoding.", f.getAbsolutePath() ) );
    }

    /**
     * Auto detect Doxia format for the given file depending:
     * <ul>
     * <li>the file name for TextMarkup based Doxia files</li>
     * <li>the file content for XMLMarkup based Doxia files</li>
     * </ul>
     *
     * @param f not null file
     * @param encoding a not null encoding.
     * @return the detected encoding from f.
     * @throws IllegalArgumentException if f is not a file.
     * @throws UnsupportedOperationException if could not detect the Doxia format.
     */
    static String autoDetectFormat( File f, String encoding )
    {
        if ( !f.isFile() )
        {
            throw new IllegalArgumentException( "The file '" + f.getAbsolutePath()
                + "' is not a file, could not detect format." );
        }

        for ( String supportedFromFormat : SUPPORTED_FROM_FORMAT )
        {
            // Handle Doxia text files
            if ( APT_PARSER.equalsIgnoreCase( supportedFromFormat ) && isDoxiaFileName( f, supportedFromFormat ) )
            {
                return supportedFromFormat;
            }

            // Handle Doxia xml files
            String firstTag = getFirstTag( f );
            if ( firstTag == null )
            {
                //noinspection UnnecessaryContinue
                continue;
            }
            else if ( "faqs".equals( firstTag ) && FML_PARSER.equalsIgnoreCase( supportedFromFormat ) )
            {
                return supportedFromFormat;
            }
            else if ( "document".equals( firstTag ) && XDOC_PARSER.equalsIgnoreCase( supportedFromFormat ) )
            {
                return supportedFromFormat;
            }
            else if ( "html".equals( firstTag ) && XHTML_PARSER.equalsIgnoreCase( supportedFromFormat ) )
            {
                return supportedFromFormat;
            }
        }

        throw new UnsupportedOperationException(
                format( "Could not detect the Doxia format for file: %s\n Specify explicitly the Doxia format.",
                        f.getAbsolutePath() ) );
    }

    /**
     * @param f not null
     * @param format could be null
     * @return <code>true</code> if the file name computes the format.
     */
    private static boolean isDoxiaFileName( File f, String format )
    {
        Objects.requireNonNull( f, "f is required." );

        Pattern pattern = Pattern.compile( "(.*?)\\." + format.toLowerCase( Locale.ENGLISH ) + "$" );
        Matcher matcher = pattern.matcher( f.getName().toLowerCase( Locale.ENGLISH ) );

        return matcher.matches();
    }

    /**
     * @param xmlFile not null and should be a file.
     * @return the first tag name if found, <code>null</code> in other case.
     */
    private static String getFirstTag( File xmlFile )
    {
        if ( xmlFile == null )
        {
            throw new IllegalArgumentException( "xmlFile is required." );
        }
        if ( !xmlFile.isFile() )
        {
            throw new IllegalArgumentException( "The file '" + xmlFile.getAbsolutePath() + "' is not a file." );
        }


        try ( Reader reader = ReaderFactory.newXmlReader( xmlFile ) )
        {
            XmlPullParser parser = new MXParser();
            parser.setInput( reader );
            int eventType = parser.getEventType();
            while ( eventType != XmlPullParser.END_DOCUMENT )
            {
                if ( eventType == XmlPullParser.START_TAG )
                {
                    return parser.getName();
                }
                eventType = parser.nextToken();
            }
        }
        catch ( IOException | XmlPullParserException e )
        {
            return null;
        }

        return null;
    }
}

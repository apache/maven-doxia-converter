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
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

import org.apache.maven.doxia.logging.Log;
import org.apache.maven.doxia.logging.SystemStreamLog;
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
import org.codehaus.plexus.PlexusContainer;
import org.codehaus.plexus.PlexusContainerException;
import org.codehaus.plexus.component.repository.exception.ComponentLookupException;
import org.codehaus.plexus.util.FileUtils;
import org.codehaus.plexus.util.PathTool;
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

    private static final String CONFLUENCE_PARSER = "confluence";

    private static final String DOCBOOK_PARSER = "docbook";

    private static final String FML_PARSER = "fml";

    private static final String TWIKI_PARSER = "twiki";

    private static final String XDOC_PARSER = "xdoc";

    private static final String XHTML_PARSER = "xhtml";

    private static final String XHTML5_PARSER = "xhtml5";

    private static final String MARKDOWN_PARSER = "markdown";


    private static final String APT_SINK = "apt";

    private static final String CONFLUENCE_SINK = "confluence";

    private static final String DOCBOOK_SINK = "docbook";

    private static final String FO_SINK = "fo";

    private static final String ITEXT_SINK = "itext";

    private static final String LATEX_SINK = "latex";

    private static final String RTF_SINK = "rtf";

    private static final String TWIKI_SINK = "twiki";

    private static final String XDOC_SINK = "xdoc";

    private static final String XHTML_SINK = "xhtml";

    private static final String XHTML5_SINK = "xhtml5";

    private static final String MARKDOWN_SINK = "markdown";

    /**
     * All supported source formats
     */
    public enum ParserFormat
    {
        APT( APT_PARSER, "apt" ),
        CONFLUENCE( CONFLUENCE_PARSER, "confluence" ),
        DOCBOOK( DOCBOOK_PARSER, "xml", "article" ),
        FML( FML_PARSER, "fml", "faqs" ),
        TWIKI( TWIKI_PARSER, "twiki" ),
        XDOC( XDOC_PARSER, "xml", "document" ),
        XHTML( XHTML_PARSER, "html", "html" ),
        XHTML5( XHTML5_PARSER, "html" ), // no autodetect support
        MARKDOWN( MARKDOWN_PARSER, "md" );

        private final String parserName;
        private final String extension;
        private final String firstElement;

        ParserFormat( String parserName, String extension )
        {
            this( parserName, extension, null );
        }

        ParserFormat( String parserName, String extension, String firstElement )
        {
            this.parserName = parserName;
            this.extension = extension;
            this.firstElement = firstElement;
        }

        /**
         * 
         * @return the primary extension used with this format
         */
        public String getExtension()
        {
            return extension;
        }

        /**
         * 
         * @return {@code true} in case this format is XML based
         */
        public boolean isXml()
        {
            return firstElement != null;
        }

        /**
         * @param plexus not null
         * @return an instance of <code>Parser</code> depending on the format.
         * @throws ComponentLookupException if could not find the Parser for the given format.
         * @throws IllegalArgumentException if any parameter is null
         */
        public Parser getParser( PlexusContainer plexus )
            throws ComponentLookupException
        {
            Objects.requireNonNull( plexus, "plexus is required" );

            return (Parser) plexus.lookup( Parser.ROLE, parserName );
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
        public static ParserFormat autoDetectFormat( File f )
        {
            if ( !f.isFile() )
            {
                throw new IllegalArgumentException( "The path '" + f.getAbsolutePath()
                    + "' does not locate a file, could not detect format." );
            }

            for ( ParserFormat format : EnumSet.allOf( ParserFormat.class ) )
            {
                if ( format.isXml() )
                {
                    // Handle Doxia xml files
                    String firstTag = getFirstTag( f );
                    if ( firstTag == null )
                    {
                        //noinspection UnnecessaryContinue
                        continue;
                    }
                    if ( firstTag.equals( format.firstElement ) )
                    {
                        return format;
                    }
                }
                else
                {
                    if ( hasFileExtensionIgnoreCase( f.getName(), format.getExtension() ) )
                    {
                        return format;
                    }
                }
            }
            throw new UnsupportedOperationException(
                    format( "Could not detect the Doxia format for file: %s\n Specify explicitly the Doxia format.",
                            f.getAbsolutePath() ) );
        }
    }

    /**
     * All supported target formats.
     */
    public enum SinkFormat
    {
        APT( APT_SINK, "apt", false ),
        CONFLUENCE( CONFLUENCE_SINK, "confluence", false ),
        DOCBOOK( DOCBOOK_SINK, "xml", true ),
        FO( FO_SINK, "fo", true ),
        ITEXT( ITEXT_SINK, "itext", false ),
        LATEXT( LATEX_SINK, "tex", false ),
        RTF( RTF_SINK, "rtf", false ),
        TWIKI( TWIKI_SINK, "twiki", false ),
        XDOC( XDOC_SINK, "xdoc", true ),
        XHTML( XHTML_SINK, "html", true ),
        XHTML5( XHTML5_SINK, "html", true ),
        MARKDOWN( MARKDOWN_SINK, "md", false );

        private final String sinkName;
        private final String extension;
        private final boolean isXml;

        SinkFormat( String sinkName, String extension, boolean isXml )
        {
            this.sinkName = sinkName;
            this.extension = extension;
            this.isXml = isXml;
        }

        public String getExtension()
        {
            return extension;
        }

        /**
         * @param plexus not null
         * @return an instance of <code>SinkFactory</code> depending on the given format.
         * @throws ComponentLookupException if could not find the SinkFactory for the given format.
         * @throws IllegalArgumentException if any parameter is null
         */
        public SinkFactory getSinkFactory( PlexusContainer plexus )
            throws ComponentLookupException
        {
            Objects.requireNonNull( plexus, "plexus is required" );

            return (SinkFactory) plexus.lookup( SinkFactory.ROLE, sinkName );
        }

        boolean isXml()
        {
            return isXml;
        }
    }

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
                    files = FileUtils.getFiles( input.getFile(), "**/*." + input.getFormat().getExtension(),
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
                    File relativeOutputDirectory = new File( 
                            PathTool.getRelativeFilePath( input.getFile().getAbsolutePath(), f.getParent() ) );
                    parse( f, input.getEncoding(), input.getFormat(), output, relativeOutputDirectory );
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
                parser = input.getFormat().getParser( plexus );
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
                sinkFactory = output.getFormat().getSinkFactory( plexus );
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
     * @param parserFormat  a not null supported format or {@link InputFileWrapper#AUTO_FORMAT}
     * @param output not null OutputFileWrapper object
     * @throws ConverterException if any
     * @throws UnsupportedFormatException if any
     */
    private void parse( File inputFile, String inputEncoding, ParserFormat parserFormat, OutputFileWrapper output )
        throws ConverterException, UnsupportedFormatException
    {
        parse( inputFile, inputEncoding, parserFormat, output, null );
    }

    /**
     * @param inputFile a not null existing file.
     * @param inputEncoding a not null supported encoding or {@link InputFileWrapper#AUTO_ENCODING}
     * @param parserFormat  a not null supported format
     * @param output not null OutputFileWrapper object
     * @param relativeOutputDirectory the relative output directory (may be null, created if it does not exist yet)
     * @throws ConverterException if any
     * @throws UnsupportedFormatException if any
     */
    private void parse( File inputFile, String inputEncoding, ParserFormat parserFormat, OutputFileWrapper output,
            File relativeOutputDirectory )
        throws ConverterException, UnsupportedFormatException
    {
        File outputDirectoryOrFile = relativeOutputDirectory != null 
                ? new File( output.getFile(), relativeOutputDirectory.getPath() ) 
                : output.getFile();
        if ( getLog().isDebugEnabled() )
        {
            getLog().debug(
                            "Parsing file from '" + inputFile.getAbsolutePath() + "' with the encoding '"
                                + inputEncoding + "' to '" + outputDirectoryOrFile.getAbsolutePath()
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


        Parser parser;
        try
        {
            parser = parserFormat.getParser( plexus );
            parser.enableLogging( log );
        }
        catch ( ComponentLookupException e )
        {
            throw new ConverterException( "ComponentLookupException: " + e.getMessage(), e );
        }

        File outputFile;
        if ( outputDirectoryOrFile.isDirectory() 
             || !SelectorUtils.match( "**.*", output.getFile().getName() ) 
             || relativeOutputDirectory != null )
        {
            // assume it is a directory
            outputDirectoryOrFile.mkdirs();
            outputFile = new File( outputDirectoryOrFile, 
                    FileUtils.removeExtension( inputFile.getName() ) + "." + output.getFormat().getExtension() );
        }
        else
        {
            outputDirectoryOrFile.getParentFile().mkdirs();
            outputFile = output.getFile();
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
            sinkFactory = output.getFormat().getSinkFactory( plexus );
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

        if ( formatOutput && output.getFormat().isXml() )
        {
            // format all xml files excluding docbook which is buggy
            // TODO Add doc book format
            if ( DOCBOOK_SINK.equals( output.getFormat() ) || DOCBOOK_PARSER.equals( parserFormat ) )
            {
                return;
            }
            
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
     * @param f not null
     * @param format could be null
     * @return <code>true</code> if the file extension matches
     */
    private static boolean hasFileExtensionIgnoreCase( String name, String extension )
    {
        Objects.requireNonNull( name, "name is required." );

        return extension.equals( FileUtils.getExtension( name.toLowerCase( Locale.ENGLISH ) ) );
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

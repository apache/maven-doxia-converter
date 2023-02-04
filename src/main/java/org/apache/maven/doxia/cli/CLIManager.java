package org.apache.maven.doxia.cli;

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

import java.util.EnumSet;
import java.util.stream.Collectors;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.maven.doxia.DefaultConverter;

import com.ibm.icu.text.CharsetDetector;

import static org.codehaus.plexus.util.StringUtils.join;

/**
 * Manager for Doxia converter CLI options.
 *
 * @author <a href="mailto:vincent.siveton@gmail.com">Vincent Siveton</a>
 */
class CLIManager
{
    /** h character */
    static final String HELP = "h";

    /** v character */
    static final String VERSION = "v";

    /** in String */
    static final String IN = "in";

    /** out String */
    static final String OUT = "out";

    /** from String */
    static final String FROM = "from";

    /** to String */
    static final String TO = "to";

    /** inEncoding String */
    static final String INENCODING = "inEncoding";

    /** f character */
    static final String FORMAT = "f";

    /** outEncoding String */
    static final String OUTENCODING = "outEncoding";

    /** X character */
    static final String DEBUG = "X";

    /** e character */
    static final String ERRORS = "e";

    public static final String AUTO_FORMAT = "auto";

    private static final Options OPTIONS;

    private static final String EOL = System.lineSeparator();

    static
    {
        OPTIONS = new Options();

        OPTIONS.addOption( Option.builder( HELP )
                .longOpt( "help" )
                .desc( "Display help information." )
                .build() );
        OPTIONS.addOption( Option.builder( VERSION )
                .longOpt( "version" )
                .desc( "Display version information." )
                .build() );
        OPTIONS.addOption( Option.builder( IN )
                .longOpt( "input" )
                .desc( "Input file or directory." )
                .hasArg()
                .build() );
        OPTIONS.addOption( Option.builder( OUT )
                .longOpt( "output" )
                .desc( "Output file or directory." )
                .hasArg()
                .build() );
        OPTIONS.addOption( Option.builder( FROM )
                .desc( "From format. If not specified, try to autodetect it." )
                .hasArg()
                .build() );
        OPTIONS.addOption( Option.builder( TO )
                .desc( "To format." )
                .hasArg()
                .build() );
        OPTIONS.addOption( Option.builder( INENCODING )
                .desc( "Input file encoding. If not specified, try to autodetect it." )
                .hasArg()
                .build() );
        OPTIONS.addOption( Option.builder( FORMAT )
                .longOpt( "format" )
                .desc( "Format the output (actually only xml based outputs) to be human readable." )
                .build() );
        OPTIONS.addOption( Option.builder( OUTENCODING )
                .desc( "Output file encoding. If not specified, use the input encoding (or autodetected)." )
                .hasArg()
                .build() );
        OPTIONS.addOption( Option.builder( DEBUG )
                .longOpt( "debug" )
                .desc( "Produce execution debug output." )
                .build() );
        OPTIONS.addOption( Option.builder( ERRORS )
                .longOpt( "errors" )
                .desc( "Produce execution error messages." )
                .build() );
    }

    /**
     * @param args not null.
     * @return a not null command line.
     * @throws ParseException if any
     * @throws IllegalArgumentException is args is null
     */
    CommandLine parse( String[] args )
        throws ParseException
    {
        if ( args == null )
        {
            throw new IllegalArgumentException( "args is required." );
        }

        DefaultParser parser = new DefaultParser();
        return parser.parse( OPTIONS, args );
    }

    static void displayHelp()
    {
        System.out.println();

        HelpFormatter formatter = new HelpFormatter();
        formatter.setWidth( 128 );
        formatter.printHelp( "doxia-converter", EOL + "Options:", OPTIONS, getSupportedFormatAndEncoding(), true );
    }

    private static String getSupportedFormatAndEncoding()
    {
        return getSupportedFormat() + "\n" + getSupportedEncoding();
    }

    private static String getSupportedFormat()
    {
        String fromFormats = EnumSet.allOf( DefaultConverter.ParserFormat.class ).stream()
                .map( f -> f.toString().toLowerCase() ).collect( Collectors.joining( ", " ) );
        String toFormats = EnumSet.allOf( DefaultConverter.SinkFormat.class ).stream()
                .map( f -> f.toString().toLowerCase() ).collect( Collectors.joining( ", " ) );
        return EOL + "Supported Formats:" + EOL + " from: " + fromFormats
            + " or " + AUTO_FORMAT + EOL + " to:   " + toFormats
            + EOL;
    }

    private static String getSupportedEncoding()
    {
        return EOL + "Supported Encoding:" + EOL + " " + join( CharsetDetector.getAllDetectableCharsets(), ", " );
    }
}

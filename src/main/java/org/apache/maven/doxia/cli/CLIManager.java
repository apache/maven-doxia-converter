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

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.GnuParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.OptionBuilder;
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
    static final char HELP = 'h';

    /** v character */
    static final char VERSION = 'v';

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
    static final char FORMAT = 'f';

    /** outEncoding String */
    static final String OUTENCODING = "outEncoding";

    /** X character */
    static final char DEBUG = 'X';

    /** e character */
    static final char ERRORS = 'e';

    private static final Options OPTIONS;

    static
    {
        OPTIONS = new Options();

        OptionBuilder.withLongOpt( "help" );
        OptionBuilder.withDescription( "Display help information." );
        OPTIONS.addOption( OptionBuilder.create( HELP ) );
        OptionBuilder.withLongOpt( "version" );
        OptionBuilder.withDescription( "Display version information." );
        OPTIONS.addOption( OptionBuilder.create( VERSION ) );

        OptionBuilder.withLongOpt( "input" );
        OptionBuilder.withDescription( "Input file or directory." );
        OptionBuilder.hasArg();
        OPTIONS.addOption( OptionBuilder.create( IN ) );
        OptionBuilder.withLongOpt( "output" );
        OptionBuilder.withDescription( "Output file or directory." );
        OptionBuilder.hasArg();
        OPTIONS.addOption( OptionBuilder.create( OUT ) );
        OptionBuilder.withDescription( "From format. If not specified, try to autodetect it." );
        OptionBuilder.hasArg();
        OPTIONS.addOption( OptionBuilder.create( FROM ) );
        OptionBuilder.withDescription( "To format." );
        OptionBuilder.hasArg();
        OPTIONS.addOption( OptionBuilder.create( TO ) );
        OptionBuilder.withLongOpt( "inputEncoding" );
        OptionBuilder.withDescription( "Input file encoding. If not specified, try to autodetect it." );
        OptionBuilder.hasArg();
        OPTIONS.addOption( OptionBuilder.create( INENCODING ) );
        OptionBuilder.withLongOpt( "format" );
        OptionBuilder.withDescription( "Format the output (actually only xml based outputs) "
                                                              + " to be human readable." );
        OPTIONS.addOption( OptionBuilder.create( FORMAT ) );
        OptionBuilder.withLongOpt( "outputEncoding" );
        OptionBuilder.withDescription( "Output file encoding. If not specified, use the "
                                                              + "input encoding (or autodetected)." );
        OptionBuilder.hasArg();
        OPTIONS.addOption( OptionBuilder.create( OUTENCODING ) );

        OptionBuilder.withLongOpt( "debug" );
        OptionBuilder.withDescription( "Produce execution debug output." );
        OPTIONS.addOption( OptionBuilder.create( DEBUG ) );
        OptionBuilder.withLongOpt( "errors" );
        OptionBuilder.withDescription( "Produce execution error messages." );
        OPTIONS.addOption( OptionBuilder.create( ERRORS ) );
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

        // We need to eat any quotes surrounding arguments...
        String[] cleanArgs = cleanArgs( args );

        CommandLineParser parser = new GnuParser();
        return parser.parse( OPTIONS, cleanArgs );
    }

    static void displayHelp()
    {
        System.out.println();

        HelpFormatter formatter = new HelpFormatter();
        formatter.printHelp( "doxia [options] -in <arg> [-from <arg>] [-inEncoding <arg>] -out <arg> "
            + "-to <arg> [-outEncoding <arg>]\n", "\nOptions:", OPTIONS, getSupportedFormatAndEncoding() );
    }

    private static String getSupportedFormatAndEncoding()
    {
        return getSupportedFormat() + "\n" + getSupportedEncoding();
    }

    private static String getSupportedFormat()
    {
        return "\nSupported Formats:\n from: " + join( DefaultConverter.SUPPORTED_FROM_FORMAT, ", " )
            + " or autodetect" + "\n out: " + join( DefaultConverter.SUPPORTED_TO_FORMAT, ", " )
            + "\n";
    }

    private static String getSupportedEncoding()
    {
        return "\nSupported Encoding:\n " + join( CharsetDetector.getAllDetectableCharsets(), ", " );
    }

    private String[] cleanArgs( String[] args )
    {
        List<String> cleaned = new ArrayList<>();

        StringBuilder currentArg = null;

        for ( String arg : args )
        {
            boolean addedToBuffer = false;

            if ( arg.startsWith( "\"" ) )
            {
                // if we're in the process of building up another arg, push it and start over.
                // this is for the case: "-Dfoo=bar "-Dfoo2=bar two" (note the first unterminated quote)
                if ( currentArg != null )
                {
                    cleaned.add( currentArg.toString() );
                }

                // start building an argument here.
                currentArg = new StringBuilder( arg.substring( 1 ) );
                addedToBuffer = true;
            }

            // this has to be a separate "if" statement, to capture the case of: "-Dfoo=bar"
            if ( arg.endsWith( "\"" ) )
            {
                String cleanArgPart = arg.substring( 0, arg.length() - 1 );

                // if we're building an argument, keep doing so.
                if ( currentArg != null )
                {
                    // if this is the case of "-Dfoo=bar", then we need to adjust the buffer.
                    if ( addedToBuffer )
                    {
                        currentArg.setLength( currentArg.length() - 1 );
                    }
                    // otherwise, we trim the trailing " and append to the buffer.
                    else
                    {
                        // TODO: introducing a space here...not sure what else to do but collapse whitespace
                        currentArg.append( ' ' ).append( cleanArgPart );
                    }

                    // we're done with this argument, so add it.
                    cleaned.add( currentArg.toString() );
                }
                else
                {
                    // this is a simple argument...just add it.
                    cleaned.add( cleanArgPart );
                }

                // the currentArg MUST be finished when this completes.
                currentArg = null;
                continue;
            }

            // if we haven't added this arg to the buffer, and we ARE building an argument
            // buffer, then append it with a preceding space...again, not sure what else to
            // do other than collapse whitespace.
            // NOTE: The case of a trailing quote is handled by nullifying the arg buffer.
            if ( !addedToBuffer )
            {
                // append to the argument we're building, collapsing whitespace to a single space.
                if ( currentArg != null )
                {
                    currentArg.append( ' ' ).append( arg );
                }
                // this is a loner, just add it directly.
                else
                {
                    cleaned.add( arg );
                }
            }
        }

        // clean up.
        if ( currentArg != null )
        {
            cleaned.add( currentArg.toString() );
        }

        int cleanedSz = cleaned.size();
        String[] cleanArgs;

        if ( cleanedSz == 0 )
        {
            // if we didn't have any arguments to clean, simply pass the original array through
            cleanArgs = args;
        }
        else
        {
            cleanArgs = cleaned.toArray( new String[cleanedSz] );
        }

        return cleanArgs;
    }
}

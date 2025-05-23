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
package org.apache.maven.doxia.cli;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.ParseException;
import org.apache.commons.lang3.reflect.FieldUtils;
import org.apache.maven.doxia.Converter;
import org.apache.maven.doxia.Converter.PostProcess;
import org.apache.maven.doxia.ConverterException;
import org.apache.maven.doxia.DefaultConverter;
import org.apache.maven.doxia.UnsupportedFormatException;
import org.apache.maven.doxia.parser.AbstractParser;
import org.apache.maven.doxia.wrapper.InputFileWrapper;
import org.apache.maven.doxia.wrapper.OutputFileWrapper;
import org.codehaus.plexus.ContainerConfiguration;
import org.codehaus.plexus.DefaultContainerConfiguration;
import org.codehaus.plexus.DefaultPlexusContainer;
import org.codehaus.plexus.PlexusConstants;
import org.codehaus.plexus.PlexusContainer;
import org.codehaus.plexus.PlexusContainerException;
import org.codehaus.plexus.component.repository.exception.ComponentLookupException;
import org.codehaus.plexus.util.Os;

/**
 * Doxia converter CLI.
 *
 * @author <a href="mailto:vincent.siveton@gmail.com">Vincent Siveton</a>
 */
public class ConverterCli {
    /**
     * Default main which terminates the JVM with <code>0</code> if no errors occurs.
     *
     * @param args command line args.
     * @see #doMain(String[])
     * @see System#exit(int)
     */
    public static void main(String[] args) throws PlexusContainerException, ComponentLookupException {
        if (args == null || args.length == 0) {
            args = new String[] {"-h"};
        }
        System.exit(ConverterCli.doMain(args));
    }

    /**
     * @param args The args
     */
    private static int doMain(String[] args) throws PlexusContainerException, ComponentLookupException {
        // ----------------------------------------------------------------------
        // Setup the command line parser
        // ----------------------------------------------------------------------

        CLIManager cliManager = new CLIManager();

        CommandLine commandLine;
        try {
            commandLine = cliManager.parse(args);
        } catch (ParseException e) {
            System.err.println("Unable to parse command line options: " + e.getMessage());
            CLIManager.displayHelp();

            return 1;
        }

        if (commandLine.hasOption(CLIManager.HELP)) {
            CLIManager.displayHelp();

            return 0;
        }

        if (commandLine.hasOption(CLIManager.VERSION)) {
            showVersion();

            return 0;
        }

        boolean debug = commandLine.hasOption(CLIManager.DEBUG);

        boolean showErrors = debug || commandLine.hasOption(CLIManager.ERRORS);

        if (showErrors) {
            System.out.println("+ Error stacktraces are turned on.");
        }
        if (debug) {
            System.setProperty(org.slf4j.impl.SimpleLogger.DEFAULT_LOG_LEVEL_KEY, "DEBUG");
        }

        PlexusContainer container = getPlexusContainer();
        Converter converter = container.lookup(Converter.class);

        InputFileWrapper input;
        OutputFileWrapper output;
        final PostProcess postProcess;
        try {
            String sourceFormat = commandLine.getOptionValue(CLIManager.FROM, CLIManager.AUTO_FORMAT);
            final DefaultConverter.DoxiaFormat parserFormat;
            if (CLIManager.AUTO_FORMAT.equalsIgnoreCase(sourceFormat)) {
                File inputFile = new File(commandLine.getOptionValue(CLIManager.IN));
                parserFormat = DefaultConverter.DoxiaFormat.autoDetectFormat(inputFile);
                if (debug) {
                    System.out.println("Auto detected input format: " + parserFormat);
                }
            } else {
                parserFormat = DefaultConverter.DoxiaFormat.valueOf(sourceFormat.toUpperCase());
            }
            String targetFormat = commandLine.getOptionValue(CLIManager.TO);
            if (commandLine.hasOption(CLIManager.REMOVE_IN)
                    && commandLine.hasOption(CLIManager.GIT_MV_INPUT_TO_OUTPUT)) {
                throw new IllegalArgumentException(
                        "Options 'removeIn' and 'gitMvInputToOutput' are mutually exclusive.");
            } else if (commandLine.hasOption(CLIManager.REMOVE_IN)) {
                postProcess = PostProcess.REMOVE_AFTER_CONVERSION;
            } else if (commandLine.hasOption(CLIManager.GIT_MV_INPUT_TO_OUTPUT)) {
                postProcess = PostProcess.GIT_MV_INPUT_TO_OUTPUT;
            } else {
                postProcess = PostProcess.NONE;
            }
            final DefaultConverter.DoxiaFormat sinkFormat =
                    DefaultConverter.DoxiaFormat.valueOf(targetFormat.toUpperCase());
            input = InputFileWrapper.valueOf(
                    commandLine.getOptionValue(CLIManager.IN),
                    parserFormat,
                    commandLine.getOptionValue(CLIManager.INENCODING));
            output = OutputFileWrapper.valueOf(
                    commandLine.getOptionValue(CLIManager.OUT),
                    sinkFormat,
                    commandLine.getOptionValue(CLIManager.OUTENCODING));
        } catch (IllegalArgumentException e) {
            showFatalError("Illegal argument: " + e.getMessage(), e, showErrors);

            CLIManager.displayHelp();

            return 1;
        } catch (UnsupportedEncodingException | FileNotFoundException e) {
            showFatalError(e.getMessage(), e, showErrors);

            return 1;
        }

        boolean format = commandLine.hasOption(CLIManager.FORMAT);
        converter.setFormatOutput(format);
        converter.setPostProcess(postProcess);

        try {
            converter.convert(input, output);
        } catch (UnsupportedFormatException e) {
            showFatalError(e.getMessage(), e, showErrors);

            return 1;
        } catch (ConverterException e) {
            showFatalError("Converter exception: " + e.getMessage(), e, showErrors);

            return 1;
        } catch (IllegalArgumentException e) {
            showFatalError("Illegal argument: " + e.getMessage(), e, showErrors);

            return 1;
        } catch (RuntimeException e) {
            showFatalError("Runtime exception: " + e.getMessage(), e, showErrors);

            return 1;
        }

        return 0;
    }

    private static void showVersion() {
        try (InputStream resourceAsStream = ConverterCli.class
                .getClassLoader()
                .getResourceAsStream("META-INF/maven/org.apache.maven.doxia/doxia-converter/pom.properties")) {
            Properties properties = new Properties();
            if (resourceAsStream != null) {
                properties.load(resourceAsStream);
            }
            if (properties.getProperty("builtOn") != null) {
                System.out.println("Doxia Converter version: " + properties.getProperty("version", "unknown")
                        + " built on " + properties.getProperty("builtOn"));
            } else {
                System.out.println("Doxia Converter version: " + properties.getProperty("version", "unknown"));
            }
            System.out.println(
                    "Doxia version: " + FieldUtils.readStaticField(AbstractParser.class, "DOXIA_VERSION", true));

            System.out.println("Java version: " + System.getProperty("java.version", "<unknown java version>"));

            System.out.println("OS name: \"" + Os.OS_NAME + "\" version: \"" + Os.OS_VERSION + "\" arch: \""
                    + Os.OS_ARCH + "\" family: \"" + Os.OS_FAMILY + "\"");

        } catch (IOException | IllegalAccessException e) {
            System.err.println("Unable to determine version from JAR file: " + e.getMessage());
        }
    }

    private static void showFatalError(String message, Exception e, boolean show) {
        System.err.println("FATAL ERROR: " + message);
        if (show) {
            System.err.println("Error stacktrace:");

            e.printStackTrace();
        } else {
            System.err.println("For more information, run with the -e flag");
        }
    }
    /**
     * Start the Plexus container.
     *
     * @throws PlexusContainerException if any
     */
    private static PlexusContainer getPlexusContainer() throws PlexusContainerException {
        Map<Object, Object> context = new HashMap<>();
        context.put("basedir", new File("").getAbsolutePath());

        ContainerConfiguration containerConfiguration = new DefaultContainerConfiguration();
        containerConfiguration.setName("Doxia");
        containerConfiguration.setContext(context);
        containerConfiguration.setAutoWiring(true);
        containerConfiguration.setClassPathScanning(PlexusConstants.SCANNING_ON);

        return new DefaultPlexusContainer(containerConfiguration);
    }
}

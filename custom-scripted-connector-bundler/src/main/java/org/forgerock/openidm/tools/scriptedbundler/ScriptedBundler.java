/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2015 ForgeRock AS. All Rights Reserved
 *
 * The contents of this file are subject to the terms
 * of the Common Development and Distribution License
 * (the License). You may not use this file except in
 * compliance with the License.
 *
 * You can obtain a copy of the License at
 * http://forgerock.org/license/CDDLv1.0.html
 * See the License for the specific language governing
 * permission and limitations under the License.
 *
 * When distributing Covered Code, include this CDDL
 * Header Notice in each file and include the License file
 * at http://forgerock.org/license/CDDLv1.0.html
 * If applicable, add the following below the CDDL Header,
 * with the fields enclosed by brackets [] replaced by
 * your own identifying information:
 * "Portions Copyrighted [year] [name of copyright owner]"
 */

package org.forgerock.openidm.tools.scriptedbundler;

import org.apache.commons.cli.BasicParser;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.ParseException;
import org.codehaus.jackson.map.ObjectMapper;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;

/**
 * This is the main class for this project.  It accepts a JSON configuration file and attempts to generate a set of
 * source files using that configuration that are suitable for building a custom connector.  It is up to the caller
 * to populate the generated Groovy scripts with useful code to turn that into a functional connector.  The end result
 * may be built into an OSGi-compatible jar with 'mvn install'.
 */
public class ScriptedBundler {
    private static int SUCCESS = 0;
    private static int FAILURE = -1;

    private static Options options;

    static {
        options = new Options();
        Option file = new Option("c", "config", true, "bundle configuration file for this connector");
        file.setArgName("file");
        options.addOption(file);
        options.addOption("v", "verbose", false, "print the configuration file post-interpretation");
        options.addOption("h", "help", false, "print this help");
    }

    /**
     * Main entry point.  Use -h for a list of arguments.
     *
     * @param args
     */
    public static void main(String[] args) {
        StringBuilder str = new StringBuilder();
        str.append("Custom Scripted Connector Bundler").append(" for OpenIDM v")
                .append(ScriptedBundler.class.getPackage().getImplementationVersion());
        System.out.println(str);

        CommandLineParser parser = new BasicParser();
        CommandLine cmdline = null;
        try {
            cmdline = parser.parse(options, args);
        } catch (ParseException e) {
            printHelp();
            return;
        }
        if (cmdline == null || cmdline.hasOption("h") || !cmdline.hasOption("c")) {
            printHelp();
        } else {
            bundleConnector(cmdline.getOptionValue("c"), cmdline.hasOption("v"));
        }
    }

    private static void printHelp() {
        HelpFormatter help = new HelpFormatter();
        help.printHelp("bundle [OPTIONS] -c <FILE>", options);

        try {
            ObjectMapper mapper = new ObjectMapper();
            System.out.println("Configuration format:");
            CustomConfiguration config = new CustomConfiguration();
            config.setPackageName("MyConnector");
            config.setDisplayName("My Connector");
            config.setDescription("This is my super awesome connector");
            config.setVersion("1.0");
            config.setAuthor("Coder McLightningfingers");
            config.setProvidedProperties(Arrays.asList(
                new ProvidedProperty() {{
                    setName("provided1");
                    setValue("default");
                    setType("String");
                }},
                new ProvidedProperty() {{
                    setName("provided2");
                    setValue(2);
                    setType("Integer");
                }}));
            config.setProperties(Arrays.asList(
                new CustomProperty() {{
                    setOrder(0);
                    setType("String");
                    setName("FirstProperty");
                    setValue("firstValue");
                    setRequired(Boolean.TRUE);
                    setConfidential(Boolean.FALSE);
                    setDisplayMessage("This is my first property");
                    setHelpMessage("This should be a String value");
                    setGroup("default");
                }},
                new CustomProperty() {{
                    setOrder(1);
                    setType("Double");
                    setName("SecondProperty");
                    setValue(1.234);
                    setRequired(Boolean.FALSE);
                    setConfidential(Boolean.FALSE);
                    setDisplayMessage("This is my second property");
                    setHelpMessage("This should be a Double value");
                    setGroup("default");
                }}));
            System.out.println(mapper.writerWithDefaultPrettyPrinter().writeValueAsString(config));
        } catch (Exception e) {
            /* no user input, won't happen */
        }
    }

    private static int bundleConnector(String filename, Boolean verbose) {
        File file = new File(filename);
        if (!file.exists() || !file.canRead()) {
            System.err.println("Unable to read from " + filename + ". Please check that the " +
                    "file exists and is readable.");
            return FAILURE;
        }

        ObjectMapper mapper = new ObjectMapper();
        CustomConfiguration config = null;
        try {
            config = mapper.readValue(file, CustomConfiguration.class);
        } catch(Exception e) {
            System.err.println("Unable to parse the configuration file");
            e.printStackTrace(System.err);
            return FAILURE;
        }

        System.out.println("Generating connector sources for " + config.getDisplayName());

        try {
            if (verbose) {
                System.out.println(mapper.writerWithDefaultPrettyPrinter().writeValueAsString(config));
            }

            SourceGenerator.generateSources(config);
        } catch (IOException e) {
            System.err.println(e.getMessage());
            e.printStackTrace(System.err);
            return FAILURE;
        }

        return SUCCESS;
    }
}
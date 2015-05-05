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
package org.forgerock.openidm.maintenance;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URLDecoder;
import java.util.LinkedHashMap;
import java.util.Map;

import org.forgerock.openidm.maintenance.impl.InvalidArgsException;
import org.forgerock.openidm.maintenance.upgrade.UpgradeException;
import org.forgerock.openidm.maintenance.upgrade.UpgradeManager;

/**
 * Main class as entry point into
 * maintenance and patching mechanisms
 */
public class Main {

    public static void main(String[] args) throws InvalidArgsException, UpgradeException {
        if (args != null && args.length > 0) {
            String urlOrFile = args[0];
            String url = ensureURL(urlOrFile);
            Map<String, String> params = parseOptions(args);
        
            String installDir = optionValueStr(params, "i", deduceInstallLocation());
            String workingDir = optionValueStr(params, "w", installDir);
        
            new UpgradeManager().execute(url, new File(workingDir), new File(installDir), params);
        } else {
            printUsage(args);
        }
    }

    /**
     * Prints basic command line usage to system out
     * @param args command line args
     */
    private static void printUsage(String[] args) {
        System.out.println("Usage: <url or file path to patch> <options> ");
        System.out.println("Where options are:");
        System.out.println("-w <working dir>");
        System.out.println("-i <install dir>");
        System.out.println("<patch specific options>");
    }

    /**
     * Convert the passed file or url argument 
     * as a stringified URL 
     * 
     * @param urlOrFile the file path or url
     * @return the stringified url
     * @throws InvalidArgsException if the passed arg is not a url, and is not 
     * a valid file
     */
    private static String ensureURL(String urlOrFile) throws InvalidArgsException {
        String urlStr = null;
        // Check if it has a protocol prefix, otherwise assume file
        if (!urlOrFile.matches("\\w+\\:.*")) {
            try {
                urlStr = new File(urlOrFile).toURI().toURL().toString();
            } catch (MalformedURLException ex) {
                throw new InvalidArgsException("Passed in url or file path invalid: " 
                        + urlOrFile + ". " + ex.getMessage(), ex);
            }
        }
        return urlStr;
    }
    
    /**
     * Assumes the install location is the parent directory of the maintenance jar
     * @return the deduced install location
     */
    private static String deduceInstallLocation() throws UpgradeException {
        String installLoc = null;
        try {
            String encoded = Main.class.getProtectionDomain().getCodeSource().getLocation().getPath();
            String decoded = URLDecoder.decode(encoded, "UTF-8");
            installLoc = new File(decoded).getParentFile().getParentFile().getCanonicalPath();
        } catch (Exception ex) {
            throw new UpgradeException("Failed to deduce a product install location");
        }
        return installLoc;
    }
    
    /**
     * Parse all command line arguments into an options map
     * Assumes that options are in the form of either
     * -<option key> value
     * or
     * -<option key>
     * 
     * @param args the command line arguments
     * @return the parsed options map with option key value pairs. 
     * Options with key only have a null value 
     */
    private static Map<String, String> parseOptions(String[] args) {
        Map<String, String> params = new LinkedHashMap<String, String>();

        for (int count = 1; count < args.length; count++) {
            String key = args[count];
            if (key != null && key.startsWith("-")) {
                key = key.substring(1);
                String value = null;
                int nextArgIdx = count + 1;
                if (nextArgIdx < args.length) {
                    String nextArg = args[nextArgIdx];
                    if (nextArg != null && !nextArg.startsWith("-")) {
                        count++;
                        value = nextArg;
                    } 
                }
                params.put(key, value);
            }
        }
        return params;
    }

    /**
     * Gets the String value (can be null) of an option 
     * @param params all command line parameters
     * @param key the option key
     * @param defaultValue the default value to assign if the option is not set. 
     * if the option is set, but is set to null, the default value is not used
     * @return the value if the entry is present, or the default value if it is not
     * @throws InvalidArgsException if the option is not of String type
     */
    private static String optionValueStr(Map<String, String> params, String key, String defaultValue)
            throws InvalidArgsException {
        Object value = optionValue(params, key, defaultValue);
        if (value == null) {
            return null;
        } else if (value instanceof String) {
            return (String) value;
        } else {
            throw new InvalidArgsException("The value for " + key + " is invalid, expected String");
        }
    }

    /**
     * Gets the value (can be null) of an option 
     * @param params all command line parameters
     * @param key the option key
     * @param defaultValue the default value to assign if the option is not set. 
     * if the option is set, but is set to null, the default value is not used
     * @return the value if the entry is present, or the default value if it is not
     */
    private static String optionValue(Map<String, String> params, String key, String defaultValue) {
        if (params.containsKey(key)) {
            return params.get(key);
        } else {
            return defaultValue;
        }
    }
}

/*
 * The contents of this file are subject to the terms of the Common Development and
 * Distribution License (the License). You may not use this file except in compliance with the
 * License.
 *
 * You can obtain a copy of the License at legal/CDDLv1.0.txt. See the License for the
 * specific language governing permission and limitations under the License.
 *
 * When distributing Covered Software, include this CDDL Header Notice in each file and include
 * the License file at legal/CDDLv1.0.txt. If applicable, add the following below the CDDL
 * Header, with the fields enclosed by brackets [] replaced by your own identifying
 * information: "Portions copyright [year] [name of copyright owner]".
 *
 * Copyright 2014 ForgeRock AS.
 */
package org.forgerock.openidm.patch;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.logging.ConsoleHandler;
import java.util.logging.FileHandler;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.PropertiesConfiguration;

import org.forgerock.openidm.patch.exception.PatchException;

import static org.forgerock.openidm.patch.utils.PatchConstants.*;
import org.forgerock.openidm.patch.utils.SingleLineFormatter;

/**
 * The Main class for the patch framework.
 */
public final class Main {

    private final static PropertiesConfiguration CONFIG = new PropertiesConfiguration();
    private final static Logger logger = Logger.getLogger("PatchLog");
    private final static Logger historyLogger = Logger.getLogger("HistoryLog");

    private static final String PATCH_DIR = "patch";
    private static final String PATCH_ARCHIVE_DIR = PATCH_DIR + File.separator + "archive";
    private static final String PATCH_HISTORY_FILE = PATCH_DIR + File.separator + "history.log";
    private static final String PATCH_LOG_FILE = "patch.log";

    private Main() {
        // Prevent instantiation.
    }

    /**
     * Execute the patch bundle.
     *
     * @param args Expects args[0] to contain the target installation directory
     * to be patched. Optionally takes a '-w' parameter specifying the working directory.
     * @throws PatchException Thrown if the patch fails to apply correctly
     */
    public static void main(String[] args) throws PatchException {
        if (args != null && args.length > 0) {
            String installDir = args[0];
            Map<String, Object> params = parseOptions(args);

            String workingDir = optionValueStr(params, "w", installDir);
            File w = new File(workingDir);
            File i = new File(installDir);
            storePatchBundle(w, i, params);
            try {
                URL url = getPatchLocation();
                execute(url, url.toString(), w, i, params);
            } catch (IOException ex) {
                Logger.getLogger(Main.class.getName()).log(Level.SEVERE, null, ex);
            }
        } else {
            printUsage(args);
        }
    }

    /**
     * Executes the specified patch bundle.
     *
     * @param patchUrl          A URL specifying the location of the patch bundle
     * @param originalUrlString The String representation of the patch bundle location
     * @param workingDir        The working directory in which store logs and temporary files
     * @param installDir        The target directory against which the patch is to be applied
     * @param params            Additional patch specific parameters
     * @throws IOException      Thrown in the event of a failure creating the patch archive
     *                          or log files
     */
    public static void execute(URL patchUrl, String originalUrlString, File workingDir,
            File installDir, Map<String, Object> params) throws IOException {

        try {
            // Load the base patch configuration
            InputStream in = CONFIG.getClass().getResourceAsStream(CONFIG_PROPERTIES_FILE);
            if (in == null) {
                throw new PatchException("Unable to locate: " + CONFIG_PROPERTIES_FILE + " in: " + patchUrl.toString());
            } else {
                CONFIG.load(in);
            }

            // Configure logging and disable parent handlers
            SingleLineFormatter formatter = new SingleLineFormatter();
            Handler historyHandler = new FileHandler(workingDir + File.separator + PATCH_HISTORY_FILE, true);
            Handler consoleHandler = new ConsoleHandler();
            consoleHandler.setFormatter(formatter);
            historyHandler.setFormatter(formatter);
            historyLogger.setUseParentHandlers(false);
            historyLogger.addHandler(consoleHandler);
            historyLogger.addHandler(historyHandler);

            // Initialize the Archive
            Archive archive = Archive.getInstance();
            archive.initialize(installDir, new File(workingDir, PATCH_ARCHIVE_DIR),
                    CONFIG.getString(PATCH_BACKUP_ARCHIVE));

            // Create the patch logger once we've got the archive directory
            Handler logHandler = new FileHandler(
                    archive.getArchiveDirectory() + File.separator + PATCH_LOG_FILE, false);
            logHandler.setFormatter(formatter);
            logger.setUseParentHandlers(false);
            logger.addHandler(logHandler);

            // Instantiate the patcgh implementation and invoke the patch
            Patch patch = instantiatePatch();
            patch.initialize(patchUrl, originalUrlString, workingDir, installDir, params);
            historyLogger.log(Level.INFO, "Applying {0}, version={1}", new Object[]{
                CONFIG.getProperty(PATCH_DESCRIPTION), CONFIG.getProperty(PATCH_RELEASE)});
            historyLogger.log(Level.INFO, "Target: {0}, Source: {1}", new Object[]{installDir, patchUrl});
            patch.apply();

            historyLogger.log(Level.INFO, "Completed");
        } catch (PatchException pex) {
            historyLogger.log(Level.SEVERE, "Failed", pex);
        } catch (ConfigurationException ex) {
            historyLogger.log(Level.SEVERE, "Failed to load patch configuration", ex);
        } finally {
            try {
                Archive.getInstance().close();
            } catch (IOException ex) {
                historyLogger.log(Level.SEVERE, "Failed to close patch archive", ex);
            }
        }
    }

    private static URL getPatchLocation() {
        return Main.class.getProtectionDomain().getCodeSource().getLocation();
    }

    private static Patch instantiatePatch() throws PatchException {
        Object obj = null;
        try {
            String patchClass = CONFIG.getString(CONFIG_PATCH_IMPL_CLASS);
            if (patchClass == null) {
                throw new PatchException("Invalid configuration, " + CONFIG_PATCH_IMPL_CLASS + " not specified.");
            }
            Class c = Class.forName(patchClass);
            obj = c.newInstance();
        } catch (SecurityException ex) {
            logger.log(Level.SEVERE, null, ex);
            throw new PatchException(ex.getMessage(), ex);
        } catch (InstantiationException ex) {
            logger.log(Level.SEVERE, null, ex);
            throw new PatchException(ex.getMessage(), ex);
        } catch (IllegalAccessException ex) {
            logger.log(Level.SEVERE, null, ex);
            throw new PatchException(ex.getMessage(), ex);
        } catch (ClassNotFoundException ex) {
            Logger.getLogger(Main.class.getName()).log(Level.SEVERE, null, ex);
        }
        return (Patch) obj;
    }

    private static void storePatchBundle(File workingDir, File installDir,
            Map<String, Object> params) throws PatchException {

        URL url = getPatchLocation();

        // Download the patch file
        ReadableByteChannel channel = null;
        try {
            channel = Channels.newChannel(url.openStream());
        } catch (IOException ex) {
            throw new PatchException("Failed to access the specified file "
                    + url + " " + ex.getMessage(), ex);
        }

        String targetFileName = new File(url.getPath()).getName();
        File patchDir = new File(workingDir, "patch/bin");
        patchDir.mkdirs();
        File targetFile = new File(patchDir, targetFileName);
        FileOutputStream fos = null;
        try {
            fos = new FileOutputStream(targetFile);
        } catch (FileNotFoundException ex) {
            throw new PatchException("Error in getting the specified file to "
                    + targetFile, ex);
        }

        try {
            fos.getChannel().transferFrom(channel, 0, Long.MAX_VALUE);
            System.out.println("Downloaded to " + targetFile);
        } catch (IOException ex) {
            throw new PatchException("Failed to get the specified file "
                    + url + " to: " + targetFile, ex);
        }
    }

    /**
     * Prints basic command line usage to system out.
     *
     * @param args command line args
     */
    private static void printUsage(String[] args) {
        System.out.println("Usage: <install dir> <options>");
        System.out.println("Where options are:");
        System.out.println("-w <working dir>");
        System.out.println("<patch specific options>");
    }

    /**
     * Parse all command line arguments into an options map Assumes that options
     * are in the form of either -<option key> value or -<option key>.
     *
     * @param args the command line arguments
     * @return the parsed options map with option key value pairs. Options with
     * key only have a null value
     */
    private static Map<String, Object> parseOptions(String[] args) {
        Map<String, Object> params = new LinkedHashMap<String, Object>();

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
     * Gets the String value (can be null) of an option.
     *
     * @param params all command line parameters
     * @param key the option key
     * @param defaultValue the default value to assign if the option is not set.
     * if the option is set, but is set to null, the default value is not used
     * @return the value if the entry is present, or the default value if it is
     * not
     * @throws InvalidArgsException if the option is not of String type
     */
    private static String optionValueStr(Map<String, Object> params, String key, String defaultValue) {
        Object value = optionValue(params, key, defaultValue);
        if (value == null) {
            return null;
        } else if (value instanceof String) {
            return (String) value;
        } else {
            throw new IllegalArgumentException("The value for " + key + " is invalid, expected String");
        }
    }

    /**
     * Gets the value (can be null) of an option.
     *
     * @param params all command line parameters
     * @param key the option key
     * @param defaultValue the default value to assign if the option is not set.
     * if the option is set, but is set to null, the default value is not used
     * @return the value if the entry is present, or the default value if it is
     * not
     */
    private static Object optionValue(Map<String, Object> params, String key, Object defaultValue) {
        if (params.containsKey(key)) {
            return params.get(key);
        } else {
            return defaultValue;
        }
    }
}

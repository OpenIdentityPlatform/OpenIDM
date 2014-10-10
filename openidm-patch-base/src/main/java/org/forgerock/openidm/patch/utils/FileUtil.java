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

package org.forgerock.openidm.patch.utils;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.nio.channels.FileChannel;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.forgerock.openidm.patch.exception.PatchException;

/**
 * Collection of File based utilities available to Patches.
 */
public class FileUtil {

    private final static Logger logger = Logger.getLogger("PatchLog");

    /**
     * Delete a file or directory and optionally recurse into subdirectories.
     *
     * @param file the file or directory to delete
     * @param recurse whether to recursively delete the contents of subdirectories
     */
    public void delete(File file, boolean recurse) {
        if (file.exists()) {
            if (recurse) {
                File[] files = file.listFiles();
                if (files != null) {
                    for (File file1 : files) {
                        if (file1.isFile()) {
                            delete(file1, false);
                        } else if (recurse) {
                            delete(file1, true);
                        }
                    }
                    file.delete();
                    logger.log(Level.INFO, "Removed {0}", file);
                }
            } else {
                if (file.canWrite() && file.delete()) {
                    logger.log(Level.INFO, "Deleted {0}", file);
                } else {
                    logger.log(Level.INFO, "Unable to delete {0}", file);
                }
            }
        }
    }

    /**
     * Find a file in a given directory with the specified regex pattern.
     *
     * @param fileDir the directory to search
     * @param fileNameRegex the (Java String matches compatible) regex pattern
     * @return the file if found, or null if not found
     * @throws RuntimeException if more than one match was found
     */
    public static File find(final File fileDir, final String fileNameRegex) {
        File[] found = fileDir.listFiles(new FilenameFilter() {
            @Override
            public boolean accept(File dir, String name) {
                return name.matches(fileNameRegex);
            }
        });
        if (found != null && found.length != 0) {
            if (found.length == 1) {
                return found[0];
            } else {
                // TODO better exception
                throw new RuntimeException("More than one matching file found in "
                        + fileDir + " for " + fileNameRegex);
            }
        } else {
            return null;
        }
    }

    /**
     * Copy a file to a new file/location.
     *
     * @param sourceFile the file to be copied
     * @param targetFile the target file to be copied to
     * @throws IOException if an IO error occurs while copying the file
     * @throws PatchException if the target file already exists
     */
    public static void copy(File sourceFile, File targetFile) throws IOException, PatchException {
        FileChannel inChannel = null;
        FileChannel outChannel = null;
        boolean success = false;

        // Don't allow overwriting files. Should do a delete first in order
        // to ensure the file is archived.
        if (targetFile.exists()) {
            throw new PatchException("Unable to copy file " + sourceFile + ", targetFile "
                    + targetFile + " already exists!");
        }

        try {
            inChannel = new FileInputStream(sourceFile).getChannel();
            outChannel = new FileOutputStream(targetFile).getChannel();
            outChannel.transferFrom(inChannel, 0, inChannel.size());
            success = true;
        } finally {
            if (inChannel != null) {
                inChannel.close();
            }
            if (outChannel != null) {
                outChannel.close();
            }
        }
        logger.log(Level.INFO, "Copy {0} -> {1}: success: {2}", new Object[]{sourceFile, targetFile, success});

    }

    /**
     * Utility method to return the relative path to a file give a specified
     * base path.
     *
     * @param base base directory
     * @param path path to file for which a relative path to the base is required
     * @return the relative path to the file
     */
    public static String constructRelativePath(File base, File path) {
        return base.toURI().relativize(path.toURI()).getPath();
    }
}

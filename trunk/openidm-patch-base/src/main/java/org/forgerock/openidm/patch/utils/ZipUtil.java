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
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.ByteBuffer;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.WritableByteChannel;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import org.forgerock.openidm.patch.Archive;

/**
 * Zip handling utility.
 */
public class ZipUtil {

    private final Archive archive = Archive.getInstance();

    private final static Logger logger = Logger.getLogger("PatchLog");

    /**
     * Extract the contents of the specified folder from the given zip file URL.
     * Place the extracted files into the specified target directory.
     *
     * @param zipUrl The zip file URL
     * @param zipDirToExtract The directory to extract from the zip file
     * @param targetDir The directory to which the files will be extracted
     * @throws IOException if operating on the zip file fails
     */
    public void extractFolder(URL zipUrl, final String zipDirToExtract, final File targetDir) throws IOException {
        ZipVisitor visitor = new ZipVisitor() {
            @Override
            public boolean visitEntry(ZipEntry zipEntry, ZipInputStream zipInputStream)
                    throws IOException {
                String entryFileName = zipEntry.getName();
                logger.log(Level.FINE, "Process entry name: {0}", entryFileName);

                // Normalize to a File representation to compare directories
                String zipDirPath = new File(zipDirToExtract).getPath() + File.separator;
                if (new File(entryFileName).getPath().startsWith(zipDirPath)) {
                    String targetFileName = entryFileName.substring(zipDirToExtract.length());
                    File targetFile = new File(targetDir, targetFileName);
                    logger.log(Level.FINE, "Calculated target file:{0}", targetFile);
                    extractEntryToDirOrFile(zipEntry, zipInputStream, targetFile);
                } else {
                    logger.log(Level.FINER, "Ignored {0}", zipEntry);
                }
                return false;
            }
        };
        visitZip(zipUrl, visitor);
    }

    /**
     * Extract the specified file from the given zip file URL.
     * Write the extracted file to the specified target file.
     *
     * @param zipUrl The zip file URL
     * @param fileToExtract The file to extract from the zip file
     * @param targetFile The file to which the extracted file will be written
     * @throws IOException if operating on the zip file fails
     */
    public void extractFile(URL zipUrl, final File fileToExtract, final File targetFile) throws IOException {
        ZipVisitor visitor = new ZipVisitor() {
            @Override
            public boolean visitEntry(ZipEntry zipEntry, ZipInputStream zipInputStream)
                    throws IOException {
                String entryFileName = zipEntry.getName();
                logger.log(Level.FINE, "Process entry name: {0}", entryFileName);
                File entryFile = new File(entryFileName);

                if (entryFile.equals(fileToExtract)) {
                    extractEntryToDirOrFile(zipEntry, zipInputStream, targetFile);
                    return true; // Stop visiting, we found the file
                } else {
                    logger.log(Level.FINER, "Ignored {0}", zipEntry);
                    return false;
                }
            }
        };
        visitZip(zipUrl, visitor);
    }

    private void extractEntryToDirOrFile(ZipEntry zipEntry, ZipInputStream zipInputStream, File targetFile)
            throws IOException {
        if (zipEntry.isDirectory()) {
            targetFile.mkdirs();
        } else {
            archive.insert(targetFile);
            targetFile.getParentFile().mkdirs();
            extractEntryToFile(zipInputStream, targetFile);
        }
    }

    private void extractEntryToFile(InputStream inputStream, File targetFile) throws IOException {
        int bufferSize = 1024 * 128;

        FileOutputStream fileOutputStream = new FileOutputStream(targetFile);
        ReadableByteChannel inChannel = Channels.newChannel(inputStream);
        WritableByteChannel outChannel = Channels.newChannel(fileOutputStream);

        ByteBuffer buffer = ByteBuffer.allocate(bufferSize);
        while (inChannel.read(buffer) != -1) {
            buffer.flip();
            outChannel.write(buffer);
            buffer.clear();
        }

        fileOutputStream.close();
        logger.log(Level.INFO, "Extracted {0}", targetFile.getAbsolutePath());
    }

    /**
     * Go through the entries of a zip.
     * and let the supplied visitor act upon each entry
     * @param zipUrl The zip file url
     * @param visitor the visitor to invoke for each entry
     * @throws IOException if operating on the zip file fails
     */
    public void visitZip(URL zipUrl, ZipVisitor visitor) throws IOException {

        InputStream inputStream = zipUrl.openStream();
        ZipInputStream zipInputStream = new ZipInputStream(inputStream);

        ZipEntry zipEntry = zipInputStream.getNextEntry();

        boolean abort = false;
        while (zipEntry != null && !abort) {
            abort = visitor.visitEntry(zipEntry, zipInputStream);
            zipEntry = zipInputStream.getNextEntry();
        }

        zipInputStream.closeEntry();
        zipInputStream.close();
    }
}

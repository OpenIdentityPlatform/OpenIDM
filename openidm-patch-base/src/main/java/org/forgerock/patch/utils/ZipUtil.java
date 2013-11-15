/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2013 ForgeRock AS. All Rights Reserved
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

package org.forgerock.patch.utils;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.ByteBuffer;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.WritableByteChannel;
import java.util.logging.Logger;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import org.forgerock.patch.Archive;

/**
 * Zip handling utility
 *
 * @author aegloff
 */
public class ZipUtil {
    
    private Archive archive = Archive.getInstance();
    
    private final static Logger logger = Logger.getLogger("PatchLog");
    
    public void extractFolder(URL zipUrl, final String zipDirToExtract, final File targetDir) throws IOException {
        ZipVisitor visitor = new ZipVisitor() {
            @Override
            public boolean visitEntry(ZipEntry zipEntry, ZipInputStream zipInputStream) 
                    throws IOException {
                String entryFileName = zipEntry.getName();
                logger.fine("Process entry name: " + entryFileName);

               // Normalize to a File representation to compare directories
               String zipDirPath = new File(zipDirToExtract).getPath() + File.separator;
               if (new File(entryFileName).getPath().startsWith(zipDirPath)) {
                   String targetFileName = entryFileName.substring(zipDirToExtract.length());
                   File targetFile = new File(targetDir, targetFileName);
                   logger.fine("Calculated target file:" + targetFile);
                   extractEntryToDirOrFile(zipEntry, zipInputStream, targetFile);
               } else {
                  logger.finer("Ignored " + zipEntry);
               }
               return false;         
            }
        };
        visitZip(zipUrl, visitor);
     }
    
    public void extractFile(URL zipUrl, final File fileToExtract, final File targetFile) throws IOException {
        ZipVisitor visitor = new ZipVisitor() {
            @Override
            public boolean visitEntry(ZipEntry zipEntry, ZipInputStream zipInputStream) 
                    throws IOException {
                String entryFileName = zipEntry.getName();
                logger.fine("Process entry name: " + entryFileName);
                File entryFile = new File(entryFileName);

               if (entryFile.equals(fileToExtract)) {
                   extractEntryToDirOrFile(zipEntry, zipInputStream, targetFile);
                   return true; // Stop visiting, we found the file
               } else {
                  logger.finer("Ignored " + zipEntry);
                  return false;
               }
            }
        };
        visitZip(zipUrl, visitor);
     }
    
    private void extractEntryToDirOrFile(ZipEntry zipEntry, ZipInputStream zipInputStream, File targetFile) throws IOException {
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
        logger.info("Extracted " + targetFile.getAbsolutePath());
    }
    
    /**
     * Go through the entries of a zip, 
     * and let the supplied visitor act upon each entry
     * @param zipUrl The zip file url
     * @param visitor the visitor to invoke for each entry
     * @throws IOException if operating on the zip file fails
     */
    void visitZip(URL zipUrl, ZipVisitor visitor) throws IOException {

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
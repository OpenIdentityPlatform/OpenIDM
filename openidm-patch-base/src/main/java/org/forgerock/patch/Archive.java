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
package org.forgerock.patch;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashSet;
import java.util.jar.JarOutputStream;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.zip.ZipEntry;
import java.util.zip.ZipException;
import org.forgerock.patch.utils.FileUtil;

/**
 *
 * @author cgdrake
 */
public class Archive {

    private JarOutputStream jos = null;
    private HashSet dirEntries = new HashSet();

    private static Archive instance = null;
    private File archiveDir = null;
    private File installDir = null;
    
    private final static Logger logger = Logger.getLogger("PatchLog");
    
    public final static String STATIC_FILES_ENTRY = "files";

    
    public void initialize(File installDir, File workingDir, String archiveName) throws FileNotFoundException, IOException {
        DateFormat dateFormat = new SimpleDateFormat("yyyyMMdd_HHmmss");
        Date date = new Date();
        archiveDir = new File(workingDir, dateFormat.format(date));
        if (archiveDir.mkdirs()) {
            logger.log(Level.INFO, "Created patch archive directory: {0}", archiveDir);
        }
        this.installDir = installDir;
        
        if (archiveName == null || archiveName.isEmpty())
            archiveName = "backup.jar";
        
        FileOutputStream outputStream;
        outputStream = new FileOutputStream(new File(archiveDir, archiveName));
        jos = new JarOutputStream(outputStream);
    }
    
    public void insert(File file) {
        if (!file.exists() || !file.canRead()) {
            logger.log(Level.FINE, "File does not exist or could not be read during backup: {0}", file);
        } else {
            try {
                String relativePath = FileUtil.constructRelativePath(installDir, file);
                addStaticFile(file, relativePath);
            } catch (ZipException ex) {
                logger.log(Level.WARNING, "Unable to archive {0}", file);
            } catch (IOException ex) {
                logger.log(Level.SEVERE, "Failed to archive {0}", file);
            }
        }
    }

    public File getArchiveDirectory() {
        return archiveDir;
    }
    
    private void addStaticFile(File fileToCompress, String targetEntry) throws IOException, ZipException {
        File entry = new File(STATIC_FILES_ENTRY, targetEntry);
        addParentDirectories(entry.getParentFile());
        addFile(entry, fileToCompress);
    }

    private void addParentDirectories(File targetPath) throws IOException, ZipException {
        File parent = targetPath.getParentFile();      
        if (parent != null) {
            addParentDirectories(parent);
        }
        
        if (!dirEntries.contains(targetPath.getPath())) {
            jos.putNextEntry(new ZipEntry(targetPath.getPath() + "/"));
            jos.closeEntry();
            dirEntries.add(targetPath.getPath());
        }
    }
    
    private void addFile(File targetEntry, File file) throws IOException, ZipException {
        FileInputStream fis = new FileInputStream(file);
        BufferedInputStream in = new BufferedInputStream(fis);
        try {
            ZipEntry entry = new ZipEntry(targetEntry.getPath());
            jos.putNextEntry(entry);

            byte[] buffer = new byte[1024];
            while (true) {
                int count = in.read(buffer);
                if (count == -1)
                    break;
                jos.write(buffer, 0, count);
            }
            jos.closeEntry();
            logger.log(Level.INFO, "Added {0} to archive.", targetEntry);
        } finally {
            in.close();
            fis.close();
        }
    }
    
    public void close() throws IOException {
        if (jos != null) {
            jos.close();
        }
    }
    
    public static Archive getInstance() {
        if (instance == null) {
            instance = new Archive();
        }
        return instance;
    }
}

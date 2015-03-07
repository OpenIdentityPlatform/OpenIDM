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
import java.util.Set;
import java.util.jar.JarOutputStream;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.zip.ZipEntry;
import java.util.zip.ZipException;
import org.forgerock.openidm.patch.utils.FileUtil;

/**
 * Provides basic patch infrastructure for generating OpenIDM patch bundles.
 */
public class Archive {

    private JarOutputStream jos = null;
    private final Set dirEntries;

    private static Archive instance = null;
    private File archiveDir = null;
    private File installDir = null;

    private final static Logger logger = Logger.getLogger("PatchLog");

    /**
     * Location of the static files within the patch JAR archive.
     */
    public final static String STATIC_FILES_ENTRY = "files";

    /**
     * Archive constructor.
     */
    public Archive() {
        this.dirEntries = new HashSet();
    }

    /**
     * Initializes the patch and creates the patch archive bundle.
     *
     * @param installDir The target directory against which the patch is to be applied.
     * @param workingDir The working directory in which to store the archive bundle.
     * @param archiveName The named of the patch archive bundle.
     * @throws FileNotFoundException If the installDir or workingDir do not exist.
     * @throws IOException If an exception occurs while performing a I/O operation.
     */
    public void initialize(File installDir, File workingDir, String archiveName)
            throws FileNotFoundException, IOException {
        DateFormat dateFormat = new SimpleDateFormat("yyyyMMdd_HHmmss");
        Date date = new Date();
        archiveDir = new File(workingDir, dateFormat.format(date));
        if (archiveDir.mkdirs()) {
            logger.log(Level.INFO, "Created patch archive directory: {0}", archiveDir);
        }
        this.installDir = installDir;

        if (archiveName == null || archiveName.isEmpty()) {
            archiveName = "backup.jar";
        }

        FileOutputStream outputStream;
        outputStream = new FileOutputStream(new File(archiveDir, archiveName));
        jos = new JarOutputStream(outputStream);
    }

    /**
     * Insert a file within the patch archive bundle.
     *
     * @param file The {@link File} to be inserted.
     */
    public void insert(File file) {
        if (!file.exists() || !file.canRead()) {
            logger.log(Level.FINE, "File does not exist or could not be read during backup: {0}", file);
        } else {
            try {
                String relativePath = FileUtil.constructRelativePath(installDir, file);
                if (file.isFile()) {
                    addStaticFile(file, relativePath);
                } else if (file.isDirectory()) {
                    addDirectory(file, relativePath, true);
                }
            } catch (ZipException ex) {
                logger.log(Level.WARNING, "Unable to archive {0}", file);
            } catch (IOException ex) {
                logger.log(Level.SEVERE, "Failed to archive {0}", file);
            }
        }
    }

    /**
     * Get the directory in which the archive bundle resides.
     *
     * @return The archive directory.
     */
    public File getArchiveDirectory() {
        return archiveDir;
    }

    private void addDirectory(File dirToCompress, String targetEntry, Boolean recurse)
            throws IOException, ZipException {
        File [] files = dirToCompress.listFiles();
        for (int i = 0; i < files.length; i++) {
            if (files[i].isDirectory() && recurse) {
                addDirectory(files[i], targetEntry + File.separator + files[i].getName(), true);
            } else {
                addStaticFile(files[i], targetEntry + File.separator + files[i].getName());
            }
        }
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
        addDirectoryEntry(targetPath);
    }

    private void addDirectoryEntry(File targetPath) throws IOException, ZipException {
        if (!dirEntries.contains(targetPath.getPath())) {
            jos.putNextEntry(new ZipEntry(targetPath.getPath() + "/"));
            jos.closeEntry();
            dirEntries.add(targetPath.getPath());
        }
    }

    private void addFile(File targetEntry, File file) throws IOException, ZipException {
        if (file.isDirectory()) {
            addDirectoryEntry(targetEntry);
        } else if (file.isFile()) {
            FileInputStream fis = new FileInputStream(file);
            BufferedInputStream in = new BufferedInputStream(fis);
            try {
                ZipEntry entry = new ZipEntry(targetEntry.getPath());
                jos.putNextEntry(entry);

                byte[] buffer = new byte[1024];
                while (true) {
                    int count = in.read(buffer);
                    if (count == -1) {
                        break;
                    }
                    jos.write(buffer, 0, count);
                }
                jos.closeEntry();
                logger.log(Level.INFO, "Added {0} to archive.", targetEntry);
            } finally {
                in.close();
                fis.close();
            }
        }
    }

    /**
     * Close the archive and associated resources.
     *
     * @throws IOException If a failure occurs while releasing any associated resources.
     */
    public void close() throws IOException {
        if (jos != null) {
            jos.close();
        }
    }

    /**
     * Get an instance of the patch archive bundle.
     *
     * @return An instance of the {@link Archive}.
     */
    public static Archive getInstance() {
        if (instance == null) {
            instance = new Archive();
        }
        return instance;
    }
}

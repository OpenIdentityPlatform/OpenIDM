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

package org.forgerock.openidm.maintenance.upgrade;

import static org.forgerock.json.JsonValue.array;
import static org.forgerock.json.JsonValue.field;
import static org.forgerock.json.JsonValue.json;
import static org.forgerock.json.JsonValue.object;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Properties;
import java.util.Set;
import java.util.jar.Attributes;
import java.util.jar.JarInputStream;

import difflib.DiffUtils;
import difflib.Patch;
import net.lingala.zip4j.core.ZipFile;
import net.lingala.zip4j.exception.ZipException;
import org.apache.commons.io.FileUtils;
import org.forgerock.json.JsonValue;
import org.forgerock.openidm.core.ServerConstants;
import org.forgerock.openidm.util.FileUtil;
import org.forgerock.util.Function;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Basic manager to initiate the product maintenance and upgrade mechanisms.
 */
public class UpgradeManager {

    private final static Logger logger = LoggerFactory.getLogger(UpgradeManager.class);

    private static final Path CHECKSUMS_FILE = Paths.get(".checksums.csv");
    private static final Path BUNDLE_PATH = Paths.get("bundle");
    private static final Path CONF_PATH = Paths.get("conf");
    private static final String JSON_EXT = ".json";
    private static final String PATCH_EXT = ".patch";
    private static final Path ARCHIVE_PATH = Paths.get("bin/update");

    // Archive manifest keys
    private static final String PROP_PRODUCT = "product";
    private static final String PROP_VERSION = "version";
    private static final String PROP_UPGRADESPRODUCT = "upgradesProduct";
    private static final String PROP_UPGRADESVERSION = "upgradesVersion";
    private static final String PROP_DESCRIPTION = "description";
    private static final String PROP_RESOURCE = "resource";
    private static final String PROP_RESTARTREQUIRED = "restartRequired";

    static <R, E extends Exception> R withInputStreamForPath(Path path, Function<InputStream, R, E> function)
            throws E, IOException {
        try (final InputStream is = Files.newInputStream(path.normalize())) {
            return function.apply(is);
        }
    }

    private class ZipArchive implements Archive {
        private final Path upgradeRoot;
        private final Set<Path> filePaths;
        private final ProductVersion version;

        ZipArchive(Path zipFilePath, Path destination) throws ArchiveException {
            upgradeRoot = destination.resolve("openidm");

            // unzip the upgrade dist
            try {
                ZipFile zipFile = new ZipFile(zipFilePath.toString());
                if (zipFile.isEncrypted()) {
                    throw new UnsupportedOperationException("Encrypted zip files are not supported");
                }
                zipFile.extractAll(destination.toString());
            } catch (ZipException e) {
                throw new ArchiveException("Can't read archive file: " + zipFilePath.toAbsolutePath(), e);
            }

            // get the file set from the upgrade dist checksum file
            try {
                filePaths = new ChecksumFile(upgradeRoot.resolve(CHECKSUMS_FILE)).getFilePaths();
            } catch (Exception e) {
                throw new ArchiveException("Archive doesn't appear to contain checksums file - invalid archive?", e);
            }

            // get the version from the embedded ServerConstants
            try {
                final URL targetUrl = upgradeRoot
                        .resolve(BUNDLE_PATH)
                        .resolve(zipFilePath
                                .getFileName()
                                .toString()
                                .replaceAll("^openidm-(.*).zip$", "openidm-system-$1.jar"))
                        .toFile()
                        .toURI()
                        .toURL();
                try (final URLClassLoader loader = new URLClassLoader(new URL[] { targetUrl })) {
                    final Class<?> c = loader.loadClass("org.forgerock.openidm.core.ServerConstants");

                    final Method getVersion = c.getMethod("getVersion");
                    final Method getRevision = c.getMethod("getRevision");

                    version = new ProductVersion(
                            String.valueOf(getVersion.invoke(null)),
                            String.valueOf(getRevision.invoke(null)));
                }
            } catch (IOException
                    | ClassNotFoundException
                    | IllegalAccessException
                    | InvocationTargetException
                    | NoSuchMethodException e) {
                throw new ArchiveException("Can't determine product version from upgrade archive", e);
            }

            System.out.println("Upgrading to " + version);
        }

        @Override
        public ProductVersion getVersion() {
            return version;
        }

        @Override
        public Set<Path> getFiles() {
            return filePaths;
        }

        @Override
        public <R, E extends Exception> R withInputStreamForPath(Path path, Function<InputStream, R, E> function)
                throws E, IOException {
            return UpgradeManager.withInputStreamForPath(upgradeRoot.resolve(path), function);
        }
    }

    private <R> R withTempDirectory(String tempDirectoryPrefix, Function<Path, R, UpgradeException> func)
            throws UpgradeException {

        Path tempUnzipDir = null;
        try {
            tempUnzipDir = Files.createTempDirectory(tempDirectoryPrefix);
            return func.apply(tempUnzipDir);
        } catch (IOException e) {
            throw new UpgradeException("Cannot create temporary directory to unzip archive");
        } finally {
            try {
                if (tempUnzipDir != null) {
                    FileUtils.deleteDirectory(tempUnzipDir.toFile());
                }
            } catch (IOException e) {
                logger.error("Could not remove temporary directory: " + tempUnzipDir.toString(), e);
            }
        }
    }

    /**
     * An interface for upgrade actions.
     *
     * @param <R> the return type (often JsonValue)
     */
    private interface UpgradeAction<R> {
        R invoke(Archive archive, FileStateChecker fileStateChecker) throws UpgradeException;
    }

    /**
     * Invoke an {@link UpgradeAction} using an archive at a given URL.  Use {@code installDir} as the location
     * of the currently-installed OpenIDM.
     *
     * @param archiveFile the {@link Path} to a ZIP archive containing a new version of OpenIDM
     * @param installDir the {@link Path} of the currently-installed OpenIDM
     * @param upgradeAction the upgrade action to perform
     * @param <R> The return type of the action
     * @return the result of the upgrade action
     * @throws UpgradeException on failure to perform the upgrade action
     */
    private <R> R usingArchive(final Path archiveFile, final Path installDir, final UpgradeAction<R> upgradeAction)
            throws UpgradeException {

        return withTempDirectory("openidm-upgrade-",
                new Function<Path, R, UpgradeException>() {
                    @Override
                    public R apply(Path tempUnzipDir) throws UpgradeException {
                        try {
                            final ZipArchive archive = new ZipArchive(archiveFile, tempUnzipDir);
                            final ChecksumFile checksumFile = new ChecksumFile(installDir.resolve(CHECKSUMS_FILE));
                            final FileStateChecker fileStateChecker = new FileStateChecker(checksumFile);

                            return upgradeAction.invoke(archive, fileStateChecker);
                        } catch (Exception e) {
                            throw new UpgradeException(e.getMessage(), e);
                        }
                    }
                });
    }

    /**
     * Provide a report of which files have been changed since they were installed.
     *
     * @param archiveFile the {@link Path} to a ZIP archive containing a new version of OpenIDM
     * @param installDir the base directory where OpenIDM is installed
     * @return a json response listed changed files by state
     * @throws UpgradeException
     */
    public JsonValue report(final Path archiveFile, final Path installDir)
            throws UpgradeException {

        return usingArchive(archiveFile, installDir,
                new UpgradeAction<JsonValue>() {
                    @Override
                    public JsonValue invoke(Archive archive, FileStateChecker fileStateChecker)
                            throws UpgradeException {

                        final List<Object> result = array();
                        for (Path path : archive.getFiles()) {
                            try {
                                FileState state = fileStateChecker.getCurrentFileState(path);
                                if (!FileState.UNCHANGED.equals(state)) {
                                    result.add(object(
                                            field("filePath", path.toString()),
                                            field("fileState", state.toString())
                                    ));
                                }
                            } catch (IOException e) {
                                throw new UpgradeException("Unable to determine file state for " + path.toString(), e);
                            }
                        }

                        return json(result);
                    }
                });
    }

    /**
     * Return the diff of a single file to show what changes will be made if we overwrite the existing file.
     *
     * @param archiveFile the {@link Path} to a ZIP archive containing a new version of OpenIDM
     * @param installDir the base directory where OpenIDM is installed
     * @param filename the file to diff
     * @return a json response showing the current file, the new file, and the diff
     * @throws UpgradeException on failure to perform diff
     */
    public JsonValue diff(final Path archiveFile, final Path installDir, final String filename) throws UpgradeException {

        return usingArchive(archiveFile, installDir,
                new UpgradeAction<JsonValue>() {
                    // Helper function for get the file content
                    private Function<InputStream, List<String>, IOException> inputStreamToLines =
                            new Function<InputStream, List<String>, IOException>() {
                                @Override
                                public List<String> apply(InputStream is) throws IOException {
                                    final List<String> lines = new LinkedList<String>();
                                    String line = "";
                                    try (final InputStreamReader isr = new InputStreamReader(is);
                                         final BufferedReader in = new BufferedReader(isr)) {
                                        while ((line = in.readLine()) != null) {
                                            lines.add(line);
                                        }
                                    }
                                    return lines;
                                }
                            };

                    @Override
                    public JsonValue invoke(Archive archive, FileStateChecker fileStateChecker) throws UpgradeException {
                        final Path file = Paths.get(filename);

                        try {
                            final List<String> currentFileLines = withInputStreamForPath(installDir.resolve(file), inputStreamToLines);
                            final List<String> newFileLines = archive.withInputStreamForPath(file, inputStreamToLines);
                            Patch<String> patch = DiffUtils.diff(currentFileLines, newFileLines);
                            return json(object(
                                    field("current", currentFileLines),
                                    field("new", newFileLines),
                                    field("diff", DiffUtils.generateUnifiedDiff(
                                            Paths.get("current").resolve(file).toString(),
                                            Paths.get("new").resolve(file).toString(),
                                            currentFileLines,
                                            patch,
                                            3))));
                        } catch (IOException e) {
                            throw new UpgradeException("Unable to retrieve file content for " + file.toString(), e);
                        }
                    }
                });
    }

    /**
     * Perform the upgrade.
     *
     * @param archiveFile the {@link Path} to a ZIP archive containing a new version of OpenIDM
     * @param installDir the base directory where OpenIDM is installed
     * @param keep whether to keep the files (replace if false)
     * @return a json response with the report of what was done to each file
     * @throws UpgradeException on failure to perform upgrade
     */
    public JsonValue upgrade(final Path archiveFile, final Path installDir, final boolean keep)
        throws UpgradeException {

        return usingArchive(archiveFile, installDir,
                new UpgradeAction<JsonValue>() {
                    @Override
                    public JsonValue invoke(Archive archive, FileStateChecker fileStateChecker)
                            throws UpgradeException {
                        final StaticFileUpdate staticFileUpdate = new StaticFileUpdate(fileStateChecker, installDir,
                                archive, new ProductVersion(ServerConstants.getVersion(),
                                ServerConstants.getRevision()));

                        // perform upgrade
                        final JsonValue result = json(object());
                        for (final Path path : archive.getFiles()) {
                            try {
                                if (path.startsWith(BUNDLE_PATH)) {
                                    // TODO do bundle upgrade
                                    result.put(path.toString(), "installed");
                                } else if (path.startsWith(CONF_PATH) &&
                                        path.getFileName().toString().endsWith(JSON_EXT)) {
                                    // a json config in the default project - ignore it
                                    result.put(path.toString(), "skipped");
                                } else if (path.startsWith(CONF_PATH) &&
                                        path.getFileName().toString().endsWith(PATCH_EXT)) {
                                    // TODO patch config in repo
                                    result.put(path.toString(), "patched");
                                } else {
                                    // normal static file; update it
                                    if (keep) {
                                        Path stockFile = staticFileUpdate.keep(path);
                                        result.put(path.toString(), "kept");
                                    } else {
                                        Path backupFile = staticFileUpdate.replace(path);
                                        result.put(path.toString(), "replaced");
                                    }
                                }
                            } catch (IOException e) {
                                throw new UpgradeException("Unable to upgrade " + path.toString(), e);
                            }
                        }
                        return result;
                    }
                });
    }

    /**
     * List the applicable update archives found in the update directory.
     *
     * @return a json list of objects describing each applicable update archive.
     * @throws UpgradeException on failure to generate archive list.
     */
    public JsonValue listAvailableUpdates() throws UpgradeException {
        JsonValue updates = json(array());

        final ChecksumFile cksum;
        try {
            cksum = new ChecksumFile(Paths.get(".").resolve(CHECKSUMS_FILE));
        } catch (IOException | NoSuchAlgorithmException e) {
            throw new UpgradeException("Failed to load checksum file from archive.", e);
        } catch (NullPointerException e) {
            throw new UpgradeException("Archive directory does not exist?", e);
        }

        for (File file : ARCHIVE_PATH.toFile().listFiles()) {
            if (file.getName().endsWith(".zip")) {
                try {
                    Properties prop = readProperties();
                    if ("OpenIDM".equals(prop.getProperty(PROP_UPGRADESPRODUCT))) {
                        // TODO also check version compatibility?
                        updates.add(object(
                                field("archive", file.getName()),
                                field("fileSize", file.length()),
                                field("fileDate", file.lastModified()),
                                field("checksum", cksum.getCurrentDigest(file.toPath())),
                                field("version", prop.getProperty(PROP_PRODUCT) + " v" +
                                        prop.getProperty(PROP_VERSION)),
                                field("description", prop.getProperty(PROP_DESCRIPTION)),
                                field("resource", prop.getProperty(PROP_RESOURCE))
                        ));
                    }
                } catch (NullPointerException | IOException e) {
                    // skip file, it does not contain a manifest or digest could not be calculated
                }
            }
        }

        return updates;
    }

    private Properties readProperties() throws UpgradeException {
        Properties prop = new Properties();
        try(InputStream inp = new FileInputStream("package.properties")) {
            prop.load(inp);
            return prop;
        } catch (IOException e) {
            throw new UpgradeException("Unable to load package properties.", e);
        }
    }

    private Attributes readManifest(File jarFile) throws UpgradeException {
        try {
            return FileUtil.readManifest(jarFile);
        } catch (FileNotFoundException e) {
            throw new UpgradeException("File " + jarFile.getName() + " does not exist.", e);
        } catch (IOException e) {
            throw new UpgradeException("Error while reading from " + jarFile.getName(), e);
        }
    }

    /**
     * Fetch the zip file from the URL and write it to the local filesystem.
     *
     * @param url
     * @return
     * @throws UpgradeException
     */
    private Path readZipFile(URL url) throws UpgradeException {

        // Download the patch file
        final ReadableByteChannel channel;
        try {
            channel = Channels.newChannel(url.openStream());
        } catch (IOException ex) {
            throw new UpgradeException("Failed to access the specified file " + url + " " + ex.getMessage(), ex);
        }

        String workingDir = "";
        final String targetFileName = new File(url.getPath()).getName();
        final File patchDir = new File(workingDir, "patch/bin");
        patchDir.mkdirs();
        final File targetFile = new File(patchDir, targetFileName);
        final FileOutputStream fos;
        try {
            fos = new FileOutputStream(targetFile);
        } catch (FileNotFoundException ex) {
            throw new UpgradeException("Error in getting the specified file to " + targetFile, ex);
        }

        try {
            fos.getChannel().transferFrom(channel, 0, Long.MAX_VALUE);
            System.out.println("Downloaded to " + targetFile);
        } catch (IOException ex) {
            throw new UpgradeException("Failed to get the specified file " + url + " to: " + targetFile, ex);
        }

        return targetFile.toPath();
    }
}

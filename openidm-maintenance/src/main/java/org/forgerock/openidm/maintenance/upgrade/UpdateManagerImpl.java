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
 * Copyright 2015-2016 ForgeRock AS.
 */
package org.forgerock.openidm.maintenance.upgrade;

import static org.forgerock.json.JsonValue.*;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
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
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.jar.Attributes;
import java.util.regex.Pattern;

import difflib.DiffUtils;
import difflib.Patch;
import net.lingala.zip4j.core.ZipFile;
import net.lingala.zip4j.exception.ZipException;
import org.apache.commons.io.FileUtils;
import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.ConfigurationPolicy;
import org.apache.felix.scr.annotations.Properties;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferencePolicy;
import org.apache.felix.scr.annotations.Service;
import org.forgerock.commons.launcher.OSGiFrameworkService;
import org.forgerock.json.JsonPointer;
import org.forgerock.json.JsonValue;
import org.forgerock.json.resource.InternalServerErrorException;
import org.forgerock.json.resource.PatchOperation;
import org.forgerock.json.resource.PatchRequest;
import org.forgerock.json.resource.QueryRequest;
import org.forgerock.json.resource.QueryResourceHandler;
import org.forgerock.json.resource.Requests;
import org.forgerock.json.resource.ResourceException;
import org.forgerock.json.resource.ResourceResponse;
import org.forgerock.json.resource.SortKey;
import org.forgerock.openidm.core.IdentityServer;
import org.forgerock.openidm.core.ServerConstants;
import org.forgerock.openidm.router.IDMConnectionFactory;
import org.forgerock.openidm.util.ContextUtil;
import org.forgerock.openidm.util.FileUtil;
import org.forgerock.openidm.util.JsonUtil;
import org.forgerock.services.context.Context;
import org.forgerock.util.Function;
import org.forgerock.util.query.QueryFilter;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleException;
import org.osgi.framework.Constants;
import org.osgi.framework.Filter;
import org.osgi.service.component.ComponentContext;
import org.osgi.util.tracker.ServiceTracker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Basic manager to initiate the product maintenance and upgrade mechanisms.
 */
@Component(name = UpdateManagerImpl.PID, policy = ConfigurationPolicy.IGNORE, metatype = false,
        description = "OpenIDM Update Manager", immediate = true)
@Service
@Properties({
        @Property(name = Constants.SERVICE_VENDOR, value = ServerConstants.SERVER_VENDOR_NAME),
        @Property(name = Constants.SERVICE_DESCRIPTION, value = "Product Update Manager")
})
public class UpdateManagerImpl implements UpdateManager {

    /** The PID for this component. */
    public static final String PID = "org.forgerock.openidm.maintenance.updatemanager";

    private final static Logger logger = LoggerFactory.getLogger(UpdateManagerImpl.class);

    private static final String UPDATE_CONFIG_FILE = "openidm/update.json";
    private static final Path CHECKSUMS_FILE = Paths.get(".checksums.csv");
    private static final Path BUNDLE_PATH = Paths.get("bundle");
    private static final Path CONF_PATH = Paths.get("conf");
    private static final String JSON_EXT = ".json";
    private static final String PATCH_EXT = ".patch";
    private static final Path ARCHIVE_PATH = Paths.get("bin/update");
    private static final String LICENSE_PATH = "legal-notices/Forgerock_License.txt";
    private static final String BUNDLE_BACKUP_EXT = ".old-";
    static final String PRODUCT_NAME = "OpenIDM";

    static final JsonPointer ORIGIN_PRODUCT = new JsonPointer("/origin/product");
    static final JsonPointer ORIGIN_VERSION = new JsonPointer("/origin/version");
    static final JsonPointer DESTINATION_PRODUCT = new JsonPointer("/destination/product");
    static final JsonPointer DESTINATION_VERSION = new JsonPointer("/destination/version");
    static final JsonPointer UPDATE_DESCRIPTION = new JsonPointer("/update/description");
    static final JsonPointer UPDATE_RESOURCE = new JsonPointer("/update/resource");
    static final JsonPointer UPDATE_RESTARTREQUIRED = new JsonPointer("/update/restartRequired");
    static final JsonPointer REMOVEFILE = new JsonPointer("/removeFile");

    protected final AtomicBoolean restartImmediately = new AtomicBoolean(false);
    private UpdateThread updateThread = null;
    private String lastUpdateId = null;

    public enum UpdateStatus {
        IN_PROGRESS,
        COMPLETE,
        FAILED
    }

    /** The OSGiFramework Service **/
    protected OSGiFrameworkService osgiFrameworkService;

    @Activate
    void activate(ComponentContext compContext) throws Exception {
        logger.debug("Activating UpdateManagerImpl {}", compContext.getProperties());
        BundleContext bundleContext = compContext.getBundleContext();
        Filter filter = bundleContext
                .createFilter("(" + Constants.OBJECTCLASS + "=org.forgerock.commons.launcher.OSGiFramework)");
        ServiceTracker serviceTracker = new ServiceTracker(bundleContext, filter, null);
        serviceTracker.open(true);
        this.osgiFrameworkService = (OSGiFrameworkService) serviceTracker.getService();

        if (osgiFrameworkService != null) {
            logger.debug("Obtained OSGiFrameworkService", compContext.getProperties());
        } else {
            throw new InternalServerErrorException("Cannot instantiate service without OSGiFrameworkService");
        }
    }

    /** The update logging service */
    @Reference(policy = ReferencePolicy.STATIC)
    private UpdateLogService updateLogService;

    /** The connection factory */
    @Reference(policy = ReferencePolicy.STATIC)
    protected IDMConnectionFactory connectionFactory;

    /**
     * Execute a {@link Function} on an input stream for the given {@link Path}.
     *
     * @param path the {@link Path} on which to open an {@link InputStream}
     * @param function the {@link Function} to be applied to that {@link InputStream}
     * @param <R> The return type of the function
     * @param <E> The exception type thrown by the function
     * @return The result of the function
     * @throws E on exception from the function
     * @throws IOException on failure to create an input stream from the path given
     */
    static <R, E extends Exception> R withInputStreamForPath(Path path, Function<InputStream, R, E> function)
            throws E, IOException {
        try (final InputStream is = Files.newInputStream(path.normalize())) {
            return function.apply(is);
        }
    }

    private class ZipArchive implements Archive {
        private final Path upgradeRoot;
        private final Set<Path> filePaths;
        private ProductVersion version = null;

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
                filePaths = resolveChecksumFile(upgradeRoot).getFilePaths();
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
                    logger.info("Upgrading to " + version);
                }
            } catch (IOException
                    | ClassNotFoundException
                    | IllegalAccessException
                    | InvocationTargetException
                    | NoSuchMethodException e) {
                logger.info("Archive does not contain a product version; Assumed to be a patch.");
            }
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
            return UpdateManagerImpl.withInputStreamForPath(upgradeRoot.resolve(path), function);
        }
    }

    /**
     * Execute a {@link Function} in a temp directory prefixed by {@code tempDirectoryPrefix}.  This function
     * is useful to do a body of work in a temp directory and then clean up the contents of the temp directory
     * and remove the temp directory once the work is complete.
     *
     * @param tempDirectoryPrefix the temp directory prefix
     * @param func a {@link Function} to execute in/on a temp directory
     * @param <R> The return type of the function
     * @return the result of the function
     * @throws UpdateException on failure to create the temp directory or execute the function
     */
    private <R> R withTempDirectory(String tempDirectoryPrefix, Function<Path, R, UpdateException> func)
            throws UpdateException {

        Path tempUnzipDir = null;
        try {
            tempUnzipDir = Files.createTempDirectory(tempDirectoryPrefix);
            return func.apply(tempUnzipDir);
        } catch (IOException e) {
            throw new UpdateException("Cannot create temporary directory to unzip archive");
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
        R invoke(Archive archive, FileStateChecker fileStateChecker) throws UpdateException;
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
     * @throws UpdateException on failure to perform the upgrade action
     */
    private <R> R usingArchive(final Path archiveFile, final Path installDir, final UpgradeAction<R> upgradeAction)
            throws UpdateException {

        return withTempDirectory("openidm-upgrade-",
                new Function<Path, R, UpdateException>() {
                    @Override
                    public R apply(Path tempUnzipDir) throws UpdateException {
                        try {
                            final ZipArchive archive = new ZipArchive(archiveFile, tempUnzipDir);
                            final ChecksumFile checksumFile = resolveChecksumFile(installDir);
                            final FileStateChecker fileStateChecker = new FileStateChecker(checksumFile);

                            return upgradeAction.invoke(archive, fileStateChecker);
                        } catch (Exception e) {
                            throw new UpdateException(e.getMessage(), e);
                        }
                    }
                });
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public JsonValue listAvailableUpdates() throws UpdateException {
        final JsonValue rejects = json(array());
        final JsonValue updates = json(array());

        final ChecksumFile checksumFile = resolveChecksumFile(Paths.get("."));

        for (final File file : getUpdateFiles()) {
            try {
                validateFileName(file);
                JsonValue updateConfig = readUpdateConfig(file);
                validateCorrectProduct(updateConfig, file);
                validateCorrectVersion(updateConfig, file);

                updates.add(validArchive(file, updateConfig, getArchiveDigest(checksumFile, file)).getObject());
            } catch (InvalidArchiveUpdateException e) {
                rejects.add(e.toJsonValue().getObject());
            }
        }

        return json(object(
                field("updates", updates.getObject()),
                field("rejects", rejects.getObject())
        ));
    }

    /**
     * Constructs the json object that holds the properties of the validated archive file.
     *
     * @param archiveFile the handle to the valid archive file.
     * @param updateConfig The configuration for the update archive.
     * @param archiveDigest The checksum digest for the archive file.
     */
    private JsonValue validArchive(File archiveFile, JsonValue updateConfig, String archiveDigest) {
        return json(object(
                field("archive", archiveFile.getName()),
                field("fileSize", archiveFile.length()),
                field("fileDate", getDateString(new Date(archiveFile.lastModified()))),
                field("checksum", archiveDigest),
                field("fromProduct", updateConfig.get(ORIGIN_PRODUCT).asString()),
                field("fromVersion", updateConfig.get(ORIGIN_VERSION).asList()),
                field("toProduct", updateConfig.get(DESTINATION_PRODUCT).asString()),
                field("toVersion", updateConfig.get(DESTINATION_VERSION).asString()),
                field("description", updateConfig.get(UPDATE_DESCRIPTION).asString()),
                field("resource", updateConfig.get(UPDATE_RESOURCE).asString()),
                field("restartRequired", updateConfig.get(UPDATE_RESTARTREQUIRED).asBoolean())
        ));
    }

    /**
     * Returns the current digest of the archiveFile from the checksumFile.
     *
     * @param checksumFile Checksum file for the archive.
     * @param archiveFile The file to get the digest for.
     * @return The current digest of the archiveFile from the checksumFile.
     * @throws InvalidArchiveUpdateException
     * @see ChecksumFile#getCurrentDigest(Path)
     */
    private String getArchiveDigest(ChecksumFile checksumFile, File archiveFile) throws InvalidArchiveUpdateException {
        try {
            return checksumFile.getCurrentDigest(archiveFile.toPath());
        } catch (IOException e) {
            throw new InvalidArchiveUpdateException(archiveFile.getName(),
                    "Unable to get checksum digest for archive " + archiveFile.getName(), e);
        }
    }

    /**
     * Check if the file is for the correct version.
     *
     * @param updateConfig Configuration for the archive.
     * @param updateFile the update archive file.
     * @throws InvalidArchiveUpdateException
     * @see ServerConstants#getVersion()
     */
    private void validateCorrectVersion(JsonValue updateConfig, File updateFile) throws InvalidArchiveUpdateException {
        if (!updateConfig.get(ORIGIN_VERSION).asList().contains(ServerConstants.getVersion())) {
            throw new InvalidArchiveUpdateException(updateFile.getName(), "The archive " + updateFile.getName() +
                    " can be used only to update version '" + updateConfig.get(ORIGIN_VERSION).asList() +
                    "' and you are running version " + ServerConstants.getVersion());
        }
    }

    /**
     * Check if the file is for the correct product.
     *
     * @param updateConfig Configuration for the archive.
     * @param updateFile the update archive file.
     * @throws InvalidArchiveUpdateException if the archive is not for the correct product.
     */
    private void validateCorrectProduct(JsonValue updateConfig, File updateFile) throws InvalidArchiveUpdateException {
        if (!PRODUCT_NAME.equals(updateConfig.get(ORIGIN_PRODUCT).asString())) {
            throw new InvalidArchiveUpdateException(updateFile.getName(),
                    "The archive " + updateFile.getName() + " can be used only to update '" +
                            updateConfig.get(ORIGIN_PRODUCT).asString() + "' and you are running " + PRODUCT_NAME);
        }
    }

    /**
     * Tests if the file's name is a zip file.
     *
     * @param archiveFile the File to test the name to end with '.zip'.
     * @throws InvalidArchiveUpdateException If the fileName is not ending with '.zip'.
     */
    private void validateFileName(File archiveFile) throws InvalidArchiveUpdateException {
        if (!archiveFile.getName().endsWith(".zip")) {
            throw new InvalidArchiveUpdateException(archiveFile.getName(),
                    archiveFile.getName() + " does not have '.zip' extension.");
        }
    }

    /**
     * Returns a new instance of a ChecksumFile that is resolved from the passed in path.
     *
     * @param path Location to expect to find the checksum file.
     * @return The resolved checksum file
     * @throws UpdateException When there is trouble resolving the checksum file from within the path.
     */
    ChecksumFile resolveChecksumFile(Path path) throws UpdateException {
        try {
            return new ChecksumFile(path.resolve(CHECKSUMS_FILE));
        } catch (IOException | NoSuchAlgorithmException e) {
            throw new UpdateException("Failed to load checksum file from archive.", e);
        } catch (NullPointerException e) {
            throw new UpdateException("Archive directory does not exist", e);
        }
    }

    /**
     * Gathers all files in the archive path. Added to support unit testing.
     *
     * @return List of files found in the ARCHIVE_PATH
     * @see #ARCHIVE_PATH
     */
    File[] getUpdateFiles() {
        return ARCHIVE_PATH.toFile().listFiles();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public JsonValue report(final Path archiveFile, final Path installDir)
            throws UpdateException {

        return usingArchive(archiveFile, installDir,
                new UpgradeAction<JsonValue>() {
                    @Override
                    public JsonValue invoke(Archive archive, FileStateChecker fileStateChecker)
                            throws UpdateException {

                        final List<Object> result = array();
                        for (Path path : archive.getFiles()) {
                            try {
                                FileState state = fileStateChecker.getCurrentFileState(path);
                                result.add(object(
                                        field("filePath", path.toString()),
                                        field("fileState", state.toString())
                                ));
                            } catch (IOException e) {
                                throw new UpdateException("Unable to determine file state for " + path.toString(), e);
                            }
                        }

                        return json(result);
                    }
                });
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public JsonValue diff(final Path archiveFile, final Path installDir, final String filename) throws UpdateException {

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
                    public JsonValue invoke(Archive archive, FileStateChecker fileStateChecker) throws UpdateException {
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
                            throw new UpdateException("Unable to retrieve file content for " + file.toString(), e);
                        }
                    }
                });
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public JsonValue upgrade(final Path archiveFile, final Path installDir, final String userName)
            throws UpdateException {

        final JsonValue updateConfig = readUpdateConfig(archiveFile.toFile());
        if (!"OpenIDM".equals(updateConfig.get(ORIGIN_PRODUCT)) ||
                !updateConfig.get(ORIGIN_VERSION).asList().contains(ServerConstants.getVersion())) {
            throw new UpdateException("Update archive does not apply to the installed product.");
        }

        Path tempUnzipDir = null;
        try {
            tempUnzipDir = Files.createTempDirectory("openidm-upgrade-");
            try {
                final ZipArchive archive = new ZipArchive(archiveFile, tempUnzipDir);
                final ChecksumFile checksumFile = resolveChecksumFile(installDir);
                final FileStateChecker fileStateChecker = new FileStateChecker(checksumFile);

                // perform upgrade
                UpdateLogEntry updateEntry = new UpdateLogEntry();
                updateEntry.setStatus(UpdateStatus.IN_PROGRESS)
                        .setStatusMessage("Initializing update")
                        .setTotalTasks(archive.getFiles().size())
                        .setStartDate(getDateString())
                        .setNodeId(IdentityServer.getInstance().getNodeName())
                        .setUserName(userName);
                try {
                    updateLogService.logUpdate(updateEntry);
                } catch (ResourceException e) {
                    throw new UpdateException("Unable to log update.", e);
                }

                updateThread = new UpdateThread(updateEntry, archive, fileStateChecker, installDir, updateConfig,
                        tempUnzipDir);
                updateThread.start();

                return updateEntry.toJson();
            } catch (Exception e) {
                throw new UpdateException(e.getMessage(), e);
            }
        } catch (IOException e) {
            throw new UpdateException("Cannot create temporary directory to unzip archive");
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public JsonValue getLicense(Path archive) throws UpdateException {
        try {
            ZipFile zip = new ZipFile(archive.toFile());
            Path tmpDir = Files.createTempDirectory(UUID.randomUUID().toString());
            zip.extractFile("openidm/" + LICENSE_PATH, tmpDir.toString());
            File file = new File(tmpDir.toString() + "/openidm/" + LICENSE_PATH);
            if (!file.exists()) {
                throw new UpdateException("Unable to locate a license file.");
            }
            try (FileInputStream inp = new FileInputStream(file)) {
                byte[] data = new byte[(int) file.length()];
                inp.read(data);
                return json(object(field("license", new String(data, "UTF-8"))));
            } catch (IOException e) {
                throw new UpdateException("Unable to load license file.", e);
            }
        } catch (IOException | ZipException e) {
            return json(object());
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void restartNow() {
        restartImmediately.set(true);
        if (updateThread == null) {
            try {
                new UpdateThread().restart();
            } catch (BundleException e) {
                logger.debug("Failed to restart!", e);
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getLastUpdateId() {
        if (lastUpdateId == null) {
            final List<JsonValue> results = new ArrayList<>();
            final QueryRequest request = Requests.newQueryRequest("repo/updates")
                    .addField("_id")
                    .setQueryFilter(QueryFilter.<JsonPointer>alwaysTrue())
                    .addSortKey(SortKey.descendingOrder("startDate"))
                    .setPageSize(1);

            try {
                connectionFactory.getConnection().query(ContextUtil.createInternalContext(), request,
                        new QueryResourceHandler() {
                            @Override
                            public boolean handleResource(ResourceResponse resourceResponse) {
                                results.add(resourceResponse.getContent());
                                return true;
                            }
                        });
            } catch (ResourceException e) {
                logger.debug("Unable to retrieve most recent update from repo", e);
                return "0";
            }

            lastUpdateId = results.size() > 0 ? results.get(0).get(ResourceResponse.FIELD_CONTENT_ID).asString() : "0";
        }
        return lastUpdateId;
    }

    /**
     * Actions taken during the update of a file
     */
    private enum UpdateAction {
        REPLACED,
        PRESERVED,
        APPLIED
    }

    private class UpdateThread extends Thread {
        private final UpdateLogEntry updateEntry;
        private final Archive archive;
        private final FileStateChecker fileStateChecker;
        private final StaticFileUpdate staticFileUpdate;
        private final JsonValue updateConfig;
        private final Path tempDirectory;
        private final long timestamp = new Date().getTime();

        public UpdateThread() {
            this.updateEntry = null;
            this.archive = null;
            this.fileStateChecker = null;
            this.staticFileUpdate = null;
            this.updateConfig = null;
            this.tempDirectory = null;
        }

        public UpdateThread(UpdateLogEntry updateEntry, Archive archive, FileStateChecker fileStateChecker,
                Path installDir, JsonValue updateConfig, Path tempDirectory) {
            this.updateEntry = updateEntry;
            this.archive = archive;
            this.fileStateChecker = fileStateChecker;
            this.updateConfig = updateConfig;
            this.tempDirectory = tempDirectory;

            this.staticFileUpdate = new StaticFileUpdate(fileStateChecker, installDir,
                    archive, new ProductVersion(ServerConstants.getVersion(),
                    ServerConstants.getRevision()), timestamp);
        }

        public void run() {
            if (updateEntry == null) {
                return;
            }

            try {
                String projectDir = IdentityServer.getInstance().getProjectLocation().toString();
                final String installDir = IdentityServer.getInstance().getInstallLocation().toString();

                // Start by removing all files we no longer need
                for (String file : updateConfig.get(REMOVEFILE).asList(String.class)) {
                    Files.delete(Paths.get(installDir, file));
                }

                // Iterate over the checksums.csv "manifest" in the new update archive and process each file
                // based on location and type of file.  Note that the old checksums.csv in the installed copy
                // of IDM is used only to compare checksums with the existing files.
                BundleHandler bundleHandler = new BundleHandler(
                        osgiFrameworkService.getSystemBundle().getBundleContext(), BUNDLE_BACKUP_EXT + timestamp,
                        new LogHandler() {
                            @Override
                            public void log(Path filePath, Path backupPath) {
                                try {
                                    Path newPath = Paths.get(filePath.toString().substring(
                                            tempDirectory.toString().length() + "/openidm/".length()));
                                    UpdateFileLogEntry fileEntry = new UpdateFileLogEntry()
                                            .setFilePath(newPath.toString())
                                            .setFileState(fileStateChecker.getCurrentFileState(newPath).name())
                                            .setActionTaken(UpdateAction.REPLACED.toString());
                                    fileEntry.setBackupFile(backupPath.toString().substring(installDir.length() + 1));
                                    logUpdate(updateEntry.addFile(fileEntry.toJson()));
                                } catch (Exception e) {
                                    logger.debug("Failed to log updated file: " + filePath.toString());
                                }
                            }
                        });

                for (final Path path : archive.getFiles()) {
                    if (path.startsWith(BUNDLE_PATH)) {
                        Path newPath = Paths.get(tempDirectory.toString(), "openidm", path.toString());
                        String symbolicName = null;
                        try {
                            Attributes manifest = readManifest(newPath);
                            symbolicName = manifest.getValue(Constants.BUNDLE_SYMBOLICNAME);
                        } catch (Exception e) {
                            // jar does not contain a manifest
                        }
                        if (symbolicName == null) {
                            // treat it as a static file
                            Path backupFile = staticFileUpdate.replace(path);
                            if (backupFile != null) {
                                UpdateFileLogEntry fileEntry = new UpdateFileLogEntry()
                                        .setFilePath(path.toString())
                                        .setFileState(fileStateChecker.getCurrentFileState(path).name())
                                        .setActionTaken(UpdateAction.REPLACED.toString());
                                fileEntry.setBackupFile(backupFile.toString());
                                logUpdate(updateEntry.addFile(fileEntry.toJson()));
                            }
                        } else {
                            bundleHandler.upgradeBundle(newPath, symbolicName);
                            fileStateChecker.updateState(path);
                        }
                    } else if (path.getFileName().toString().endsWith(JSON_EXT) &&
                            !projectDir.equals(installDir) &&
                            path.startsWith(projectDir.substring(installDir.length() + 1) + "/" + CONF_PATH)) {
                        // a json config in the current project - ignore it
                    } else if (path.startsWith(CONF_PATH) &&
                            path.getFileName().toString().endsWith(JSON_EXT)) {
                        // a json config in the default project - ignore it
                    } else if (path.startsWith(CONF_PATH) &&
                            path.getFileName().toString().endsWith(PATCH_EXT)) {
                        // a patch file for a config in the repo
                        patchConfig(ContextUtil.createInternalContext(),
                                "repo/config", json(FileUtil.readFile(path.toFile())));
                        UpdateFileLogEntry fileEntry = new UpdateFileLogEntry()
                                .setFilePath(path.toString())
                                .setFileState(fileStateChecker.getCurrentFileState(path).name())
                                .setActionTaken(UpdateAction.APPLIED.toString());
                        logUpdate(updateEntry.addFile(fileEntry.toJson()));
                    } else {
                        // normal static file; update it
                        UpdateFileLogEntry fileEntry = new UpdateFileLogEntry()
                                .setFilePath(path.toString())
                                .setFileState(fileStateChecker.getCurrentFileState(path).name());

                        if (!isReadOnly(path)) {
                            Path stockFile = staticFileUpdate.keep(path);
                            fileEntry.setActionTaken(UpdateAction.PRESERVED.toString());
                            if (stockFile != null) {
                                fileEntry.setStockFile(stockFile.toString());
                            }
                        } else {
                            Path backupFile = staticFileUpdate.replace(path);
                            fileEntry.setActionTaken(UpdateAction.REPLACED.toString());
                            if (backupFile != null) {
                                fileEntry.setBackupFile(backupFile.toString());
                            }
                        }

                        if (fileEntry.getStockFile() == null && fileEntry.getBackupFile() == null) {
                            fileEntry.setActionTaken(UpdateAction.REPLACED.toString());
                        }
                        logUpdate(updateEntry.addFile(fileEntry.toJson()));
                    }
                    logUpdate(updateEntry.setCompletedTasks(updateEntry.getCompletedTasks() + 1)
                            .setStatusMessage("Processed " + path.getFileName().toString()));
                }
                logUpdate(updateEntry.setEndDate(getDateString())
                        .setStatus(UpdateStatus.COMPLETE)
                        .setStatusMessage("Update complete."));

            } catch (Exception e) {
                try {
                    logUpdate(updateEntry.setEndDate(getDateString())
                            .setStatus(UpdateStatus.FAILED)
                            .setStatusMessage("Update failed."));
                } catch (UpdateException ue) {}
                logger.debug("Failed to install update!", e);
                return;
            } finally {
                try {
                    if (tempDirectory != null) {
                        FileUtils.deleteDirectory(tempDirectory.toFile());
                    }
                } catch (IOException e) {
                    logger.error("Could not remove temporary directory: " + tempDirectory.toString(), e);
                }
            }

            if (updateConfig.get(UPDATE_RESTARTREQUIRED).asBoolean()) {
                try {
                    restart();
                } catch (BundleException e) {
                    logger.debug("Failed to restart!", e);
                }
            }
        }

        protected void restart() throws BundleException {
            long timeout = System.currentTimeMillis() + 30000;
            try {
                do {
                    sleep(200);
                } while (System.currentTimeMillis() < timeout && !restartImmediately.get());
            } catch (Exception e) {
                // restart now
            }
            // Send updated FrameworkEvent
            osgiFrameworkService.getSystemBundle().update();
        }

        /**
         * Apply a JsonPatch to a config object on the router.
         *
         * @param context the context for the patch request.
         * @param resourceName the name of the resource to be patched.
         * @param patch a JsonPatch to be applied to the named config resource.
         * @throws UpdateException
         */
        private void patchConfig(Context context, String resourceName, JsonValue patch) throws UpdateException {
            try {
                PatchRequest request = Requests.newPatchRequest(resourceName);
                for (PatchOperation op : PatchOperation.valueOfList(patch)) {
                    request.addPatchOperation(op);
                }
                UpdateManagerImpl.this.connectionFactory.getConnection().patch(context, request);
            } catch (ResourceException e) {
                throw new UpdateException("Patch request failed", e);
            }
        }
    }

    private boolean isReadOnly(Path path) {
        Pattern uiDefaults = Pattern.compile("^ui/*/default");
        return path.startsWith("bin") || uiDefaults.matcher(path.toString()).find();
    }

    private void logUpdate(UpdateLogEntry entry) throws UpdateException {
        try {
            updateLogService.updateUpdate(entry);
        } catch (ResourceException e) {
            throw new UpdateException("Failed to modify update log entry.", e);
        }
    }

    private String getDateString() {
        return getDateString(new Date());
    }

    private String getDateString(Date date) {
        // Ex: 2011-09-09T14:58:17.654+02:00
        SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd'T'hh:mm:ss.SXXX");
        return formatter.format(date);
    }

    JsonValue readUpdateConfig(File file) throws UpdateException {
        try {
            ZipFile zip = new ZipFile(file);
            Path tmpDir = Files.createTempDirectory(UUID.randomUUID().toString());
            zip.extractFile(UPDATE_CONFIG_FILE, tmpDir.toString());
            try (InputStream inp = new FileInputStream(tmpDir.toString() + "/" + UPDATE_CONFIG_FILE)) {
                StringBuilder sb = new StringBuilder();
                Reader reader = new InputStreamReader(inp, "UTF-8");
                char[] buf = new char[1024];
                int chr = reader.read(buf);
                while(chr > 0) {
                    sb.append(buf, 0, chr);
                    chr = reader.read(buf);
                }
                return JsonUtil.parseStringified(sb.toString());
            } catch (IOException e) {
                throw new UpdateException("Unable to load " + UPDATE_CONFIG_FILE + ".", e);
            } finally {
                new File(tmpDir.toString() + "/" + UPDATE_CONFIG_FILE).delete();
            }
        } catch (IOException | ZipException e) {
            throw new UpdateException("Unable to load " + UPDATE_CONFIG_FILE + ".", e);
        }
    }

    private Attributes readManifest(Path jarFile) throws UpdateException {
        try {
            return FileUtil.readManifest(jarFile.toFile());
        } catch (FileNotFoundException e) {
            throw new UpdateException("File " + jarFile.toFile().getName() + " does not exist.", e);
        } catch (Exception e) {
            throw new UpdateException("Error while reading from " + jarFile.toFile().getName(), e);
        }
    }

    /**
     * Fetch the zip file from the URL and write it to the local filesystem.
     *
     * @param url
     * @return
     * @throws UpdateException
     */
    private Path readZipFile(URL url) throws UpdateException {

        // Download the patch file
        final ReadableByteChannel channel;
        try {
            channel = Channels.newChannel(url.openStream());
        } catch (IOException ex) {
            throw new UpdateException("Failed to access the specified file " + url + " " + ex.getMessage(), ex);
        }

        String workingDir = "";
        final String targetFileName = new File(url.getPath()).getName();
        final File patchDir = ARCHIVE_PATH.toFile();
        patchDir.mkdirs();
        final File targetFile = new File(patchDir, targetFileName);
        final FileOutputStream fos;
        try {
            fos = new FileOutputStream(targetFile);
        } catch (FileNotFoundException ex) {
            throw new UpdateException("Error in getting the specified file to " + targetFile, ex);
        }

        try {
            fos.getChannel().transferFrom(channel, 0, Long.MAX_VALUE);
            System.out.println("Downloaded to " + targetFile);
        } catch (IOException ex) {
            throw new UpdateException("Failed to get the specified file " + url + " to: " + targetFile, ex);
        }

        return targetFile.toPath();
    }
}

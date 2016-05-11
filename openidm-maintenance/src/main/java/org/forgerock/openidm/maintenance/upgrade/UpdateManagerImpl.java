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
import java.util.Collections;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
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
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Properties;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferencePolicy;
import org.apache.felix.scr.annotations.Service;
import org.forgerock.commons.launcher.OSGiFrameworkService;
import org.forgerock.guava.common.base.Strings;
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
import org.forgerock.openidm.config.persistence.ConfigBootstrapHelper;
import org.forgerock.openidm.core.IdentityServer;
import org.forgerock.openidm.core.ServerConstants;
import org.forgerock.openidm.maintenance.impl.UpdateContext;
import org.forgerock.openidm.repo.RepoBootService;
import org.forgerock.openidm.router.IDMConnectionFactory;
import org.forgerock.openidm.util.ContextUtil;
import org.forgerock.openidm.util.FileUtil;
import org.forgerock.openidm.util.JsonUtil;
import org.forgerock.openidm.util.NaturalOrderComparator;
import org.forgerock.services.context.Context;
import org.forgerock.util.Function;
import org.forgerock.util.query.QueryFilter;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleException;
import org.osgi.framework.Constants;
import org.osgi.framework.Filter;
import org.osgi.framework.ServiceReference;
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

    private static final String UPDATE_CONFIG_FILE = "update.json";
    private static final Path CHECKSUMS_FILE = Paths.get(".checksums.csv");
    private static final Path CHECKSUMS_FILE_IN_OPENIDM = Paths.get("openidm/.checksums.csv");
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

    /** The currently-running update thread */
    private UpdateThread updateThread = null;

    private String lastUpdateId = null;

    /** The context of this service */
    private ComponentContext context;

    /** Listener for repo service used by {@link #getDbDirName()} */
    private ServiceTracker repoServiceTracker = null;

    public enum UpdateStatus {
        IN_PROGRESS,
        COMPLETE,
        PENDING_REPO_UPDATES,
        FAILED
    }

    /** The OSGiFramework Service **/
    protected OSGiFrameworkService osgiFrameworkService;

    @Activate
    void activate(ComponentContext compContext) throws Exception {
        logger.debug("Activating UpdateManagerImpl {}", compContext.getProperties());
        BundleContext bundleContext = compContext.getBundleContext();
        Filter osgiFrameworkFilter = bundleContext
                .createFilter("(" + Constants.OBJECTCLASS + "=org.forgerock.commons.launcher.OSGiFramework)");
        ServiceTracker serviceTracker = new ServiceTracker(bundleContext, osgiFrameworkFilter, null);
        serviceTracker.open(true);
        this.osgiFrameworkService = (OSGiFrameworkService) serviceTracker.getService();
        this.context = compContext;

        repoServiceTracker = new ServiceTracker(bundleContext, RepoBootService.class.getName(), null);
        repoServiceTracker.open(true);

        if (osgiFrameworkService != null) {
            logger.debug("Obtained OSGiFrameworkService", compContext.getProperties());
        } else {
            throw new InternalServerErrorException("Cannot instantiate service without OSGiFrameworkService");
        }
    }

    @Deactivate
    void deactivate(ComponentContext compContext) {
        if (repoServiceTracker != null) {
            repoServiceTracker.close();
            repoServiceTracker = null;
        }
        this.context = null;
        this.osgiFrameworkService = null;
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
        private final Path destination;
        private ProductVersion version = null;

        /** Get the destination of the exploded archive */
        Path getDestination() {
            return destination;
        }

        ZipArchive(Path zipFilePath, Path destination) throws ArchiveException {
            this.destination = destination;
            this.upgradeRoot = destination.resolve("openidm");

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
            // Not sure how I feel about this going off upgradeRoot
            // Are we NEVER going to have root folders other than "openidm" in the zip?
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
            if (tempUnzipDir != null) {
                try {
                    FileUtils.deleteDirectory(tempUnzipDir.toFile());
                } catch (IOException e) {
                    logger.error("Could not remove temporary directory: " + tempUnzipDir.toString(), e);
                }
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

        validateFileName(archiveFile.toFile());
        final JsonValue updateConfig = readUpdateConfig(archiveFile.toFile());
        validateCorrectProduct(updateConfig, archiveFile.toFile());
        validateCorrectVersion(updateConfig, archiveFile.toFile());
        validateHasChecksumFile(archiveFile.toFile());

        return withTempDirectory("openidm-update-", new Function<Path, R, UpdateException>() {
            @Override
            public R apply(Path tempDir) throws UpdateException {
                try {
                    final ZipArchive archive = new ZipArchive(archiveFile, tempDir);
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
     * Return a list of required repo updates. Update is deemed required if it is not currently present in the install.
     *
     * @param archive
     * @param checker
     * @return
     */
    private List<Path> listRequiredRepoUpdates(final Archive archive, final FileStateChecker checker) throws IOException, UpdateException {
        final List<Path> newUpdates = new ArrayList<>();

        final String dbDirName = getDbDirName();

        if (Strings.isNullOrEmpty(dbDirName)) {
            return new ArrayList<>();
        }

        final Pattern updatePattern = Pattern.compile("db/"+dbDirName+"/scripts/updates/v(\\d+)_(\\w+).(pg)?sql");

        for (Path updateFile : archive.getFiles()) {
            if (updatePattern.matcher(updateFile.toString()).matches()
                    && checker.getCurrentFileState(updateFile) == FileState.NONEXISTENT) {
                newUpdates.add(updateFile);
            }
        }

        Collections.sort(newUpdates, new NaturalOrderComparator());

        return newUpdates;
    }

    private JsonValue formatRepoUpdateList(final List<Path> updates) {
        final JsonValue response = json(array());

        for (Path p : updates) {
            response.add(object(
                    field("file", p.getFileName().toString()),
                    field("path", p.toString())
            ));
        }

        return response;
    }

    @Override
    public JsonValue listRepoUpdates(final Path archivePath) throws UpdateException {
        if (Strings.isNullOrEmpty(getDbDirName())) {
            return json(array());
        }

        // If archive is requested and it's the running thread we must use the cached thread updates
        // since they have already been replaced on disk
        if (updateThread != null
                && updateThread.isAlive()
                && archivePath.equals(updateThread.archivePath)) {
            return formatRepoUpdateList(updateThread.repoUpdates);
        } else {
            return usingArchive(archivePath, IdentityServer.getInstance().getInstallLocation().toPath(),
                    new UpgradeAction<JsonValue>() {
                        @Override
                        public JsonValue invoke(Archive archive, FileStateChecker fileStateChecker) throws UpdateException {
                            try {
                                return formatRepoUpdateList(listRequiredRepoUpdates(archive, fileStateChecker));
                            } catch (IOException e) {
                                throw new UpdateException(e);
                            }
                        }
                    });
        }
    }

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
                validateHasChecksumFile(file);

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
            throw new InvalidArchiveUpdateException(updateFile.getName(), "The archive " + updateFile.getName()
                    + " can be used only to update version '" + updateConfig.get(ORIGIN_VERSION).asList()
                    + "' and you are running version " + ServerConstants.getVersion());
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
            throw new InvalidArchiveUpdateException(updateFile.getName(), "The archive " + updateFile.getName()
                    + " can be used only to update '" + updateConfig.get(ORIGIN_PRODUCT).asString()
                    + "' and you are running " + PRODUCT_NAME);
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
            throw new InvalidArchiveUpdateException(archiveFile.getName(), "The archive " + archiveFile.getName()
                    + " does not have a '.zip' extension.");
        }
    }

    /**
     * Check if the checksums file in the zip file is present and can be resolved.
     *
     * @param archiveFile the update archive file.
     * @throws InvalidArchiveUpdateException
     */
    private void validateHasChecksumFile(File archiveFile) throws InvalidArchiveUpdateException {
        try {
            resolveChecksumFile(extractFileToDirectory(archiveFile, CHECKSUMS_FILE_IN_OPENIDM));
        } catch (Exception e) {
            throw new InvalidArchiveUpdateException(archiveFile.getName(), "The archive " + archiveFile.getName()
                    + " does not appear to contain a checksums file.", e);
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

    @Override
    public JsonValue diff(final Path archiveFile, final Path installDir, final String filename)
            throws UpdateException {
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

    @Override
    public JsonValue upgrade(final Path archiveFile, final Path installDir, final String userName)
            throws UpdateException {

        if (updateThread != null && updateThread.isAlive()) {
            throw new UpdateException("Only one update may be run at a time");
        }

        validateFileName(archiveFile.toFile());
        final JsonValue updateConfig = readUpdateConfig(archiveFile.toFile());
        validateCorrectProduct(updateConfig, archiveFile.toFile());
        validateCorrectVersion(updateConfig, archiveFile.toFile());
        validateHasChecksumFile(archiveFile.toFile());

        try {
            final Path tempDir = Files.createTempDirectory("openidm-update-");
            final ZipArchive archive = new ZipArchive(archiveFile, tempDir);
            final ChecksumFile checksumFile = resolveChecksumFile(installDir);
            final FileStateChecker fileStateChecker = new FileStateChecker(checksumFile);

            // perform upgrade
            UpdateLogEntry updateEntry = new UpdateLogEntry();
            updateEntry.setStatus(UpdateStatus.IN_PROGRESS)
                    .setArchive(archiveFile.getFileName().toString())
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

            updateThread = new UpdateThread(updateEntry, archiveFile, archive, fileStateChecker, installDir, updateConfig,
                    archive.getDestination());
            updateThread.start();

            return updateEntry.toJson();
        } catch (Exception e) {
            throw new UpdateException(e.getMessage(), e);
        }
    }

    @Override
    public JsonValue completeRepoUpdates(final String updateId) throws UpdateException {
        if (updateThread == null
                || !updateThread.isAlive()
                || !updateThread.getUpdateEntry().getId().equals(updateId)) {
            throw new UpdateException("Update is not currently running");
        } else {
            return updateThread.complete();
        }
    }

    @Override
    public JsonValue getLicense(Path archiveFile) throws UpdateException {
        validateFileName(archiveFile.toFile());
        final JsonValue updateConfig = readUpdateConfig(archiveFile.toFile());
        validateCorrectProduct(updateConfig, archiveFile.toFile());
        validateCorrectVersion(updateConfig, archiveFile.toFile());
        validateHasChecksumFile(archiveFile.toFile());

        try {
            ZipFile zip = new ZipFile(archiveFile.toFile());
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

    @Override
    public JsonValue getArchiveFile(final Path archiveFile, final Path file) throws UpdateException {
        return usingArchive(archiveFile, IdentityServer.getInstance().getInstallLocation().toPath(),
                new UpgradeAction<JsonValue>() {
                    @Override
                    public JsonValue invoke(Archive archive, FileStateChecker fileStateChecker) throws UpdateException {
                        try {
                            return archive.withInputStreamForPath(file, new Function<InputStream, JsonValue, UpdateException>() {
                                @Override
                                public JsonValue apply(InputStream is) throws UpdateException {
                                    // \0 delimiter to scan to end-of-file and get single token
                                    java.util.Scanner s = new java.util.Scanner(is).useDelimiter("\\Z");
                                    String contents = s.hasNext() ? s.next() : "";
                                    return json(object(field("contents", contents)));
                                }
                            });
                        } catch (IOException e) {
                            throw new UpdateException(e);
                        }
                    }
                });
    }

    @Override
    public void restartNow() {
        restartImmediately.set(true);
        if (updateThread == null) {
            try {
                // FIXME - New thread just to restart? yuck.
                new UpdateThread().restart();
            } catch (BundleException e) {
                logger.debug("Failed to restart!", e);
            }
        }
    }

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
        APPLIED,
        REMOVED
    }

    private class UpdateThread extends Thread {
        private final UpdateLogEntry updateEntry;
        private final Archive archive;
        private final FileStateChecker fileStateChecker;
        private final StaticFileUpdate staticFileUpdate;
        private final JsonValue updateConfig;
        private final Path tempDirectory;
        private final long timestamp = new Date().getTime();
        private final Path archivePath;
        private boolean completeable = false;

        private final Lock lock = new ReentrantLock();
        private final Condition complete = lock.newCondition();

        /** List of new repo updates found in archive */
        private final List<Path> repoUpdates;

        // FIXME - Does this even work? Can we just make this private?
        public UpdateThread() {
            this.updateEntry = null;
            this.archive = null;
            this.archivePath = null;
            this.fileStateChecker = null;
            this.staticFileUpdate = null;
            this.updateConfig = null;
            this.tempDirectory = null;
            this.repoUpdates = new ArrayList<>();
        }

        public UpdateThread(UpdateLogEntry updateEntry, Path archivePath, Archive archive, FileStateChecker fileStateChecker,
                Path installDir, JsonValue updateConfig, Path tempDirectory) throws IOException, UpdateException {
            this.updateEntry = updateEntry;
            this.archive = archive;
            this.archivePath = archivePath;
            this.fileStateChecker = fileStateChecker;
            this.updateConfig = updateConfig;
            this.tempDirectory = tempDirectory;

            this.staticFileUpdate = new StaticFileUpdate(fileStateChecker, installDir,
                    archive, new ProductVersion(ServerConstants.getVersion(),
                    ServerConstants.getRevision()), timestamp);
            this.repoUpdates = listRequiredRepoUpdates(archive, fileStateChecker);
        }

        public void run() {
            // FIXME - smelly ...
            if (updateEntry == null) {
                return;
            }

            try {
                final String projectDir = IdentityServer.getInstance().getProjectLocation().toString();
                final String installDir = IdentityServer.getInstance().getInstallLocation().toString();
                final Path repoConfPath;

                if (Strings.isNullOrEmpty(getDbDirName())) {
                    repoConfPath = null;
                } else {
                    repoConfPath = Paths.get("db", getDbDirName(), "conf");
                }

                // Start by removing all files we no longer need
                for (String file : updateConfig.get(REMOVEFILE).asList(String.class)) {
                    try {
                        final FileState fileState = fileStateChecker.getCurrentFileState(Paths.get(file));
                        if (Files.deleteIfExists(Paths.get(installDir, file))) {
                            final UpdateFileLogEntry fileEntry = new UpdateFileLogEntry()
                                    .setFilePath(file)
                                    .setFileState(fileState.name())
                                    .setActionTaken(UpdateAction.REMOVED.toString());
                            logUpdate(updateEntry.addFile(fileEntry.toJson()));
                        }
                    } catch (IOException e) {
                        logger.debug("Unable to remove file " + file + ", continuing update", e);
                    }
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
                    } else if ((path.startsWith(CONF_PATH) || (repoConfPath != null && path.startsWith(repoConfPath))) &&
                            path.getFileName().toString().endsWith(PATCH_EXT)) {
                        // a patch file for a config in the repo
                        File patchFile = new File(new File(tempDirectory.toString(), "openidm").toString(),
                                path.toString());
                        Path configFile = Paths.get(path.toString()
                                .substring(0, path.toString().length() - PATCH_EXT.length()));
                        UpdateFileLogEntry fileEntry = new UpdateFileLogEntry()
                                .setFilePath(path.toString())
                                .setFileState(fileStateChecker.getCurrentFileState(configFile).name());
                        String pid = parsePid(configFile.getFileName().toString());
                        patchConfig(ContextUtil.createInternalContext(),
                                "config/" + pid, JsonUtil.parseStringified(FileUtil.readFile(patchFile)));
                        fileEntry.setActionTaken(UpdateAction.APPLIED.toString());
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

                // un-block complete()
                completeable = true;

                // If this update contained repo updates wait until they are done
                if (!repoUpdates.isEmpty()) {
                    logUpdate(updateEntry
                            .setStatus(UpdateStatus.PENDING_REPO_UPDATES)
                            .setStatusMessage("Update complete. Repo updates pending."));

                    lock.lock();
                    try {
                        complete.await();
                    } finally {
                        lock.unlock();
                    }
                } else {
                    complete();
                }

            } catch (Exception e) {
                try {
                    logUpdate(updateEntry.setEndDate(getDateString())
                            .setStatus(UpdateStatus.FAILED)
                            .setStatusMessage(e.getMessage()));
                } catch (UpdateException ue) {}
                logger.debug("Failed to install update!", e);
                return;
            } finally {
                if (tempDirectory != null) {
                    try {
                        FileUtils.deleteDirectory(tempDirectory.toFile());
                    } catch (IOException e) {
                        logger.error("Could not remove temporary directory: " + tempDirectory.toString(), e);
                    }
                }
            }

            // Reset the last update ID so it will be repopulated next time it is requested
            lastUpdateId = null;

            // Restart if necessary
            if (updateConfig.get(UPDATE_RESTARTREQUIRED).asBoolean()) {
                try {
                    restart();
                } catch (BundleException e) {
                    logger.debug("Failed to restart!", e);
                }
            }
        }

        /**
         * Mark the update for this thread as complete.
         *
         * @return The log entry for the current update as json
         * @throws UpdateException if the thread is not ready to be completed or cannot update the log
         */
        JsonValue complete() throws UpdateException {
            lock.lock();
            try {
                if (!completeable) {
                    throw new UpdateException("Update cannot be completed or has already been marked complete");
                } else {
                    logUpdate(updateEntry.setEndDate(getDateString())
                            .setStatus(UpdateStatus.COMPLETE)
                            .setStatusMessage("Update complete."));
                    completeable = false;

                    complete.signalAll();

                    return updateEntry.toJson();
                }
            } finally {
                lock.unlock();
            }
        }

        /*
            Based on JSONConfigInstaller#parsePid()
         */
        String parsePid(String path) {
            String pid = path.substring(0, path.lastIndexOf('.'));
            int n = pid.indexOf('-');
            if (n > 0) {
                String factoryPid = pid.substring(n + 1);
                pid = pid.substring(0, n);
            }
            pid = ConfigBootstrapHelper.qualifyPid(pid);
            return pid;
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
                UpdateManagerImpl.this.connectionFactory.getConnection().patch(new UpdateContext(context), request);
            } catch (ResourceException e) {
                throw new UpdateException("Patch request failed", e);
            }
        }

        UpdateLogEntry getUpdateEntry() {
            return updateEntry;
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

    /**
     * Given a zip file, this will extract the specified file into the returned directory.
     * @param zipFile the zip file given
     * @param fileToExtract the path to the file to extract from the zipFile
     * @return the directory that holds the file to be extracted
     * @throws UpdateException
     */
    Path extractFileToDirectory(File zipFile, Path fileToExtract) throws UpdateException {
        try {
            ZipFile zip = new ZipFile(zipFile);
            Path tmpDir = Files.createTempDirectory(UUID.randomUUID().toString());
            zip.extractFile(fileToExtract.toString(), tmpDir.toString());
            return tmpDir.resolve(fileToExtract).getParent();
        } catch (IOException | ZipException e) {
            throw new UpdateException("Unable to load " + fileToExtract + ".", e);
        }
    }

    /**
     * Given a zip file, this will read data in UPDATE_CONFIG_FILE.
     * @param file the zip file given
     * @return jsonValue that holds data read from UPDATE_CONFIG_FILE.
     * @throws UpdateException
     */
    JsonValue readUpdateConfig(File file) throws UpdateException {
        Path tmpDir = extractFileToDirectory(file, Paths.get("openidm/" + UPDATE_CONFIG_FILE));
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
     * Return the name of the db directory in db/ representing the current repo.
     *
     * This is retrieved from the OSGi context stored as the db.dirname property on the RepositoryService
     *
     * @return The name of the directory in db/ for the current repo or null if none exists
     * @throws UpdateException If the repo bundle cannot be found
     */
    private String getDbDirName() throws UpdateException {
        final ServiceReference repoReference = repoServiceTracker.getServiceReference();

        if (repoReference == null) {
            throw new UpdateException("Could not find repo service");
        } else {
            return (String) repoReference.getProperty("db.dirname");
        }
    }
}

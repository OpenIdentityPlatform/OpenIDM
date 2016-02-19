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
 * Copyright 2015 ForgeRock AS.
 */
package org.forgerock.openidm.maintenance.upgrade;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.CopyOption;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;

/**
 * Designed to use the systemBundle from the OSGI framework to handle
 * the actions that can be taken on a {@link org.osgi.framework.Bundle}.
 */
public class BundleHandler {

    private final static Logger logger = LoggerFactory.getLogger(BundleHandler.class);

    private final String archiveExtension;
    private final LogHandler updateLogger;
    private BundleContext systemBundleContext;

    /**
     * Construct a BundleHandler
     *
     * @param systemBundleContext the {@link BundleContext} of the system bundle
     * @param archiveExtension the extension used with backing up existing files
     * @param updateLogger the logger used to record bundle changes
     */
    public BundleHandler(BundleContext systemBundleContext, final String archiveExtension,
        final LogHandler updateLogger) {
        this.systemBundleContext = systemBundleContext;
        this.archiveExtension = archiveExtension;
        this.updateLogger = updateLogger;
    }

    /**
     * Upgrades a Bundle by removing the old Bundle if it is installed
     * and replacing it with the new. If the new Bundle has not been
     * installed in the framework it will install it.
     *
     * @param newBundle Bundle to install in the Felix framework.
     * @param symbolicName is the key used to look up installed bundles
     *                     in the Felix framework.
     */
    public void upgradeBundle(Path newBundle, String symbolicName) throws UpdateException {
        // check to see if the bundle is installed in the list of bundles
        List<Bundle> installedBundles = getBundles(symbolicName);
        if (!installedBundles.isEmpty()) {
            // replace the current bundle with the new bundle in the directory
            replaceBundle(installedBundles, newBundle);
        } else {
            // install new bundle
            installBundle(newBundle);
        }
    }

    /**
     * Returns the absolute path to the installed bundle
     * which does not include the file protocol.
     *
     * @param bundle Location where the Bundle is installed.
     * @return Path to location where Bundle is located.
     */
    private Path getBundlePath(Bundle bundle) throws UpdateException {
        try {
            return Paths.get(new URI(bundle.getLocation()).getPath());
        } catch (URISyntaxException e) {
            throw new UpdateException(e.getMessage(), e);
        }
    }

    /**
     * Modifies a current installed bundle that has the same symbolicName as the
     * new Bundle to be installed with a archive extension. Keeping it in the
     * same directory for back up. It will then copy the new Bundle into the same
     * directory as the old Bundle.
     *
     * For example: Replacing oldBundle.jar with newBundle.jar
     *          /Users/tmp/
     *                    oldBundle.jar
     *                    oldBundle.jar.bak
     *                    newBundle.jar
     *
     * @param installedBundles bundles installed with the same symbolic name.
     * @param newBundlePath URI to the new @{Bundle}
     * @throws UpdateException if anything goes wrong with copying or deleting the files.
     */
    private void replaceBundle(List<Bundle> installedBundles, Path newBundlePath) throws UpdateException {
        try {
            Path oldBundlePath = getBundlePath(installedBundles.get(0));
            for (Bundle b : installedBundles) {
                Path path = getBundlePath(b);
                updateLogger.log(newBundlePath, concatArchiveExtension(path));
                Files.move(path, concatArchiveExtension(path));
            }
            Files.copy(newBundlePath, oldBundlePath.getParent().resolve(newBundlePath.getFileName()));
        } catch (IOException e) {
            throw new UpdateException("Cannot replace file " + newBundlePath.toString(), e);
        }

    }

    /**
     * Appends the archive extension to the file Path.
     *
     * @param path file path to current installed Bundle.
     * @return Path with appended archive extension.
     * @throws UpdateException if getting the we cannot
     *         retrieve the Path
     */
    private Path concatArchiveExtension(Path path) throws UpdateException {
        try {
            return Paths.get(new URI(path.toUri().toString().concat(archiveExtension)));
        } catch (URISyntaxException e) {
            throw new UpdateException(e.getMessage(), e);
        }
    }

    /**
     * Retrieves a list of installed bundles with the same
     * symbolicName from the felix framework. It is possible
     * to have multiple versions that have the same symbolic
     * name.
     *
     * @param symbolicName is used as a key to retrieve that bundle
     * @return @{Bundle} associated with that symbolicName
     */
    private List<Bundle> getBundles(final String symbolicName) {
        List<Bundle> bundle = new ArrayList<>();
        for (Bundle b : systemBundleContext.getBundles()) {
            if (symbolicName.equalsIgnoreCase(b.getSymbolicName())) {
                logger.debug("Found bundle {} version : {}", symbolicName, b.getVersion());
                bundle.add(b);
            }
        }
        return bundle;
    }

    /**
     *  Update a current installed bundle.
     * @param bundle that will be updated
     * @throws UpdateException
     */
    public void updateBundle(Bundle bundle) throws UpdateException {
        try {
            bundle.update();
        } catch (BundleException e) {
            throw new UpdateException("Cannot update bundle " + bundle.toString(), e);
        }
    }

    /**
     *  Stop a currently running bundle.
     * @param bundle that will be stopped
     * @throws UpdateException
     */
    public void stopBundle(Bundle bundle) throws UpdateException {
        try {
            bundle.stop();
        } catch (BundleException e) {
            throw new UpdateException("Cannot stop bundle " + bundle.toString(), e);
        }
    }

    /**
     * Uninstall a currently installed bundle.
     * @param bundle that will be uninstalled
     * @throws UpdateException
     */
    public void uninstallBundle(Bundle bundle) throws UpdateException {
        try {
            bundle.uninstall();
        } catch (BundleException e) {
            throw new UpdateException("Cannot uninstall bundle " + bundle.toString(), e);
        }
    }

    /**
     * Installs a bundle in the OSGI Framework.
     * @param path to bundle which will be installed
     * @throws UpdateException
     */
    public void installBundle(Path path) throws UpdateException {
        try {
            if (systemBundleContext.getBundles().length > 1) {
                /* Resolve the path of the new file against the first non-system bundle.  The system bundle
                    has no parent and cannot be used for this.
                 */
                Files.copy(path,
                        getBundlePath(systemBundleContext.getBundles()[1]).getParent().resolve(path.getFileName()),
                        StandardCopyOption.REPLACE_EXISTING);

                systemBundleContext.installBundle(path.toUri().toString());
            } else {
                throw new UpdateException("Unable to install bundle " + path.toUri().toString() +
                        ", cannot resolve path to running installation");
            }
        } catch (IOException | BundleException e) {
            throw new UpdateException("Cannot install bundle " + path.toUri().toString(), e);
        }

    }

    /**
     * Start the bundle.
     * @param bundle to be started
     * @throws UpdateException
     */
    public void startBundle(Bundle bundle) throws UpdateException{
        try {
            bundle.start();
        } catch (BundleException e) {
            throw new UpdateException("Cannot start bundle " + bundle.toString(), e);
        }
    }
}

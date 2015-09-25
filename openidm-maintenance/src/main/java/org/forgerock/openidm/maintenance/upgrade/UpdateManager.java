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

import org.forgerock.json.JsonValue;
import org.osgi.framework.BundleContext;

import java.nio.file.Path;

/**
 * Update manager interface
 */
public interface UpdateManager {
    /**
     * Provide a report of which files have been changed since they were installed.
     *
     * @param archiveFile the {@link java.nio.file.Path} to a ZIP archive containing a new version of OpenIDM
     * @param installDir the base directory where OpenIDM is installed
     * @return a json response listed changed files by state
     * @throws UpdateException
     */
    public JsonValue report(final Path archiveFile, final Path installDir) throws UpdateException;

    /**
     * Return the diff of a single file to show what changes will be made if we overwrite the existing file.
     *
     * @param archiveFile the {@link Path} to a ZIP archive containing a new version of OpenIDM
     * @param installDir the base directory where OpenIDM is installed
     * @param filename the file to diff
     * @return a json response showing the current file, the new file, and the diff
     * @throws UpdateException on failure to perform diff
     */
    public JsonValue diff(final Path archiveFile, final Path installDir, final String filename) throws UpdateException;

    /**
     * Perform the upgrade.
     *
     * @param archiveFile the {@link Path} to a ZIP archive containing a new version of OpenIDM
     * @param installDir the base directory where OpenIDM is installed
     * @param userName the name of the user who initiated the update install
     * @param bundleContext the BundleContext used when installing new bundles
     * @return a json response with the report of what was done to each file
     * @throws UpdateException on failure to perform upgrade
     */
    public JsonValue upgrade(final Path archiveFile, final Path installDir, final String userName,
            final BundleContext bundleContext) throws UpdateException;

    /**
     * List the applicable update archives found in the update directory.
     *
     * @return a json list of objects describing each applicable update archive.
     * @throws UpdateException on failure to generate archive list.
     */
    public JsonValue listAvailableUpdates() throws UpdateException;

    /**
     * Return the license for a given update archive.  Defaults to the license found in the current OpenIDM
     * deployment if the archive does not contain a license file.
     *
     * @param archive Path to the archive to search for a license file.
     * @return json object containing the license.
     * @throws UpdateException on failure to provide a license.
     */
    public JsonValue getLicense(Path archive) throws UpdateException;
}

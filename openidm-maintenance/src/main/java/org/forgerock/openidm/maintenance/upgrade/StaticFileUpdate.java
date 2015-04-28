/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2015 ForgeRock AS. All Rights Reserved
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

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;

/**
 * Updates / replaces / adds a new static file in the distribution.
 */
public class StaticFileUpdate {

    static final String IDM_SUFFIX = ".idm-";

    private final Path path;
    private final FileStateChecker fileStateChecker;
    private final Archive archive;
    private final ProductVersion currentVersion;
    private final ProductVersion upgradedVersion;

    StaticFileUpdate(Path path, FileStateChecker fileStateChecker, Archive archive,
            ProductVersion currentVersion, ProductVersion upgradedVersion) {
        this.path = path;
        this.fileStateChecker = fileStateChecker;
        this.archive = archive;
        this.currentVersion = currentVersion;
        this.upgradedVersion = upgradedVersion;
    }

    /**
     * @return whether the static file exists
     */
    boolean exists() {
        return Files.exists(path);
    }

    /**
     * @return whether the static file has been changed
     */
    boolean isChanged() throws IOException {
        return exists()
                && FileStateChecker.FileState.DIFFERS.equals(fileStateChecker.getCurrentFileState(path));
    }

    /**
     * Replaces this static file with the new one from the archive.  If the file has been changed, copy it to
     * <em>&lt;filepath&gt;-.idm-old</em>.
     *
     * @throws IOException
     */
    void replace() throws IOException {
        if (isChanged()) {
            Files.move(path, Paths.get(path.toString() + IDM_SUFFIX + currentVersion.toString()));
        }
        Files.copy(archive.getInputStream(path), path, StandardCopyOption.REPLACE_EXISTING);
    }

    /**
     * Keep the static file that already exists.  Install the new file as <em>&lt;filepath&gt;-.idm-new</em>.
     *
     * @throws IOException
     */
    void keep() throws IOException {
        if (!isChanged()) {
            throw new IOException("The file " + path.toString() + " does not exist - cannot \"keep\" it");
        }
        Files.copy(archive.getInputStream(path), Paths.get(path.toString() + IDM_SUFFIX + upgradedVersion.toString()));
    }
}

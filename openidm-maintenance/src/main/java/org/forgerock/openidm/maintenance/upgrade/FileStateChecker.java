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

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.NoSuchAlgorithmException;

/**
 * Utility class to provide file state for files in the distribution.  Currently implemented by comparing
 * the digest of the original, shipped files with the digest of the files as they presently exist on the
 * filesystem.
 */
class FileStateChecker {

    // Path to checksum file
    private final ChecksumFile checksum;

    /**
     * Construct the FileStateChecker from a checksum file.
     *
     * @param checksum the checksum file
     * @throws IOException on failure to read file, bad format, etc.
     * @throws NoSuchAlgorithmException if checksum algorithm in checksum file is unknown
     */
    FileStateChecker(ChecksumFile checksum) throws IOException, NoSuchAlgorithmException {
        this.checksum = checksum;
    }

    /**
     * Return the current state of a shipped file.
     *
     * @param originalDeployFile a path to the original, shipped file.
     * @return the current file state
     */
    public FileState getCurrentFileState(Path originalDeployFile) throws IOException {
        if (!checksum.containsKey(originalDeployFile)) {
            return Files.exists(originalDeployFile)
                    ? FileState.UNEXPECTED
                    : FileState.NONEXISTENT;
        }
        if (!Files.exists(checksum.resolvePath(originalDeployFile))) {
            return FileState.DELETED;
        }
        return checksum.getCurrentDigest(originalDeployFile).equalsIgnoreCase(
                checksum.getOriginalDigest(originalDeployFile))
            ? FileState.UNCHANGED
            : FileState.DIFFERS;
    }

    /**
     * Record a new/updated checksum in the digest cache and persist it to disk.
     *
     * @param newFile the file for which a checksum should be added, updated or removed.
     * @throws IOException
     */
    public void updateState(Path newFile) throws IOException {
        Path cacheKey = newFile;
        if (!Files.exists(checksum.resolvePath(newFile))) {
            checksum.remove(cacheKey);
        } else {
            checksum.put(cacheKey, checksum.getCurrentDigest(newFile));
        }
        checksum.persistChecksums();
    }
}

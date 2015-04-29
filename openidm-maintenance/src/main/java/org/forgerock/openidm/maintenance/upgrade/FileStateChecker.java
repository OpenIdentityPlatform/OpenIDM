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

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import javax.xml.bind.annotation.adapters.HexBinaryAdapter;

/**
 * Utility class to retrieve digestCache of original, shipped files and files as they presently exist.
 */
public class FileStateChecker {
    public enum FileState {
        MISSING,
        DIFFERS,
        UNCHANGED
    }

    /** Hex adapter for converting hex sums in checksum file to byte arrays */
    private static final HexBinaryAdapter hexAdapter = new HexBinaryAdapter();

    // Cache of the checksum file's contents.
    private final Map<String, String> digestCache = new HashMap<String, String>();

    // MessageDigest appropriate for algorithm used for checksum
    private final MessageDigest digest;

    // Path to checksums file
    private final Path checksums;

    /**
     * Construct the FileStateChecker from a checksums file.
     *
     * @param checksums the checksum file
     * @throws IOException on failure to read file, bad format, etc.
     * @throws NoSuchAlgorithmException if checksum algorithm in checksum file is unknown
     */
    FileStateChecker(Path checksums) throws IOException, NoSuchAlgorithmException {
        if (!Files.exists(checksums)) {
            throw new FileNotFoundException(checksums.toString() + " does not exist");
        }

        this.checksums = checksums;

        // Assumes csv of #File,algorithm.
        // Supports MD5, SHA-1 and SHA-256 at a minimum.
        try (final BufferedReader reader = Files.newBufferedReader(this.checksums, Charset.defaultCharset())) {
            // read first line for algorithm header
            String line = reader.readLine();
            String[] parts = line.split(",");
            if (!line.startsWith("#") || parts.length < 2) {
                throw new IllegalArgumentException(checksums.toString() + " format is invalid.");
            }
            digest = MessageDigest.getInstance(parts[1]);

            // read other lines
            while ((line = reader.readLine()) != null) {
                parts = line.split(",");
                if (!line.startsWith("#") && parts.length > 1) {
                    digestCache.put(parts[0], parts[1]);
                } else {
                    throw new IllegalArgumentException(checksums.toString() + " has incomplete line.");
                }
            }
        }
    }

    /**
     * Return the current state of a shipped file.
     *
     * @param shippedFile a path to the original, shipped file.
     * @return the current file state
     */
    public FileState getCurrentFileState(Path shippedFile) throws IOException {
        if (!Files.exists(shippedFile)) {
            return FileState.MISSING;
        }
        return Arrays.equals(getCurrentDigest(shippedFile), getOriginalDigest(shippedFile))
            ? FileState.UNCHANGED
            : FileState.DIFFERS;
    }

    /**
     * Record a new/updated checksum in the digest cache and persist it to disk.
     *
     * @param newFile the file for which a checksum should be added or updated.
     * @throws IOException
     */
    public void recordUpdate(Path newFile) throws IOException {
        digestCache.put(newFile.toString(), Arrays.toString(getCurrentDigest(newFile)));
        persistChecksums();
    }

    /**
     * Get the path of the provided file relative to the checksum file's path.  We assume the checksum
     * file is at the root of the project, so all files in the digestCache (sourced from the checksums
     * file) are relative to this root.
     *
     * @param file a file in the distribution
     * @return the path of the file relative to the checksum file (root path)
     */
    private Path getRelativzedPath(Path file) {
        return checksums.getParent().relativize(file);
    }

    /**
     * Returns the digestCache of the original, shipped file.
     *
     * @param shippedFile the original, shipped file.
     * @return the digestCache
     */
    private byte[] getOriginalDigest(Path shippedFile) {
        return hexAdapter.unmarshal(digestCache.get(getRelativzedPath(shippedFile).toString()));
    }

    /**
     * Computes and returns the digestCache of a current file on disk.
     *
     * @param currentFile the current file
     * @return the digestCache
     */
    private byte[] getCurrentDigest(Path currentFile) throws IOException {
        return digest.digest(Files.readAllBytes(currentFile));
    }

    /**
     * Persist the current digest cache to disk.
     *
     * @throws IOException
     */
    private void persistChecksums() throws IOException {
        BufferedWriter writer = Files.newBufferedWriter(checksums, Charset.defaultCharset());

        // Write the header line.
        writer.write("#File," + digest.getAlgorithm());
        writer.newLine();

        // Write all of the checksums.
        for (String path : digestCache.keySet()) {
            writer.write(path + "," + digestCache.get(path));
            writer.newLine();
        }

        writer.close();
    }
}

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

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import javax.xml.bind.annotation.adapters.HexBinaryAdapter;

/**
 * Map-backed checksum file abstraction of the format
 * <pre>
 *      file1,6cd3556deb0da54bca060b4c39479839
 * </pre>
 */
public class ChecksumFile extends HashMap<Path, String> {

    static final long serialVersionUID = 1L;

    /** Hex adapter for converting hex sums in checksum file to byte arrays */
    private static final HexBinaryAdapter hexAdapter = new HexBinaryAdapter();

    // Path to checksums file
    private final Path checksums;

    // MessageDigest appropriate for algorithm used for checksum
    private final MessageDigest digest;

    ChecksumFile(Path checksums) throws IOException, NoSuchAlgorithmException {
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
                    this.put(Paths.get(parts[0]), parts[1]);
                } else {
                    throw new IllegalArgumentException(checksums.toString() + " has incomplete line.");
                }
            }
        }
    }

    /**
     * Return the set of files represented in the checksum file.
     *
     * @return the set of files listed in the checksum file
     */
    Set<Path> getFilePaths() {
        return keySet();
    }

    /**
     * Resolve the full path of the provided file relative to the checksum file's path.
     *
     * @param file a file in the distribution
     * @return the path of the file relative to the checksum file (root path)
     */
    Path resolvePath(Path file) {
        return checksums.getParent().resolve(file);
    }

    private String computeDigest(byte[] data) {
        return hexAdapter.marshal(digest.digest(data));
    }

    /**
     * Returns the digestCache of the original, shipped file.
     *
     * @param shippedFile the original, shipped file.
     * @return the digestCache
     */
    String getOriginalDigest(Path shippedFile) {
        return get(shippedFile);
    }

    /**
     * Computes and returns the digestCache of a current file on disk.
     *
     * @param currentFile the current file
     * @return the digestCache
     */
    String getCurrentDigest(Path currentFile) throws IOException {
        return computeDigest(Files.readAllBytes(resolvePath(currentFile)));
    }

    /**
     * Persist the current digest cache to disk.
     *
     * @throws IOException
     */
    void persistChecksums() throws IOException {
        try (final BufferedWriter writer = Files.newBufferedWriter(checksums, Charset.defaultCharset())) {

            // Write the header line.
            writer.write("#File," + digest.getAlgorithm());
            writer.newLine();

            // Write all of the checksums.
            for (Map.Entry<Path, String> entry : entrySet()) {
                writer.write(entry.getKey().toString() + "," + entry.getValue());
                writer.newLine();
            }
        }
    }


}

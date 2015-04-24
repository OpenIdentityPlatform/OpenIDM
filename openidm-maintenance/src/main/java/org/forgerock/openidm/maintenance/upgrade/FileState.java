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
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

/**
 * Utility class to retrieve digests of original, shipped files and files as they presently exist.
 */
public class FileState {
    public enum State {
        MISSING,
        DIFFERS,
        UNCHANGED
    }

    // Pathname of the checksum file.
    private final Path checksums;

    // Cache of the checksum file's contents.
    private final Map<String, String> digest = new HashMap<String, String>();

    FileState(Path checksums) throws FileNotFoundException {
        if (!Files.exists(checksums)) {
            throw new FileNotFoundException(checksums + " does not exist");
        }
        this.checksums = checksums;
    }

    /**
     * Return the current state of a shipped file.
     *
     * @param shippedFile a path to the original, shipped file.
     * @return the current file state
     */
    public State getCurrentFileState(Path shippedFile) {
        if (!Files.exists(shippedFile)) {
            return State.MISSING;
        }
        return Arrays.equals(getCurrentDigest(shippedFile), getOriginalDigest(shippedFile))
            ? State.UNCHANGED
            : State.DIFFERS;
    }

    /**
     * Returns the digest of the original, shipped file.
     *
     * @param shippedFile the original, shipped file.
     * @return the digest
     */
    private byte[] getOriginalDigest(Path shippedFile) {
        // TODO implement
        return new byte[] { };
    }

    /**
     * Computes and returns the digest of a current file on disk.
     *
     * @param currentFile the current file
     * @return the digest
     */
    private byte[] getCurrentDigest(Path currentFile) {
        // TODO implement
        return new byte[] { };
    }

    private Map<String, String> getDigests() {
        if (digest.isEmpty()) {
            try (final BufferedReader reader = Files.newBufferedReader(checksums, Charset.defaultCharset())) {
                String line;
                while ((line = reader.readLine()) != null) {
                    if (!line.startsWith("#") && line.length() > 2) {
                        String[] parts = line.split(",");
                        if (parts.length == 2) {
                            digest.put(parts[0], parts[1]);
                        }
                    }
                }
            } catch (FileNotFoundException e) {

            } catch (IOException e) {

            }
        }
        return digest;
    }

}

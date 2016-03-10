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

import static org.assertj.core.api.Assertions.assertThat;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.NoSuchAlgorithmException;
import java.util.Set;

import org.testng.annotations.Test;

/**
 * Test ChecksumFile methods.
 */
public class ChecksumFileTest {
    @Test(expectedExceptions = FileNotFoundException.class)
    public void testChecksumFileNotFound() throws IOException, NoSuchAlgorithmException {
        new ChecksumFile(Paths.get("foo"));
    }

    @Test(expectedExceptions = IllegalArgumentException.class)
    public void testChecksumFileMissingHeader() throws IOException, NoSuchAlgorithmException, URISyntaxException {
        new ChecksumFile(Paths.get(getClass().getResource("/missingheader.csv").toURI()));
    }

    @Test(expectedExceptions = IllegalArgumentException.class)
    public void testChecksumFileBadFormat() throws IOException, NoSuchAlgorithmException, URISyntaxException {
        new ChecksumFile(Paths.get(getClass().getResource("/badformat.csv").toURI()));
    }

    @Test(expectedExceptions = NoSuchAlgorithmException.class)
    public void testChecksumFileNoSuchAlgorithm() throws IOException, NoSuchAlgorithmException, URISyntaxException {
        new ChecksumFile(Paths.get(getClass().getResource("/unknownalgorithm.csv").toURI()));
    }

    @Test
    public void testGetFiles() throws URISyntaxException, IOException, NoSuchAlgorithmException {
        Set<Path> files = new ChecksumFile(Paths.get(getClass().getResource("/checksums.csv").toURI())).getFilePaths();
        assertThat(files.size()).isEqualTo(2);
        assertThat(files.contains(Paths.get("file1"))).isTrue();
        assertThat(files.contains(Paths.get("file2"))).isTrue();
    }
}

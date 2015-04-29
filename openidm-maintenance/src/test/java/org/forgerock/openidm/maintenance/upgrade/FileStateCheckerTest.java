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

import static org.fest.assertions.api.Assertions.assertThat;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.NoSuchAlgorithmException;

import org.testng.annotations.Test;

/**
 * Test the FileStateChecker.
 */
public class FileStateCheckerTest {

    @Test(expectedExceptions = FileNotFoundException.class)
    public void testChecksumFileNotFound() throws IOException, NoSuchAlgorithmException {
        new FileStateChecker(Paths.get("foo"));
    }

    @Test(expectedExceptions = IllegalArgumentException.class)
    public void testChecksumFileMissingHeader() throws IOException, NoSuchAlgorithmException, URISyntaxException {
        new FileStateChecker(Paths.get(getClass().getResource("/missingheader.csv").toURI()));
    }

    @Test(expectedExceptions = IllegalArgumentException.class)
    public void testChecksumFileBadFormat() throws IOException, NoSuchAlgorithmException, URISyntaxException {
        new FileStateChecker(Paths.get(getClass().getResource("/badformat.csv").toURI()));
    }

    @Test(expectedExceptions = NoSuchAlgorithmException.class)
    public void testChecksumFileNoSuchAlgorithm() throws IOException, NoSuchAlgorithmException, URISyntaxException {
        new FileStateChecker(Paths.get(getClass().getResource("/unknownalgorithm.csv").toURI()));
    }

    @Test
    public void testFileMissing() throws IOException, NoSuchAlgorithmException, URISyntaxException {
        Path checksumFile = Paths.get(getClass().getResource("/checksums.csv").toURI());
        FileStateChecker checker = new FileStateChecker(checksumFile);
        assertThat(checker.getCurrentFileState(checksumFile.resolveSibling("file0")))
                .isEqualTo(FileState.MISSING);
    }

    @Test
    public void testFileUnchanged() throws IOException, NoSuchAlgorithmException, URISyntaxException {
        FileStateChecker checker = new FileStateChecker(Paths.get(getClass().getResource("/checksums.csv").toURI()));
        assertThat(checker.getCurrentFileState(Paths.get(getClass().getResource("/file1").toURI())))
                .isEqualTo(FileState.UNCHANGED);
    }

    @Test
    public void testFileDiffers() throws IOException, NoSuchAlgorithmException, URISyntaxException {
        FileStateChecker checker = new FileStateChecker(Paths.get(getClass().getResource("/checksums.csv").toURI()));
        assertThat(checker.getCurrentFileState(Paths.get(getClass().getResource("/file2").toURI())))
                .isEqualTo(FileState.DIFFERS);
    }
}

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

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.security.NoSuchAlgorithmException;

import org.apache.commons.io.FileUtils;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.AfterSuite;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.BeforeSuite;
import org.testng.annotations.Test;

/**
 * Test the FileStateChecker.
 */
public class FileStateCheckerTest {

    private Path tempPath;
    private Path tempFile;

    @BeforeSuite
    public void createTempDirPath() throws IOException {
        tempPath = Files.createTempDirectory(this.getClass().getSimpleName());
    }

    @AfterSuite
    public void destroyTempDirPath() throws IOException {
        FileUtils.deleteDirectory(tempPath.toFile());
    }

    @BeforeMethod
    public void setupTempChecksumFile() throws IOException, NoSuchAlgorithmException {
        tempFile = Files.createTempFile(tempPath, null, null);
    }

    @AfterMethod
    public void deleteCheckSumsCopy() throws IOException {
        Files.delete(tempFile);
    }

    @Test
    public void testUpdateStateNoChange() throws IOException, URISyntaxException, NoSuchAlgorithmException {
        Files.copy(Paths.get(getClass().getResource("/checksums.csv").toURI()), tempFile,
                StandardCopyOption.REPLACE_EXISTING);
        ChecksumFile tempChecksumFile = new ChecksumFile(tempFile);
        FileStateChecker checker = new FileStateChecker(tempChecksumFile);
        Path filepath = Paths.get("file1");
        checker.updateState(filepath);
        checker = new FileStateChecker(tempChecksumFile);
        assertThat(checker.getCurrentFileState(filepath).equals(FileState.UNCHANGED));
    }

    @Test
    public void testUpdateStateWithChange() throws IOException, URISyntaxException, NoSuchAlgorithmException {
        Files.copy(Paths.get(getClass().getResource("/checksums2.csv").toURI()), tempFile,
                StandardCopyOption.REPLACE_EXISTING);
        ChecksumFile tempChecksumFile = new ChecksumFile(tempFile);
        FileStateChecker checker = new FileStateChecker(tempChecksumFile);
        Path filepath = Paths.get("file1");
        checker.updateState(filepath);
        checker = new FileStateChecker(tempChecksumFile);
        assertThat(checker.getCurrentFileState(filepath).equals(FileState.DIFFERS));
    }

    @Test
    public void testUpdateStateRemoval() throws IOException, URISyntaxException, NoSuchAlgorithmException {
        Files.copy(Paths.get(getClass().getResource("/checksums2.csv").toURI()), tempFile,
                StandardCopyOption.REPLACE_EXISTING);
        ChecksumFile tempChecksumFile = new ChecksumFile(tempFile);
        FileStateChecker checker = new FileStateChecker(tempChecksumFile);
        Path filepath = Paths.get("file3");
        checker.updateState(filepath);
        checker = new FileStateChecker(tempChecksumFile);
        assertThat(checker.getCurrentFileState(filepath).equals(FileState.DELETED));
    }

    @Test
    public void testUpdateStateAddition() throws IOException, URISyntaxException, NoSuchAlgorithmException {
        Files.copy(Paths.get(getClass().getResource("/checksums2.csv").toURI()), tempFile,
                StandardCopyOption.REPLACE_EXISTING);
        ChecksumFile tempChecksumFile = new ChecksumFile(tempFile);
        FileStateChecker checker = new FileStateChecker(tempChecksumFile);
        Path filepath = Paths.get("badformat.csv");
        assertThat(checker.getCurrentFileState(filepath).equals(FileState.NONEXISTENT));
        checker.updateState(filepath);
        checker = new FileStateChecker(tempChecksumFile);
        assertThat(checker.getCurrentFileState(filepath).equals(FileState.UNCHANGED));
    }


    @Test
    public void testFileMissing() throws IOException, NoSuchAlgorithmException, URISyntaxException {
        Path path = Paths.get(getClass().getResource("/checksums.csv").toURI());
        ChecksumFile checksumFile = new ChecksumFile(path);
        FileStateChecker checker = new FileStateChecker(checksumFile);
        assertThat(checker.getCurrentFileState(path.resolveSibling("file0")))
                .isEqualTo(FileState.NONEXISTENT);
    }

    @Test
    public void testFileUnchanged() throws IOException, NoSuchAlgorithmException, URISyntaxException {
        FileStateChecker checker = new FileStateChecker(
                new ChecksumFile(Paths.get(getClass().getResource("/checksums.csv").toURI())));
        assertThat(checker.getCurrentFileState(Paths.get("file1"))).isEqualTo(FileState.UNCHANGED);
    }

    @Test
    public void testFileDiffers() throws IOException, NoSuchAlgorithmException, URISyntaxException {
        FileStateChecker checker = new FileStateChecker(
                new ChecksumFile(Paths.get(getClass().getResource("/checksums.csv").toURI())));
        assertThat(checker.getCurrentFileState(Paths.get("file2"))).isEqualTo(FileState.DIFFERS);
    }
}

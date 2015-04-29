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

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.apache.commons.io.FileUtils;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.AfterSuite;
import org.testng.annotations.BeforeSuite;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import static org.fest.assertions.api.Assertions.assertThat;
import static org.mockito.Mockito.*;
import static org.testng.AssertJUnit.assertFalse;
import static org.testng.AssertJUnit.assertTrue;
import static org.forgerock.openidm.maintenance.upgrade.StaticFileUpdate.IDM_SUFFIX;

/**
 * Tests updating static files.
 */
public class StaticFileUpdateTest {

    private static final ProductVersion oldVersion = new ProductVersion("3.2.0", "5000");
    private static final ProductVersion newVersion = new ProductVersion("4.0.0", "6000");

    private static final byte[] oldBytes = "oldcontent".getBytes();
    private static final byte[] newBytes = "newcontent".getBytes();

    private Path getOldVersionPath(Path file) {
        return Paths.get(file + IDM_SUFFIX + oldVersion.toString());
    }

    private Path getNewVersionPath(Path file) {
        return Paths.get(file + IDM_SUFFIX + newVersion.toString());
    }

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
    public void createTempFile() throws IOException {
        tempFile = Files.createTempFile(tempPath, null, null);
    }

    @AfterMethod
    public void deleteTempFile() throws IOException {
        Files.deleteIfExists(tempFile.resolveSibling(getNewVersionPath(tempFile)));
        Files.deleteIfExists(tempFile.resolveSibling(getOldVersionPath(tempFile)));
        Files.delete(tempFile);
    }

    private StaticFileUpdate getStaticFileUpdate(FileStateChecker fileStateChecker) {
        Archive archive = mock(Archive.class);
        when(archive.getInputStream(tempFile)).thenReturn(new ByteArrayInputStream(newBytes));
        StaticFileUpdate update = new StaticFileUpdate(fileStateChecker, archive, oldVersion, newVersion);
        return update;
    }

    /**
     * Test that a path does not exist.
     *
     * @throws IOException
     */
    @Test
    public void testDoesNotExist() throws IOException {
        Path file = tempPath.resolve("test");
        StaticFileUpdate update = getStaticFileUpdate(mock(FileStateChecker.class));
        assertFalse(update.exists(file));
    }

    /**
     * Test that a path exists.
     */
    @Test
    public void testExists() throws IOException {
        StaticFileUpdate update = getStaticFileUpdate(mock(FileStateChecker.class));
        assertTrue(update.exists(tempFile));
    }

    /**
     * Test that a path is unchanged.
     */
    @Test
    public void testUnchanged() throws IOException {
        FileStateChecker fileStateChecker = mock(FileStateChecker.class);
        when(fileStateChecker.getCurrentFileState(tempFile)).thenReturn(FileState.UNCHANGED);
        StaticFileUpdate update = getStaticFileUpdate(fileStateChecker);
        assertFalse(update.isChanged(tempFile));
    }

    /**
     * Test that a path differs.
     */
    @Test
    public void testDiffers() throws IOException {
        FileStateChecker fileStateChecker = mock(FileStateChecker.class);
        when(fileStateChecker.getCurrentFileState(tempFile)).thenReturn(FileState.DIFFERS);
        StaticFileUpdate update = getStaticFileUpdate(fileStateChecker);
        assertTrue(update.isChanged(tempFile));
    }

    /**
     * Test a replacement on an unchangaed path.
     */
    @Test
    public void testReplaceIsUnchanged() throws IOException {
        FileStateChecker fileStateChecker = mock(FileStateChecker.class);
        when(fileStateChecker.getCurrentFileState(tempFile)).thenReturn(FileState.UNCHANGED);
        StaticFileUpdate update = getStaticFileUpdate(fileStateChecker);
        update.replace(tempFile);
        assertThat(Files.readAllBytes(tempFile)).isEqualTo(newBytes);
        assertFalse(Files.exists(getOldVersionPath(tempFile)));
        assertFalse(Files.exists(getNewVersionPath(tempFile)));
    }

    /**
     * Test a replacement on a path with differences.  The file should be updated, with the old content
     * moved to &lt;filename&gt;.idm-old
     */
    @Test
    public void testReplaceDiffers() throws IOException {
        Files.write(tempFile, oldBytes);
        FileStateChecker fileStateChecker = mock(FileStateChecker.class);
        when(fileStateChecker.getCurrentFileState(tempFile)).thenReturn(FileState.DIFFERS);
        StaticFileUpdate update = getStaticFileUpdate(fileStateChecker);
        update.replace(tempFile);
        assertThat(Files.readAllBytes(tempFile)).isEqualTo(newBytes);
        assertThat(Files.readAllBytes(getOldVersionPath(tempFile))).isEqualTo(oldBytes);
        assertFalse(Files.exists(getNewVersionPath(tempFile)));
    }

    /**
     * Test keeping a file with no differences.  This should throw an exception as there are no differences
     * to keep.
     */
    @Test(expectedExceptions = IOException.class)
    public void testKeepIsUnchanged() throws IOException {
        Files.write(tempFile, oldBytes);
        FileStateChecker fileStateChecker = mock(FileStateChecker.class);
        when(fileStateChecker.getCurrentFileState(tempFile)).thenReturn(FileState.UNCHANGED);
        StaticFileUpdate update = getStaticFileUpdate(fileStateChecker);
        update.keep(tempFile);
    }

    /**
     * Test keeping a file with differences.  The file should retain the old content, with the new content
     * written to &lt;filename&gt;.idm-new.
     */
    @Test
    public void testKeepDiffers() throws IOException {
        Files.write(tempFile, oldBytes);
        FileStateChecker fileStateChecker = mock(FileStateChecker.class);
        when(fileStateChecker.getCurrentFileState(tempFile)).thenReturn(FileState.DIFFERS);
        StaticFileUpdate update = getStaticFileUpdate(fileStateChecker);
        update.keep(tempFile);
        assertThat(Files.readAllBytes(tempFile)).isEqualTo(oldBytes);
        assertThat(Files.readAllBytes(getNewVersionPath(tempFile))).isEqualTo(newBytes);
        assertFalse(Files.exists(getOldVersionPath(tempFile)));
    }
}

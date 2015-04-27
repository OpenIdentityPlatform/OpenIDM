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
import org.testng.annotations.AfterSuite;
import org.testng.annotations.BeforeSuite;

import static org.fest.assertions.api.Assertions.assertThat;
import static org.mockito.Mockito.*;
import static org.testng.AssertJUnit.assertFalse;
import static org.testng.AssertJUnit.assertTrue;


import org.testng.annotations.Test;

/**
 * Tests updating static files.
 */
public class StaticFileUpdateTest {

    Path tempPath;

    @BeforeSuite
    public void createTempDirPath() throws IOException {
        tempPath = Files.createTempDirectory(this.getClass().getSimpleName());
    }

    @AfterSuite
    public void destroyTempDirPath() throws IOException {
        FileUtils.deleteDirectory(tempPath.toFile());
    }

    /**
     * Test that a path does not exist.
     *
     * @throws IOException
     */
    @Test
    public void testDoesNotExist() throws IOException {
        Path file = tempPath.resolve("test");
        FileStateChecker fileStateChecker = mock(FileStateChecker.class);
        Archive archive = mock(Archive.class);
        StaticFileUpdate update = new StaticFileUpdate(file, fileStateChecker, archive);
        assertFalse(update.exists());
    }

    /**
     * Test that a path exists.
     */
    @Test
    public void testExists() throws IOException {
        Path file = Files.createTempFile(tempPath, null, null);
        FileStateChecker fileStateChecker = mock(FileStateChecker.class);
        Archive archive = mock(Archive.class);
        StaticFileUpdate update = new StaticFileUpdate(file, fileStateChecker, archive);
        assertTrue(update.exists());
        Files.delete(file);
    }

    /**
     * Test that a path is unchanged.
     */
    @Test
    public void testUnchanged() throws IOException {
        Path file = Files.createTempFile(tempPath, null, null);
        FileStateChecker fileStateChecker = mock(FileStateChecker.class);
        when(fileStateChecker.getCurrentFileState(file)).thenReturn(FileStateChecker.FileState.UNCHANGED);
        Archive archive = mock(Archive.class);
        StaticFileUpdate update = new StaticFileUpdate(file, fileStateChecker, archive);
        assertFalse(update.isChanged());
    }

    /**
     * Test that a path differs.
     */
    @Test
    public void testDiffers() throws IOException {
        Path file = Files.createTempFile(tempPath, null, null);
        FileStateChecker fileStateChecker = mock(FileStateChecker.class);
        when(fileStateChecker.getCurrentFileState(file)).thenReturn(FileStateChecker.FileState.DIFFERS);
        Archive archive = mock(Archive.class);
        StaticFileUpdate update = new StaticFileUpdate(file, fileStateChecker, archive);
        assertTrue(update.isChanged());
    }

    /**
     * Test a replacement on an unchangaed path.
     */
    @Test
    public void testReplaceIsUnchanged() throws IOException {
        byte[] newBytes = "newcontent".getBytes();
        Path file = Files.createTempFile(tempPath, null, null);
        FileStateChecker fileStateChecker = mock(FileStateChecker.class);
        when(fileStateChecker.getCurrentFileState(file)).thenReturn(FileStateChecker.FileState.UNCHANGED);
        Archive archive = mock(Archive.class);
        when(archive.getInputStream(file)).thenReturn(new ByteArrayInputStream(newBytes));
        StaticFileUpdate update = new StaticFileUpdate(file, fileStateChecker, archive);
        update.replace();
        assertThat(Files.readAllBytes(file)).isEqualTo(newBytes);
        assertFalse(Files.exists(Paths.get(file + ".idm-old")));
        assertFalse(Files.exists(Paths.get(file + ".idm-new")));
    }

    /**
     * Test a replacement on a path with differences.  The file should be updated, with the old content
     * moved to &lt;filename&gt;.idm-old
     */
    @Test
    public void testReplaceDiffers() throws IOException {
        byte[] oldBytes = "oldcontent".getBytes();
        byte[] newBytes = "newcontent".getBytes();
        Path file = Files.createTempFile(tempPath, null, null);
        Files.write(file, oldBytes);
        FileStateChecker fileStateChecker = mock(FileStateChecker.class);
        when(fileStateChecker.getCurrentFileState(file)).thenReturn(FileStateChecker.FileState.DIFFERS);
        Archive archive = mock(Archive.class);
        when(archive.getInputStream(file)).thenReturn(new ByteArrayInputStream(newBytes));
        StaticFileUpdate update = new StaticFileUpdate(file, fileStateChecker, archive);
        update.replace();
        assertThat(Files.readAllBytes(file)).isEqualTo(newBytes);
        assertThat(Files.readAllBytes(Paths.get(file + ".idm-old"))).isEqualTo(oldBytes);
        assertFalse(Files.exists(Paths.get(file + ".idm-new")));
    }

    /**
     * Test keeping a file with no differences.  This should throw an exception as there are no differences
     * to keep.
     */
    @Test(expectedExceptions = IOException.class)
    public void testKeepIsUnchanged() throws IOException {
        byte[] oldBytes = "oldcontent".getBytes();
        byte[] newBytes = "newcontent".getBytes();
        Path file = Files.createTempFile(tempPath, null, null);
        Files.write(file, oldBytes);
        FileStateChecker fileStateChecker = mock(FileStateChecker.class);
        when(fileStateChecker.getCurrentFileState(file)).thenReturn(FileStateChecker.FileState.UNCHANGED);
        Archive archive = mock(Archive.class);
        when(archive.getInputStream(file)).thenReturn(new ByteArrayInputStream(newBytes));
        StaticFileUpdate update = new StaticFileUpdate(file, fileStateChecker, archive);
        update.keep();
    }

    /**
     * Test keeping a file with differences.  The file should retain the old content, with the new content
     * written to &lt;filename&gt;.idm-new.
     */
    @Test
    public void testKeepDiffers() throws IOException {
        byte[] oldBytes = "oldcontent".getBytes();
        byte[] newBytes = "newcontent".getBytes();
        Path file = Files.createTempFile(tempPath, null, null);
        Files.write(file, oldBytes);
        FileStateChecker fileStateChecker = mock(FileStateChecker.class);
        when(fileStateChecker.getCurrentFileState(file)).thenReturn(FileStateChecker.FileState.DIFFERS);
        Archive archive = mock(Archive.class);
        when(archive.getInputStream(file)).thenReturn(new ByteArrayInputStream(newBytes));
        StaticFileUpdate update = new StaticFileUpdate(file, fileStateChecker, archive);
        update.keep();
        assertThat(Files.readAllBytes(file)).isEqualTo(oldBytes);
        assertThat(Files.readAllBytes(Paths.get(file + ".idm-new"))).isEqualTo(newBytes);
        assertFalse(Files.exists(Paths.get(file + ".idm-old")));
    }
}

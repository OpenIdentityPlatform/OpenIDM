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

import org.testng.annotations.AfterSuite;
import org.testng.annotations.BeforeSuite;

import static org.mockito.Mockito.*;
import static org.testng.AssertJUnit.assertFalse;
import static org.testng.AssertJUnit.assertTrue;


import org.testng.annotations.Test;

/**
 */
public class StaticFileUpdateTest {

    Path tempPath;

    @BeforeSuite
    public void createTempDirPath() throws IOException {
        tempPath = Files.createTempDirectory(this.getClass().getSimpleName());
    }

    @AfterSuite
    public void destroyTempDirPath() throws IOException {
        Files.delete(tempPath);
    }

    @Test
    public void testDoesNotExist() throws IOException {
        Path file = tempPath.resolve("test");
        FileState fileState = mock(FileState.class);
        Archive archive = mock(Archive.class);
        StaticFileUpdate update = new StaticFileUpdate(file, fileState, archive);
        assertFalse(update.exists());
    }

    // when(archive.getInputStream(any(Path.class))).thenReturn(Files.newInputStream(file));
    @Test
    public void testExists() throws IOException {
        Path file = Files.createTempFile(tempPath, null, null);
        FileState fileState = mock(FileState.class);
        Archive archive = mock(Archive.class);
        StaticFileUpdate update = new StaticFileUpdate(file, fileState, archive);
        assertTrue(update.exists());
        Files.delete(file);
    }

    /*
    try (PrintWriter writer = new PrintWriter(Files.newOutputStream(file))) {
        writer.write("Hello, world!");
    } catch (IOException e) {
        fail("IOException creating file");
    }
    */
}

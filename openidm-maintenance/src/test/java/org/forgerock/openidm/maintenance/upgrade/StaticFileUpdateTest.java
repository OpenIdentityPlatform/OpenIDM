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
 * Copyright 2016 ForgeRock AS.
 */
package org.forgerock.openidm.maintenance.upgrade;

import static org.assertj.core.api.Assertions.assertThat;
import static org.forgerock.openidm.maintenance.upgrade.StaticFileUpdate.NEW_SUFFIX;
import static org.forgerock.openidm.maintenance.upgrade.StaticFileUpdate.OLD_SUFFIX;
import static org.mockito.Mockito.*;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Date;

import org.apache.commons.io.FileUtils;
import org.forgerock.util.Function;
import org.mockito.Matchers;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.AfterSuite;
import org.testng.annotations.BeforeSuite;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

/**
 * Tests updating static files.
 */
public class StaticFileUpdateTest {

    private static final ProductVersion oldVersion = new ProductVersion("3.2.0", "5000");
    private static final ProductVersion newVersion = new ProductVersion("4.0.0", "6000");
    private static final long timestamp = new Date().getTime();

    private static final byte[] oldBytes = "oldcontent".getBytes();
    private static final byte[] newBytes = "newcontent".getBytes();

    private Path getOldVersionPath(Path file) {
        return Paths.get(file + OLD_SUFFIX + timestamp);
    }

    private Path getNewVersionPath(Path file) {
        return Paths.get(file + NEW_SUFFIX + timestamp);
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

    private StaticFileUpdate getStaticFileUpdate(FileStateChecker fileStateChecker) throws IOException {
        Archive archive = mock(Archive.class);
        when(archive.getVersion()).thenReturn(newVersion);
        when(archive.withInputStreamForPath(eq(tempFile), Matchers.<Function<InputStream, Void, IOException>>any()))
                .then(
                        new Answer<Void>() {
                            @Override
                            @SuppressWarnings("unchecked")
                            public Void answer(InvocationOnMock invocation) throws Throwable {
                                // first argument - (Path) invocation.getArguments()[0] - is the Path, unused
                                Function<InputStream, Void, IOException> function =
                                        (Function<InputStream, Void, IOException>) invocation.getArguments()[1];
                                return function.apply(new ByteArrayInputStream(newBytes));
                            }
                        });
        return new StaticFileUpdate(fileStateChecker, tempPath, archive, oldVersion, timestamp);
    }

    /**
     * Test a replacement on an unchanged path.
     */
    @Test
    public void testReplaceIsUnchanged() throws IOException {
        FileStateChecker fileStateChecker = mock(FileStateChecker.class);
        when(fileStateChecker.getCurrentFileState(tempFile)).thenReturn(FileState.UNCHANGED);
        StaticFileUpdate update = getStaticFileUpdate(fileStateChecker);
        update.replace(tempFile);
        assertThat(Files.readAllBytes(tempFile)).isEqualTo(newBytes);
        assertThat(Files.exists(getOldVersionPath(tempFile))).isFalse();
        assertThat(Files.exists(getNewVersionPath(tempFile))).isFalse();
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
        assertThat(Files.exists(getNewVersionPath(tempFile))).isFalse();
    }

    /**
     * Test keeping a file with no differences.  This should simply write the new file.
     */
    @Test
    public void testKeepIsUnchanged() throws IOException {
        Files.write(tempFile, oldBytes);
        FileStateChecker fileStateChecker = mock(FileStateChecker.class);
        when(fileStateChecker.getCurrentFileState(tempFile)).thenReturn(FileState.UNCHANGED);
        StaticFileUpdate update = getStaticFileUpdate(fileStateChecker);
        assertThat(update.keep(tempFile)).isNull();
        assertThat(Files.readAllBytes(tempFile)).isEqualTo(newBytes);
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
        assertThat(Files.exists(getOldVersionPath(tempFile))).isFalse();
    }
}

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
 * Copyright 2026 3A Systems LLC.
 */
package org.forgerock.openidm.maintenance.impl;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.assertTrue;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.Path;

import org.forgerock.json.JsonValue;
import org.forgerock.json.resource.BadRequestException;
import org.forgerock.json.resource.InternalServerErrorException;
import org.forgerock.json.resource.ReadRequest;
import org.forgerock.json.resource.Requests;
import org.forgerock.json.resource.ResourceException;
import org.forgerock.json.resource.ResourceResponse;
import org.forgerock.openidm.maintenance.upgrade.UpdateException;
import org.forgerock.openidm.maintenance.upgrade.UpdateManager;
import org.forgerock.services.context.Context;
import org.forgerock.services.context.RootContext;
import org.forgerock.util.promise.Promise;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

/**
 * Tests for path traversal protections in {@link UpdateArchiveService}.
 *
 * <p>Because mockito-all 1.10.19 does not support {@code mockStatic}, we avoid
 * the {@code IdentityServer.getInstance()} static call by subclassing
 * {@link UpdateArchiveService} and overriding {@link UpdateArchiveService#getInstallLocation()} —
 * a package-private hook that returns the temp directory. No production source
 * changes are required.
 *
 * <p>The fixture creates a real directory tree on disk (required by
 * {@code toRealPath()} which resolves paths against the live filesystem):
 * <pre>
 *   tmpDir/
 *     bin/
 *       update/               ← only path that should ever be accessible
 *         legit.zip
 *         subdir/
 *           nested.zip
 *     sensitive/
 *       secret.txt            ← must never be reachable via the service
 * </pre>
 */
public class UpdateArchiveServiceTest {

    // -----------------------------------------------------------------------
    // Test-only subclass — breaks the static IdentityServer dependency
    // -----------------------------------------------------------------------

    /**
     * Subclass that replaces the {@code IdentityServer.getInstance()} call with
     * a simple field so tests can supply any install root they like.
     */
    private static class TestableUpdateArchiveService extends UpdateArchiveService {

        private final File installLocation;

        TestableUpdateArchiveService(File installLocation) {
            this.installLocation = installLocation;
        }

        /**
         * Called by {@link #handleRead} instead of
         * {@code IdentityServer.getInstance().getInstallLocation()}.
         * Keeping this package-private means no public API is added to the
         * production class.
         */
        @Override
        File getInstallLocation() {
            return installLocation;
        }
    }

    // -----------------------------------------------------------------------
    // Fixture fields
    // -----------------------------------------------------------------------

    /** Root of the temporary install tree created per test method. */
    private Path tmpDir;

    /** The service instance wired to {@code tmpDir}. */
    private TestableUpdateArchiveService service;

    /** Mockito mock — verified in every test. */
    private UpdateManager updateManager;

    /** Minimal context required by the CREST handler signature. */
    private Context context;

    // -----------------------------------------------------------------------
    // Set-up / tear-down
    // -----------------------------------------------------------------------

    @BeforeMethod
    public void setUp() throws Exception {
        // Real filesystem tree — toRealPath() needs the directories to exist
        tmpDir = Files.createTempDirectory("openidm-test-");

        Path updateDir = tmpDir.resolve("bin/update");
        Files.createDirectories(updateDir);
        Files.createFile(updateDir.resolve("legit.zip"));

        Path subDir = updateDir.resolve("subdir");
        Files.createDirectories(subDir);
        Files.createFile(subDir.resolve("nested.zip"));

        Path sensitiveDir = tmpDir.resolve("sensitive");
        Files.createDirectories(sensitiveDir);
        Files.writeString(sensitiveDir.resolve("secret.txt"), "top-secret");

        updateManager = mock(UpdateManager.class);

        service = new TestableUpdateArchiveService(tmpDir.toFile());
        injectField(service, "updateManager", updateManager);

        context = new RootContext("test");
    }

    @AfterMethod
    public void tearDown() {
        deleteRecursively(tmpDir.toFile());
    }

    // -----------------------------------------------------------------------
    // Happy-path tests
    // -----------------------------------------------------------------------

    @Test(description = "A legitimate archive name with no sub-path reaches UpdateManager")
    public void testLegitimateArchiveRootRead() throws Exception {
        JsonValue expected = JsonValue.json(
                JsonValue.object(JsonValue.field("file", "legit.zip")));
        when(updateManager.getArchiveFile(any(Path.class), any(Path.class)))
                .thenReturn(expected);

        ResourceResponse response = invoke("legit.zip");

        assertNotNull(response);
        assertEquals(response.getContent().get("file").asString(), "legit.zip");
        verify(updateManager).getArchiveFile(any(Path.class), any(Path.class));
    }

    @Test(description = "A legitimate archive name with a nested in-archive sub-path is accepted")
    public void testLegitimateArchiveWithSubPath() throws Exception {
        when(updateManager.getArchiveFile(any(Path.class), any(Path.class)))
                .thenReturn(JsonValue.json(JsonValue.object()));

        invoke("legit.zip/content/inner.txt");

        verify(updateManager).getArchiveFile(any(Path.class), any(Path.class));
    }

    @Test(description = "An archive that lives in a sub-directory of update/ is accepted")
    public void testLegitimateNestedArchive() throws Exception {
        when(updateManager.getArchiveFile(any(Path.class), any(Path.class)))
                .thenReturn(JsonValue.json(JsonValue.object()));

        invoke("subdir/nested.zip");

        verify(updateManager).getArchiveFile(any(Path.class), any(Path.class));
    }

    // -----------------------------------------------------------------------
    // Archive-name path traversal  (Fix 1 — first path segment)
    // -----------------------------------------------------------------------

    @DataProvider(name = "archiveTraversalPaths")
    public Object[][] archiveTraversalPaths() {
        return new Object[][] {
                { "../sensitive/secret.txt",
                        "single dot-dot in archive name" },
                { "../../etc/passwd",
                        "double dot-dot targeting system file" },
                { "../update/legit.zip",
                        "dot-dot then back into update/ — still uses traversal" },
                { "%2e%2e/sensitive/secret.txt",
                        "percent-encoded dot-dot (router decodes before handler)" },
                { "legit.zip\u0000../../sensitive/secret.txt",
                        "null-byte injection carrying embedded traversal" },
        };
    }

    @Test(dataProvider = "archiveTraversalPaths",
            description = "Traversal via archive name is rejected with HTTP 400")
    public void testArchiveNameTraversalRejected(String maliciousPath, String description)
            throws Exception {
        assertBadRequest(
                service.handleRead(context, readRequest(maliciousPath)),
                description);
        verify(updateManager, never()).getArchiveFile(any(Path.class), any(Path.class));
    }

    // -----------------------------------------------------------------------
    // In-archive file-path traversal  (Fix 2 — tail path segments)
    // -----------------------------------------------------------------------

    @DataProvider(name = "inArchiveTraversalPaths")
    public Object[][] inArchiveTraversalPaths() {
        return new Object[][] {
                { "legit.zip/../../../sensitive/secret.txt",
                        "dot-dot in in-archive tail exits archive root" },
                { "legit.zip/content/../../../../../../etc/shadow",
                        "deep dot-dot chain in tail" },
                { "legit.zip/a/b/../../../sensitive/secret.txt",
                        "mixed traversal hidden inside directory names" },
        };
    }

    @Test(dataProvider = "inArchiveTraversalPaths",
            description = "Traversal via in-archive file path is rejected with HTTP 400")
    public void testInArchivePathTraversalRejected(String maliciousPath, String description)
            throws Exception {
        assertBadRequest(
                service.handleRead(context, readRequest(maliciousPath)),
                description);
        verify(updateManager, never()).getArchiveFile(any(Path.class), any(Path.class));
    }

    // -----------------------------------------------------------------------
    // UpdateManager failure propagation
    // -----------------------------------------------------------------------

    @Test(description = "UpdateException from UpdateManager is wrapped as HTTP 500")
    public void testUpdateExceptionBecomesInternalServerError() throws Exception {
        when(updateManager.getArchiveFile(any(Path.class), any(Path.class)))
                .thenThrow(new UpdateException("simulated failure"));

        ResourceException ex = expectException("legit.zip");

        assertTrue(ex instanceof InternalServerErrorException,
                "Expected InternalServerErrorException but got: "
                        + ex.getClass().getSimpleName());
        assertEquals(ex.getCode(), ResourceException.INTERNAL_ERROR);
    }

    // -----------------------------------------------------------------------
    // Symlink escape — boundary documentation test
    // -----------------------------------------------------------------------

    @Test(description = "Symlink inside update/ pointing outside is rejected by defense-in-depth toRealPath check")
    public void testSymlinkEscapeOutsideUpdateDirRejected() throws Exception {
        Path updateDir = tmpDir.resolve("bin/update");
        Path symlink = updateDir.resolve("evil-link");
        try {
            Files.createSymbolicLink(symlink, tmpDir.resolve("sensitive"));
        } catch (UnsupportedOperationException | IOException e) {
            // Symlinks not supported on this OS/FS — skip
            return;
        }

        // The archive path "evil-link" resolves to update/evil-link which nominally
        // starts with updateDir, so the prefix check passes.
        // However the defense-in-depth toRealPath() on archivePath resolves the
        // symlink target (<tmpDir>/sensitive) which does NOT start with updateDir,
        // so it is rejected with BadRequestException.
        assertBadRequest(
                service.handleRead(context, readRequest("evil-link/secret.txt")),
                "symlink escape outside update dir");
        verify(updateManager, never()).getArchiveFile(any(Path.class), any(Path.class));
    }

    @Test(description = "Broken symlink (target does not exist) is rejected")
    public void testBrokenSymlinkRejected() throws Exception {
        Path updateDir = tmpDir.resolve("bin/update");
        Path symlink = updateDir.resolve("broken-link");
        try {
            // Target deliberately does not exist on disk
            Files.createSymbolicLink(symlink, tmpDir.resolve("nonexistent"));
        } catch (UnsupportedOperationException | IOException e) {
            // Symlinks not supported on this OS/FS — skip
            return;
        }

        // toRealPath() throws IOException for a dangling symlink, which the
        // defense-in-depth block catches and converts to BadRequestException.
        assertBadRequest(
                service.handleRead(context, readRequest("broken-link/some-file.txt")),
                "broken symlink");
        verify(updateManager, never()).getArchiveFile(any(Path.class), any(Path.class));
    }

    @Test(description = "Symlink within update/ pointing to another location within update/ is accepted")
    public void testValidSymlinkWithinUpdateDirAccepted() throws Exception {
        Path updateDir = tmpDir.resolve("bin/update");
        // Create a real target directory and file inside update/
        Path realTarget = updateDir.resolve("real-target");
        Files.createDirectories(realTarget);
        Files.createFile(realTarget.resolve("data.zip"));

        // Create a symlink inside update/ pointing to another path inside update/
        Path symlink = updateDir.resolve("link-to-target");
        try {
            Files.createSymbolicLink(symlink, realTarget);
        } catch (UnsupportedOperationException | IOException e) {
            // Symlinks not supported on this OS/FS — skip
            return;
        }

        when(updateManager.getArchiveFile(any(Path.class), any(Path.class)))
                .thenReturn(JsonValue.json(JsonValue.object()));

        ResourceResponse response = invoke("link-to-target/data.zip");

        assertNotNull(response);
        verify(updateManager).getArchiveFile(any(Path.class), any(Path.class));
    }

    // -----------------------------------------------------------------------
    // Helpers
    // -----------------------------------------------------------------------

    /** Invokes the handler for the given resource path and expects success. */
    private ResourceResponse invoke(String resourcePath) throws Exception {
        Promise<ResourceResponse, ResourceException> promise =
                service.handleRead(context, readRequest(resourcePath));
        return promise.getOrThrow();
    }

    /** Invokes the handler and expects a {@link ResourceException}. */
    private ResourceException expectException(String resourcePath) throws Exception {
        Promise<ResourceResponse, ResourceException> promise =
                service.handleRead(context, readRequest(resourcePath));
        try {
            ResourceResponse response = promise.getOrThrow();
            throw new AssertionError(
                    "Expected a ResourceException but request succeeded: "
                            + response.getContent().asMap());
        } catch (ResourceException e) {
            return e;
        }
    }

    private static ReadRequest readRequest(String resourcePath) {
        return Requests.newReadRequest(resourcePath);
    }

    private static void assertBadRequest(
            Promise<ResourceResponse, ResourceException> promise, String description)
            throws Exception {
        ResourceException ex;
        try {
            ResourceResponse response = promise.getOrThrow();
            throw new AssertionError(
                    "Expected BadRequestException for [" + description
                            + "] but request succeeded: " + response.getContent().asMap());
        } catch (ResourceException e) {
            ex = e;
        }
        assertTrue(ex instanceof BadRequestException,
                "Expected BadRequestException for [" + description + "] but got: "
                        + ex.getClass().getSimpleName() + " — " + ex.getMessage());
        assertEquals(ex.getCode(), ResourceException.BAD_REQUEST,
                "HTTP status must be 400 for [" + description + "]");
    }

    private static void injectField(Object target, String fieldName, Object value)
            throws Exception {
        Class<?> clazz = target.getClass();
        while (clazz != null) {
            try {
                Field f = clazz.getDeclaredField(fieldName);
                f.setAccessible(true);
                f.set(target, value);
                return;
            } catch (NoSuchFieldException e) {
                clazz = clazz.getSuperclass();
            }
        }
        throw new NoSuchFieldException(fieldName + " not found in hierarchy of "
                + target.getClass().getName());
    }

    private static void deleteRecursively(File f) {
        if (f.isDirectory()) {
            File[] children = f.listFiles();
            if (children != null) {
                for (File child : children) {
                    deleteRecursively(child);
                }
            }
        }
        f.delete();
    }
}
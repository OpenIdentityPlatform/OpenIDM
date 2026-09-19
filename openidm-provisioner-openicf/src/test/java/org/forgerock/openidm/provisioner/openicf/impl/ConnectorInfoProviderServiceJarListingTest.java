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
 * Copyright 2026 3A Systems, LLC.
 */
package org.forgerock.openidm.provisioner.openicf.impl;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.lang.management.ManagementFactory;
import java.lang.management.OperatingSystemMXBean;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.jar.JarOutputStream;
import java.util.zip.CRC32;
import java.util.zip.ZipEntry;

import com.sun.management.UnixOperatingSystemMXBean;
import org.testng.SkipException;
import org.testng.annotations.Test;

/**
 * {@code ConnectorInfoProviderService.getJarFileListing} must not leak the file it opens when the
 * jar cannot be read.
 */
public class ConnectorInfoProviderServiceJarListingTest {

    private static final int ITERATIONS = 10;

    @Test
    public void testUnreadableJarDoesNotLeakFileDescriptor() throws Exception {
        OperatingSystemMXBean os = ManagementFactory.getOperatingSystemMXBean();
        if (!(os instanceof UnixOperatingSystemMXBean)) {
            throw new SkipException("Open file descriptor count is only available on Unix JVMs");
        }
        UnixOperatingSystemMXBean unix = (UnixOperatingSystemMXBean) os;

        File jar = truncatedJar();
        try {
            // the same shape as the bundle URL the service resolves: jar:file:/path/to.jar!/META-INF/bundles
            URL location = new URL("jar:" + jar.toURI() + "!/META-INF/bundles");
            Method listing = ConnectorInfoProviderService.class.getDeclaredMethod(
                    "getJarFileListing", URL.class, String.class);
            listing.setAccessible(true);

            // First call loads every class involved, which may open jars of its own.
            assertListingFails(listing, location);

            long before = unix.getOpenFileDescriptorCount();
            for (int i = 0; i < ITERATIONS; i++) {
                assertListingFails(listing, location);
            }
            long after = unix.getOpenFileDescriptorCount();

            assertThat(after)
                    .as("open file descriptors after " + ITERATIONS + " failed listings")
                    .isEqualTo(before);
        } finally {
            assertThat(jar.delete()).isTrue();
        }
    }

    private static void assertListingFails(Method listing, URL location) throws Exception {
        try {
            listing.invoke(null, location, null);
        } catch (InvocationTargetException e) {
            assertThat(e.getCause())
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageStartingWith("Unable to get Jar input stream from");
            return;
        }
        throw new AssertionError("listing a truncated jar should fail");
    }

    /**
     * A jar whose manifest entry is cut short: the {@code JarInputStream} constructor reads the
     * manifest and fails with an {@code IOException} before the stream can be returned.
     */
    private static File truncatedJar() throws Exception {
        byte[] manifest = new byte[1000];
        Arrays.fill(manifest, (byte) 'a');
        byte[] header = "Manifest-Version: 1.0\r\nX-Padding: ".getBytes(StandardCharsets.US_ASCII);
        System.arraycopy(header, 0, manifest, 0, header.length);
        CRC32 crc = new CRC32();
        crc.update(manifest);

        // Stored, so the entry data is exactly as long as the manifest and the cut lands inside it.
        ZipEntry entry = new ZipEntry("META-INF/MANIFEST.MF");
        entry.setMethod(ZipEntry.STORED);
        entry.setSize(manifest.length);
        entry.setCompressedSize(manifest.length);
        entry.setCrc(crc.getValue());

        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try (JarOutputStream out = new JarOutputStream(bytes)) {
            out.putNextEntry(entry);
            out.write(manifest);
            out.closeEntry();
        }

        File jar = File.createTempFile("truncated", ".jar");
        try (FileOutputStream out = new FileOutputStream(jar)) {
            out.write(bytes.toByteArray(), 0, 200);
        }
        return jar;
    }
}

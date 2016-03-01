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

import static org.forgerock.json.JsonValue.*;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.*;
import static org.testng.Assert.*;

import java.io.File;
import java.nio.file.Path;

import org.forgerock.json.JsonValue;
import org.forgerock.openidm.core.ServerConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

/**
 * Unit test to cover testing of the implementation of UpdateManager.
 *
 * @see UpdateManagerImpl
 */
public class UpdateManagerImplTest {
    private final static Logger logger = LoggerFactory.getLogger(UpdateManagerImplTest.class);
    private UpdateManagerImpl updateManager;
    private JsonValue testConfig = json(object());
    private File archiveFile;

    @BeforeMethod
    public void setupListAvailableUpdates() throws Exception {
        archiveFile = mock(File.class);
        when(archiveFile.getName()).thenReturn("test.zip");
        updateManager = mock(UpdateManagerImpl.class);
        when(updateManager.resolveChecksumFile(any(Path.class))).thenReturn(mock(ChecksumFile.class));
        when(updateManager.getUpdateFiles()).thenReturn(new File[]{archiveFile});
        when(updateManager.listAvailableUpdates()).thenCallRealMethod();

        // Setup update properties for further tests.
        testConfig.clear();
        when(updateManager.readUpdateConfig(any(File.class))).thenReturn(testConfig);
    }

    @Test
    public void testListAvailableUpdatesBadFilename() throws Exception {
        when(archiveFile.getName()).thenReturn("test.xyz");

        // Test when properties file isn't found in the archive.
        JsonValue responseJson = updateManager.listAvailableUpdates();
        logger.info("response json is {}", responseJson.toString());
        assertFalse(responseJson.get("rejects").asList().isEmpty(),
                "Rejects should be populated as can't read properties of the mocked archiveFile.");
        assertTrue(responseJson.get("rejects").get(0).get("reason").asString().endsWith("'.zip' extension."),
                "Reject reason message is expected to be regarding properties testing.");
    }

    @Test
    public void testListAvailableUpdatesBadProperties() throws Exception {
        when(updateManager.readUpdateConfig(any(File.class))).thenThrow(
                new InvalidArchiveUpdateException("test.zip", "bad properties"));

        // Test when properties file isn't found in the archive.
        JsonValue responseJson = updateManager.listAvailableUpdates();
        logger.info("response json is {}", responseJson.toString());
        assertFalse(responseJson.get("rejects").asList().isEmpty(),
                "Rejects should be populated as can't read properties of the mocked archiveFile.");
        assertTrue(responseJson.get("rejects").get(0).get("reason").asString().equals("bad properties"),
                "Reject reason message is expected to be regarding properties testing.");
    }

    @Test
    public void testListAvailableUpdatesDifferentProducts() throws Exception {
        // Test when update is for different product.
        testConfig.add("origin", object(
                field("product", "OTHER_PRODUCT")
                ));

        JsonValue responseJson = updateManager.listAvailableUpdates();
        logger.info("response json is {}", responseJson.toString());
        assertFalse(responseJson.get("rejects").asList().isEmpty(),
                "Rejects should be populated as the update is for a different product.");
        assertTrue(responseJson.get("rejects").get(0).get("reason").asString().endsWith(UpdateManagerImpl.PRODUCT_NAME),
                "Reject reason message is expected to be the product mismatch error.");
    }

    @Test
    public void testListAvailableUpdatesDifferentVersion() throws Exception {
        // Test when update is for different version.
        testConfig.add("origin", object(
                field("product", UpdateManagerImpl.PRODUCT_NAME),
                field("version", array("X.X.X"))
                ));

        JsonValue responseJson = updateManager.listAvailableUpdates();
        logger.info("response json is {}", responseJson.toString());
        assertFalse(responseJson.get("rejects").asList().isEmpty(),
                "Rejects should be populated as the update is for a different product version.");
        assertTrue(responseJson.get("rejects").get(0).get("reason").asString().endsWith(ServerConstants.getVersion()),
                "Reject reason message is expected to be the version mismatch error.");
    }

    @Test
    public void testListAvailableUpdatesIsValid() throws Exception {
        // Test when update should be valid.
        testConfig.add("origin", object(
                field("product", UpdateManagerImpl.PRODUCT_NAME),
                field("version", array(ServerConstants.getVersion()))
                ));
        testConfig.add("destination", object(
                field("product", UpdateManagerImpl.PRODUCT_NAME),
                field("version", ServerConstants.getVersion())
                ));
        testConfig.add("update", object(
                field("description", "description"),
                field("resource", "url"),
                field("restartRequired", false)
                ));
        when(updateManager.extractFileToDirectory(any(File.class), any(Path.class))).thenReturn(mock(Path.class));
        JsonValue responseJson = updateManager.listAvailableUpdates();
        logger.info("response json is {}", responseJson.toString());
        assertTrue(responseJson.get("rejects").asList().isEmpty(), "Archive should not be rejected.");
        assertTrue(responseJson.get("updates").get(0).get("archive").asString().equals("test.zip"),
                "The archive should match our test filename.");
    }

    @Test
    public void testListAvailableUpdatesMissingChecksum() throws Exception {
        testConfig.add("origin", object(
                field("product", UpdateManagerImpl.PRODUCT_NAME),
                field("version", array(ServerConstants.getVersion()))
        ));
        when(updateManager.extractFileToDirectory(any(File.class), any(Path.class))).thenThrow(
                new UpdateException("missing checksum file"));
        JsonValue responseJson = updateManager.listAvailableUpdates();
        logger.info("response json is {}", responseJson.toString());
        assertFalse(responseJson.get("rejects").asList().isEmpty(),
                "Rejects should be populated as checksum file is missing.");
        assertTrue(responseJson.get("rejects").get(0).get("archive").asString().equals("test.zip"),
                "The archive should match our test filename.");
        assertTrue(responseJson.get("rejects").get(0).get("reason").asString().equals("Archive doesn't appear to contain checksums file."));
        assertTrue(responseJson.get("rejects").get(0).get("errorMessage").asString().equals("missing checksum file"));
    }
}

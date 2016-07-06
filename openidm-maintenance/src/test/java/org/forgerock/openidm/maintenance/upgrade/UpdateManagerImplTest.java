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
import static org.forgerock.json.test.assertj.AssertJJsonValueAssert.assertThat;
import static org.mockito.Mockito.*;

import java.io.File;
import java.nio.file.Path;

import org.assertj.core.api.Assertions;
import org.forgerock.json.JsonValue;
import org.forgerock.openidm.core.ServerConstants;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

/**
 * Unit test to cover testing of the implementation of UpdateManager.
 *
 * @see UpdateManagerImpl
 */
public class UpdateManagerImplTest {
    private final static Logger logger = LoggerFactory.getLogger(UpdateManagerImplTest.class);

    private JsonValue testConfig = json(object());
    private File archiveFile;

    @BeforeMethod
    public void setupListAvailableUpdates() throws Exception {
        archiveFile = mock(File.class);
        when(archiveFile.getName()).thenReturn("test.zip");

        // Setup update properties for further tests.
        testConfig.clear();
    }

    // Create a "concrete", mock UpdateManagerImpl - good for many tests
    private UpdateManagerImpl newUpdateManager() {
        return new UpdateManagerImpl() {
            File[] getUpdateFiles() {
                return new File[]{ archiveFile };
            }
            JsonValue readUpdateConfig(File file) throws InvalidArchiveUpdateException {
                return testConfig;
            }
            ChecksumFile resolveChecksumFile(Path path) throws UpdateException {
                return mock(ChecksumFile.class);
            }
            Path extractFileToDirectory(File zipFile, Path fileToExtract) throws UpdateException {
                return mock(Path.class);
            }
        };
    }

    @Test
    public void testListAvailableUpdatesBadFilename() throws Exception {
        when(archiveFile.getName()).thenReturn("test.xyz");

        UpdateManagerImpl updateManager = newUpdateManager();

        // Test when properties file isn't found in the archive.
        JsonValue responseJson = updateManager.listAvailableUpdates();
        logger.info("response json is {}", responseJson.toString());
        // Rejects should be populated as can't read properties of the mocked archiveFile.
        assertThat(responseJson).hasArray("rejects").hasSize(1);
        // Reject reason message is expected to be regarding properties testing.
        assertThat(responseJson.get("rejects").get(0)).stringAt("reason").endsWith("'.zip' extension.");
    }

    @Test
    public void testListAvailableUpdatesBadProperties() throws Exception {
        // this UpdateManagerImpl throws an exception on reading the update config to test
        // listAvailableUpdates' ability to return the proper error
        UpdateManagerImpl updateManager = new UpdateManagerImpl() {
            File[] getUpdateFiles() {
                return new File[]{ archiveFile };
            }
            JsonValue readUpdateConfig(File file) throws InvalidArchiveUpdateException {
                throw new InvalidArchiveUpdateException("test.zip", "bad properties");
            }
            ChecksumFile resolveChecksumFile(Path path) throws UpdateException {
                return mock(ChecksumFile.class);
            }
        };

        // Test when properties file isn't found in the archive.
        JsonValue responseJson = updateManager.listAvailableUpdates();
        logger.info("response json is {}", responseJson.toString());
        // Rejects should be populated as can't read properties of the mocked archiveFile.
        assertThat(responseJson).hasArray("rejects").hasSize(1);
        // Reject reason message is expected to be regarding properties testing.
        assertThat(responseJson.get("rejects").get(0)).stringAt("reason").isEqualTo("bad properties");
    }

    @Test
    public void testListAvailableUpdatesDifferentProducts() throws Exception {
        // Test when update is for different product.
        testConfig.add("origin", object(
                field("product", "OTHER_PRODUCT")
                ));

        UpdateManagerImpl updateManager = newUpdateManager();

        JsonValue responseJson = updateManager.listAvailableUpdates();
        logger.info("response json is {}", responseJson.toString());
        // Rejects should be populated as the update is for a different product.
        assertThat(responseJson).hasArray("rejects").hasSize(1);
        // Reject reason message is expected to be the product mismatch error.
        assertThat(responseJson.get("rejects").get(0)).stringAt("reason").endsWith(UpdateManagerImpl.PRODUCT_NAME);
    }

    @Test
    public void testListAvailableUpdatesDifferentVersion() throws Exception {
        // Test when update is for different version.
        testConfig.add("origin", object(
                field("product", UpdateManagerImpl.PRODUCT_NAME),
                field("version", array("X.X.X"))
                ));

        UpdateManagerImpl updateManager = newUpdateManager();

        JsonValue responseJson = updateManager.listAvailableUpdates();
        logger.info("response json is {}", responseJson.toString());
        // Rejects should be populated as the update is for a different product version.
        assertThat(responseJson).hasArray("rejects").hasSize(1);
        // Reject reason message is expected to be the version mismatch error.
        assertThat(responseJson.get("rejects").get(0)).stringAt("reason").endsWith(ServerConstants.getVersion());
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
        UpdateManagerImpl updateManager = newUpdateManager();

        JsonValue responseJson = updateManager.listAvailableUpdates();
        logger.info("response json is {}", responseJson.toString());
        // Archive should not be rejected.
        assertThat(responseJson).hasArray("rejects").isEmpty();
        // The archive should match our test filename.
        assertThat(responseJson).hasArray("updates").hasSize(1);
        assertThat(responseJson.get("updates").get(0)).stringAt("archive").isEqualTo("test.zip");
    }

    @Test
    public void testListAvailableUpdatesMissingChecksum() throws Exception {
        testConfig.add("origin", object(
                field("product", UpdateManagerImpl.PRODUCT_NAME),
                field("version", array(ServerConstants.getVersion()))
        ));
        // this UpdateManagerImpl throws an exception when extracting the file
        // to simulate a bad checksum in order to test listAvailableUpdates'
        // ability to return the proper error
        UpdateManagerImpl updateManager = new UpdateManagerImpl() {
            File[] getUpdateFiles() {
                return new File[]{ archiveFile };
            }
            JsonValue readUpdateConfig(File file) throws InvalidArchiveUpdateException {
                return testConfig;
            }
            ChecksumFile resolveChecksumFile(Path path) throws UpdateException {
                return mock(ChecksumFile.class);
            }
            Path extractFileToDirectory(File zipFile, Path fileToExtract) throws UpdateException {
                throw new UpdateException("missing checksum file");
            }
        };

        JsonValue responseJson = updateManager.listAvailableUpdates();
        logger.info("response json is {}", responseJson.toString());
        // Rejects should be populated as checksum file is missing.
        assertThat(responseJson).hasArray("rejects").hasSize(1);
        // The archive should match our test filename.
        assertThat(responseJson.get("rejects").get(0)).stringAt("archive").isEqualTo("test.zip");
        assertThat(responseJson.get("rejects").get(0)).stringAt("reason").isEqualTo("The archive test.zip does not appear to contain a checksums file.");
        assertThat(responseJson.get("rejects").get(0)).stringAt("errorMessage").isEqualTo("missing checksum file");
    }

    @DataProvider
    public Object[][] versions() {
        return new Object[][] {
                // @formatter:off
                { "5.0.0", "5.0.0", true },
                { "5.0.0-1", "5.0.0-1", false },
                { "5.0.1", "5.0.1", false },
                { "5.0.0-RC1", "5.0.0", true },
                { "5.0.0-SNAPSHOT", "5.0.0", true },
                { "5.0.0-RC3-SNAPSHOT", "5.0.0", true },
                { "5.0.0-1-SNAPSHOT", "5.0.0-1", false }
                // @formatter:on
        };
    }

    @Test(dataProvider = "versions")
    public void testGetBaseProductVersion(final String fullVersion, final String baseVersion, boolean unused) {
        UpdateManagerImpl updateManager = new UpdateManagerImpl() {
            String getProductVersion() {
                return fullVersion;
            }
        };
        Assertions.assertThat(baseVersion).isEqualTo(updateManager.getBaseProductVersion());
    }

    @Test(dataProvider = "versions")
    public void testArchiveVersionMatch(final String version, final String baseVersion, final boolean shouldMatch)
            throws Exception {
        testConfig.add("origin", object(
                field("product", UpdateManagerImpl.PRODUCT_NAME),
                field("version", array("5.0.0"))
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
        UpdateManagerImpl updateManager = new UpdateManagerImpl() {
            String getProductVersion() {
                return version;
            }
            String getBaseProductVersion() {
                return baseVersion;
            }
        };
        try {
            updateManager.validateCorrectVersion(testConfig, new File("foo"));
            org.assertj.core.api.Assertions.assertThat(shouldMatch).isTrue();
        } catch (InvalidArchiveUpdateException e) {
            org.assertj.core.api.Assertions.assertThat(shouldMatch).isFalse();
        }
    }
}

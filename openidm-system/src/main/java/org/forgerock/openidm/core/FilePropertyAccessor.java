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
 * Copyright 2017 ForgeRock AS.
 */
package org.forgerock.openidm.core;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Property Accessor that provides access to properties kept in a properties file.
 */
class FilePropertyAccessor implements PropertyAccessor {

    private static final Logger logger = LoggerFactory.getLogger(FilePropertyAccessor.class);

    private Map<String, String> properties = new HashMap<>();

    FilePropertyAccessor(String fileLocation, String serverRoot) {
        properties = loadProps(fileLocation, serverRoot);
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T> T getProperty(String key, T defaultValue, Class<T> expected) {
        T value = null;
        if (null != key
                && ((null != expected && expected.isAssignableFrom(String.class)) || defaultValue instanceof String)) {
            value = (T) properties.get(key);
        }
        return (null != value)
                ? value
                : defaultValue;
    }

    /**
     * Loads properties file
     *
     * @return map of the properties in the properties file.
     */
    private Map<String, String> loadProps(String fileLocation, String serverRoot) {
        File propertyFile = IdentityServer.getFileForPath(fileLocation, serverRoot);
        Map<String, String> entries = new HashMap<>();

        if (null == propertyFile) {
            logger.error("No file properties: " + serverRoot + fileLocation);
        } else {
            String fileAbsolutePath = propertyFile.getAbsolutePath();
            if (!propertyFile.exists()) {
                logger.error("No properties file detected at " + fileAbsolutePath);
            } else {
                logger.info("Using properties at " + fileAbsolutePath);
                Properties prop = new Properties();
                try (
                        FileInputStream fileInputStream = new FileInputStream(propertyFile);
                        InputStream in = new BufferedInputStream(fileInputStream)
                ) {
                    // Load the properties.
                    prop.load(in);

                    // Load them into the String/String map.
                    for (Map.Entry<Object, Object> entry : prop.entrySet()) {
                        entries.put((String) entry.getKey(), (String) entry.getValue());
                    }
                } catch (IOException ex) {
                    throw new IllegalStateException("Failed to load properties file " + fileAbsolutePath, ex);
                }
            }
        }
        return entries;
    }

}

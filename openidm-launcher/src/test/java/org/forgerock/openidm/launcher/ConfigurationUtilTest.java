/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2012-2015 ForgeRock AS. All Rights Reserved
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

package org.forgerock.openidm.launcher;

import static org.junit.Assert.*;

import java.io.InputStream;
import java.net.URL;
import java.util.Arrays;
import java.util.Vector;

import org.testng.annotations.Test;

/**
 * A NAME does ...
 * 
 * @author Laszlo Hordos
 */
public class ConfigurationUtilTest {

    public static final String NORTH = "North";
    public static final String SOUTH = "South";

    @Test
    public void testGetZipFileListing() throws Exception {
        URL zip = ConfigurationUtilTest.class.getResource("/test2/bundles.zip");
        Vector<URL> result = ConfigurationUtil.getZipFileListing(zip, null, null);
        assertEquals("Find all files", 3, result.size());
        for (URL file : result) {
            InputStream is = null;
            try {
                is = file.openConnection().getInputStream();
                if (is != null) {
                    assertTrue("Stream is empty", is.available() > 0);
                } else {
                    fail("Can not read from " + file);
                }
            } catch (Exception e) {
                fail(e.getMessage());
            } finally {
                if (null != is) {
                    try {
                        is.close();
                    } catch (Exception e) {/* ignore */
                    }
                }
            }
        }
        result =
                ConfigurationUtil.getZipFileListing(zip, Arrays.asList(new String[] { "**/*jar" }),
                        null);
        assertEquals("Find all jar files", 2, result.size());
        result =
                ConfigurationUtil.getZipFileListing(zip, Arrays.asList(new String[] { "*jar" }),
                        null);
        assertEquals("Find jar file in the root", 1, result.size());
        result =
                ConfigurationUtil.getZipFileListing(zip, Arrays
                        .asList(new String[] { "bundle/*jar" }), null);
        assertEquals("Find jar file in the bundle", 1, result.size());
        result =
                ConfigurationUtil.getZipFileListing(zip, Arrays.asList(new String[] { "**/*jar" }),
                        Arrays.asList(new String[] { "bundle/*jar" }));
        assertEquals("Find jar file in the root exclude the bundle", 1, result.size());
    }

    @Test
    public void testParameterizedProperties() {
        PropertyAccessor configuration = new PropertyAccessor() {
            @Override
            public <T> T get(String name) {
                if (name.equals("value1")) {
                    return (T) NORTH;
                } else if (name.equals("value2")) {
                    return (T) SOUTH;
                }
                return null;
            }
        };

        //validate $ evaluation
        Object substVars = ConfigurationUtil.substVars("${value1}", configuration);
        assertEquals("validate $ evaluation",NORTH, substVars.toString());

        //validate & evaluation
        substVars = ConfigurationUtil.substVars("&{value2}",configuration);
        assertEquals("validate & evaluation", SOUTH, substVars.toString());

        //validate both $ and & evaluation
        substVars = ConfigurationUtil.substVars("${value1} vs &{value2}",configuration);
        assertEquals("validate $ and & evaluation", NORTH + " vs "+ SOUTH, substVars.toString());
    }
}

/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2011-2013 ForgeRock AS. All Rights Reserved
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

package org.forgerock.openidm.osgi;

import org.testng.Assert;
import org.testng.annotations.Test;

/**
 * @version $Revision$ $Date$
 */
public class OsgiNameTest {

    @Test
    public void checkValidNames() throws Exception {
        validateStringName("openidm", "services", "org.forgerock.openidm.objset.ObjectSet",
                "(component.name=org.forgerock.openidm.router)");
        validateStringName("openidm", "services", "org.forgerock.openidm.objset.ObjectSet");
        validateStringName("osgi", "service", "org.forgerock.openidm.objset.ObjectSet");
        validateStringName("osgi", "service", "org.forgerock.openidm.objset.ObjectSet",
                "(component.name=org.forgerock.openidm.router)");
        validateStringName("osgi", "servicelist", "org.forgerock.openidm.objset.ObjectSet");
        validateStringName("osgi", "servicelist", "org.forgerock.openidm.objset.ObjectSet",
                "(component.name=org.forgerock.openidm.router)");
        validateStringName("osgi", "servicelist", "jdbc", "openidm", "DataSource");
        validateStringName("osgi", "framework", "bundleContext");
        validateStringName("osgi", "service", "javax.sql.DataSource",
                "(osgi.jndi.servicee.name=jdbc/openidm)");
        validateStringName("osgi", "service", "javax.sql.DataSource", "(&(a=/b)(c=/d))");
        validateStringName("osgi", "service");
    }

    private void validateStringName(String scheme, String path, String... elements)
            throws Exception {
        StringBuilder builder = new StringBuilder();
        StringBuilder serviceName = new StringBuilder();

        builder.append(scheme);
        builder.append(':');
        builder.append(path);

        if (elements.length > 0) {
            builder.append('/');

            for (String element : elements) {
                serviceName.append(element);
                serviceName.append('/');
            }

            serviceName.deleteCharAt(serviceName.length() - 1);

            builder.append(serviceName);
        }

        OsgiName n = OsgiName.parse(builder.toString());

        Assert.assertEquals(scheme, n.getScheme());
        Assert.assertEquals(path, n.getSchemePath());

        if (elements.length > 1) {
            Assert.assertEquals(elements[0], n.getInterface());
            if (elements.length == 2) {
                Assert.assertTrue(n.hasFilter(), "There is no filter in the name");
                Assert.assertEquals(elements[1], n.getFilter());
            } else {
                Assert.assertFalse(n.hasFilter());
            }
        }

        if (elements.length == 1) {
            Assert.assertFalse(n.hasFilter(), "There is a filter in the name");
        }

        Assert.assertEquals(serviceName.toString(), n.getServiceName());
    }
}

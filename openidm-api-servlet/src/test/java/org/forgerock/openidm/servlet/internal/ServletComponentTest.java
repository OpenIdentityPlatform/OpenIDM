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
 * Copyright 2025 3A Systems LLC.
 */

package org.forgerock.openidm.servlet.internal;

import static org.assertj.core.api.Assertions.assertThat;

import org.testng.annotations.AfterMethod;
import org.testng.annotations.Test;

/**
 * Unit tests for {@link ServletComponent} context path configuration.
 */
public class ServletComponentTest {

    @AfterMethod
    public void clearSystemProperty() {
        System.clearProperty(ServletComponent.OPENIDM_CONTEXT_PATH_PROPERTY);
    }

    @Test
    public void testDefaultServletAlias() {
        // When no system property is set, should return the default /openidm
        System.clearProperty(ServletComponent.OPENIDM_CONTEXT_PATH_PROPERTY);
        assertThat(ServletComponent.getServletAlias()).isEqualTo("/openidm");
    }

    @Test
    public void testCustomServletAlias() {
        // When system property is set to /myidm, should return /myidm
        System.setProperty(ServletComponent.OPENIDM_CONTEXT_PATH_PROPERTY, "/myidm");
        assertThat(ServletComponent.getServletAlias()).isEqualTo("/myidm");
    }

    @Test
    public void testServletAliasWithoutLeadingSlash() {
        // Should add leading slash if missing
        System.setProperty(ServletComponent.OPENIDM_CONTEXT_PATH_PROPERTY, "myidm");
        assertThat(ServletComponent.getServletAlias()).isEqualTo("/myidm");
    }

    @Test
    public void testServletAliasWithTrailingSlash() {
        // Should remove trailing slash
        System.setProperty(ServletComponent.OPENIDM_CONTEXT_PATH_PROPERTY, "/myidm/");
        assertThat(ServletComponent.getServletAlias()).isEqualTo("/myidm");
    }

    @Test
    public void testServletAliasConstants() {
        assertThat(ServletComponent.OPENIDM_CONTEXT_PATH_PROPERTY).isEqualTo("openidm.context.path");
        assertThat(ServletComponent.OPENIDM_CONTEXT_PATH_DEFAULT).isEqualTo("/openidm");
    }
}

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
 * Copyright 2013-2016 ForgeRock AS.
 */

package org.forgerock.openidm.auth.modules;

import static org.testng.Assert.assertEquals;

import org.forgerock.http.protocol.Request;
import org.forgerock.openidm.auth.modules.IDMAuthModuleWrapper.Credential;
import org.forgerock.openidm.auth.modules.IDMAuthModuleWrapper.CredentialHelper;
import org.forgerock.util.encode.Base64;
import org.testng.annotations.Test;

/**
 * Test the {@link CredentialHelper} implementations.
 */
public class CredentialHelperTest {

    /**
     * Test that password with colon will be correctly handled by BASIC_AUTH_CRED_HELPER.
     * Password with colon is valid according to the RFC 2617(https://tools.ietf.org/html/rfc2617#section-2).
     */
    @Test
    public void shouldSplitHttpBasicPasswordWithColon() {
        // Prepare dummy request with HTTP Basic authorization header
        String username = "username";
        String password = "foo:bar";
        Request request = new Request();
        request.getHeaders().put("Authorization", "Basic " + Base64.encode(
                (username + ":" + password).getBytes()));
        // Extract credentials from prepared request
        Credential credential = IDMAuthModuleWrapper.BASIC_AUTH_CRED_HELPER.getCredential(request);
        // Assert that credentials were extracted correctly
        assertEquals(credential.username, username);
        assertEquals(credential.password, password);
    }
}

/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2014 ForgeRock AS. All Rights Reserved
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

import AzureADOAuth2HttpClientFactory
import org.apache.http.auth.UsernamePasswordCredentials
import org.apache.olingo.client.api.v3.ODataClient
import org.apache.olingo.client.core.ODataClientFactory
import org.forgerock.openicf.misc.scriptedcommon.ScriptedConfiguration
import org.identityconnectors.common.logging.Log

def configuration = configuration as ScriptedConfiguration
def log = log as Log

def oauth2 = configuration.getPropertyBag().get("oauth2") as ConfigObject

final AzureADOAuth2HttpClientFactory oauth2HCF = new AzureADOAuth2HttpClientFactory(
        oauth2.get("authority"),
        oauth2.get("clientId"),
        oauth2.get("clientSecret"),
        oauth2.get("redirectURI"),
        oauth2.get("resourceURI"),
        new UsernamePasswordCredentials(
                oauth2.get("username"),
                oauth2.get("password")));

//final ODataClient client = ODataClientFactory.getEdmEnabledV3("https://graph.windows.net/contoso.onmicrosoft.com/");
final ODataClient client = ODataClientFactory.getEdmEnabledV3(configuration.getPropertyBag().get("graphServiceUrl") as String)
client.getConfiguration().setHttpClientFactory(oauth2HCF);


configuration.propertyBag.putIfAbsent("ODataClient", client)

configuration.releaseClosure = {
    def c = propertyBag.get("ODataClient") as ODataClient
    ((Closeable) c.configuration.getHttpClientFactory()).close()
}

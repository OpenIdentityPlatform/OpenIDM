/**
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

define([
    "jquery",
    "underscore",
    "org/forgerock/openidm/ui/admin/connector/ConnectorTypeAbstractView"
], function($, _,
            ConnectorTypeAbstractView) {

    var MarketoView = ConnectorTypeAbstractView.extend({
        connectorSaved: function(patchs, connectorDetails) {

            _.each(patchs, (patch) => {
                if(patch.field === "/configurationProperties/clientId" || patch.field === "/configurationProperties/clientSecret") {
                    patch.value = patch.value.trim();
                }
            });

            patchs.push(
                {
                    "operation": "replace",
                    "field": "/configurationProperties/scriptRoots",
                    "value": ["jar:file:connectors/marketo-connector-" +connectorDetails.connectorRef.bundleVersion +".jar!/script/marketo/"]
                },
                {
                    "operation": "replace",
                    "field": "/configurationProperties/createScriptFileName",
                    "value": "CreateMarketo.groovy"
                },
                {
                    "operation": "replace",
                    "field": "/configurationProperties/deleteScriptFileName",
                    "value": "DeleteMarketo.groovy"
                },
                {
                    "operation": "replace",
                    "field": "/configurationProperties/schemaScriptFileName",
                    "value": "SchemaMarketo.groovy"
                },
                {
                    "operation": "replace",
                    "field": "/configurationProperties/searchScriptFileName",
                    "value": "SearchMarketo.groovy"
                },
                {
                    "operation": "replace",
                    "field": "/configurationProperties/testScriptFileName",
                    "value": "TestMarketo.groovy"
                },
                {
                    "operation": "replace",
                    "field": "/configurationProperties/updateScriptFileName",
                    "value": "UpdateMarketo.groovy"
                },
                {
                    "operation": "replace",
                    "field": "/configurationProperties/reloadScriptOnExecution",
                    "value": false
                }
            );

            return patchs;
        },
        connectorCreate : function(details) {
            if(_.isNull(details.configurationProperties.scriptRoots)){
                details.configurationProperties.scriptRoots = ["jar:file:connectors/marketo-connector-" +details.connectorRef.bundleVersion +".jar!/script/marketo/"];
            }

            if(_.isNull(details.configurationProperties.clientSecret) && _.isObject(details.configurationProperties.clientSecret)){
                details.configurationProperties.clientSecret = details.configurationProperties.clientSecret;
            }

            details.configurationProperties.clientId = details.configurationProperties.clientId.trim();
            details.configurationProperties.clientSecret = details.configurationProperties.clientSecret.trim();

            details.configurationProperties.createScriptFileName = "CreateMarketo.groovy";
            details.configurationProperties.deleteScriptFileName = "DeleteMarketo.groovy";
            details.configurationProperties.schemaScriptFileName = "SchemaMarketo.groovy";
            details.configurationProperties.searchScriptFileName = "SearchMarketo.groovy";
            details.configurationProperties.testScriptFileName = "TestMarketo.groovy";
            details.configurationProperties.updateScriptFileName = "UpdateMarketo.groovy";
            details.configurationProperties.reloadScriptOnExecution = false;

            return details;
        }
    });

    return new MarketoView();
});

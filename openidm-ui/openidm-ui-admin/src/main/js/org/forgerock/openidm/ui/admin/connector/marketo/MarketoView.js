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
        connectorSaved: function(details) {
            details.configurationProperties.scriptRoots = ["jar:file:connectors/marketo-connector-1.4.0.0.jar!/script/marketo/"];
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

/**
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2014 ForgeRock AS. All rights reserved.
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

/*global $, define, _ */

define("org/forgerock/openidm/ui/admin/delegates/ConnectorDelegate", [
    "org/forgerock/commons/ui/common/util/Constants",
    "org/forgerock/commons/ui/common/main/AbstractDelegate",
    "org/forgerock/commons/ui/common/main/EventManager"
], function(constants, AbstractDelegate, eventManager) {

    var obj = new AbstractDelegate(constants.host + "/openidm/system");

    obj.availableConnectors = function() {
        return obj.serviceCall({
            url: "?_action=CREATECONFIGURATION", 
            type: "POST"

        });
    };

    obj.detailsConnector = function(connectorParams, errorsHandlers) {
        return obj.serviceCall({
            url: "?_action=CREATECONFIGURATION",
            type: "POST",
            data: JSON.stringify(connectorParams),
            errorsHandlers: errorsHandlers
        });
    };

    obj.testConnector = function(connectorParams) {
        var errorHandlers = {
            "error": {
                status: "500"
            }
        };

        return this.detailsConnector(connectorParams, errorHandlers);
    }; 

    obj.currentConnectors = function() {
        return obj.serviceCall({
            url: "?_action=test", 
            type: "POST"
        });
    };
    
    return obj;
});





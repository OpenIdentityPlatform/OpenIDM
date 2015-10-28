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
 * Copyright 2015 ForgeRock AS.
 */

/*global define */

define("org/forgerock/openidm/ui/admin/delegates/MaintenanceDelegate", [
    "jquery",
    "org/forgerock/commons/ui/common/util/Constants",
    "org/forgerock/commons/ui/common/main/AbstractDelegate"
], function($, constants, AbstractDelegate) {

    var obj = new AbstractDelegate(constants.host + "/openidm/maintenance");

    obj.getStatus = function () {
        return obj.serviceCall({
            url: "?_action=status",
            type: "POST"
        });
    };

    obj.disable = function () {
        return obj.serviceCall({
            url: "?_action=disable",
            type: "POST"
        });
    };

    obj.enable = function () {
        return obj.serviceCall({
            url: "?_action=enable",
            type: "POST"
        });
    };

    obj.getUpdateLogs = function () {
        return obj.serviceCall({
            url: "/update/log/?_queryFilter=true",
            type: "GET"
        });
    };

    obj.availableUpdateVersions = function () {
        return obj.serviceCall({
            url: "/update?_action=available",
            type: "POST"
        });
    };

    obj.getLicense = function (archive) {
        return obj.serviceCall({
            url: "/update?_action=getLicense&archive=" + archive,
            type: "POST"
        });
    };

    obj.preview = function (archive) {
        return obj.serviceCall({
            url: "/update?_action=preview&archive=" + archive,
            type: "POST"
        });
    };

    obj.update = function (archive) {
        return obj.serviceCall({
            url: "/update?_action=update&archive=" + archive,
            type: "POST"
        });
    };

    obj.getLastUpdateId = function () {
        return obj.serviceCall({
            url: "/update?_action=lastUpdateId",
            type: "POST",
            timeout: 1000,
            errorsHandlers: {
                "timeout": {
                    value: 0,
                    field: "readyState"
                }
            }
        });
    };

    obj.getLogDetails = function (logId) {
        return obj.serviceCall({
            url: "/update/log/" + logId,
            type: "GET"
        });
    };

    obj.restartIDM = function () {
        return obj.serviceCall({
            url: "/update?_action=restart",
            type: "POST"
        });
    };

    return obj;
});

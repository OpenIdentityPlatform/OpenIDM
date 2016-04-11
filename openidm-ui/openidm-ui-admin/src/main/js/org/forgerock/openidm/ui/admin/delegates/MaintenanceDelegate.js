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
 * Copyright 2015-2016 ForgeRock AS.
 */

/*global define */

define("org/forgerock/openidm/ui/admin/delegates/MaintenanceDelegate", [
    "jquery",
    "lodash",
    "org/forgerock/commons/ui/common/util/Constants",
    "org/forgerock/commons/ui/common/main/AbstractDelegate"
], function($, _, constants, AbstractDelegate) {

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

    obj.getRepoUpdates = function (archive) {
        return obj.serviceCall({
            url: "/update?_action=listRepoUpdates&archive=" + archive,
            type: "POST"
        });
    };

    obj.getUpdateFile = function(archive, path) {
        return obj.serviceCall({
            url: "/update/archives/" + archive + "/" + path + "?_fields=/contents",
            type: "GET"
        });
    };

    obj.markComplete = function (updateId) {
        return obj.serviceCall({
            url: "/update?_action=markComplete&updateId=" + updateId,
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

    obj.restartIDM = function () {
        return obj.serviceCall({
            url: "/update?_action=restart",
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

    obj.getUpdateLogs = function (options) {
        var queryString = this.parseQueryOptions(this.getUpdateLogFields(), options, true);
        return obj.serviceCall({
            url: "/update/log/?_queryFilter=true" + queryString,
            type: "GET"
        });
    };

    obj.getUpdateLogFields = function() {
        return [
            'archive',
            'completedTasks',
            'endDate',
            'files',
            'nodeId',
            'startDate',
            'status',
            'statusMessage',
            'totalTasks',
            'userName'
        ];
    };

    obj.parseQueryOptions = function(fields, options) {
        if (options) {
            if (options.fields) {
                return '&_fields=' + options.fields.join(',');
            } else if (options.excludeFields) {
                return '&_fields=' + fields.filter(function(field) {
                    return options.excludeFields.indexOf(field) === -1;
                }).join(',');
            }
        }
        return '';
    };

    return obj;
});

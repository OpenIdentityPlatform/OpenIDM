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

/*global define*/

define("org/forgerock/openidm/ui/admin/settings/audit/AuditAdminAbstractView", [
    "jquery",
    "underscore",
    "org/forgerock/openidm/ui/admin/util/AdminAbstractView",
    "org/forgerock/openidm/ui/common/delegates/ConfigDelegate",
    "org/forgerock/commons/ui/common/main/EventManager",
    "org/forgerock/commons/ui/common/util/Constants"


], function($, _, AdminAbstractView,
            ConfigDelegate,
            EventManager,
            Constants) {

    var auditDataChanges = {},
        auditData = {},

        AuditAdminAbstractView = AdminAbstractView.extend({
            retrieveAuditData: function (callback) {
                ConfigDelegate.readEntity("audit").then(_.bind(function (data) {
                    auditDataChanges = _.clone(data, true);
                    auditData = _.clone(data, true);
                    if (callback) {
                        callback();
                    }
                }, this));
            },

            getAuditData: function () {
                return _.clone(auditDataChanges, true);
            },

            getTopics: function() {
                return _.union(_.keys(_.clone(auditDataChanges.eventTopics, true)), ["authentication", "access", "activity", "recon", "sync", "config"]);
            },

            setProperties: function(properties, object) {
                _.each(properties, function(prop) {
                    if (_.isEmpty(object[prop]) &&
                        !_.isNumber(object[prop]) &&
                        !_.isBoolean(object[prop])) {
                        delete auditDataChanges[prop];
                    } else {
                        auditDataChanges[prop] = object[prop];
                    }
                }, this);
            },

            setFilterPolicies: function(policies) {
                auditDataChanges.auditServiceConfig.filterPolicies = policies;
            },

            setUseForQueries: function(event) {
                auditDataChanges.auditServiceConfig.handlerForQueries = event;
            },

            saveAudit: function(callback) {
                ConfigDelegate.updateEntity("audit", auditDataChanges).then(_.bind(function() {
                    EventManager.sendEvent(Constants.EVENT_DISPLAY_MESSAGE_REQUEST, "auditSaveSuccess");
                    auditData = _.clone(auditDataChanges, true);

                    if (callback) {
                        callback();
                    }
                }, this));
            }
        });

    return AuditAdminAbstractView;
});

/**
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2015 ForgeRock AS. All rights reserved.
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

            setEvents: function (data, msgContainer) {
                auditDataChanges.customEventTypes = {};
                auditDataChanges.extendedEventTypes = {};

                _.each(data, function (event, key) {
                    if (_.contains(["activity", "authentication", "access"], key)) {
                        if (!_.has(auditDataChanges, "extendedEventTypes")) {
                            auditDataChanges.extendedEventTypes = {};
                        }
                        auditDataChanges.extendedEventTypes[key] = event;
                    } else {
                        if (!_.has(auditDataChanges, "customEventTypes")) {
                            auditDataChanges.customEventTypes = {};
                        }
                        auditDataChanges.customEventTypes[key] = event;
                    }
                });

                if (_.keys(auditDataChanges.customEventTypes).length === 0) {
                    delete auditDataChanges.customEventTypes;
                }

                if (_.keys(auditDataChanges.extendedEventTypes).length === 0) {
                    delete auditDataChanges.extendedEventTypes;
                }

                if (this.compareObjects("customEventTypes", auditDataChanges, auditData) || this.compareObjects("extendedEventTypes", auditDataChanges, auditData)) {
                    msgContainer.show();
                } else {
                    msgContainer.hide();
                }
            },

            setEventHandlers: function (data, msgContainer) {
                auditDataChanges.auditServiceConfig = data.auditServiceConfig;
                auditDataChanges.eventHandlers = data.eventHandlers;

                if (_.keys(auditDataChanges.auditServiceConfig).length === 0) {
                    delete auditDataChanges.auditServiceConfig;
                }

                if (auditDataChanges.eventHandlers.length === 0) {
                    delete auditDataChanges.eventHandlers;
                }

                if (!this.compareObjects("eventHandlers", auditDataChanges, auditData) || !this.compareObjects("auditServiceConfig", auditDataChanges, auditData)) {
                    msgContainer.show();
                } else {
                    msgContainer.hide();
                }
            },

            setExceptionFormatter: function (data, msgContainer) {
                if (_.isNull(data)) {
                    if (_.has(auditDataChanges, "exceptionFormatter")) {
                        delete auditDataChanges.exceptionFormatter;
                    }
                } else {
                    auditDataChanges.exceptionFormatter = data;
                }

                if (this.compareObjects("exceptionFormatter", auditDataChanges, auditData)) {
                    msgContainer.hide();
                } else {
                    msgContainer.show();
                }

            },

            undo: function(prop) {
                if (_.has(auditData,prop)) {
                    auditDataChanges[prop] = _.clone(auditData[prop], true);
                }
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

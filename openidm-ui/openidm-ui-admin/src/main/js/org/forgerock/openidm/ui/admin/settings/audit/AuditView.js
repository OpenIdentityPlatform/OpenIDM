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

define("org/forgerock/openidm/ui/admin/settings/audit/AuditView", [
    "jquery",
    "underscore",
    "org/forgerock/openidm/ui/admin/settings/audit/AuditAdminAbstractView",
    "org/forgerock/openidm/ui/admin/settings/audit/AuditEventHandlersView",
    "org/forgerock/openidm/ui/admin/settings/audit/AuditEventsView",
    "org/forgerock/openidm/ui/admin/settings/audit/ExceptionFormatterView",
    "org/forgerock/commons/ui/common/util/Constants"

], function($, _, AuditAdminAbstractView,
            AuditEventHandlersView,
            AuditEventsView,
            ExceptionFormatterView,
            constants) {

    var AuditView = AuditAdminAbstractView.extend({
        template: "templates/admin/settings/audit/AuditTemplate.html",
        element: "#auditContainer",
        noBaseTemplate: true,
        events: {
            "click #submitAudit": "save",
            "click .undo-events": "undoEvents",
            "click .undo-exceptions": "undoExceptionFormatter",
            "click .undo-event-handlers": "undoEventHandlers"
        },

        render: function (args, callback) {
            this.data.docHelpUrl = constants.DOC_URL;

            this.retrieveAuditData(_.bind(function(){
                this.parentRender(this.renderViews);
            }, this));
        },

        renderViews: function() {
            AuditEventHandlersView.render();
            AuditEventsView.render();
            ExceptionFormatterView.render();
        },

        undoExceptionFormatter: function(e) {
            e.preventDefault();
            this.undo("exceptionFormatter");
            this.renderViews();
        },

        undoEvents: function(e) {
            e.preventDefault();
            this.undo("customEventTypes");
            this.undo("extendedEventTypes");
            this.renderViews();
        },

        undoEventHandlers: function(e) {
            e.preventDefault();
            this.undo("eventHandlers");
            this.undo("auditServiceConfig");
            this.renderViews();
        },

        save: function(e) {
            e.preventDefault();
            this.saveAudit(this.renderViews);
        }
    });

    return new AuditView();
});

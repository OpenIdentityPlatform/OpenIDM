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

define("org/forgerock/openidm/ui/admin/settings/audit/AuditView", [
    "jquery",
    "underscore",
    "org/forgerock/openidm/ui/admin/settings/audit/AuditAdminAbstractView",
    "org/forgerock/openidm/ui/admin/settings/audit/AuditEventHandlersView",
    "org/forgerock/openidm/ui/admin/settings/audit/AuditTopicsView",
    "org/forgerock/openidm/ui/admin/settings/audit/AuditFilterPoliciesView",
    "org/forgerock/openidm/ui/admin/settings/audit/ExceptionFormatterView",
    "org/forgerock/commons/ui/common/util/Constants"

], function ($, _, AuditAdminAbstractView,
             AuditEventHandlersView,
             AuditTopicsView,
             AuditFilterPoliciesView,
             ExceptionFormatterView,
             constants) {

    var AuditView = AuditAdminAbstractView.extend({
        template: "templates/admin/settings/audit/AuditTemplate.html",
        element: "#auditContainer",
        noBaseTemplate: true,
        events: {
            "click #submitAudit": "save"
        },

        render: function (args, callback) {
            this.data.docHelpUrl = constants.DOC_URL;

            this.retrieveAuditData(_.bind(function () {
                this.parentRender(function () {
                    AuditEventHandlersView.render();
                    AuditTopicsView.render();
                    AuditFilterPoliciesView.render();
                    ExceptionFormatterView.render();
                });
            }, this));
        },

        save: function (e) {
            e.preventDefault();
            this.saveAudit();
            AuditEventHandlersView.render({"saved": true});
            AuditTopicsView.render({"saved": true});
            AuditFilterPoliciesView.render({"saved": true});
            ExceptionFormatterView.render({ "saved": true });
        }
    });

    return new AuditView();
});

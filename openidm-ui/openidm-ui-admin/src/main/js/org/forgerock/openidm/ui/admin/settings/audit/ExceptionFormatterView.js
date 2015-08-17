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

define("org/forgerock/openidm/ui/admin/settings/audit/ExceptionFormatterView", [
    "jquery",
    "underscore",
    "org/forgerock/openidm/ui/admin/settings/audit/AuditAdminAbstractView",
    "org/forgerock/commons/ui/common/main/EventManager",
    "org/forgerock/commons/ui/common/util/Constants",
    "org/forgerock/openidm/ui/admin/util/InlineScriptEditor"

], function($, _, AuditAdminAbstractView,
            eventManager,
            constants,
            InlineScriptEditor) {

    var ExceptionFormatterView = AuditAdminAbstractView.extend({
        template: "templates/admin/settings/audit/ExceptionFormatterTemplate.html",
        element: "#ExceptionFormatterView",
        noBaseTemplate: true,
        events: {},
        model: {
            auditData: {},
            exceptionFormatterData: {}
        },
        data: {},

        render: function (args, callback) {
            this.parentRender(_.bind(function() {
                this.model.auditData = this.getAuditData();
                if (_.has(this.model.auditData, "exceptionFormatter")) {
                    this.model.exceptionFormatterData = this.model.auditData.exceptionFormatter;
                }

                this.model.exceptionFormatterScript = InlineScriptEditor.generateScriptEditor({
                    "element": this.$el.find("#exceptionFormatterScript"),
                    "eventName": "exceptionFormatterScript",
                    "disableValidation": true,
                    "onChange": _.bind(function(e) {
                        this.setExceptionFormatter(this.model.exceptionFormatterScript.generateScript(), this.$el.find(".alert"));
                    }, this),
                    "scriptData": this.model.exceptionFormatterData
                });
            }, this));
        }
    });

    return new ExceptionFormatterView();
});

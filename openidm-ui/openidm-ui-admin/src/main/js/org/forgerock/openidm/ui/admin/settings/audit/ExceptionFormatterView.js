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

define("org/forgerock/openidm/ui/admin/settings/audit/ExceptionFormatterView", [
    "jquery",
    "underscore",
    "org/forgerock/openidm/ui/admin/settings/audit/AuditAdminAbstractView",
    "org/forgerock/commons/ui/common/main/EventManager",
    "org/forgerock/commons/ui/common/util/Constants",
    "org/forgerock/openidm/ui/admin/util/InlineScriptEditor",
    "org/forgerock/commons/ui/common/components/ChangesPending"

], function($, _, AuditAdminAbstractView,
            eventManager,
            constants,
            InlineScriptEditor,
            ChangesPending) {

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
            this.parentRender(_.bind(function () {
                if (args && args.undo) {
                    this.model.auditData = args.auditData;
                } else {
                    this.model.auditData = this.getAuditData();
                }

                if (!_.has(this.model, "changesModule")) {
                    this.model.changesModule = ChangesPending.watchChanges({
                        element: this.$el.find(".exception-formatter-alert"),
                        undo: true,
                        watchedObj: _.clone(this.model.auditData, true),
                        watchedProperties: ["exceptionFormatter"],
                        undoCallback: _.bind(function (original) {
                            _.each(this.model.changesModule.data.watchedProperties, function (prop) {
                                if (_.has(original, prop)) {
                                    this.model.auditData[prop] = original[prop];
                                } else if (_.has(this.model.auditData, prop)) {
                                    delete this.model.auditData[prop];
                                }
                            }, this);

                            this.setProperties(["exceptionFormatter"], this.model.auditData);

                            this.render({
                                "undo": true,
                                "auditData": this.model.auditData
                            });
                        }, this)
                    });
                } else {
                    this.model.changesModule.reRender(this.$el.find(".exception-formatter-alert"));
                    if (args && args.saved) {
                        this.model.changesModule.saveChanges();
                    }
                }

                if (_.has(this.model.auditData, "exceptionFormatter")) {
                    this.model.exceptionFormatterData = this.model.auditData.exceptionFormatter;
                } else {
                    this.model.exceptionFormatterData = {};
                }

                this.model.exceptionFormatterScript = InlineScriptEditor.generateScriptEditor({
                    "element": this.$el.find("#exceptionFormatterScript"),
                    "eventName": "exceptionFormatterScript",
                    "disableValidation": true,
                    "onBlurPassedVariable": _.bind(this.checkChanges, this),
                    "onDeletePassedVariable": _.bind(this.checkChanges, this),
                    "onAddPassedVariable": _.bind(this.checkChanges, this),
                    "onChange": _.bind(this.checkChanges, this),
                    "scriptData": this.model.exceptionFormatterData,
                    "autoFocus": false
                });
            }, this));
        },

        checkChanges: function () {
            this.model.auditData.exceptionFormatter = this.model.exceptionFormatterScript.generateScript();
            this.setProperties(["exceptionFormatter"], this.model.auditData);
            this.model.changesModule.makeChanges(_.clone(this.model.auditData, true));
        }

    });

    return new ExceptionFormatterView();
});
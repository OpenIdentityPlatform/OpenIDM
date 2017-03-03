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

define([
    "jquery",
    "lodash",
    "bootstrap",
    "handlebars",
    "form2js",
    "org/forgerock/openidm/ui/admin/selfservice/AbstractSelfServiceView",
    "org/forgerock/commons/ui/common/util/UIUtils",
    "org/forgerock/openidm/ui/admin/util/AdminUtils",
    "bootstrap-dialog",
    "libs/codemirror/lib/codemirror",
    "libs/codemirror/mode/xml/xml",
    "libs/codemirror/addon/display/placeholder"
], function($, _,
            boostrap,
            handlebars,
            form2js,
            AbstractSelfServiceView,
            UiUtils,
            AdminUtils,
            BootstrapDialog,
            codeMirror) {

    var GenericSelfServiceStageView = AbstractSelfServiceView.extend({
        noBaseTemplate: true,
        element: "#SelfServiceStageDialog",
        partials: [
            "partials/selfservice/_translationMap.html",
            "partials/selfservice/_translationItem.html",
            "partials/form/_basicInput.html",
            "partials/form/_basicSelectize.html",
            "partials/form/_tagSelectize.html"
        ],
        model: {
            codeMirrorConfig: {
                lineNumbers: true,
                autofocus: false,
                viewportMargin: Infinity,
                theme: "forgerock",
                mode: "xml",
                htmlMode: true,
                lineWrapping: true
            }
        },

        render: function(args, dialogRef) {
            var self = this;
            this.data = _.clone(args.data, true);
            this.args = _.clone(args, true);

            this.template = "partials/selfservice/_" + args.type + ".html";

            this.parentRender(() => {

                _.each(dialogRef.$modalBody.find(".email-message-code-mirror-disabled"), (instance) => {
                    codeMirror.fromTextArea(instance, _.extend({readOnly: true, cursorBlinkRate: -1}, this.model.codeMirrorConfig));
                });

                if (dialogRef.$modalBody.find(".email-message-code-mirror")[0]) {
                    this.cmBox = codeMirror.fromTextArea(dialogRef.$modalBody.find(".email-message-code-mirror")[0], this.model.codeMirrorConfig);
                    this.cmBox.on("change", () => {
                        this.checkAddTranslation();
                    });
                }

                //Setup for both selectizes for the identity provider and email field
                this.model.identityServiceSelect = dialogRef.$modalBody.find("#select-identityServiceUrl").selectize({
                    "create": true,
                    "persist": false,
                    "allowEmptyOption": true,
                    onChange: function(value) {
                        self.model.identityEmailFieldSelect[0].selectize.clearOptions();
                        self.model.identityEmailFieldSelect[0].selectize.load((callback) => {
                            AdminUtils.findPropertiesList(value.split("/")).then(_.bind(function(properties) {
                                var keyList = _.chain(properties).keys().sortBy().value(),
                                    propertiesList = [];

                                _.each(keyList, (key) => {
                                    propertiesList.push({
                                        text: key,
                                        value: key
                                    });
                                });

                                callback(propertiesList);

                                self.model.identityEmailFieldSelect[0].selectize.setValue(propertiesList[0].value);
                            }, this));
                        });
                    }
                });

                this.model.identityEmailFieldSelect = dialogRef.$modalBody.find("#select-identityEmailField").selectize({
                    "create": true,
                    "persist": false,
                    "allowEmptyOption": true
                });

                dialogRef.$modalBody.on("submit", "form", function (e) {
                    e.preventDefault();
                    return false;
                });
                dialogRef.$modalBody.on("click", ".translationMapGroup button.add",
                    {currentStageConfig: this.args.data},
                    _.bind(this.addTranslation, this));

                dialogRef.$modalBody.on("click", ".translationMapGroup button.delete",
                    {currentStageConfig: this.args.data},
                    _.bind(this.deleteTranslation, this));

                dialogRef.$modalBody.on("keyup", ".translationMapGroup .newTranslationLocale, .translationMapGroup .newTranslationText",
                    {currentStageConfig: this.args.data},
                    _.bind(this.checkAddTranslation, this));
            }, this);
        },

        getData: function() {
            var formData = form2js("configDialogForm", ".", true);

            if(this.args.type === "idmUserDetails") {
                _.filter(this.args.stageConfigs, {"name" : "idmUserDetails"})[0].identityEmailField = formData.identityEmailField;
                _.filter(this.args.stageConfigs, {"name" : "idmUserDetails"})[0].successUrl = formData.successUrl;
                _.filter(this.args.stageConfigs, {"name" : "selfRegistration"})[0].identityServiceUrl = formData.identityServiceUrl;
            } else {
                _.extend(this.args.data, formData);
            }

            if(formData.snapshotToken) {
                this.args.snapshotToken = formData.snapshotToken;
            }

            return this.args;
        }
    });

    return new GenericSelfServiceStageView();
});

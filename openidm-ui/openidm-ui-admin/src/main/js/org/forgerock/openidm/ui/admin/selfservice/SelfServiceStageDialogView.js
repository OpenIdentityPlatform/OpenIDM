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
    "org/forgerock/openidm/ui/admin/util/AdminAbstractView",
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
            AdminAbstractView,
            UiUtils,
            AdminUtils,
            BootstrapDialog,
            codeMirror) {

    var SelfServiceStageDialogView = AdminAbstractView.extend({
        element: "#dialogs",
        noBaseTemplate: true,
        events: {},
        partials: [
            "partials/selfservice/_emailValidation.html",
            "partials/selfservice/_kbaStage.html",
            "partials/selfservice/_userDetails.html",
            "partials/selfservice/_captcha.html",
            "partials/selfservice/_translationMap.html",
            "partials/selfservice/_translationItem.html",
            "partials/selfservice/_termsAndConditions.html",
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

        render: function(args, callback) {
            var self = this;

            this.parentRender(() => {
                this.dialog = BootstrapDialog.show({
                    title: $.t("templates.selfservice.user." + args.type + "Title"),
                    type: BootstrapDialog.TYPE_DEFAULT,
                    size: BootstrapDialog.SIZE_WIDE,
                    message: $(handlebars.compile("{{> selfservice/_" + args.type + "}}")(args.data)),
                    onshown: _.bind(function (dialogRef) {
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
                        self.model.identityServiceSelect = dialogRef.$modalBody.find("#select-identityServiceUrl").selectize({
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

                        self.model.identityEmailFieldSelect = dialogRef.$modalBody.find("#select-identityEmailField").selectize({
                            "create": true,
                            "persist": false,
                            "allowEmptyOption": true
                        });

                        dialogRef.$modalBody.on("submit", "form", function (e) {
                            e.preventDefault();
                            return false;
                        });
                        dialogRef.$modalBody.on("click", ".translationMapGroup button.add",
                            {currentStageConfig: args.data},
                            _.bind(this.addTranslation, this));

                        dialogRef.$modalBody.on("click", ".translationMapGroup button.delete",
                            {currentStageConfig: args.data},
                            _.bind(this.deleteTranslation, this));


                        dialogRef.$modalBody.on("keyup", ".translationMapGroup .newTranslationLocale, .translationMapGroup .newTranslationText",
                            {currentStageConfig: args.data},
                            _.bind(this.checkAddTranslation, this));
                    }, this),
                    buttons: [
                        {
                            label: $.t("common.form.close"),
                            action: function (dialogRef) {
                                dialogRef.close();
                            }
                        },
                        {
                            label: $.t("common.form.save"),
                            cssClass: "btn-primary",
                            id: "saveUserConfig",
                            action: function (dialogRef) {
                                var formData = form2js("configDialogForm", ".", true),
                                    tempData;

                                if(args.type === "userDetails") {
                                    tempData = _.filter(args.stageConfigs, {"name" : "userDetails"})[0];
                                    tempData.identityEmailField = formData.identityEmailField;

                                    tempData = _.filter(args.stageConfigs, {"name" : "selfRegistration"})[0];
                                    tempData.identityServiceUrl = formData.identityServiceUrl;
                                } else {
                                    _.extend(args.data, formData);
                                }

                                args.saveCallback();

                                dialogRef.close();
                            }
                        }
                    ]
                });
            });
        },

        checkAddTranslation: function(e) {
            var container,
                locale,
                translation,
                btn,
                usesCodeMirror = false;

            if (_.has(e, "currentTarget")) {
                container = $(e.currentTarget).closest(".translationMapGroup");
                if (container.attr("data-uses-codemirror")) {
                    usesCodeMirror = true;
                }
                // This function was triggered from the codeM "/
                // irror onchange
            } else {
                container = $(this.cmBox.getTextArea()).closest(".translationMapGroup");
                usesCodeMirror = true;
            }

            btn = container.find(".add");

            if (usesCodeMirror) {
                translation = this.cmBox.getValue();
            } else {
                translation = container.find(".newTranslationText").val();
            }

            locale = container.find(".newTranslationLocale").val();

            if (translation.length > 0 && locale.length > 0) {
                btn.prop( "disabled", false);
            } else {
                btn.prop( "disabled", true );
            }
        },

        addTranslation: function (e) {
            e.preventDefault();

            var translationMapGroup = $(e.target).closest(".translationMapGroup"),
                useCodeMirror = translationMapGroup.attr("data-uses-codemirror") || false,
                addBtn = translationMapGroup.find(".add"),
                currentStageConfig = e.data.currentStageConfig,
                field = translationMapGroup.attr("field"),
                locale = translationMapGroup.find(".newTranslationLocale"),
                text = "";

            if (useCodeMirror) {
                text = this.cmBox.getValue();
            } else {
                text = translationMapGroup.find(".newTranslationText").val();
            }

            if (!_.has(currentStageConfig[field][locale.val()])) {
                currentStageConfig[field][locale.val()] = text;
                translationMapGroup
                    .find("ul")
                    .append(
                    handlebars.compile("{{> selfservice/_translationItem useCodeMirror="+ useCodeMirror +"}}")({
                        locale: locale.val(),
                        text: text
                    })
                );

                if (useCodeMirror) {
                    codeMirror.fromTextArea(
                        translationMapGroup.find(".email-message-code-mirror-disabled:last")[0],
                        _.extend({readOnly: true, cursorBlinkRate: -1}, this.model.codeMirrorConfig)
                    );

                    this.cmBox.setValue("");

                } else {
                    translationMapGroup.find(".newTranslationText").val("");
                }
                locale.val("").focus();
                addBtn.attr("disabled", true);
            }
        },

        deleteTranslation: function (e) {
            e.preventDefault();

            var translationMapGroup = $(e.target).closest(".translationMapGroup"),
                currentStageConfig = e.data.currentStageConfig,
                field = translationMapGroup.attr("field"),
                localeField = translationMapGroup.find(".newTranslationLocale"),
                textField = translationMapGroup.find(".newTranslationText"),
                localeValue = $(e.target).closest("li").attr("locale"),
                textValue = $(e.target).closest("li").find(".localizedText").text();

            delete currentStageConfig[field][localeValue];
            translationMapGroup.find("li[locale='"+localeValue+"']").remove();

            localeField.val(localeValue);
            textField.val(textValue).focus();
        }
    });

    return new SelfServiceStageDialogView();
});

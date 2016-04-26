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
 * Copyright 2014-2016 ForgeRock AS.
 */

define([
    "jquery",
    "underscore",
    "org/forgerock/commons/ui/common/main/AbstractView",
    "org/forgerock/commons/ui/common/main/ValidatorsManager",
    "org/forgerock/commons/ui/common/main/Configuration",
    "org/forgerock/commons/ui/common/main/EventManager",
    "org/forgerock/commons/ui/common/util/Constants",
    "org/forgerock/commons/ui/common/main/Router",
    "org/forgerock/commons/ui/common/util/ModuleLoader",
    "bootstrap-dialog",
    "org/forgerock/commons/ui/common/util/UIUtils"
], function($, _, AbstractView, validatorsManager, conf, eventManager, constants, router, ModuleLoader, BootstrapDialog, uiUtils) {
    var MandatoryPasswordChangeDialog = AbstractView.extend({
        contentTemplate: "templates/admin/MandatoryPasswordChangeDialogTemplate.html",
        events: {
            "onValidate": "onValidate",
            "customValidate": "customValidate"
        },
        model: {

        },

        render: function(args, callback) {
            ModuleLoader.load(router.configuration.routes.landingPage.view).then((landingView) => {
                var _this = this,
                    currentDialog = $('<div id="mandatoryPasswordChangeDialog"></div>');

                if (landingView.baseTemplate) {
                    this.baseTemplate = landingView.baseTemplate;
                }

                this.setElement(currentDialog);

                uiUtils.renderTemplate(
                    _this.contentTemplate,
                    _this.$el,
                    _.extend({}, conf.globalData, _this.data),
                    _.bind(function() {
                        this.model.dialog = BootstrapDialog.show({
                            title: $.t("templates.MandatoryChangePassword.title"),
                            type: BootstrapDialog.TYPE_DEFAULT,
                            message: currentDialog,
                            onshow: _.bind(function(dialogRef){
                                dialogRef.$modalFooter.find("#submitPasswordChange").prop('disabled', true);
                            }, this),
                            onshown : _.bind(function (dialogRef) {
                                validatorsManager.bindValidators(this.$el, conf.loggedUser.component + "/" + conf.loggedUser.id, _.bind(function () {
                                    this.$el.find("[name=password]").focus();

                                    if (callback) {
                                        callback();
                                    }
                                }, this));
                            }, _this),
                            onhide: _.bind(function(){
                                eventManager.sendEvent(constants.EVENT_DIALOG_CLOSE);
                            }, this),
                            buttons:
                                [{
                                    label: $.t("common.form.submit"),
                                    id: "submitPasswordChange",
                                    cssClass: "btn-primary",
                                    action: _.bind(function(dialogRef) {
                                        if (validatorsManager.formValidated(this.$el.find("#passwordChange"))) {
                                            conf.loggedUser.save({"password": this.$el.find("input[name=password]").val()}).then(function () {
                                                delete conf.passwords;
                                                dialogRef.close();
                                                eventManager.sendEvent(constants.EVENT_DISPLAY_MESSAGE_REQUEST, "securityDataChanged");
                                            });
                                        }
                                    }, this)
                                }]
                        });
                    }, _this),
                    "append");

            });

        },

        close: function() {
            this.model.dialog.close();
        },

        customValidate: function () {
            if(validatorsManager.formValidated(this.$el.find("#passwordChange")) || validatorsManager.formValidated(this.$el.find("#securityDataChange"))) {
                this.model.dialog.$modalFooter.find("#submitPasswordChange").prop('disabled', false);
            }
            else {
                this.model.dialog.$modalFooter.find("#submitPasswordChange").prop('disabled', true);
            }

        }
    });

    return new MandatoryPasswordChangeDialog();
});

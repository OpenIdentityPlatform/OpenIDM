/**
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2014 ForgeRock AS. All rights reserved.
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

/*global define */

define("org/forgerock/openidm/ui/common/MandatoryPasswordChangeDialog", [
    "jquery",
    "underscore",
    "org/forgerock/openidm/ui/common/delegates/InternalUserDelegate",
    "org/forgerock/commons/ui/common/main/AbstractView",
    "org/forgerock/commons/ui/common/main/ValidatorsManager",
    "org/forgerock/commons/ui/common/main/Configuration",
    "org/forgerock/commons/ui/common/main/EventManager",
    "org/forgerock/commons/ui/common/util/Constants",
    "org/forgerock/commons/ui/common/main/Router",
    "org/forgerock/commons/ui/common/util/ModuleLoader",
    "AuthnDelegate",
    "bootstrap-dialog",
    "org/forgerock/commons/ui/common/util/UIUtils"
], function($, _, InternalUserDelegate, AbstractView, validatorsManager, conf, eventManager, constants, router, ModuleLoader, authnDelegate, BootstrapDialog, uiUtils) {
    var MandatoryPasswordChangeDialog = AbstractView.extend({
        contentTemplate: "templates/admin/MandatoryPasswordChangeDialogTemplate.html",
        delegate: InternalUserDelegate,
        events: {
            "onValidate": "onValidate",
            "customValidate": "customValidate"
        },
        model: {

        },

        render: function(args, callback) {
            ModuleLoader.load(router.configuration.routes.landingPage.view).then(_.bind(function (landingView) {
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
                            onshown : _.bind(function (dialogRef) {
                                validatorsManager.bindValidators(this.$el, this.delegate.baseEntity + "/" + conf.loggedUser._id, _.bind(function () {
                                    this.$el.find("[name=password]").focus();

                                    this.model.dialog.$modalFooter.find("#submitPasswordChange").prop('disabled', true);

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
                                        event.preventDefault();

                                        var patchDefinitionObject = [], element;
                                        if(validatorsManager.formValidated(this.$el.find("#passwordChange"))) {
                                            patchDefinitionObject.push({operation: "replace", field: "/password", value: this.$el.find("input[name=password]").val()});
                                        }

                                        this.delegate.patchSelectedUserAttributes(conf.loggedUser._id, conf.loggedUser._rev, patchDefinitionObject, _.bind(function(r) {
                                            eventManager.sendEvent(constants.EVENT_DISPLAY_MESSAGE_REQUEST, "securityDataChanged");
                                            delete conf.passwords;

                                            dialogRef.close();

                                            return authnDelegate.getProfile()
                                                .then(function(user) {
                                                    conf.loggedUser = user;
                                                    return user;
                                                });

                                        }, this));

                                    }, this)
                                }]
                        });
                    }, _this),
                    "append");

            }, this));

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

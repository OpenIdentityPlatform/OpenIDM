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

/*global define, $, _, Handlebars, form2js */

define("org/forgerock/openidm/ui/admin/connector/ldap/LDAPTypeView", [
    "org/forgerock/openidm/ui/admin/connector/ConnectorTypeAbstractView",
    "org/forgerock/commons/ui/common/main/ValidatorsManager",
    "org/forgerock/openidm/ui/admin/connector/ldap/LDAPFilterDialog",
    "org/forgerock/openidm/ui/admin/delegates/ConnectorDelegate",
    "org/forgerock/commons/ui/common/util/UIUtils",
    "org/forgerock/openidm/ui/admin/delegates/SecurityDelegate",
    "bootstrap-dialog"
], function(ConnectorTypeAbstractView, validatorsManager, ldapFilterDialog, ConnectorDelegate, uiUtils, securityDelegate, BootstrapDialog) {

    var LDAPTypeView = ConnectorTypeAbstractView.extend({
        events: {
            "click .add-btn": "addField",
            "click .remove-btn": "removeField",
            "focus .filter": "showFilterDialog",
            "click #ssl": "toggleSSLPort",
            "click #syncBaseContext" : "toggleSyncBaseContext",
            "click #toggleCert": "toggleCert",
            "change #ldapTemplateType" : "changeLdapType"
        },
        data : {
            ldapSelector : [
                {
                    "displayName" : "Generic LDAP Configuration",
                    "fileName" : "baseConfig"
                },
                {
                    "displayName" : "AD LDAP Configuration",
                    "fileName" : "provisioner.openicf-adldap"
                },
                {
                    "displayName" : "ADLDS LDAP Configuration",
                    "fileName" : "provisioner.openicf-adldsldap"
                },
                {
                    "displayName" : "DJ LDAP Configuration",
                    "fileName" : "provisioner.openicf-ldap"
                },
                {
                    "displayName" : "IBM LDAP Configuration",
                    "fileName" : "provisioner.openicf-racfldap"
                }
            ]
        },
        model : {

        },
        render: function(args, callback) {
            var base = "templates/admin/connector/";

            this.data.publicKey = "";

            $("#connectorDetails").hide();

            this.data.connectorDefaults = args.connectorDefaults;

            if(!this.model.defaultLdap) {
                this.model.defaultLdap = _.clone(this.data.connectorDefaults);
            }

            this.data.editState = args.editState;

            this.model.systemType = args.systemType;
            this.model.connectorType = args.connectorType;

            this.template = base + args.connectorType +".html";

            if(!this.data.editState) {
                if(args.ldapType) {
                    this.model.ldapType = args.ldapType;
                    this.$el.find("#ldapTemplateType").val(args.ldapType);
                } else {
                    this.model.ldapType = "baseConfig";
                }
            }

            if(!_.isUndefined(this.data.connectorDefaults.configurationProperties.baseContexts) && !_.isUndefined(this.data.connectorDefaults.configurationProperties.baseContextsToSynchronize)) {
                this.data.baseContextsSameResults = this.compareBaseContext(this.data.connectorDefaults.configurationProperties.baseContexts, this.data.connectorDefaults.configurationProperties.baseContextsToSynchronize);
            } else {
                this.data.baseContextsSameResults = true;
            }

            if(this.data.editState) {
                securityDelegate.getPublicKeyCert("truststore", "openidm_" +args.connectorDefaults.name).then(_.bind(function(cert){
                    this.data.publicKey = cert;
                    this.ldapParentRender(args, _.bind(function(){
                        this.fieldButtonCheck();
                        callback();
                    }, this));
                }, this));
            } else {
                this.ldapParentRender(args, _.bind(function(){
                    this.fieldButtonCheck();
                    if(callback) {
                        callback();
                    }
                }, this));
            }
        },
        compareBaseContext : function(base, sync) {
            var sameResults = true,
                compare;

            if(base.length === sync.length) {
                compare = _.difference(base, sync);

                if(compare.length !== 0) {
                    sameResults = false;
                }
            } else {
                sameResults = false;
            }

            return sameResults;
        },
        ldapParentRender : function(args, callback) {
            this.parentRender(_.bind(function() {
                if(!this.data.editState) {
                    this.$el.find("#ldapTemplateType").val(this.model.ldapType);
                }

                if(args.animate) {
                    $("#connectorDetails").slideDown("slow", function() {});
                } else {
                    $("#connectorDetails").show();
                }

                if(this.data.connectorDefaults.configurationProperties.ssl){
                    this.$el.find("#toggleCert").show();
                }

                validatorsManager.bindValidators(this.$el, "config/provisioner.openicf/ldap", _.bind(function () {
                    if(this.$el.find("#syncBaseContext")) {
                        this.$el.find("#baseContextsToSynchronizeHolder input").attr("data-validator", "");
                        this.$el.find("#baseContextsToSynchronizeHolder input").attr("data-validation-status", "");
                        this.$el.find("#baseContextsToSynchronizeHolder input").unbind("blur");
                    }

                    validatorsManager.validateAllFields(this.$el);
                }, this));

                if(callback){
                    callback();
                }
            }, this));
        },

        changeLdapType: function(event) {
            var value = $(event.target).val();

            uiUtils.jqConfirm($.t("templates.connector.ldapConnector.ldapTypeChange"), _.bind(function(){
                    if(value === "baseConfig") {
                        this.render({
                            "animate": false,
                            "connectorDefaults": this.model.defaultLdap,
                            "editState" : this.data.editState,
                            "systemType" : this.model.systemType,
                            "connectorType" : this.model.connectorType,
                            "ldapType" : value
                        });
                    } else {
                        ConnectorDelegate.connectorDefault(value, "ldap").then(_.bind(function (result) {
                                this.render({
                                    "animate": false,
                                    "connectorDefaults": result,
                                    "editState" : this.data.editState,
                                    "systemType" : this.model.systemType,
                                    "connectorType" : this.model.connectorType,
                                    "ldapType" : value
                                });
                            }, this)
                        );
                    }
                }, this),

                _.bind(function() {
                    this.$el.find("#ldapTemplateType").val(this.model.ldapType);
                }, this), "330px");
        },

        showFilterDialog: function (event) {
            event.preventDefault();

            var filterProp = $(event.target).attr("id"),
                updatePromise = $.Deferred();

            ldapFilterDialog.render({
                filterString : this.data.connectorDefaults.configurationProperties[filterProp],
                type: $.t("templates.connector.ldapConnector." + filterProp),
                promise: updatePromise
            });

            updatePromise.then(_.bind(function (filterString) {
                this.data.connectorDefaults.configurationProperties[filterProp] = filterString;
                this.data.connectorDefaults.configurationProperties[filterProp.replace('Search', 'Synchronization')] = filterString;
                $("#" + filterProp).val(filterString);
            }, this));
        },
        toggleSyncBaseContext : function(event) {
            if (!$(event.target).is(":checked")) {
                this.$el.find("#baseContextsToSynchronizeHolder").show();
                this.$el.find("#baseContextsToSynchronizeHolder input").attr("data-validator", "required");
                validatorsManager.bindValidators(this.$el.find('#baseContextsToSynchronizeHolder'));
            } else {
                this.$el.find("#baseContextsToSynchronizeHolder").hide();
                this.$el.find("#baseContextsToSynchronizeHolder input").attr("data-validator", "");
                this.$el.find("#baseContextsToSynchronizeHolder input").attr("data-validation-status", "");
                this.$el.find("#baseContextsToSynchronizeHolder input").unbind("blur");
            }

            validatorsManager.validateAllFields(this.$el);
        },
        toggleSSLPort: function (event) {
            if ($(event.target).is(":checked")) {
                this.$el.find("#port").val("636").trigger("change");
                this.$el.find("#toggleCert").show();
            } else {
                this.$el.find("#port").val("389").trigger("change");
                this.$el.find("#toggleCert").hide();
            }

            validatorsManager.validateAllFields($(event.target).parents(".group-field-block"));
        },

        toggleCert: function (event) {
            var _this = this;

            if(event){
                event.preventDefault();
            }

            //change dialog
            BootstrapDialog.show({
                title: "SSL Certificate",
                type: BootstrapDialog.TYPE_DEFAULT,
                message: this.$el.find("#certContainer").clone().attr("id","certificateContainerClone"),
                onshown : function (dialogRef) {
                    var saveBtn = dialogRef.$modalFooter.find("#sslSaveButton"),
                        textarea = dialogRef.$modalDialog.find("textarea"),
                        updateBtnStatus = function () {
                            if (textarea.attr("data-validation-status") === "ok") {
                                saveBtn.prop("disabled", false);
                                dialogRef.$modalDialog.find("textarea").toggleClass("field-error", false);

                            } else {
                                saveBtn.prop("disabled", true);
                                dialogRef.$modalDialog.find("textarea").toggleClass("field-error", true);
                            }
                        };

                    $("#certificateContainerClone").show();

                    validatorsManager.bindValidators(dialogRef.$modalDialog);
                    validatorsManager.validateAllFields(dialogRef.$modalDialog);

                    textarea.on("keyup change", updateBtnStatus);

                    updateBtnStatus();
                },
                buttons: [{
                    label: $.t('common.form.cancel'),
                    action: function(dialogRef){
                        dialogRef.close();
                    }
                },
                    {
                        label: $.t('common.form.save'),
                        id: "sslSaveButton",
                        cssClass: "btn-primary",
                        action: function(dialogRef) {
                            var saveBtn = dialogRef.$modalFooter.find("#sslSaveButton"),
                                certField;

                            if (!saveBtn.prop("disabled")) {
                                certField = _this.$el.find("#certContainer").find('[name=certificate]');

                                certField.text($('#certificateContainerClone').find('textarea').val());
                                certField.val(certField.text()); // seems to be necessary for IE

                                validatorsManager.validateAllFields(_this.$el);
                            }

                            dialogRef.close();
                        }
                    }
                ]
            });
        },

        connectorValidate: function(callback, details) {
            if(this.$el.find("#syncBaseContext").is(":checked")) {
                details.configurationProperties.baseContextsToSynchronize = details.configurationProperties.baseContexts;
            }

            if (this.$el.find("#certificate").val().length) {
                securityDelegate.uploadCert("truststore", "openidm_" +details.name, this.$el.find("#certificate").val()).then(function () {
                    if(callback) {
                        callback(details);
                    }
                });
            } else {
                securityDelegate.deleteCert("truststore", "openidm_" +details.name).always(function () {
                    if(callback) {
                        callback(details);
                    }
                });
            }
        },

        connectorSaved: function(details) {
            if(this.$el.find("#syncBaseContext").is(":checked")) {
                details.configurationProperties.baseContextsToSynchronize = details.configurationProperties.baseContexts;
            }

            return details;
        }
    });

    return new LDAPTypeView();
});
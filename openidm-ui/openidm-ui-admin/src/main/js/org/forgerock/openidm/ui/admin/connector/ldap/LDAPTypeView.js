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
    "org/forgerock/openidm/ui/admin/delegates/SecurityDelegate"
], function(ConnectorTypeAbstractView, validatorsManager, ldapFilterDialog, ConnectorDelegate, uiUtils, securityDelegate) {

    var LDAPTypeView = ConnectorTypeAbstractView.extend({
        events: {
            "click .add-btn": "addField",
            "click .remove-btn": "removeField",
            "focus .filter": "showFilterDialog",
            "click #ssl": "toggleSSLPort",
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

            if(this.data.editState) {
                securityDelegate.getPublicKeyCert("truststore", "openidm_" +args.connectorDefaults.name).then(_.bind(function(cert){
                    this.data.publicKey = cert;
                    this.ldapParentRender(args, callback);
                }, this));
            } else {
                this.ldapParentRender(args, callback);
            }

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

            this.$el.find("#certContainer").clone().attr("id","certificateContainerClone").dialog({
                appendTo: this.$el,
                title: "SSL Certificate",
                autoOpen: true,
                width: 640,
                modal: true,
                open: function (e, ui) {
                    var saveBtn = $(this).parent(".ui-dialog").find(".ui-dialog-buttonpane .ui-button:first"),
                        textarea = $("textarea", this),
                        updateBtnStatus = function () {
                            if (textarea.attr("data-validation-status") === "ok") {
                                saveBtn.prop("disabled", false);
                            } else {
                                saveBtn.prop("disabled", true);
                            }
                        };

                    validatorsManager.bindValidators(_this.$el.find("#certificateContainerClone"));
                    validatorsManager.validateAllFields(_this.$el);

                    textarea.on("keyup change", updateBtnStatus);

                    updateBtnStatus();
                },
                close: function(e, ui) {
                    _this.$el.find('#certificateContainerClone').dialog("destroy");
                },
                buttons: [
                    {
                        text : $.t("common.form.save"),
                        id: "sslSaveButton",
                        click: function(e) {
                            var saveBtn = $(this).parent(".ui-dialog").find(".ui-dialog-buttonpane .ui-button:first"),
                                certField;

                            if (!saveBtn.prop("disabled")) {
                                certField = _this.$el.find("#certContainer").find('[name=certificate]');

                                certField.text($('#certificateContainerClone').find('textarea').val());
                                certField.val(certField.text()); // seems to be necessary for IE

                                validatorsManager.validateAllFields(_this.$el);

                                $(this).dialog("close");
                            }
                        }
                    },
                    {
                        text: $.t("common.form.cancel"),
                        click: function() {
                            $(this).dialog("close");
                        }
                    }
                ]
            });
        },

        connectorSaved: function(callback, details) {
            if (this.$el.find("#certificate").val().length) {
                securityDelegate.uploadCert("truststore", "openidm_" +details.name, this.$el.find("#certificate").val()).then(function () {
                    if(callback) {
                        callback();
                    }
                });
            } else {
                securityDelegate.deleteCert("truststore", "openidm_" +details.name).always(function () {
                    if(callback) {
                        callback();
                    }
                });
            }
        }
    });

    return new LDAPTypeView();
});
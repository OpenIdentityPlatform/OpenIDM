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
    "org/forgerock/openidm/ui/admin/connector/ldap/LDAPFilterDialog"
], function(ConnectorTypeAbstractView, validatorsManager, ldapFilterDialog) {

    var LDAPTypeView = ConnectorTypeAbstractView.extend({
        events: {
            "click .add-btn": "addField",
            "click .remove-btn": "removeField",
            "focus .filter": "showFilterDialog",
            "click #ssl": "toggleSSLPort",
            "click #toggleCert": "toggleCert"
        },

        render: function(args, callback) {
            var base = "templates/admin/connector/",
                bcRemoveBtns,
                aocRemoveBtns,
                gocRemoveBtns;

            $("#connectorDetails").hide();

            this.data.connectorDefaults = args.connectorDefaults;

            this.template = base + args.connectorType +".html";

            this.parentRender(_.bind(function() {
                if(args.animate) {
                    $("#connectorDetails").slideDown("slow", function() {});
                } else {
                    $("#connectorDetails").show();
                }

                bcRemoveBtns = $('#baseContextWrapper').find('.remove-btn');
                aocRemoveBtns = $('#accountObjectClassesWrapper').find('.remove-btn');
                gocRemoveBtns = $('#groupObjectClassesWrapper').find('.remove-btn');

                if(bcRemoveBtns.size() === 1) {
                    bcRemoveBtns.hide();
                } else {
                    bcRemoveBtns.show();
                }

                if(aocRemoveBtns.size() === 1) {
                    aocRemoveBtns.hide();
                } else {
                    aocRemoveBtns.show();
                }

                if(gocRemoveBtns.size() === 1) {
                    gocRemoveBtns.hide();
                } else {
                    gocRemoveBtns.show();
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

        addField: function (event){
            event.preventDefault();

            var clickedEle = event.target,
                field_type,
                field;

            if($(clickedEle).not("button")){
                clickedEle = $(clickedEle).closest("button");
            }

            field_type = $(clickedEle).attr('field_type');
            field = $(clickedEle).parent().next().clone();
            field.find('input[type=text]').val('');

            $('#' + field_type + 'Wrapper').append(field);
            $('#' + field_type + 'Wrapper').find('.remove-btn').show();

            validatorsManager.bindValidators(this.$el.find('#' + field_type + 'Wrapper'));
            validatorsManager.validateAllFields(this.$el.find('#' + field_type + 'Wrapper'));
        },

        removeField: function (event){
            event.preventDefault();

            var clickedEle = event.target,
                field_type;

            if($(clickedEle).not("button")){
                clickedEle = $(clickedEle).closest("button");
            }

            field_type = $(clickedEle).attr('field_type');

            $(clickedEle).parents(".group-field-block").remove();

            if($('#' + field_type + 'Wrapper').find('.field').size() === 1){
                $('#' + field_type + 'Wrapper').find('.remove-btn').hide();
            }

            validatorsManager.bindValidators(this.$el.find('#' + field_type + 'Wrapper'));
            validatorsManager.validateAllFields(this.$el.find('#' + field_type + 'Wrapper'));
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
                    validatorsManager.validateAllFields(this.$el);

                    textarea.on("keyup change", updateBtnStatus);

                    updateBtnStatus();
                },
                close: function(e, ui) {
                    $('#certificateContainerClone').dialog("destroy");
                },
                buttons: [
                    {
                        text : $.t("common.form.save"),
                        click: function(e) {
                            var saveBtn = $(this).parent(".ui-dialog").find(".ui-dialog-buttonpane .ui-button:first"),
                                certField;

                            if (!saveBtn.prop("disabled")) {
                                certField = _this.$el.find("#certContainer").find('[name=certificate]');

                                certField.text($('#certificateContainerClone').find('textarea').val());
                                certField.val(certField.text()); // seems to be necessary for IE

                                validatorsManager.validateAllFields(_this.$el);

                                $( this ).dialog( "close" );
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
        }
    });

    return new LDAPTypeView();
});
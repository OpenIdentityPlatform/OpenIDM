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
    "jsonEditor",
    "org/forgerock/commons/ui/common/main/AbstractView",
    "org/forgerock/commons/ui/common/main/ValidatorsManager",
    "org/forgerock/openidm/ui/admin/delegates/ConnectorDelegate",
    "org/forgerock/commons/ui/common/util/UIUtils",
    "org/forgerock/commons/ui/common/util/Constants"
], function($, _, JSONEditor, AbstractView, validatorsManager, ConnectorDelegate, UIUtils, constants) {
    var ConnectorTypeAbstractView = AbstractView.extend({
        element: "#connectorDetails",
        noBaseTemplate: true,

        render: function(args, callback) {
            var base = "templates/admin/connector/";
            this.data.docHelpUrl = constants.DOC_URL;

            $("#connectorDetails").hide();

            this.data.connectorDefaults = args.connectorDefaults;

            // Check if it's kerberos
            this.handleKerberos(args);

            ConnectorDelegate.templateCheck(args.connectorType).then(
                (data) => {
                    this.template = base + args.connectorType + ".html";

                    UIUtils.templates[constants.host + this.template] = data;

                    this.renderTemplate(callback, false, args);

                },
                (result) => {
                    this.template = base + "GenericConnector.html";

                    this.renderTemplate(callback, true, args);

                }
            );
        },

        renderTemplate: function(callback, jsonEditorLoad, args) {
            var schema = {
                    $schema: "http://forgerock.org/json-schema#",
                    "type": "object",
                    "properties": {

                    }
                },
                orderCount = 0;

            this.parentRender(_.bind(function() {
                if (args.animate) {
                    $("#connectorDetails").slideDown("slow", function() {});
                } else {
                    $("#connectorDetails").show();
                }

                if (!jsonEditorLoad) {
                    this.fieldButtonCheck();

                    this.isGeneric = false;

                    validatorsManager.bindValidators(this.$el);
                } else {
                    JSONEditor.defaults.options = {
                        theme: "bootstrap3",
                        iconlib: "fontawesome4",
                        disable_edit_json: true,
                        disable_array_reorder: true,
                        disable_collapse: true,
                        disable_properties: true,
                        show_errors: "never"
                    };

                    this.isGeneric = true;

                    // For now we will allow the schema to be generic with no restrictions
                    // this.setSchema(schema, orderCount);

                    this.editor = new JSONEditor(this.$el.find("#genericConnectorBody")[0], {
                        schema: schema
                    });

                    this.editor.setValue(this.data.connectorDefaults.configurationProperties);
                }

                if (callback) {
                    callback();
                }
            }, this));
        },

        setSchema: function(schema, orderCount) {
            _.each(this.data.connectorDefaults.configurationProperties, function(value, key, obj) {
                if (value === null) {
                    this.data.connectorDefaults.configurationProperties[key] = "";

                    schema.properties[key] = {
                        type:"string",
                        propertyOrder: orderCount
                    };

                } else if (value === true || value === false) {
                    schema.properties[key] = {
                        type:"boolean",
                        propertyOrder: orderCount
                    };
                } else if (_.isObject(value)) {
                    schema.properties[key] = {
                        type:"object",
                        propertyOrder: orderCount
                    };
                } else if (_.isArray(value)) {
                    schema.properties[key] = {
                        type:"array",
                        propertyOrder: orderCount
                    };
                } else {
                    schema.properties[key] = {
                        type:"string",
                        propertyOrder: orderCount
                    };
                }
                orderCount++;
            }, this);
        },

        getGenericState: function() {
            return this.isGeneric;
        },

        getGenericConnector: function() {
            return this.editor.getValue();
        },

        fieldButtonCheck: function() {
            var arrayComponents = $(".connector-array-component");

            _.each(arrayComponents, function(component){
                if($(component).find(".remove-btn").length === 1) {
                    $(component).find(".input-group-addon").hide();
                } else {
                    $(component).find(".input-group-addon").show();
                }
            }, this);
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
            $('#' + field_type + 'Wrapper').find('.input-group-addon').show();

            validatorsManager.bindValidators(this.$el.find('#' + field_type + 'Wrapper'));

            $(field).find("input").trigger("validate");
        },

        removeField: function (event){
            event.preventDefault();

            var clickedEle = event.target,
                field_type;

            if($(clickedEle).not("button")){
                clickedEle = $(clickedEle).closest("button");
            }

            field_type = $(clickedEle).attr('field_type');

            $(clickedEle).parents(".form-group").remove();

            if($('#' + field_type + 'Wrapper').find('.field').size() === 1){
                $('#' + field_type + 'Wrapper').find('.input-group-addon').hide();
            }

            validatorsManager.bindValidators(this.$el.find('#' + field_type + 'Wrapper'));
            validatorsManager.validateAllFields(this.$el.find('#' + field_type + 'Wrapper'));
        },

        handleKerberos: function(args) {
            if (args.connectorType.match("kerberos")) {

                var configProps = this.data.connectorDefaults.configurationProperties,
                    customConfigString = "kadmin { cmd = '/usr/sbin/kadmin.local'; user='<KADMIN USERNAME>'; default_realm='<REALM, e.g. EXAMPLE.COM>' }",
                    connectorType = "org.forgerock.openicf.connectors.ssh.SSHConnector_1.4";

                args.connectorType = connectorType;
                this.data.isKerberos = true;

                // set defaults for the template if these are not already set
                // in the provisioner
                if (!configProps.customConfiguration) {
                    configProps.customConfiguration = customConfigString;
                }
            }
        }
    });

    return ConnectorTypeAbstractView;
});

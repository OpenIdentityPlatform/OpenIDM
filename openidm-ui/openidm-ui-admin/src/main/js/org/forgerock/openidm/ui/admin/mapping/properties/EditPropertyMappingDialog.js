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
 * Copyright 2014-2015 ForgeRock AS.
 */

/*global define, JSON */

define("org/forgerock/openidm/ui/admin/mapping/properties/EditPropertyMappingDialog", [
    "jquery",
    "underscore",
    "form2js",
    "org/forgerock/openidm/ui/admin/mapping/util/MappingAdminAbstractView",
    "org/forgerock/openidm/ui/admin/delegates/SyncDelegate",
    "org/forgerock/commons/ui/common/main/ValidatorsManager",
    "org/forgerock/commons/ui/common/main/Configuration",
    "org/forgerock/commons/ui/common/util/UIUtils",
    "org/forgerock/commons/ui/common/main/EventManager",
    "org/forgerock/commons/ui/common/util/Constants",
    "org/forgerock/commons/ui/common/main/SpinnerManager",
    "org/forgerock/openidm/ui/admin/util/InlineScriptEditor",
    "org/forgerock/openidm/ui/admin/mapping/util/LinkQualifierFilterEditor",
    "org/forgerock/openidm/ui/admin/util/AdminUtils",
    "bootstrap-dialog",
    "bootstrap-tabdrop",
    "selectize"
], function($, _, form2js,
            MappingAdminAbstractView,
            syncDelegate,
            validatorsManager,
            conf,
            uiUtils,
            eventManager,
            constants,
            spinner,
            inlineScriptEditor,
            LinkQualifierFilterEditor,
            AdminUtils,
            BootstrapDialog,
            tabdrop,
            selectize) {

    var EditPropertyMappingDialog = MappingAdminAbstractView.extend({
        template: "templates/admin/mapping/properties/EditPropertyMappingDialogTemplate.html",
        el: "#dialogs",
        events: {
            "click input[type=submit]": "formSubmit",
            "change :input[name=source]": "updateProperty",
            "change :input": "validateMapping",
            "onValidate": "onValidate",
            "click .toggle-view-btn": "conditionalUpdateType",
            "shown.bs.tab #conditionalScript" : "conditionalTabChange"
        },

        updateProperty: function (e) {
            if ($(e.target).val().length || _.has(this.data.property, "source")) {
                this.data.property.source = $(e.target).val();
            }
        },

        conditionalUpdateType: function (event) {
            var type = $(event.target).attr("id"),
                filter = "";

            $(event.target).toggleClass("active", true);
            this.$el.find(".toggle-view-btn").not(event.target).toggleClass("active", false);

            if (type === "conditionalFilter") {
                if(this.conditionFilterEditor === null) {
                    this.conditionFilterEditor = new LinkQualifierFilterEditor();

                    if (_.has(this.data.property, "condition")) {
                        if (!_.has(this.data.property.condition, "type")) {
                            filter = this.data.property.condition;
                        }
                    }

                    this.conditionFilterEditor.render({
                        "queryFilter": filter,
                        "mappingName" : this.data.mappingName,
                        "mapProps": this.data.availableSourceProps,
                        "element": "#" + "conditionFilterHolder",
                        "resource": ""
                    });
                }

            }
        },

        validateMapping: function () {
            var source = $("input[name='source']", this.currentDialog).val(),
                hasAvailableSourceProps = this.data.availableSourceProps && this.data.availableSourceProps.length,
                hasSourceValue = source && source.length,
                invalidSourceProp = hasAvailableSourceProps && hasSourceValue && !_.contains(this.data.availableSourceProps,source),
                proplistValidationMessage = $("#Property_List .validation-message", this.currentDialog),
                transformValidationMessage = $("#Transformation_Script .validation-message", this.currentDialog),
                conditionValidationMessage = $("#Condition_Script .validation-message", this.currentDialog),
                disableSave = function(el, message){
                    $("#scriptDialogUpdate").prop("disabled", true);
                    el.text(message);
                };

            if (invalidSourceProp) {
                disableSave(proplistValidationMessage, $.t("templates.mapping.validPropertyRequired"));
                return false;
            } else if ($("#exampleResult", this.currentDialog).val() === "ERROR WITH SCRIPT") {
                disableSave(transformValidationMessage, $.t("templates.mapping.invalidScript"));
                return false;
            } else if ($("#conditionResult", this.currentDialog).text() === "ERROR WITH SCRIPT") {
                disableSave(conditionValidationMessage, $.t("templates.mapping.invalidConditionScript"));
                return false;
            } else {
                $("#scriptDialogUpdate").prop("disabled", false);
                transformValidationMessage.text("");
                conditionValidationMessage.text("");
                proplistValidationMessage.text("");
                return true;
            }
        },

        formSubmit: function(event) {
            if (event) {
                event.preventDefault();
            }

            var formContent = form2js(this.el),
                mappingProperties = this.data.currentProperties,
                target = this.property,
                propertyObj = mappingProperties[target - 1];

            // in the case when our property isn't currently found in the sync config...
            if (!propertyObj) {
                propertyObj = {"target": this.property};
                _.find(this.getCurrentMapping(), function (o) { return o.name === this.data.mappingName; })
                    .properties
                    .push(propertyObj);
            }

            if (formContent.source) {
                propertyObj.source = formContent.source;
            } else {
                delete propertyObj.source;
            }

            if (this.transform_script_editor !== undefined) {
                propertyObj.transform = this.transform_script_editor.generateScript();

                if(propertyObj.transform === null) {
                    delete propertyObj.transform;
                } else if (!propertyObj.source) {
                    propertyObj.source = "";
                }
            } else {
                delete propertyObj.transform;
            }

            if (this.currentDialog.find(".toggle-view-btn.active").attr("id") === "conditionalScript" && this.conditional_script_editor !== undefined) {
                propertyObj.condition = this.conditional_script_editor.generateScript();

                if (propertyObj.condition === null) {
                    delete propertyObj.condition;
                }
            } else if (this.currentDialog.find(".toggle-view-btn.active").attr("id") === "conditionalFilter") {
                propertyObj.condition = {};
                propertyObj.condition = this.conditionFilterEditor.getFilterString();
                // applies when the filter option selected is "No Filter"
                if (propertyObj.condition.length === 0) {
                    delete propertyObj.condition;
                }
            } else {
                delete propertyObj.condition;
            }

            if (_.has(formContent, "default")) {
                if($('#default_desc', this.currentDialog).text().length > 0){
                    propertyObj["default"] = $('#default_desc', this.currentDialog).text();
                } else{
                    propertyObj["default"] = formContent["default"];
                }

            } else {
                delete propertyObj["default"];
            }

            this.data.saveCallback(mappingProperties);
        },

        close: function () {
            $("#dialogs").hide();
        },

        conditionalTabChange: function () {
            this.conditional_script_editor.refresh();
        },

        render: function(params, callback) {
            var currentProperties,
                sourceType;

            this.data.mappingName = this.getMappingName();
            this.property = params.id;
            this.transform_script_editor = undefined;
            this.conditional_script_editor = undefined;
            this.conditionFilterEditor = null;

            this.data.saveCallback = params.saveCallback;
            this.data.availableSourceProps = params.availProperties || [];
            this.data.currentProperties = currentProperties = params.mappingProperties || this.getCurrentMapping().properties;
            this.data.property = currentProperties[this.property - 1];

            if (conf.globalData.sampleSource && _.isUndefined(this.data.property.source)) {
                this.data.sampleSourceTooltip = JSON.stringify(conf.globalData.sampleSource, null, 2);
            } else {
                this.data.sampleSourceTooltip = null;
            }

            this.data.currentMappingDetails = this.getCurrentMapping();

            sourceType = this.data.currentMappingDetails.source.split("/");

            AdminUtils.findPropertiesList(sourceType).then(_.bind(function(properties){
                this.data.resourceSchema = properties;

                this.renderEditProperty(callback);
            }, this));
        },

        renderEditProperty: function(callback) {
            var _this = this,
                settings;

            settings = {
                "title": $.t("templates.mapping.propertyEdit.title", {"property": this.data.property.target}),
                "template": this.template,
                "postRender": _.bind(this.loadData, this)
            };

            this.currentDialog = $('<form id="propertyMappingDialogForm"></form>');

            $('#dialogs').append(this.currentDialog);
            this.setElement(this.currentDialog);

            BootstrapDialog.show({
                title: settings.title,
                closable: false,
                type: BootstrapDialog.TYPE_DEFAULT,
                message: this.currentDialog,
                size: BootstrapDialog.SIZE_WIDE,
                onshown : function (dialogRef) {
                    uiUtils.renderTemplate(settings.template, _this.currentDialog,
                        _.extend(conf.globalData, _this.data),
                        function () {
                            settings.postRender();

                            _this.$el.find("#sourcePropertySelect").selectize({
                                persist: false,
                                create: false
                            });

                            if(_this.data.property.source) {
                                _this.$el.find("#sourcePropertySelect")[0].selectize.setValue(_this.data.property.source);
                            }

                            _this.currentDialog.find(".nav-tabs").tabdrop();

                            _this.currentDialog.find(".nav-tabs").on("shown.bs.tab", function (e) {
                                if($(e.target).attr("href") === "#Transformation_Script"){
                                    _this.transform_script_editor.refresh();
                                } else if ($(e.target).attr("href") === "#Condition_Script") {
                                    _this.conditional_script_editor.refresh();
                                }
                            });

                            if(callback){
                                callback();
                            }

                            _this.$el.find(".details-tooltip").popover({
                                content: function () { return $(this).find(".tooltip-details").clone().show();},
                                placement:'right',
                                container: 'body',
                                html: 'true',
                                title: ''
                            });
                        }, "replace");
                },
                buttons: [{
                    label: $.t("common.form.cancel"),
                    id:"scriptDialogCancel",
                    action: function(dialogRef) {
                        dialogRef.close();
                    }
                },
                    {
                        label: $.t("common.form.update"),
                        id:"scriptDialogUpdate",
                        cssClass: 'btn-primary',
                        action: _.bind(function(dialogRef) {
                            this.formSubmit();
                            dialogRef.close();
                        },_this)
                    }]
            });
        },

        loadData: function() {
            var _this = this,
                prop = this.data.property,
                filter = "",
                conditionData = null;

            if (prop) {
                if (typeof(prop.transform) === "object" && prop.transform.type === "text/javascript" &&
                    typeof (prop.transform.source) === "string") {
                    this.transform_script_editor = inlineScriptEditor.generateScriptEditor({
                        "element": this.currentDialog.find("#transformationScriptHolder"),
                        "eventName": "",
                        "noValidation": true,
                        "scriptData": prop.transform,
                        "disablePassedVariable": false,
                        "placeHolder": "source.givenName.toLowerCase() + \" .\" + source.sn.toLowerCase()"
                    });
                }

                if (_.has(prop, "condition")) {
                    this.$el.find("#conditionTabButtons").toggleClass("active", false);
                    if (_.has(prop.condition, "type")) {
                        this.currentDialog.find("#conditionalScript").toggleClass("active", true);
                        this.currentDialog.find("#conditionScriptTab").toggleClass("active", true);
                    } else {
                        this.currentDialog.find("#conditionalFilter").toggleClass("active", true);
                        this.currentDialog.find("#conditionFilterTab").toggleClass("active", true);

                        this.conditionFilterEditor = new LinkQualifierFilterEditor();

                        if (_.has(this.data.property, "condition")) {
                            if (!_.has(this.data.property.condition, "type")) {
                                filter = this.data.property.condition;
                            }
                        }

                        this.conditionFilterEditor.render({
                            "queryFilter": filter,
                            "mappingName" : this.data.mappingName,
                            "mapProps": this.data.availableSourceProps,
                            "element": "#" + "conditionFilterHolder",
                            "resource": ""
                        });
                    }
                } else {
                    this.currentDialog.find("#conditionalNone").toggleClass("active", true);
                    this.currentDialog.find("#noneTab").toggleClass("active", true);
                }
            }

            _this.transform_script_editor = inlineScriptEditor.generateScriptEditor({
                "element": _this.currentDialog.find("#transformationScriptHolder"),
                "eventName": "transform",
                "noValidation": true,
                "scriptData": _this.data.property.transform,
                "disablePassedVariable": false,
                "placeHolder" : "source.givenName.toLowerCase() + \" .\" + source.sn.toLowerCase()"
            });

            if(!_.isString(_this.data.property.condition)) {
                conditionData = _this.data.property.condition;
            }

            _this.conditional_script_editor = inlineScriptEditor.generateScriptEditor({
                "element": _this.currentDialog.find("#conditionScriptHolder"),
                "eventName": "conditional",
                "noValidation": true,
                "scriptData": conditionData,
                "disablePassedVariable": false
            });

            $('#mappingDialogTabs a', this.currentDialog).click(function (e) {
                e.preventDefault();
                $(this).tab('show');
                $('#mappingDialogTabs .active :input:first', _this.currentDialog).focus();
            });

            $('#mappingDialogTabs a:first', this.currentDialog).tab('show');

            $('#mappingDialogTabs .active :input:first', this.currentDialog).focus();

            $("input[name='source']", this.currentDialog).on('change autocompleteclose', function (e, initialRender) {
                var val = $(this).val(),
                    isValid;

                if (val) {
                    $("#currentSourceDisplay", _this.currentDialog).val(val);
                } else {
                    $("#currentSourceDisplay", _this.currentDialog).val($.t("templates.mapping.completeSourceObject"));
                }

                isValid = _this.validateMapping();

                if(isValid && initialRender !== "true" && $('#propertyMappingDialogForm').size() === 0){
                    _this.formSubmit();
                }
            });

            $("input[name='source']", this.currentDialog).trigger('change', 'true');

            spinner.hideSpinner();
        }
    });

    return new EditPropertyMappingDialog();
});

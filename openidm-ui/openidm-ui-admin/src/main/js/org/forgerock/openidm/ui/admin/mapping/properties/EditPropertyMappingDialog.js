/**
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2014-2015 ForgeRock AS. All rights reserved.
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
    "org/forgerock/openidm/ui/admin/util/AutoCompleteUtils",
    "org/forgerock/openidm/ui/admin/util/InlineScriptEditor",
    "org/forgerock/openidm/ui/admin/mapping/util/LinkQualifierFilterEditor",
    "bootstrap-dialog",
    "bootstrap-tabdrop"
], function($, _, form2js,
            MappingAdminAbstractView,
            syncDelegate,
            validatorsManager,
            conf,
            uiUtils,
            eventManager,
            constants,
            spinner,
            autoCompleteUtils,
            inlineScriptEditor,
            LinkQualifierFilterEditor,
            BootstrapDialog,
            tabdrop) {

    var EditPropertyMappingDialog = MappingAdminAbstractView.extend({
        template: "templates/admin/mapping/properties/EditPropertyMappingDialogTemplate.html",
        el: "#dialogs",
        events: {
            "click input[type=submit]": "formSubmit",
            "change :input[name=source]": "updateProperty",
            "change :input": "validateMapping",
            "onValidate": "onValidate",
            "change .conditionalUpdateType": "conditionalUpdateType"
        },

        updateProperty: function (e) {
            if ($(e.target).val().length || _.has(this.data.property, "source")) {
                this.data.property.source = $(e.target).val();
            }
        },

        conditionalUpdateType: function () {
            var type = this.currentDialog.find(".conditionalUpdateType:checked").val(),
                filter = "";

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

                this.currentDialog.find(".conditionalFilter").show();
                this.currentDialog.find(".conditionalScript").hide();

            } else if (type === "script") {
                this.currentDialog.find(".conditionalFilter").hide();
                this.currentDialog.find(".conditionalScript").show();

            } else if (type === "none") {
                this.currentDialog.find(".conditionalFilter").hide();
                this.currentDialog.find(".conditionalScript").hide();
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

            if (this.currentDialog.find("input[name=conditionalUpdate]:checked").val() === "script" && this.conditional_script_editor !== undefined) {
                propertyObj.condition = this.conditional_script_editor.generateScript();

                if (propertyObj.condition === null) {
                    delete propertyObj.condition;
                }
            } else if (this.currentDialog.find("#Condition_Script input[name=conditionalUpdate]:checked").val() === "conditionalFilter") {
                propertyObj.condition = {};
                propertyObj.condition = this.conditionFilterEditor.getFilterString();

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

        render: function(params, callback) {
            var _this = this,
                currentProperties,
                settings;

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

            settings = {
                "title": $.t("templates.mapping.propertyEdit.title", {"property": this.data.property.target}),
                "template": this.template,
                "postRender": _.bind(this.loadData, this)
            };

            this.data.currentMappingDetails = this.getCurrentMapping();

            this.currentDialog = $('<form id="propertyMappingDialogForm"></form>');

            $('#dialogs').append(this.currentDialog);
            this.setElement(this.currentDialog);

            BootstrapDialog.show({
                title: settings.title,
                type: BootstrapDialog.TYPE_DEFAULT,
                message: this.currentDialog,
                size: BootstrapDialog.SIZE_WIDE,
                onshown : function (dialogRef) {
                    uiUtils.renderTemplate(settings.template, _this.currentDialog,
                        _.extend(conf.globalData, _this.data),
                        function () {
                            settings.postRender();
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
                                trigger:'hover',
                                placement:'right',
                                container: 'body',
                                html: 'true',
                                template: '<div class="popover popover-info popover-large" role="tooltip"><div class="popover-header">Raw Source Data:</div><div class="popover-content"></div></div>'
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
                prop = this.data.property;

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
                    this.$el.find("#conditionTabButtons label").toggleClass("active", false);

                    if (_.has(prop.condition, "type")) {
                        this.currentDialog.find("#conditionalScript").parent().toggleClass("active", true);
                        this.currentDialog.find("#conditionalScript").prop("checked", true).change();
                    } else {
                        this.currentDialog.find("#conditionalFilter").parent().toggleClass("active", true);
                        this.currentDialog.find("#conditionalFilter").prop("checked", true).change();
                    }
                } else {
                    this.currentDialog.find("#conditionalNone").parent().toggleClass("active", true);
                    this.currentDialog.find("#conditionalNone").prop("checked", true).change();
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

            _this.conditional_script_editor = inlineScriptEditor.generateScriptEditor({
                "element": _this.currentDialog.find("#conditionScriptHolder"),
                "eventName": "conditional",
                "noValidation": true,
                "scriptData": _this.data.property.condition,
                "disablePassedVariable": false
            });

            _this.conditionalUpdateType();

            $('#mappingDialogTabs a', this.currentDialog).click(function (e) {
                e.preventDefault();
                $(this).tab('show');
                $('#mappingDialogTabs .active :input:first', _this.currentDialog).focus();
            });

            $('#mappingDialogTabs a:first', this.currentDialog).tab('show');

            $('#mappingDialogTabs .active :input:first', this.currentDialog).focus();

            if (this.data.availableSourceProps) {
                autoCompleteUtils.selectionSetup($("input[name='source']:last", this.currentDialog), _.sortBy(this.data.availableSourceProps,function(s){ return s; }));
            }

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

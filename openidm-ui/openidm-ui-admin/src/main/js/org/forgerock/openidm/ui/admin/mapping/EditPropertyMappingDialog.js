/**
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2014-2015 ForgeRock AS. All rights reserved.
 */

/*global define, $, _, Handlebars, form2js, window */
/*jslint evil: true */

define("org/forgerock/openidm/ui/admin/mapping/EditPropertyMappingDialog", [
    "org/forgerock/commons/ui/common/main/AbstractView",
    "org/forgerock/openidm/ui/admin/delegates/SyncDelegate",
    "org/forgerock/commons/ui/common/main/ValidatorsManager",
    "org/forgerock/commons/ui/common/main/Configuration",
    "org/forgerock/commons/ui/common/util/UIUtils",
    "org/forgerock/commons/ui/common/main/EventManager",
    "org/forgerock/commons/ui/common/util/Constants",
    "org/forgerock/commons/ui/common/main/SpinnerManager",
    "org/forgerock/openidm/ui/admin/delegates/BrowserStorageDelegate",
    "org/forgerock/openidm/ui/admin/util/AutoCompleteUtils",
    "org/forgerock/openidm/ui/admin/util/InlineScriptEditor",
    "org/forgerock/openidm/ui/admin/sync/LinkQualifierFilterEditor",
    "bootstrap-dialog",
    "bootstrap-tabdrop"
], function(AbstractView, syncDelegate, validatorsManager, conf, uiUtils, eventManager, constants, spinner, browserStorageDelegate, autoCompleteUtils, inlineScriptEditor, LinkQualifierFilterEditor, BootstrapDialog, tabdrop) {
    var EditPropertyMappingDialog = AbstractView.extend({
        template: "templates/admin/mapping/PropertyMappingDialogEditTemplate.html",
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
            if(event){
                event.preventDefault();
            }

            var formContent = form2js(this.el),
                mappingProperties = browserStorageDelegate.get(this.data.mappingName + "_Properties"),
                target = this.property,
                propertyObj = mappingProperties[target - 1];

            // in the case when our property isn't currently found in the sync config...
            if (!propertyObj) {
                propertyObj = {"target": this.property};
                _.find(browserStorageDelegate.get("currentMapping"), function (o) { return o.name === this.data.mappingName; })
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
                }
                else{
                    propertyObj["default"] = formContent["default"];
                }

            } else {
                delete propertyObj["default"];
            }

            browserStorageDelegate.set(this.data.mappingName + "_Properties",mappingProperties);

            eventManager.sendEvent(constants.ROUTE_REQUEST, {routeName: "propertiesView", args: [this.data.mappingName]});
        },

        close: function () {
            $("#dialogs").hide();
        },

        render: function(params, callback) {
            var _this = this,
                currentProperties,
                settings,
                syncConfig;

            this.data.mappingName = params[0];
            this.property = params[1];
            this.transform_script_editor = undefined;
            this.conditional_script_editor = undefined;
            this.conditionFilterEditor = null;

            currentProperties = browserStorageDelegate.get(this.data.mappingName + "_Properties") || browserStorageDelegate.get("currentMapping").properties;

            this.data.currentProperties = currentProperties;

            browserStorageDelegate.set(this.data.mappingName + "_Properties",currentProperties);

            this.data.property = currentProperties[this.property - 1];

            settings = {
                "title": $.t("templates.mapping.propertyEdit.title", {"property": this.data.property.target}),
                "template": this.template,
                "postRender": _.bind(this.loadData, this)
            };

            syncConfig = syncDelegate.mappingDetails(this.data.mappingName);

            syncConfig.then(_.bind(function(details){

                this.data.currentMappingDetails = _.find(details.mappings, function(map) {
                    return map.name === this.data.mappingName;
                }, this);

                this.currentDialog = $('<form id="propertyMappingDialogForm"></form>');

                $('#dialogs').append(this.currentDialog);
                this.setElement(this.currentDialog);

                BootstrapDialog.show({
                    title: settings.title,
                    type: BootstrapDialog.TYPE_DEFAULT,
                    message: this.currentDialog,
                    size: BootstrapDialog.SIZE_WIDE,
                    onhide: function(dialogRef){
                        eventManager.sendEvent(constants.ROUTE_REQUEST, {routeName: "propertiesView", args: [_this.data.mappingName]});
                    },
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
            }, this));
        },

        loadData: function() {
            var selectedTab = 0,
                _this = this,
                prop = this.data.property;

            this.data.availableSourceProps = browserStorageDelegate.get(this.data.mappingName + "_AvailableObjects").source.properties || [];

            if (prop) {
                if (typeof(prop.source) !== "undefined" && prop.source.length) {
                    selectedTab = 0;
                } else if (typeof(prop.transform) === "object" && prop.transform.type === "text/javascript" &&
                    typeof (prop.transform.source) === "string") {
                    this.transform_script_editor = inlineScriptEditor.generateScriptEditor({
                        "element": this.currentDialog.find("#transformationScriptHolder"),
                        "eventName": "",
                        "noValidation": true,
                        "scriptData": prop.transform,
                        "disablePassedVariable": false,
                        "placeHolder" : "source.givenName.toLowerCase() + \" .\" + source.sn.toLowerCase()"
                    });

                    selectedTab = 1;
                } else if (typeof(prop["default"]) !== "undefined" && prop["default"].length) {
                    selectedTab = 3;
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

            if(this.data.availableSourceProps){
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
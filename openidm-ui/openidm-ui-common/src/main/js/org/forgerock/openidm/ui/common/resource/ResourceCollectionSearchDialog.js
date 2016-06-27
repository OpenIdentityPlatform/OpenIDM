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
 * Copyright 2011-2016 ForgeRock AS.
 */

define([
    "jquery",
    "underscore",
    "org/forgerock/commons/ui/common/main/AbstractView",
    "org/forgerock/commons/ui/common/main/Configuration",
    "org/forgerock/commons/ui/common/main/EventManager",
    "org/forgerock/commons/ui/common/util/Constants",
    "org/forgerock/commons/ui/common/util/UIUtils",
    "bootstrap-dialog",
    "org/forgerock/openidm/ui/common/util/ResourceCollectionUtils"
], function($, _, AbstractView, conf, eventManager, constants, uiUtils, BootstrapDialog, resourceCollectionUtils) {
    var ResourceCollectionSearchDialog = AbstractView.extend({
        template: "templates/admin/resource/ResourceCollectionSearchDialogTemplate.html",
        el: "#dialogs",
        data: {},
        /**
         * valid opts:
         *     property = the schema def for a property
         *     propertyValue = either null or the value of the property being set (Can be either an actual JSON object or a raw text representation of a JSON object)
         *     schema = the schema for the current resource
         *     onChange = a function that gets called after the save button is pressed
         *     multiSelect = a boolean flag to tell selectize it can select multiple items
         */
        render: function(opts, onLoadCallback) {
            var _this = this,
                title = $.t("templates.admin.ResourceEdit.addResource",{ resource: opts.property.title }),
                saveButtonText = $.t("common.form.add");

            this.onLoadCallback = onLoadCallback;

            this.currentDialog = $('<div id="resourceCollectionSearchDialog"></div>');
            this.schema = opts.schema;
            this.onChange = opts.onChange;
            this.maxSelectionItems = (opts.multiSelect) ? 10 : 1;

            this.data.property = opts.property;
            this.data.propertyValue = {};

            if (opts.propertyValue && opts.propertyValue !== "") {
                this.data.propertyValue = (_.isObject(opts.propertyValue)) ? opts.propertyValue : JSON.parse(opts.propertyValue);
                this.propertyValuePath = resourceCollectionUtils.getPropertyValuePath(this.data.propertyValue);
                title = $.t("templates.admin.ResourceEdit.updateResource",{ resource: opts.property.title });
                saveButtonText = $.t("common.form.save");
            }

            this.data.originalPropertyValue = _.cloneDeep(this.data.propertyValue);

            this.setRefProperties();

            $('#dialogs').append(this.currentDialog);

            //change dialog
            this.dialog = BootstrapDialog.show({
                title: title,
                type: BootstrapDialog.TYPE_DEFAULT,
                message: this.currentDialog,
                size: BootstrapDialog.SIZE_WIDE,
                cssClass : "objecttype-windows",
                onshown : _.bind(function (dialogRef) {
                    this.loadTemplate();
                }, _this),
                buttons: [
                    {
                        label: $.t('common.form.close'),
                        id: "resourceCollectionSearchDialogCloseBtn",
                        action: function(dialogRef){
                            dialogRef.close();
                        }
                    },
                    {
                        label: saveButtonText,
                        cssClass: "btn-primary",
                        id: "resourceCollectionSearchDialogSaveBtn",
                        action: function(dialogRef) {
                            var promise,
                                newValueArray;

                            if (_this.currentDialog.find("#select_" + _this.data.property.propName).val()) {
                                newValueArray = _this.getNewValArray();
                                _.each(newValueArray, function (item, index) {
                                    var isFinalPromise = index === newValueArray.length - 1;
                                    if (!promise) {
                                        promise = opts.onChange(item.val, _this.data.originalPropertyValue, item.text, isFinalPromise);
                                    } else {
                                        promise = promise.then( () => {
                                            return opts.onChange(item.val, _this.data.originalPropertyValue, item.text, isFinalPromise);
                                        });
                                    }
                                });
                                dialogRef.close();
                            }
                        }
                    }
                ]
            });
        },
        loadTemplate: function () {
            uiUtils.renderTemplate(
                    this.template,
                    this.currentDialog,
                    _.extend({}, conf.globalData, this.data),
                    _.bind(function(){
                        this.setupResourceTypeField(this.propertyValuePath);
                    }, this),
                    "replace"
                );
        },
        setRefProperties: function () {
            var properties,
                relationshipPropName = this.data.property.propName,
                relationshipProp = this.schema.properties[this.data.property.propName];

            if (relationshipProp.items) {
                relationshipProp = relationshipProp.items;
            }

            if (relationshipProp) {
                properties = relationshipProp.properties._refProperties.properties;
            } else {
                properties = relationshipProp.properties._refProperties.properties;
            }

            this.data._refProperties = [];
            _.map(_.omit(properties,"_id"), _.bind(function (val,key) {
                var value = (!_.isEmpty(this.data.propertyValue)) ? this.data.propertyValue._refProperties[key] : "";

                this.data._refProperties.push({
                    label: val.label,
                    name: key,
                    value: value
                });
            }, this));
        },
        setupResourceTypeField: function(typePath) {
            var _this = this,
                autocompleteField = this.currentDialog.find("#select_" + this.data.property.propName + "_Type"),
                pathToLabel = function (path) {
                    var pathArr = path.split("/"),
                        relationshipProp = (_this.data.property.items) ? _this.data.property.items : _this.data.property,
                        resourceCollection = _.findWhere(relationshipProp.resourceCollection,{ path: path });

                    pathArr = _.map(pathArr, function (item) {
                        return item.charAt(0).toUpperCase() + item.slice(1);
                    });

                    return (resourceCollection && resourceCollection.label) ? resourceCollection.label : pathArr.join(" ");
                },
                opts = {
                    valueField: 'path',
                    searchField: 'label',
                    create: false,
                    preload: true,
                    placeholder: $.t("templates.admin.ResourceEdit.search",{ objectTitle: this.data.property.title || this.data.property.name }),
                    render: {
                        item: function(item, escape) {
                            return "<div>" + item.label + "</div>";
                        },
                        option: function(item, escape) {
                            return "<div>" + item.label + "</div>";
                        }
                    },
                    load: function(query, callback) {
                        var resourceCollections = [],
                            prop = _this.data.property;

                        if (prop.items) {
                            prop = prop.items;
                        }

                        _.map(prop.resourceCollection, function (resourceCollection) {
                            resourceCollections.push({
                                path : resourceCollection.path,
                                label : resourceCollection.label || pathToLabel(resourceCollection.path)
                            });
                        });

                        if (!typePath) {
                            typePath = resourceCollections[0].path;
                            autocompleteField[0].selectize.addOption({ path: typePath, label: pathToLabel(typePath) });
                            autocompleteField[0].selectize.setValue(typePath);
                        }

                        callback(resourceCollections);
                    },
                    onChange: _.bind(function (val) {
                        this.setupResourceField(resourceCollectionUtils.getResourceCollectionIndex(this.schema,val, this.data.property.propName));
                    }, this)
                };

            autocompleteField.selectize(opts);

            if (typePath && typePath.length) {
                autocompleteField[0].selectize.addOption({ path: typePath, label: pathToLabel(typePath) });
                autocompleteField[0].selectize.setValue(typePath);
            }

            if ((this.data.property.resourceCollection && this.data.property.resourceCollection.length < 2) || (this.data.property.items && this.data.property.items.resourceCollection && this.data.property.items.resourceCollection.length < 2)) {
                this.currentDialog.find("#formGroupType").hide();
                this.setupResourceField(0);
            }

            if (this.onLoadCallback) {
                this.onLoadCallback();
            }
        },
        setupResourceField: function(resourceCollectionIndex) {
            var autocompleteField = $("#select_" + this.data.property.propName),
                resourceTypeValue = $("#select_" + this.data.property.propName + "_Type").val(),
                addResourceLink = $("#addResourceLink"),
                resourceCollection = (this.data.property.items) ? this.data.property.items.resourceCollection[resourceCollectionIndex] : this.data.property.resourceCollection[resourceCollectionIndex];

            if ( resourceCollection.path.split("/")[0] === "system" || resourceCollection.path.split("/")[0] === "managed") {
                addResourceLink.attr("href", "#resource/" + resourceCollection.path + "/add/");
                addResourceLink.find("span").text($.t("templates.admin.ResourceEdit.createNewResource", { resource : resourceCollection.label || resourceCollection.path }));
                addResourceLink.show();
            } else {
                addResourceLink.hide();
            }

            if ((resourceTypeValue && resourceTypeValue.length) || resourceCollectionIndex === 0) {
                resourceCollectionUtils.setupAutocompleteField(autocompleteField, this.data.property, { maxItems : this.maxSelectionItems }, resourceCollectionIndex, this.data.propertyValue );
                this.currentDialog.find("#relationshipForm").show();
            } else {
                this.currentDialog.find("#relationshipForm").hide();
            }
        },
        /**
        *  this function gathers all the info from the dialog and returns an array of relationship objects
        *
        *   @param {object} refPropsOverride - optional parameter used to override the default functionality for getting _refProperties
        *   @returns {array} - an array of relationship objects
        */
        getNewValArray: function (refPropsOverride) {
            var propVal = this.data.propertyValue,
                typeValue = this.currentDialog.find("#select_" + this.data.property.propName + "_Type").val(),
                relationshipValue = this.currentDialog.find("#select_" + this.data.property.propName).val(),
                refProperties = this.currentDialog.find("._refProperties:input"),
                getRefProps = function () {
                    var refProps = propVal._refProperties || {};
                    _.map(refProperties, function (refProp) {
                        refProps[$(refProp).attr("propName")] = $(refProp).val();
                    });
                    return refProps;
                },
                valueArray = [],
                relationshipValuePath;

            //if relationshipValue is not an array there is only one value
            if (!_.isArray(relationshipValue)) {
                relationshipValuePath = typeValue + "/" + relationshipValue;
                valueArray.push({
                    val: _.extend(propVal, { "_ref": relationshipValuePath , "_refProperties" : refPropsOverride || getRefProps() }),
                    text: this.currentDialog.find(".selectize-input:eq(1)").find("[data-value]").text()
                });
            } else {
                _.each(relationshipValue, (val) => {
                    var readPath = typeValue + "/" + val;

                    valueArray.push({
                        val: { "_ref": readPath , "_refProperties" : refPropsOverride || getRefProps() }
                    });
                });
            }

            return valueArray;
        }
    });

    return ResourceCollectionSearchDialog;
});

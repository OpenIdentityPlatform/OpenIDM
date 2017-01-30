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
 * Copyright 2017 ForgeRock AS.
 */

define([
    "jquery",
    "underscore",
    "handlebars",
    "backbone",
    "form2js",
    "org/forgerock/openidm/ui/admin/managed/AbstractManagedView",
    "org/forgerock/commons/ui/common/util/UIUtils",
    "org/forgerock/openidm/ui/common/delegates/ConfigDelegate",
    "org/forgerock/commons/ui/common/components/ChangesPending",
    "org/forgerock/openidm/ui/admin/delegates/RepoDelegate",
    "org/forgerock/openidm/ui/admin/managed/schema/dataTypes/RelationshipTypeView",
    "org/forgerock/openidm/ui/admin/managed/schema/PolicyView",
    "org/forgerock/openidm/ui/admin/managed/schema/dataTypes/ObjectTypeView",
    "org/forgerock/openidm/ui/admin/managed/schema/dataTypes/ArrayTypeView",
    "org/forgerock/openidm/ui/admin/managed/schema/util/SchemaUtils",
    "org/forgerock/openidm/ui/admin/util/ScriptList",
    "org/forgerock/commons/ui/common/main/EventManager",
    "org/forgerock/commons/ui/common/util/Constants"
], function($, _,
    handlebars,
    Backbone,
    form2js,
    AbstractManagedView,
    UIUtils,
    ConfigDelegate,
    ChangesPending,
    RepoDelegate,
    RelationshipTypeView,
    PolicyView,
    ObjectTypeView,
    ArrayTypeView,
    SchemaUtils,
    ScriptList,
    EventManager,
    Constants
) {

    var SchemaPropertyView = AbstractManagedView.extend({
        template: "templates/admin/managed/schema/dataTypes/SchemaPropertyViewTemplate.html",
        events: {
            "click .advanced-options-toggle" : "toggleAdvanced",
            "change input,textarea,select" : "makeChanges",
            "change .secureHash_selection" : "makeChanges",
            "keyup input" : "makeChanges",
            "change #hashedToggle" : "toggleHashSelect",
            "change #encryptionToggle" : "toggleEncryptionSelect",
            "click .savePropertyDetails" : "saveProperty",
            "click #deleteProperty" : "deleteProperty"
        },
        data: {},
        model: {},
        partials: [],
        /**
        * @param {array} args - args[0] = managed object name, args[1] = slash separated list representing property path
        * @param {function} callback - a function to be executed after load
        */
        render: function(args, callback, isReload) {
            var partialPromise = UIUtils.preloadPartial("partials/managed/schema/_propertyBreadcrumbLink.html"),
                managedConfigPromise = ConfigDelegate.readEntity("managed"),
                repoPromise = RepoDelegate.findRepoConfig();

            this.args = args;

            //set this.data and this.model every time this view is rendered so always start with a clean slate
            this.data = {
                availableHashes: ["MD5","SHA-1","SHA-256","SHA-384","SHA-512"],
                showAdvanced: this.data.showAdvanced,
                objectProperties: this.data.objectProperties,
                currentTab: this.data.currentTab
            };
            this.model = {
                eventList: ["onValidate", "onRetrieve", "onStore"]
            };

            this.data.managedObjectName = args[0];
            this.propertyArgs = args[1].split("/");

            $.when(managedConfigPromise, repoPromise, partialPromise).then( (managedConfig, repoConfig) => {
                this.data.managedConfig = managedConfig;
                this.data.repoConfig = repoConfig;

                //get the breadcrumb trail
                this.data.breadcrumbs = this.buildBreadcrumbArray(this.data.managedObjectName, this.propertyArgs);

                //dig in get the currentManagedObject and get a reference to the property to be edited based on propertyArgs
                this.data.currentManagedObjectIndex = _.findIndex( managedConfig.objects, { name : this.data.managedObjectName });
                this.data.currentManagedObject = managedConfig.objects[this.data.currentManagedObjectIndex];
                this.data.property = this.getPropertyFromCurrentManagedObject(this.data.currentManagedObject, this.propertyArgs);

                //check to see if there is actually a property
                if (this.data.property === "INVALID") {
                    this.data.invalidProperty = true;
                }

                this.setTypeSpecificElements();

                //if this is an obect type property we always want to default to the properties tab showSchema flag does this
                //otherwise the details tab will be the default
                if (this.data.property.type === "object") {
                    this.data.showSchema = true;
                }

                this.parentRender(() => {
                    this.setupChangesPending();

                    this.setTabChangeEvent();

                    PolicyView.render([this]);

                    this.setupScriptList();

                    if (this.loadTypeSpecificElements) {
                        this.loadTypeSpecificElements();
                    }

                    if (this.data.currentTab) {
                        this.$el.find('a[href="' + this.data.currentTab + '"]').tab('show');
                    }

                    if (callback) {
                        callback();
                    }

                });
            });

        },
        setupChangesPending: function () {
            var watchedObj = _.clone(this.getCurrentFormValue(this.data.property), true);
            this.model.changesModule = ChangesPending.watchChanges({
                element: this.$el.find(".changes-pending-container"),
                undo: true,
                watchedObj: watchedObj,
                watchedProperties: _.keys(watchedObj).concat(["encryption","scope","secureHash"]),
                undoCallback: () => {
                    this.render(this.args, () => {
                        if (this.data.showAdvanced) {
                            this.toggleAdvanced();
                        }
                    }, true);
                }
            });
        },
        setupScriptList: function () {
            var eventKeys = _.chain(this.data.property)
                                .keys()
                                .without("name", "properties")
                                .value();

            //Added events are used for the events that are currently set by the managed object
            this.data.addedEvents = _.intersection(eventKeys, this.model.eventList);

            //Select events are the events currently available for the select
            this.data.selectEvents = _.difference(this.model.eventList, eventKeys);

            this.model.propertyScripts = ScriptList.generateScriptList({
                element: this.$el.find("#managedPropertyEvents"),
                label: $.t("templates.managed.addPropertyScript"),
                selectEvents: this.data.selectEvents,
                addedEvents: this.data.addedEvents,
                currentObject: this.data.property,
                hasWorkflow: true,
                workflowContext: this.data.currentManagedObject.schema.order,
                saveCallback: () => {
                    this.saveProperty(false, () => {
                        this.$el.find('a[href="#scriptsContainer"]').tab('show');
                    });
                }
            });
        },
        /**
        * This function digs in to the currentManagedObject and returns the property to be edited based on the propertyArgs.
        * It also sets the this.data.parentProperty object to be used when saving the property.
        * If the property does not exist then the string "INVALID PROPERTY" is returned.
        * @param {object} currentManagedObject
        * @param {array} propertyArgs
        * @returns {object}
        */
        getPropertyFromCurrentManagedObject: function (currentManagedObject, propertyArgs) {
            var property = currentManagedObject.schema.properties[propertyArgs[0]],
                nestedItems,
                invalid = "INVALID",
                getPropertyFromArrayObject = function (prop, propName) {
                    if (prop.items) {
                        prop = getPropertyFromArrayObject(prop.items, propName);
                    } else {
                        prop = prop.properties[propName];
                    }

                    return prop || invalid;
                },
                getPropertyFromProperty = (prop,propName) => {
                    this.data.parentProperty = prop;

                    if (prop.type === "array") {
                        prop = getPropertyFromArrayObject(prop.items, propName);
                        nestedItems = SchemaUtils.handleArrayNest(this.data.parentProperty.items);
                        prop.requiredByParent = _.indexOf(nestedItems.required, propName) >= 0;
                    } else {
                        prop = prop.properties[propName];

                        if (prop) {
                            prop.requiredByParent =  _.indexOf(this.data.parentProperty.required, propName) >= 0;
                        }
                    }

                    return prop || invalid;
                };

            if (!property) {
                return invalid;
            }

            property.requiredByParent = _.indexOf(currentManagedObject.schema.required, propertyArgs[0]) >= 0;

            this.data.parentProperty = currentManagedObject.schema;

            _.each(propertyArgs, (propName, index) => {
                if (index > 0) {
                    property = getPropertyFromProperty(property,propName);
                }

                property.nullable = false;

                if (_.isArray(property.type)) {
                    property.type = property.type[0];
                    property.nullable = true;
                }

                if (property.type === "array" && property.items.type === "relationship") {
                    property.type = "relationships";
                }

                if (index === propertyArgs.length - 1) {
                    this.data.propertyName = propName;
                } else {
                    delete property.requiredByParent;
                }
            });

            property = _.cloneDeep(property);

            return property;
        },
        getCurrentFormValue: function (currentProperty) {
            var formVal = form2js("propertyDetailsForm",".", false);

            if (!this.$el.find("#encryptionToggle:checked").length && currentProperty.encryption) {
                delete currentProperty.encryption;
                delete formVal.encryption;
            }

            if (!this.$el.find("#privateToggle:checked").length && currentProperty.scope) {
                delete currentProperty.scope;
                delete formVal.scope;
            }

            if (!this.$el.find("#hashedToggle:checked").length) {
                delete currentProperty.secureHash;
                delete formVal.secureHash;
            }

            if (this.model.propertyScripts) {
                _.extend(currentProperty, this.model.propertyScripts.getScripts());
            }

            if (this.data.arrayTypeView) {
                currentProperty.items = this.data.arrayTypeView.getValue();
            }

            if (this.data.relationshipTypeView) {
                if (currentProperty.items) {
                    currentProperty.items = _.extend(currentProperty.items, this.data.relationshipTypeView.getValue());
                } else {
                    currentProperty = _.extend(currentProperty, this.data.relationshipTypeView.getValue());
                }
            }

            return _.extend(currentProperty, formVal);
        },
        /**
        * This function builds an array of breadcrumb objects base on the props array
        */
        buildBreadcrumbArray: function(managedObject, props) {
            var link = "#managed/edit/" + managedObject,
                breadcrumbArray = [ {
                    html : handlebars.compile("{{> managed/schema/_propertyBreadcrumbLink}}")({
                        link: link + "/",
                        prop: managedObject
                    }),
                    text : managedObject,
                    route: {
                        name: "editManagedView",
                        args: [managedObject]
                    }
                }],
                propsClone = _.cloneDeep(props);

            link += "/property";

            _.each(propsClone, (prop, index) => {
                var html,
                    route = {
                        name: "editSchemaPropertyView",
                        args: [managedObject, propsClone]
                    },
                    isLast =  index === propsClone.length - 1;

                link += "/" + prop;

                html = handlebars.compile("{{> managed/schema/_propertyBreadcrumbLink}}")({
                    link: link,
                    prop: prop,
                    isLast: isLast
                });

                breadcrumbArray.push({
                    html : html,
                    text : prop,
                    route : route,
                    isLast : isLast
                });
            });

            return breadcrumbArray;
        },
        setTypeSpecificElements : function () {
            this.loadTypeSpecificElements = false;

            switch (this.data.property.type) {
                case "object":
                    this.loadTypeSpecificElements = this.loadPropertiesGrid;
                    this.data.headerIcon = "fa-cube";
                    break;
                case "array":
                    this.loadTypeSpecificElements = this.loadArrayTypeView;
                    this.data.headerIcon = "fa-list";
                    break;
                case "relationship":
                case "relationships":
                    this.loadTypeSpecificElements = this.loadRelationshipTypeView;
                    this.data.headerIcon = "fa-arrows-h";
                    break;
                case "string":
                    this.data.headerIcon = "fa-font";
                    break;
                case "boolean":
                    this.data.headerIcon = "fa-toggle-on";
                    break;
                case "number":
                    this.data.headerIcon = "fa-hashtag";
                    break;
            }
        },
        toggleAdvanced: function (e) {
            if (e) {
                e.preventDefault();
            }

            this.$el.find(".advanced-options-toggle").toggle();
            this.$el.find(".advancedShowHide").slideToggle(() => {
                this.data.showAdvanced = this.$el.find(".advancedShowHide").is(":visible");
            });
        },
        /**
        This function is called any time the form is updated. It updates the current config,
        checks with the changes pending module for any differences with the original form,
        toggles the save button on when there are changes, and off when the form is current.
        **/
        makeChanges: function () {
            this.data.property = this.getCurrentFormValue(this.data.property);

            this.model.changesModule.makeChanges(_.clone(this.data.property));

            if (this.model.changesModule.isChanged()) {
                this.$el.find(".btn-save").prop("disabled",false);
            } else {
                this.$el.find(".btn-save").prop("disabled",true);
            }
        },
        toggleHashSelect: function (e) {
            e.preventDefault();

            if ($(e.target).is(":checked")) {
                this.$el.find(".secureHash_selection").show();
                if(this.$el.find("#encryptionToggle").is(":checked")) {
                    this.$el.find("#encryptionToggle").click();
                }
            } else {
                this.$el.find(".secureHash_selection").hide();
            }
        },
        toggleEncryptionSelect: function (e) {
            e.preventDefault();

            if ($(e.target).is(":checked") && this.$el.find("#hashedToggle").is(":checked")) {
                this.$el.find("#hashedToggle").click();
            }
        },
        saveProperty: function (e,callback) {
            var self = this,
                requiredIndex = _.indexOf(this.data.parentProperty.required, this.data.propertyName),
                property = this.data.property,
                parentProperty = this.data.parentProperty,
                currentTab = this.data.currentTab,
                scriptsView = this.model.propertyScripts,
                saveCallback = callback,
                objectProperties,
                nestedItems;

            if (e) {
                e.preventDefault();
                saveCallback = () => {
                    self.$el.find('a[href="' + currentTab + '"]').tab('show');
                    if (callback) {
                        callback();
                    }
                };
            }

            if (parentProperty.type === "array") {
                nestedItems = SchemaUtils.handleArrayNest(parentProperty.items);
                requiredIndex = _.indexOf(nestedItems.required, this.data.propertyName);
            }

            if (property.nullable) {
                property.type = [property.type, "null"];
            }

            delete property.nullable;

            if (scriptsView) {
                //remove all previously existing scripts
                property = _.omit(property, this.model.eventList);
                //then add the lastest version of these scripts back to the property
                property = _.extend(property, scriptsView.getScripts());
            }

            if (property.type === "object") {
                property = _.extend(property, this.data.objectProperties.getValue());
            }

            if (this.data.arrayTypeView) {
                property.items = this.data.arrayTypeView.getValue();
            }

            if (this.data.relationshipTypeView) {
                if (property.type === "relationships") {
                    property.type = "array";
                    property.items = _.extend(property.items, this.data.relationshipTypeView.getValue());
                } else {
                    property = _.extend(property, this.data.relationshipTypeView.getValue());
                }
            }

            if (property.requiredByParent && requiredIndex === -1) {
                if (parentProperty.type === "array") {
                    nestedItems.required.push(this.data.propertyName);
                } else {
                    parentProperty.required.push(this.data.propertyName);
                }
            } else if (!property.requiredByParent && requiredIndex > -1) {
                if (parentProperty.type === "array") {
                    nestedItems.required.splice(requiredIndex,1);
                } else {
                    parentProperty.required.splice(requiredIndex,1);
                }
            }

            delete property.requiredByParent;

            if (parentProperty.type === "array") {
                nestedItems.properties[this.data.propertyName] = property;
            } else {
                parentProperty.properties[this.data.propertyName] = property;
            }


            this.saveManagedObject(this.data.currentManagedObject, this.data.managedConfig, false, saveCallback);
        },
        deleteProperty : function (e) {
            var breadcrumbs = this.data.breadcrumbs,
                previousObjectBreadcrumb = breadcrumbs[breadcrumbs.length - 2],
                route = previousObjectBreadcrumb.route,
                propertyArgs = route.args[1],
                parentProperty = this.data.parentProperty,
                propertyName = this.data.propertyName,
                nestedItems;

            e.preventDefault();

            UIUtils.confirmDialog($.t("templates.managed.schemaEditor.confirmPropertyDelete", { propName: this.data.propertyName }), "danger", () => {
                if (_.isArray(propertyArgs)) {
                    propertyArgs.pop();
                    propertyArgs = propertyArgs.join("/");
                }

                if (parentProperty.type === "array") {
                    nestedItems = SchemaUtils.handleArrayNest(parentProperty.items);
                    nestedItems.order = _.without(nestedItems.order, propertyName);
                    nestedItems.required = _.without(nestedItems.required, propertyName);
                    delete nestedItems.properties[propertyName];
                } else {
                    parentProperty.order = _.without(parentProperty.order, propertyName);
                    parentProperty.required = _.without(parentProperty.required, propertyName);
                    delete parentProperty.properties[propertyName];
                }

                this.saveManagedObject(this.data.currentManagedObject, this.data.managedConfig, false, () => {
                    //take us back to the deleted property's parent object edit page
                    EventManager.sendEvent(Constants.ROUTE_REQUEST, {routeName: route.name, args:[route.args[0], propertyArgs]});
                });
            });
        },
        loadPropertiesGrid: function () {
            var wasJustSaved = false;

            if (
                _.isObject(this.data.objectProperties) &&
                this.data.propertyName === this.args[this.args.length - 1]
            ) {
                wasJustSaved = true;
            }


            this.data.objectProperties = new ObjectTypeView();

            this.data.objectProperties.render({
                elementId: "object-properties-list",
                schema: this.data.property,
                saveSchema: () => {
                    this.saveProperty();
                },
                parentObjectName: this.data.propertyName,
                propertyRoute: this.args.join("/"),
                wasJustSaved: wasJustSaved
            });
        },
        loadArrayTypeView: function () {
            this.data.arrayTypeView = new ArrayTypeView();

            this.data.arrayTypeView.render({
                elementId: "arrayTypeContainer",
                propertyName: this.data.propertyName,
                propertyRoute: this.args.join("/"),
                items: this.data.property.items,
                makeChanges: _.bind(this.makeChanges,this),
                nestingIndex: 0
            });
        },
        loadRelationshipTypeView: function () {
            var propertySchema = _.cloneDeep(this.data.property);

            this.data.relationshipTypeView = new RelationshipTypeView();

            if (this.data.property.type === "relationships") {
                propertySchema = propertySchema.items;
            }

            this.data.relationshipTypeView.render({
                elementId: "relationshipTypeContainer",
                propertySchema: propertySchema,
                makeChanges: _.bind(this.makeChanges,this)
            });
        },
        /**
        * This function sets an event for each bootstrap tab on "show" which looks for any
        * pending form changes in the currently visible tab. If there are changes the the tab
        * change is halted and a dialog is displayed asking the user if he/she would like to discard
        * or save the changes before actually changing tabs.
        *
        * @param {string} tabId - (optional) specific tab on which to set the change event...otherwise the event will be set on all tabs
        **/
        setTabChangeEvent: function (tabId) {
            var scope = this.$el;

            if (tabId) {
                scope = scope.find("#" + tabId);
            }

            //look for all bootstrap tabs within "scope"
            scope.on('show.bs.tab', 'a[data-toggle="tab"]', (e) => {
                this.data.currentTab = e.target.hash;

                //check to see if there are changes pending
                if (this.$el.find(".changes-pending-container:visible").length) {
                    //stop processing this tab change
                    e.preventDefault();
                    //throw up a confirmation dialog
                    SchemaUtils.confirmSaveChanges(this, e.target.hash, () => {
                        //once confirmed save the form then continue showing the new tab
                        this.saveProperty(false, () => {
                            this.$el.find('a[href="' + e.target.hash + '"]').tab('show');
                        });
                    });
                }
            });

        }
    });

    return new SchemaPropertyView();
});

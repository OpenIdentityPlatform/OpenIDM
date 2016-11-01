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
 * Copyright 2015-2016 ForgeRock AS.
 */

define([
    "jquery",
    "underscore",
    "org/forgerock/openidm/ui/admin/util/AdminAbstractView",
    "org/forgerock/openidm/ui/common/delegates/ConfigDelegate",
    "org/forgerock/commons/ui/common/main/EventManager",
    "org/forgerock/commons/ui/common/util/Constants",
    "org/forgerock/openidm/ui/admin/delegates/RepoDelegate",
    "org/forgerock/commons/ui/common/main/Router",
    "org/forgerock/openidm/ui/admin/managed/schema/SchemaEditorView"
], function($, _,
            AdminAbstractView,
            ConfigDelegate,
            EventManager,
            Constants,
            RepoDelegate,
            Router,
            SchemaEditorView) {

    var AbstractManagedView = AdminAbstractView.extend({
        data: {},

        saveManagedObject: function(managedObject, saveObject, isNewManagedObject) {
            var promises = [];

            if (!isNewManagedObject) {
                managedObject.schema = SchemaEditorView.getManagedSchema();
            }

            managedObject.schema.icon = this.$el.find("#managedObjectIcon").val();

            managedObject = this.handlePreferences(managedObject);

            this.combineSchemaAndProperties();

            promises.push(ConfigDelegate.updateEntity("managed", {"objects" : saveObject.objects}));

            switch (RepoDelegate.getRepoTypeFromConfig(this.data.repoConfig)) {
                case "orientdb":
                    this.data.repoConfig = RepoDelegate.addManagedObjectToOrientClasses(this.data.repoConfig, managedObject.name);
                    promises.push(RepoDelegate.updateEntity(this.data.repoConfig._id, this.data.repoConfig));
                    break;
                case "jdbc":
                    let resourceMapping = RepoDelegate.findGenericResourceMappingForRoute(this.data.repoConfig, "managed/"+managedObject.name);
                    if (resourceMapping && resourceMapping.searchableDefault !== true) {
                        let searchablePropertiesList = _(managedObject.schema.properties)
                                                    .pairs()
                                                    .map((prop) => {
                                                        if (prop[1].searchable) {
                                                            return prop[0];
                                                        }
                                                    })
                                                    .filter()
                                                    .value();
                        // modifies this.data.repoConfig via object reference in resourceMapping
                        RepoDelegate.syncSearchablePropertiesForGenericResource(resourceMapping, searchablePropertiesList);

                        promises.push(RepoDelegate.updateEntity(this.data.repoConfig._id, this.data.repoConfig));
                    }
                    break;
            }

            $.when.apply($, promises).then(_.bind(function() {
                EventManager.sendEvent(Constants.EVENT_DISPLAY_MESSAGE_REQUEST, "managedObjectSaveSuccess");
                EventManager.sendEvent(Constants.EVENT_UPDATE_NAVIGATION);

                if (isNewManagedObject) {
                    EventManager.sendEvent(Constants.EVENT_CHANGE_VIEW, {route: Router.configuration.routes.editManagedView, args: [managedObject.name]});
                } else {
                    this.render(this.args);
                }
            }, this));
        },

        checkManagedName: function(name, managedList) {
            var found = false;

            _.each(managedList, function(managedObject){
                if(managedObject.name === name){
                    found = true;
                }
            }, this);

            return found;
        },

        splitSchemaAndProperties: function () {
            var propertiesFields = ["encryption","scope", "onRetrieve", "onValidate", "onStore", "isVirtual", "secureHash"],
                properties = [],
                schemaProperties = {};

            if (this.data.currentManagedObject && this.data.currentManagedObject.schema) {
                _.each(this.data.currentManagedObject.schema.properties, function (val, key) {
                    var property = _.pick(val,propertiesFields);

                    if (!_.isEmpty(property)) {
                        property.name = key;

                        if (property.isVirtual) {
                            property.type = "virtual";
                            delete property.isVirtual;
                        }

                        properties.push(property);
                    }
                });

                this.data.currentManagedObject.properties = properties;

                _.each(this.data.currentManagedObject.schema.properties, function (val, key) {
                    val = _.omit(val,propertiesFields);
                    schemaProperties[key] = val;
                });

                this.data.currentManagedObject.schema.properties = schemaProperties;
            }
        },

        combineSchemaAndProperties: function () {
            if (this.data.currentManagedObject) {
                _.each(this.data.currentManagedObject.properties, _.bind(function (property) {
                    if (property.type === "virtual") {
                        this.data.currentManagedObject.schema.properties[property.name].isVirtual = true;
                    }
                    _.extend(this.data.currentManagedObject.schema.properties[property.name], _.omit(property,"type","name","addedEvents","selectEvents"));
                }, this));

                delete this.data.currentManagedObject.properties;
            }
        },
        handlePreferences: function (managedObject) {
            var preferencesTabValue,
                preferencesIndex,
                fullPreferencesSchema;
            /**
             * check to make sure preferences are defined and not an empty object before adding
             * the property to the schema
             */
            if (this.getManagedPreferences && !_.isEmpty(this.getManagedPreferences())) {
                preferencesTabValue = this.getManagedPreferences();
                fullPreferencesSchema = {
                    "description" : "",
                    "title" : $.t("templates.preferences.preferences"),
                    "viewable" : true,
                    "searchable" : false,
                    "userEditable" : true,
                    "policies" : [ ],
                    "returnByDefault" : false,
                    "minLength" : null,
                    "pattern" : "",
                    "type" : "object",
                    "properties" : preferencesTabValue,
                    "required" : [ ],
                    "order" : _.keys(preferencesTabValue)
                };

                if (!managedObject.schema.properties.preferences) {
                    managedObject.schema.properties.preferences = fullPreferencesSchema;
                } else {
                    _.set(managedObject.schema, "properties.preferences.properties", preferencesTabValue);
                    _.set(managedObject.schema, "properties.preferences.order", _.keys(preferencesTabValue));
                }

                if (_.indexOf(managedObject.schema.order, "preferences") === -1) {
                    if(_.isUndefined(managedObject.schema.order)) {
                        managedObject.schema.order = [];
                    }

                    managedObject.schema.order.push("preferences");
                }
            }
            //if no preferences remove it from schema
            if (this.getManagedPreferences && _.isEmpty(this.getManagedPreferences())) {
                delete managedObject.schema.properties.preferences;
                preferencesIndex = managedObject.schema.order.indexOf('preferences');
                if (preferencesIndex !== -1) {
                    managedObject.schema.order.splice(preferencesIndex, 1);
                }
            }

            return managedObject;
        }
    });

    return AbstractManagedView;
});

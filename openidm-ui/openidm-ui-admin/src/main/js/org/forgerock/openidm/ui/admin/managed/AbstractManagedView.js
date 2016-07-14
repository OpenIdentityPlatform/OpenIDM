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
    "org/forgerock/commons/ui/common/main/Router",
    "org/forgerock/openidm/ui/admin/managed/schema/SchemaEditorView"
], function($, _,
            AdminAbstractView,
            ConfigDelegate,
            EventManager,
            Constants,
            Router,
            SchemaEditorView) {

    var AbstractManagedView = AdminAbstractView.extend({
        data: {},

        orientRepoChange: function(managedObject) {
            var orientClasses = this.data.repoObject.dbStructure.orientdbClass;

            if(_.isUndefined(orientClasses["managed_" +managedObject.name])) {
                orientClasses["managed_" + managedObject.name] = {
                    "index" : [
                        {
                            "propertyName" : "_openidm_id",
                            "propertyType" : "string",
                            "indexType" : "unique"
                        }
                    ]
                };
            }
        },

        checkRepo: function(configFiles, callback) {
            this.data.currentRepo = _.find(configFiles.configurations, function(file){
                return file.pid.search("repo.") !== -1;
            }, this).pid;

            if(this.data.currentRepo === "repo.orientdb") {
                ConfigDelegate.readEntity(this.data.currentRepo).then(_.bind(function (repo) {
                    this.data.repoObject = repo;
                    callback();
                }, this));
            } else {
                callback(callback);
            }
        },

        saveManagedObject: function(managedObject, saveObject, isNewManagedObject) {
            var promises = [];

            if (!isNewManagedObject) {
                managedObject.schema = SchemaEditorView.getManagedSchema();
            }

            managedObject.schema.icon = this.$el.find("#managedObjectIcon").val();

            this.combineSchemaAndProperties();

            promises.push(ConfigDelegate.updateEntity("managed", {"objects" : saveObject.objects}));

            if(this.data.currentRepo === "repo.orientdb") {
                this.orientRepoChange(managedObject);
                promises.push(ConfigDelegate.updateEntity(this.data.currentRepo, this.data.repoObject));
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

            if (this.data.currentManagedObject) {
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
        }
    });

    return AbstractManagedView;
});

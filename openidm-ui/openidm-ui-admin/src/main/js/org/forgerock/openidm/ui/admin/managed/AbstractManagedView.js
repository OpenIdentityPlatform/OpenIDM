/**
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2015 ForgeRock AS. All rights reserved.
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

/*global define */

define("org/forgerock/openidm/ui/admin/managed/AbstractManagedView", [
    "jquery",
    "underscore",
    "org/forgerock/openidm/ui/admin/util/AdminAbstractView",
    "org/forgerock/openidm/ui/common/delegates/ConfigDelegate",
    "org/forgerock/commons/ui/common/main/EventManager",
    "org/forgerock/commons/ui/common/util/Constants",
    "org/forgerock/commons/ui/common/main/Router"
], function($, _,
            AdminAbstractView,
            ConfigDelegate,
            EventManager,
            Constants,
            Router) {

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

        saveManagedObject: function(managedObject, saveObject, routeTo) {
            var promises = [];

            promises.push(ConfigDelegate.updateEntity("managed", {"objects" : saveObject.objects}));

            if(this.data.currentRepo === "repo.orientdb") {
                this.orientRepoChange(managedObject);
                promises.push(ConfigDelegate.updateEntity(this.data.currentRepo, this.data.repoObject));
            }

            $.when.apply($, promises).then(function() {
                EventManager.sendEvent(Constants.EVENT_DISPLAY_MESSAGE_REQUEST, "managedObjectSaveSuccess");
                EventManager.sendEvent(Constants.EVENT_UPDATE_NAVIGATION);

                routeTo();
            });
        },

        checkManagedName: function(name, managedList) {
            var found = false;

            _.each(managedList, function(managedObject){
                if(managedObject.name === name){
                    found = true;
                }
            }, this);

            return found;
        }
    });

    return AbstractManagedView;
});

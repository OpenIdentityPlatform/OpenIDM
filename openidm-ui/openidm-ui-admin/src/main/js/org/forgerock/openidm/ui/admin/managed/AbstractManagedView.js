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
 * Copyright 2015-2017 ForgeRock AS.
 */

define([
    "jquery",
    "underscore",
    "org/forgerock/openidm/ui/admin/util/AdminAbstractView",
    "org/forgerock/openidm/ui/common/delegates/ConfigDelegate",
    "org/forgerock/commons/ui/common/main/EventManager",
    "org/forgerock/commons/ui/common/util/Constants",
    "org/forgerock/openidm/ui/admin/delegates/RepoDelegate",
    "org/forgerock/commons/ui/common/main/Router"
], function($, _,
            AdminAbstractView,
            ConfigDelegate,
            EventManager,
            Constants,
            RepoDelegate,
            Router) {

    var AbstractManagedView = AdminAbstractView.extend({
        data: {},

        saveManagedObject: function(managedObject, saveObject, isNewManagedObject, callback) {
            var promises = [];

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
                    this.render(this.args,callback);
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
        }
    });

    return AbstractManagedView;
});

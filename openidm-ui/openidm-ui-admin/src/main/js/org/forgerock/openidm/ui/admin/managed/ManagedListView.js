/**
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2014 ForgeRock AS. All rights reserved.
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

define("org/forgerock/openidm/ui/admin/managed/ManagedListView", [
    "jquery",
    "underscore",
    "org/forgerock/openidm/ui/admin/util/AdminAbstractView",
    "org/forgerock/commons/ui/common/main/EventManager",
    "org/forgerock/commons/ui/common/util/Constants",
    "org/forgerock/commons/ui/common/main/Router",
    "org/forgerock/openidm/ui/admin/delegates/ConnectorDelegate",
    "org/forgerock/commons/ui/common/util/UIUtils",
    "org/forgerock/openidm/ui/admin/util/ConnectorUtils",
    "org/forgerock/openidm/ui/common/delegates/ConfigDelegate"
], function($, _, AdminAbstractView, eventManager, constants, router, ConnectorDelegate, uiUtils, connectorUtils, ConfigDelegate) {
    var ManagedListView = AdminAbstractView.extend({
        template: "templates/admin/managed/ManagedListViewTemplate.html",
        events: {
            "click .managed-delete": "deleteManaged"
        },
        render: function(args, callback) {
            var managedPromise,
                repoCheckPromise,
                iconPromise,
                tempIconClass;

            this.data.docHelpUrl = constants.DOC_URL;

            managedPromise = ConfigDelegate.readEntity("managed");
            repoCheckPromise = ConfigDelegate.getConfigList();
            iconPromise = connectorUtils.getIconList();

            $.when(managedPromise, repoCheckPromise, iconPromise).then(_.bind(function(managedObjects, configFiles, iconList){
                this.data.currentManagedObjects = _.sortBy(managedObjects.objects, 'name');

                _.each(this.data.currentManagedObjects, _.bind(function(managedObject){
                    tempIconClass = connectorUtils.getIcon("managedobject", iconList);

                    managedObject.iconClass = tempIconClass.iconClass;
                    managedObject.iconSrc = tempIconClass.src;
                }, this));


                this.data.currentRepo = _.find(configFiles[0].configurations, function(file){
                    return file.pid.search("repo.") !== -1;
                }, this).pid;

                if(this.data.currentRepo === "repo.orientdb") {
                    ConfigDelegate.readEntity(this.data.currentRepo).then(_.bind(function (repo) {
                        this.data.repoObject = repo;
                        this.resourceRender(callback);
                    }, this));
                } else {
                    this.resourceRender(callback);
                }
            }, this));
        },

        resourceRender: function(callback) {
            this.parentRender(_.bind(function(){
                if (callback) {
                    callback();
                }
            }, this));
        },

        deleteManaged: function(event) {
            var selectedItems = $(event.currentTarget).parents(".card"),
                promises = [],
                tempManaged = _.clone(this.data.currentManagedObjects);

            uiUtils.jqConfirm($.t("templates.managed.managedDelete"), _.bind(function(){
                _.each(tempManaged, function(managedObject, index){
                    if(managedObject.name === selectedItems.attr("data-managed-title")) {
                        this.data.currentManagedObjects.splice(index, 1);
                    }
                }, this);

                if(this.data.currentRepo === "repo.orientdb") {
                    if(this.data.repoObject.dbStructure.orientdbClass["managed_"+selectedItems.attr("data-managed-title")] !== undefined){
                        delete this.data.repoObject.dbStructure.orientdbClass["managed_"+selectedItems.attr("data-managed-title")];
                    }

                    promises.push(ConfigDelegate.updateEntity(this.data.currentRepo, this.data.repoObject));
                }

                promises.push(ConfigDelegate.updateEntity("managed", {"objects" : this.data.currentManagedObjects}));

                $.when.apply($, promises).then(function(){
                        selectedItems.parent().parent().remove();

                        eventManager.sendEvent(constants.EVENT_UPDATE_NAVIGATION);
                        eventManager.sendEvent(constants.EVENT_DISPLAY_MESSAGE_REQUEST, "deleteManagedSuccess");
                    },
                    function(){
                        eventManager.sendEvent(constants.EVENT_DISPLAY_MESSAGE_REQUEST, "deleteManagedFail");
                    });
            },this));
        }
    });

    return new ManagedListView();
});

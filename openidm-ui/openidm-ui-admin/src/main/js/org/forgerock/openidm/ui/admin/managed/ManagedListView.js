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
    "backbone",
    "org/forgerock/openidm/ui/admin/util/AdminAbstractView",
    "org/forgerock/commons/ui/common/main/EventManager",
    "org/forgerock/commons/ui/common/util/Constants",
    "org/forgerock/commons/ui/common/main/Router",
    "org/forgerock/openidm/ui/admin/delegates/ConnectorDelegate",
    "org/forgerock/openidm/ui/admin/util/ConnectorUtils",
    "org/forgerock/openidm/ui/common/delegates/ConfigDelegate",
    "backgrid",
    "org/forgerock/openidm/ui/admin/util/BackgridUtils",
    "org/forgerock/commons/ui/common/util/UIUtils"
], function($, _, Backbone,
            AdminAbstractView,
            eventManager,
            constants,
            router,
            ConnectorDelegate,
            connectorUtils,
            ConfigDelegate,
            Backgrid,
            BackgridUtils,
            UIUtils) {
    var ManagedListView = AdminAbstractView.extend({
        template: "templates/admin/managed/ManagedListViewTemplate.html",
        events: {
            "click .managed-delete": "deleteManaged",
            "click .toggle-view-btn": "toggleButtonChange",
            "keyup .filter-input" : "filterManagedObjects",
            "paste .filter-input" : "filterManagedObjects"
        },
        model: {

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
            var ManagedModel = Backbone.Model.extend({}),
                ManagedObjects = Backbone.Collection.extend({ model: ManagedModel }),
                managedObjectGrid,
                RenderRow,
                _this = this;

            this.model.managedObjectCollection = new ManagedObjects();

            _.each(this.data.currentManagedObjects, function(managedObject) {
                managedObject.type = $.t("templates.connector.managedObjectType");
                this.model.managedObjectCollection.add(managedObject);
            }, this);

            RenderRow = Backgrid.Row.extend({
                render: function () {
                    RenderRow.__super__.render.apply(this, arguments);

                    this.$el.attr('data-managed-title', this.model.attributes.name);

                    return this;
                }
            });

            this.parentRender(_.bind(function(){
                managedObjectGrid = new Backgrid.Grid({
                    className: "table backgrid-table",
                    row: RenderRow,
                    columns: BackgridUtils.addSmallScreenCell([
                        {
                            name: "source",
                            sortable: false,
                            editable: false,
                            cell: Backgrid.Cell.extend({
                                render: function () {
                                    var icon = this.model.attributes.iconClass,
                                        display;

                                    if(this.model.attributes.schema.icon)  {
                                        icon = "fa " +this.model.attributes.schema.icon;
                                    }

                                    display = '<a class="table-clink" href="#connectors/edit/'+this.model.attributes.cleanUrlName +'/"><div class="image circle">'
                                        + '<i class="' +icon +'"></i></div>' +this.model.attributes.name +'</a>';

                                    this.$el.html(display);

                                    return this;
                                }
                            })
                        },
                        {
                            name: "type",
                            label: "type",
                            cell: "string",
                            sortable: false,
                            editable: false
                        },
                        {
                            name: "",
                            sortable: false,
                            editable: false,
                            cell: Backgrid.Cell.extend({
                                className: "button-right-align",
                                render: function () {
                                    var display = $('<div class="btn-group"><button type="button" class="btn btn-link fa-lg dropdown-toggle" data-toggle="dropdown" aria-expanded="false">'
                                        + '<i class="fa fa-ellipsis-v"></i>'
                                        + '</button></div>');

                                    $(display).append(_this.$el.find("[data-managed-title='" + this.model.attributes.name + "'] .dropdown-menu").clone());

                                    this.$el.html(display);

                                    return this;
                                }
                            })
                        }
                    ]),
                    collection: this.model.managedObjectCollection
                });

                this.$el.find("#managedGrid").append(managedObjectGrid.render().el);

                if (callback) {
                    callback();
                }
            }, this));
        },

        toggleButtonChange: function(event) {
            var target = $(event.target);

            if(target.hasClass("fa")) {
                target = target.parents(".btn");
            }

            this.$el.find(".toggle-view-btn").toggleClass("active", false);
            target.toggleClass("active", true);
        },

        deleteManaged: function(event) {
            var selectedItem = $(event.currentTarget).parents(".card-spacer"),
                promises = [],
                alternateItem,
                tempManaged = _.clone(this.data.currentManagedObjects);

            if(selectedItem.length > 0) {
                _.each(this.$el.find(".backgrid-table tbody tr"), function(row) {
                    if($(row).attr("data-managed-title") === selectedItem.attr("data-managed-title")) {
                        alternateItem = $(row);
                    }
                });
            } else {
                selectedItem = $(event.currentTarget).parents("tr");

                _.each(this.$el.find(".card-spacer"), function(card) {
                    if($(card).attr("data-managed-title") === selectedItem.attr("data-managed-title")) {
                        alternateItem = $(card);
                    }
                });
            }

            UIUtils.confirmDialog($.t("templates.managed.managedDelete"), "danger", _.bind(function(){
                _.each(tempManaged, function(managedObject, index){
                    if(managedObject.name === selectedItem.attr("data-managed-title")) {
                        this.data.currentManagedObjects.splice(index, 1);
                    }
                }, this);

                if(this.data.currentRepo === "repo.orientdb") {
                    if(this.data.repoObject.dbStructure.orientdbClass["managed_"+selectedItem.attr("data-managed-title")] !== undefined){
                        delete this.data.repoObject.dbStructure.orientdbClass["managed_"+selectedItem.attr("data-managed-title")];
                    }

                    promises.push(ConfigDelegate.updateEntity(this.data.currentRepo, this.data.repoObject));
                }

                promises.push(ConfigDelegate.updateEntity("managed", {"objects" : this.data.currentManagedObjects}));

                $.when.apply($, promises).then(function(){
                        selectedItem.remove();
                        alternateItem.remove();

                        eventManager.sendEvent(constants.EVENT_UPDATE_NAVIGATION);
                        eventManager.sendEvent(constants.EVENT_DISPLAY_MESSAGE_REQUEST, "deleteManagedSuccess");
                    },
                    function(){
                        eventManager.sendEvent(constants.EVENT_DISPLAY_MESSAGE_REQUEST, "deleteManagedFail");
                    });
            },this));
        },

        filterManagedObjects: function(event) {
            var search = $(event.target).val().toLowerCase();

            if(search.length > 0) {
                _.each(this.$el.find(".card-spacer"), function(card) {
                    if($(card).attr("data-managed-title").toLowerCase().indexOf(search) > -1) {
                        $(card).fadeIn();
                    } else {
                        $(card).fadeOut();
                    }
                }, this);

                _.each(this.$el.find(".backgrid-table tbody tr"), function(row) {
                    if($(row).attr("data-managed-title").toLowerCase().indexOf(search) > -1) {
                        $(row).fadeIn();
                    } else {
                        $(row).fadeOut();
                    }
                }, this);
            } else {
                this.$el.find(".card-spacer").fadeIn();
                this.$el.find(".backgrid-table tbody tr").fadeIn();
            }
        }
    });

    return new ManagedListView();
});
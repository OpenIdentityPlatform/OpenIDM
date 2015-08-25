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

define("org/forgerock/openidm/ui/admin/connector/ConnectorListView", [
    "jquery",
    "underscore",
    "backbone",
    "org/forgerock/openidm/ui/admin/util/AdminAbstractView",
    "org/forgerock/commons/ui/common/main/EventManager",
    "org/forgerock/commons/ui/common/util/Constants",
    "org/forgerock/commons/ui/common/main/Router",
    "org/forgerock/openidm/ui/admin/delegates/ConnectorDelegate",
    "org/forgerock/commons/ui/common/util/UIUtils",
    "org/forgerock/openidm/ui/admin/util/ConnectorUtils",
    "org/forgerock/openidm/ui/common/delegates/ConfigDelegate",
    "backgrid",
    "org/forgerock/openidm/ui/admin/util/BackgridUtils"
], function($, _, Backbone,
            AdminAbstractView,
            eventManager,
            constants,
            router,
            ConnectorDelegate,
            uiUtils,
            connectorUtils,
            ConfigDelegate,
            Backgrid,
            BackgridUtils) {
    var ConnectorListView = AdminAbstractView.extend({
        template: "templates/admin/connector/ConnectorListViewTemplate.html",
        events: {
            "click .connector-delete": "deleteConnections",
            "click .toggle-view-btn": "toggleButtonChange",
            "keyup .filter-input" : "filterConnectors",
            "paste .filter-input" : "filterConnectors"
        },
        model: {

        },
        render: function(args, callback) {
            var connectorPromise,
                iconPromise,
                splitConfig,
                tempIconClass,
                ConnectorModel = Backbone.Model.extend({}),
                Connectors = Backbone.Collection.extend({ model: ConnectorModel }),
                connectorGrid,
                RenderRow = null,
                _this = this;

            RenderRow = Backgrid.Row.extend({
                render: function () {
                    RenderRow.__super__.render.apply(this, arguments);

                    this.$el.attr('data-name', this.model.attributes.name);
                    this.$el.attr('data-type', this.model.attributes.displayName);
                    this.$el.attr('data-connector-title', this.model.attributes.cleanUrlName);

                    return this;
                }
            });

            this.data.docHelpUrl = constants.DOC_URL;
            this.model.connectorCollection = new Connectors();

            connectorPromise = ConnectorDelegate.currentConnectors();
            iconPromise = connectorUtils.getIconList();

            $.when(connectorPromise, iconPromise).then(_.bind(function(connectors, iconList){
                _.each(connectors, _.bind(function(connector){
                    tempIconClass = connectorUtils.getIcon(connector.connectorRef.connectorName, iconList);
                    connector.iconClass = tempIconClass.iconClass;
                    connector.iconSrc = tempIconClass.src;

                    splitConfig = connector.config.split("/");

                    connector.cleanUrlName = splitConfig[1] + "_" +splitConfig[2];
                    connector.cleanEditName = splitConfig[2];

                    //remove the __ALL__ objectType
                    connector.objectTypes = _.reject(connector.objectTypes, function(ot) { return ot === "__ALL__"; });

                    this.model.connectorCollection.add(connector);
                }, this));

                this.data.currentConnectors = connectors;

                this.parentRender(_.bind(function(){
                    connectorGrid = new Backgrid.Grid({
                        className: "table backgrid-table",
                        row: RenderRow,
                        columns: BackgridUtils.addSmallScreenCell([
                            {
                                name: "source",
                                sortable: false,
                                editable: false,
                                cell: Backgrid.Cell.extend({
                                    render: function () {
                                        var display = '<a class="table-clink" href="#connectors/edit/'+this.model.attributes.cleanUrlName +'/"><div class="image circle">'
                                            + '<i class="' +this.model.attributes.iconClass +'"></i></div>' +this.model.attributes.name +'</a>';

                                        this.$el.html(display);

                                        return this;
                                    }
                                })
                            },
                            {
                                name: "displayName",
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

                                        $(display).append(_this.$el.find("[data-connector-title='" + this.model.attributes.cleanUrlName + "'] .dropdown-menu").clone());

                                        this.$el.html(display);

                                        return this;
                                    }
                                })
                            }
                        ]),
                        collection: this.model.connectorCollection
                    });

                    this.$el.find("#connectorGrid").append(connectorGrid.render().el);

                    if(this.$el.find(".resource-unavailable").length !== 0) {
                        this.$el.find(".resource-unavailable").tooltip({
                            tooltipClass: "resource-error-tooltip"
                        });
                    }

                    if(this.$el.find(".resource-disabled").length !== 0) {
                        this.$el.find(".resource-disabled").tooltip({
                            tooltipClass: "resource-warning-tooltip"
                        });
                    }

                    if (callback) {
                        callback();
                    }

                }, this));
            }, this));
        },

        deleteConnections: function(event) {
            var selectedItem = $(event.currentTarget).parents(".card-spacer"),
                alternateItem,
                url,
                tempConnector = _.clone(this.data.currentConnectors);

            if(selectedItem.length > 0) {
                _.each(this.$el.find(".backgrid-table tbody tr"), function(row) {
                    if($(row).attr("data-connector-title") === selectedItem.attr("data-connector-title")) {
                        alternateItem = $(row);
                    }
                });
            } else {
                selectedItem = $(event.currentTarget).parents("tr");

                _.each(this.$el.find(".card-spacer"), function(card) {
                    if($(card).attr("data-connector-title") === selectedItem.attr("data-connector-title")) {
                        alternateItem = $(card);
                    }
                });
            }

            uiUtils.jqConfirm($.t("templates.connector.connectorDelete"), _.bind(function(){
                _.each(tempConnector, function(connectorObject, index){
                    if(connectorObject.cleanUrlName === selectedItem.attr("data-connector-title")) {
                        this.data.currentConnectors.splice(index, 1);
                    }
                }, this);

                url = selectedItem.attr("data-connector-title").split("_");

                ConfigDelegate.deleteEntity(url[0] +"/" +url[1]).then(function(){
                        ConnectorDelegate.deleteCurrentConnectorsCache();
                        selectedItem.remove();
                        alternateItem.remove();
                        eventManager.sendEvent(constants.EVENT_DISPLAY_MESSAGE_REQUEST, "deleteConnectorSuccess");
                    },
                    function(){
                        eventManager.sendEvent(constants.EVENT_DISPLAY_MESSAGE_REQUEST, "deleteConnectorFail");
                    });
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

        filterConnectors: function(event) {
            var search = $(event.target).val().toLowerCase();

            if(search.length > 0) {
                _.each(this.$el.find(".card-spacer"), function(card) {
                    console.log($(card).attr("data-type"));
                    console.log($(card).attr("data-name"));
                    console.log(search);
                    if($(card).attr("data-type").toLowerCase().indexOf(search) > -1 || $(card).attr("data-name").toLowerCase().indexOf(search) > -1) {
                        $(card).fadeIn();
                    } else {
                        $(card).fadeOut();
                    }
                }, this);

                _.each(this.$el.find(".backgrid-table tbody tr"), function(row) {
                    if($(row).attr("data-type").toLowerCase().indexOf(search) > -1 || $(row).attr("data-name").toLowerCase().indexOf(search) > -1) {
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

    return new ConnectorListView();
});
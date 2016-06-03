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
 * Copyright 2014-2015 ForgeRock AS.
 */

/*global define */

define("org/forgerock/openidm/ui/admin/mapping/MappingListView", [
    "jquery",
    "underscore",
    "handlebars",
    "backbone",
    "org/forgerock/openidm/ui/admin/util/AdminAbstractView",
    "org/forgerock/commons/ui/common/main/EventManager",
    "org/forgerock/openidm/ui/common/delegates/ConfigDelegate",
    "org/forgerock/commons/ui/common/util/Constants",
    "org/forgerock/openidm/ui/admin/delegates/ReconDelegate",
    "org/forgerock/commons/ui/common/util/DateUtil",
    "org/forgerock/openidm/ui/admin/delegates/SyncDelegate",
    "org/forgerock/openidm/ui/admin/util/ConnectorUtils",
    "org/forgerock/commons/ui/common/util/UIUtils",
    "backgrid",
    "org/forgerock/openidm/ui/admin/util/BackgridUtils",
    "org/forgerock/commons/ui/common/util/AutoScroll",
    "dragula"
], function($, _, handlebars,
            Backbone,
            AdminAbstractView,
            eventManager,
            configDelegate,
            constants,
            reconDelegate,
            dateUtil,
            syncDelegate,
            connectorUtils,
            UIUtils,
            Backgrid,
            BackgridUtils,
            AutoScroll,
            dragula) {

    var MappingListView = AdminAbstractView.extend({
        template: "templates/admin/mapping/MappingListTemplate.html",
        events: {
            "click #addMapping": "addMapping",
            "click .delete-button" : "deleteMapping",
            "click .mapping-config-body": "mappingDetail",
            "click .toggle-view-btn": "toggleButtonChange",
            "keyup .filter-input" : "filterMappings",
            "paste .filter-input" : "filterMappings"
        },
        model: {},
        partials: [
            "partials/mapping/list/_sourceTargetGridCellDisplay.html",
            "partials/mapping/list/_syncStatusCellDisplay.html",
            "partials/mapping/list/_actionCellDisplay.html",
            "partials/mapping/list/_linkName.html"
        ],
        mappingDetail: function(e){
            if(!$(e.target).closest("button").hasClass("delete-button")){
                e.preventDefault();

                eventManager.sendEvent(constants.ROUTE_REQUEST, {routeName: "propertiesView", args: [$(e.target).closest(".mapping-config-body").attr("mapping")]});
            }
        },

        toggleButtonChange: function(event) {
            var target = $(event.target);

            if(target.hasClass("fa")) {
                target = target.parents(".btn");
            }

            this.$el.find(".toggle-view-btn").toggleClass("active", false);
            target.toggleClass("active", true);
        },
        render: function(args, callback) {
            var syncConfig = syncDelegate.mappingDetails(),
                mappingDetails = [],
                cleanName;

            syncConfig.then(_.bind(function(sync) {
                this.data.mappingConfig = sync.mappings;
                this.data.docHelpUrl = constants.DOC_URL;

                this.cleanConfig = _.chain(sync.mappings)
                    .map(function (m) {
                        return _.clone(_.omit(m, "recon"));
                    })
                    .value();

                _.each(this.data.mappingConfig, function (sync) {
                    sync.targetType = this.syncType(sync.target);
                    sync.sourceType = this.syncType(sync.source);

                    mappingDetails.push(connectorUtils.getMappingDetails(sync.sourceType, sync.targetType));
                }, this);

                $.when.apply($, mappingDetails).then(_.bind(function () {
                    var results = arguments,
                        Mappings = Backbone.Collection,
                        mappingGrid,
                        RenderRow,
                        _this = this;
    
                    this.model.mappingCollection = new Mappings();

                    _.each(results, function (mappingInfo, index) {
                        this.data.mappingConfig[index].targetIcon = mappingInfo.targetIcon.iconClass;
                        this.data.mappingConfig[index].sourceIcon = mappingInfo.sourceIcon.iconClass;

                        this.data.mappingConfig[index].targetConnector = mappingInfo.targetConnector;
                        this.data.mappingConfig[index].sourceConnector = mappingInfo.sourceConnector;

                        if (this.data.mappingConfig[index].sourceConnector){
                            this.data.mappingConfig[index].sourceConnector.displayName = $.t("templates.connector." +connectorUtils.cleanConnectorName(this.data.mappingConfig[index].sourceConnector.connectorRef.connectorName));

                            cleanName = this.data.mappingConfig[index].sourceConnector.config.split("/");
                            cleanName = cleanName[1] +"_" +cleanName[2];

                            this.data.mappingConfig[index].sourceConnector.url = "#connectors/edit/" + cleanName +"/";
                        } else {
                            this.data.mappingConfig[index].sourceConnector = {
                                "displayName" : $.t("templates.connector.managedObjectType"),
                                "url" : "#managed/edit/" +this.data.mappingConfig[index].source.split("/")[1] +"/"
                            };
                        }

                        if (this.data.mappingConfig[index].targetConnector){
                            this.data.mappingConfig[index].targetConnector.displayName = $.t("templates.connector." +connectorUtils.cleanConnectorName(this.data.mappingConfig[index].targetConnector.connectorRef.connectorName));

                            cleanName = this.data.mappingConfig[index].targetConnector.config.split("/");
                            cleanName = cleanName[1] +"_" +cleanName[2];

                            this.data.mappingConfig[index].targetConnector.url = "#connectors/edit/" + cleanName +"/";
                        } else {
                            this.data.mappingConfig[index].targetConnector = {
                                "displayName" : $.t("templates.connector.managedObjectType"),
                                "url" : "#managed/edit/" +this.data.mappingConfig[index].target.split("/")[1] +"/"
                            };
                        }
                        
                        this.model.mappingCollection.add(this.data.mappingConfig[index]);
                    }, this);
    
                    RenderRow = Backgrid.Row.extend({
                        render: function () {
                            RenderRow.__super__.render.apply(this, arguments);
    
                            this.$el.attr('data-mapping-title', this.model.attributes.name);
    
                            return this;
                        }
                    });

                    this.parentRender(_.bind(function () {
                        var start,
                            dragDropInstance = dragula([$("#mappingConfigHolder")[0]]);

                        dragDropInstance.on("drag", _.bind(function(el) {
                            start = _.indexOf($("#mappingConfigHolder .card"), el);
                            AutoScroll.startDrag();
                        }, this));

                        dragDropInstance.on("dragend", _.bind(function(el) {
                            var tempRemoved,
                                stop = _.indexOf($("#mappingConfigHolder .card"), el);

                            AutoScroll.endDrag();

                            if (start !== stop) {
                                tempRemoved = this.cleanConfig.splice(start, 1);
                                this.cleanConfig.splice(stop, 0, tempRemoved[0]);

                                configDelegate.updateEntity("sync", {"mappings": this.cleanConfig}).then(_.bind(function () {
                                    eventManager.sendEvent(constants.EVENT_DISPLAY_MESSAGE_REQUEST, "mappingSaveSuccess");
                                }, this));
                            }

                        }, this));
                        
                        mappingGrid = new Backgrid.Grid({
                            className: "table backgrid",
                            row: RenderRow,
                            columns: BackgridUtils.addSmallScreenCell([
                                {
                                    name: "name",
                                    label: "name",
                                    sortable: false,
                                    editable: false,
                                    cell: Backgrid.Cell.extend({
                                        render: function () {
                                            var display = handlebars.compile("{{> mapping/list/_linkName}}")({
                                                name: this.model.attributes.name
                                            });

                                            this.$el.html(display);

                                            return this;
                                        }
                                    })
                                },
                                {
                                    name: "source",
                                    sortable: false,
                                    editable: false,
                                    cell: Backgrid.Cell.extend({
                                        render: function () {
                                            var display = handlebars.compile("{{> mapping/list/_sourceTargetGridCellDisplay}}")({
                                                    linkUrl: this.model.attributes.sourceConnector.url,
                                                    icon: this.model.attributes.sourceIcon,
                                                    displayName: this.model.attributes.sourceConnector.displayName
                                                });
                                                
                                            this.$el.html(display);

                                            return this;
                                        }
                                    })
                                },
                                {
                                    name: "target",
                                    sortable: false,
                                    editable: false,
                                    cell: Backgrid.Cell.extend({
                                        render: function () {
                                            var display = handlebars.compile("{{> mapping/list/_sourceTargetGridCellDisplay}}")({
                                                linkUrl: this.model.attributes.targetConnector.url,
                                                icon: this.model.attributes.targetIcon,
                                                displayName: this.model.attributes.targetConnector.displayName
                                            });
                                            this.$el.html(display);

                                            return this;
                                        }
                                    })
                                },
                                {
                                    name: "status",
                                    sortable: false,
                                    editable: false,
                                    cell: Backgrid.Cell.extend({
                                        render: function () {
                                            var display = handlebars.compile("{{> mapping/list/_syncStatusCellDisplay}}")({ name: this.model.attributes.name });

                                            this.$el.html(display);

                                            return this;
                                        }
                                    })
                                },
                                {
                                    name: "",
                                    sortable: false,
                                    editable: false,
                                    cell: Backgrid.Cell.extend({
                                        className: "button-right-align",
                                        render: function () {
                                            var display = handlebars.compile("{{> mapping/list/_actionCellDisplay}}")({ name: this.model.attributes.name }),
                                                actions = _this.$el.find("[mapping='" + this.model.attributes.name + "'] .dropdown-menu").clone();
                                            
                                            display = $(display).append(actions);

                                            this.$el.html(display);

                                            return this;
                                        }
                                    })
                                }
                            ]),
                            collection: this.model.mappingCollection
                        });

                        this.$el.find("#mappingGrid").append(mappingGrid.render().el);

                        this.showSyncStatus();

                        if (callback) {
                            callback();
                        }
                    }, this));
                }, this));
            }, this));
        },
        syncType: function(type) {
            var tempType = type.split("/");

            if(tempType[0] === "managed") {
                type = "managed";
            } else {
                type = tempType[1];
            }

            return type;
        },
        deleteMapping: function(event) {
            var selectedEl = $(event.target).parents(".mapping-config-body"),
                index = this.$el.find("#mappingConfigHolder .mapping-config-body").index(selectedEl);
    
            if(!selectedEl.length) {
                selectedEl = $(event.currentTarget).parents("tr");
                
                _.each(this.$el.find(".backgrid tbody tr"), function(row,idx) {
                    if($(row).attr("data-mapping-title") === selectedEl.attr("data-mapping-title")) {
                        index = idx;
                    }
                });
            }

            UIUtils.confirmDialog($.t("templates.mapping.confirmDeleteMapping", {"mappingName": this.cleanConfig[index].name}), "danger", _.bind(function(){
                this.cleanConfig.splice(index, 1);

                configDelegate.updateEntity("sync", {"mappings":this.cleanConfig}).then(_.bind(function() {
                    eventManager.sendEvent(constants.EVENT_DISPLAY_MESSAGE_REQUEST, "mappingDeleted");

                    this.render();
                }, this));
            }, this));
        },
        showSyncStatus: function(){
            _.each(this.data.mappingConfig, function (sync){
                var el = this.$el.find("." + sync.name + "_syncStatus"),
                    icon = this.$el.find("." + sync.name + "_syncStatus_icon"),
                    txt = $.t("templates.mapping.notYetSynced");
                
                icon.removeClass("fa-check-circle");
                el.toggleClass("text-danger", true);
                icon.addClass("fa-exclamation-circle");
                
                if(sync.recon){
                    if(sync.recon.state === "CANCELED") {
                        txt = $.t("templates.mapping.lastSyncCanceled");
                        el.toggleClass("text-success", false);
                        el.toggleClass("text-danger", true);
                        icon.removeClass("fa-check-circle text-success");
                        icon.addClass("fa-exclamation-circle");
                    } else if(sync.recon.state === "ACTIVE") {
                        txt = $.t("templates.mapping.inProgress");
                        el.toggleClass("text-success", false);
                        el.toggleClass("text-danger", true);
                        icon.removeClass("fa-check-circle text-success");
                        icon.addClass("fa-exclamation-circle");
                    } else {
                        txt = $.t("templates.mapping.lastSynced") + " " + dateUtil.formatDate(sync.recon.ended,"MMMM dd, yyyy HH:mm");
                        el.toggleClass("text-success", true);
                        el.toggleClass("text-danger", false);
                        icon.addClass("fa-check-circle text-success");
                        icon.removeClass("fa-exclamation-circle");
                    }
                }

                el.text(txt);
            }, this);
        },

        filterMappings: function(event) {
            var search = $(event.target).val().toLowerCase();

            if(search.length > 0) {
                _.each(this.$el.find(".mapping-config-body"), function(card) {
                    if($(card).attr("mapping").toLowerCase().indexOf(search) > -1) {
                        $(card).fadeIn();
                    } else {
                        $(card).fadeOut();
                    }
                }, this);

                _.each(this.$el.find(".backgrid tbody tr"), function(row) {
                    if($(row).attr("data-mapping-title").toLowerCase().indexOf(search) > -1) {
                        $(row).fadeIn();
                    } else {
                        $(row).fadeOut();
                    }
                }, this);
            } else {
                this.$el.find(".mapping-config-body").fadeIn();
                this.$el.find(".backgrid tbody tr").fadeIn();
            }
        }
    });

    return new MappingListView();
});
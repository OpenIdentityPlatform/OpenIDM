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
 * Copyright 2014-2016 ForgeRock AS.
 */

define([
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
    "dragula",
    "org/forgerock/openidm/ui/admin/mapping/util/MappingUtils"
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
            dragula,
            mappingUtils) {

    var MappingListView = AdminAbstractView.extend({
        template: "templates/admin/mapping/MappingListTemplate.html",
        events: {
            "click #addMapping": "addMapping",
            "click .delete-button" : "deleteMapping",
            "click .mapping-config-body": "mappingDetail",
            "click .toggle-view-btn": "toggleButtonChange",
            "keyup .filter-input" : "filterMappings",
            "paste .filter-input" : "filterMappings",
            "click .sync-now": "syncNow",
            "click .stop-sync": "stopSync"
        },
        model: {},
        partials: [
            "partials/mapping/list/_sourceTargetGridCellDisplay.html",
            "partials/mapping/list/_emptyConnectorGridCell.html",
            "partials/mapping/list/_syncStatusCellDisplay.html",
            "partials/mapping/list/_actionCellDisplay.html",
            "partials/mapping/list/_linkName.html"
        ],
        mappingDetail: function(e){
            var button = $(e.target).closest("button");
            if(!button.hasClass("card-button")){
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
                managedPromise = configDelegate.readEntity("managed"),
                mappingDetails = [];

            $.when(syncConfig, managedPromise).then((sync, managedDetails) => {
                this.data.mappingConfig = sync.mappings;
                this.data.docHelpUrl = constants.DOC_URL;
                this.model.managedDetails = managedDetails;

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

                $.when.apply($, mappingDetails).then(_.bind(function() {
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

                        if(!this.data.mappingConfig[index].sourceConnector) {
                            this.data.mappingConfig[index].sourceConnector = {};
                        }

                        if(!this.data.mappingConfig[index].targetConnector) {
                            this.data.mappingConfig[index].targetConnector = {};
                        }

                        this.data.mappingConfig[index].sourceConnector = this.setCardState(this.data.mappingConfig[index].sourceConnector, this.data.mappingConfig[index].sourceType, this.data.mappingConfig[index].source, this.model.managedDetails);
                        this.data.mappingConfig[index].targetConnector = this.setCardState(this.data.mappingConfig[index].targetConnector, this.data.mappingConfig[index].targetType, this.data.mappingConfig[index].target, this.model.managedDetails);

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
                                            var display;
                                            if (this.model.attributes.sourceType === "managed" || this.model.attributes.sourceConnector.connectorRef) {
                                                display = handlebars.compile("{{> mapping/list/_sourceTargetGridCellDisplay}}")({
                                                    linkUrl: this.model.attributes.sourceConnector.url,
                                                    icon: this.model.attributes.sourceIcon,
                                                    displayName: this.model.attributes.sourceConnector.displayName
                                                });
                                            } else {
                                                display = handlebars.compile("{{> mapping/list/_emptyConnectorGridCell}}")({
                                                    displayName: this.model.attributes.sourceConnector.displayName
                                                });
                                            }
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
                                            var display;
                                            if (this.model.attributes.sourceType === "managed" || this.model.attributes.targetConnector.connectorRef) {
                                                display = handlebars.compile("{{> mapping/list/_sourceTargetGridCellDisplay}}")({
                                                    linkUrl: this.model.attributes.targetConnector.url,
                                                    icon: this.model.attributes.targetIcon,
                                                    displayName: this.model.attributes.targetConnector.displayName
                                                });
                                            } else {
                                                display = handlebars.compile("{{> mapping/list/_emptyConnectorGridCell}}")({
                                                    displayName: this.model.attributes.targetConnector.displayName
                                                });
                                            }
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

                        this.showSyncStatus(true);

                        if (callback) {
                            callback();
                        }
                    }, this));
                }, this));
            });
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
        /**
         * This function is to detect what state a resource is in before generating a card. If it is determined to be missing it will display
         * properly letting the user know that resource is no longer available.
         *
         * @param resource - Details about the connector or managed object
         * @param type - A string of managed or connector
         * @param location - Path to managed object or connector (managed/roles or system/ldap/account)
         * @param managedDetails - Array of current available managed objects
         * @returns {*} returns the config state for the card display
         */
        setCardState: function(resource, type, location, managedDetails) {
            var cleanName,
                managedName,
                managedCheck;

            if (!_.isEmpty(resource)){
                resource.displayName = $.t("templates.connector." +connectorUtils.cleanConnectorName(resource.connectorRef.connectorName));

                cleanName = resource.config.split("/");
                cleanName = cleanName[1] +"_" +cleanName[2];

                resource.url = "#connectors/edit/" + cleanName +"/";
            } else {
                if (type === "managed") {
                    managedName = location.split("/")[1];

                    _.each(managedDetails.objects, (managedObject) => {
                        if(managedObject.name === managedName) {
                            managedCheck = true;
                        }
                    });

                    resource.displayName = $.t("templates.connector.managedObjectType");

                    if(managedCheck) {
                        resource.url = "#managed/edit/" +location.split("/")[1] +"/";
                    } else {
                        resource.isMissing = true;
                    }
                } else {
                    resource.displayName = type;
                    resource.isMissing = true;
                }

            }

            return resource;
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

            mappingUtils.confirmDeleteMapping(this.cleanConfig[index].name, this.cleanConfig, _.bind(function() {
                eventManager.sendEvent(constants.EVENT_DISPLAY_MESSAGE_REQUEST, "mappingDeleted");

                if(this.cleanConfig.length === 0) {
                    this.$el.find("#noMappingsDefined").show();
                }

                selectedEl.remove();
            }, this));
        },
        showSyncStatus: function(isOnPageLoad){
            _.each(this.data.mappingConfig, function (mapping){
                var el = this.$el.find("." + mapping.name + "_syncStatus"),
                    icon = this.$el.find("." + mapping.name + "_syncStatus_icon"),
                    recon = mapping.recon,
                    parent = el.parent(),
                    text,
                    type,
                    total,
                    processed;

                if(recon){
                    if(recon.state === "CANCELED") {
                        text = $.t("templates.mapping.lastSyncCanceled");
                        type = "DANGER";
                    } else if(recon.state === "ACTIVE") {
                        text = $.t("templates.mapping.inProgress") + ": ";

                        if (recon.progress.source.existing.total !== "?"  && recon.stage === "ACTIVE_RECONCILING_SOURCE") {
                            processed = parseInt(recon.progress.source.existing.processed, 10);
                            total = parseInt(recon.progress.source.existing.total, 10);
                        } else if(recon.progress.target.existing.total !== "?" && recon.stage === "ACTIVE_RECONCILING_TARGET") {
                            total = parseInt(recon.progress.target.existing.total, 10);
                            processed = parseInt(recon.progress.target.existing.processed, 10);
                        } else {
                            total = 0;
                            processed = 0;
                        }

                        if(total !== 0 && processed !== 0) {
                            text +=  recon.stageDescription + " - <span class='bold-message'>" + processed + "/" + total + "</span>";
                        } else {
                            text += recon.stageDescription;
                        }

                        type = "SUCCESS";

                        if (isOnPageLoad) {
                            this.updateReconStatus(mapping.name, recon._id);
                        }
                    } else {
                        text = $.t("templates.mapping.lastSynced") + " " + dateUtil.formatDate(mapping.recon.ended,"MMMM dd, yyyy HH:mm");
                        type = "SUCCESS";
                    }
                } else {
                    text = $.t("templates.mapping.notYetSynced");
                    type = "DANGER";
                }

                if (mapping.sourceConnector.isMissing && mapping.targetConnector.isMissing) {
                    text = $.t("templates.mapping.missingBoth");
                    type = "DANGER";
                } else if (mapping.sourceConnector.isMissing){
                    text = $.t("templates.mapping.missingSourceConnector");
                    type = "DANGER";
                } else if (mapping.targetConnector.isMissing) {
                    text = $.t("templates.mapping.missingTargetConnector");
                    type = "DANGER";
                }


                parent.removeClass("text-success text-danger text-muted");
                icon.removeClass("fa-exclamation-circle fa-check-circle fa-question-circle");

                if (type === "DANGER") {
                    parent.addClass("text-danger");
                    icon.addClass("fa-exclamation-circle");
                } else if (type === "SUCCESS") {
                    parent.addClass("text-success");
                    icon.addClass("fa-check-circle");
                }

                el.html(text);
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
        },
        /**
         * This function sets the recon for the associated mapping in this.data.mappingConfig
         * (based on mappingName) to the newly updated recon. It then fires off showSyncStatus()
         * to reload recon status information
         *
         * @param mappingName {string} - name of the mapping to find in this.data.mappingConfig
         * @param recon {object} - recon object
         */
        updateMappingRecon: function (mappingName, recon) {
            var mapping = _.findWhere(this.data.mappingConfig, { name : mappingName });
            mapping.recon = recon;
            this.showSyncStatus();
        },
        /**
         * This function disables/enables hides/shows the "Reconcile Now"
         * and "Stop Reconciliation" buttons for an individual mapping
         *
         * @param mappingName {string}
         * @param reconInProgress {boolean}
         */
        toggleReconButtons: function (mappingName, reconInProgress) {
            var syncNowButton = this.$el.find(".sync-now[mappingName=" + mappingName + "]"),
                stopSyncButton = this.$el.find(".stop-sync[mappingName=" + mappingName + "]");

            if (reconInProgress) {
                //disable/hide the syncNowButton
                syncNowButton.hide();
                syncNowButton.prop("disabled", true);
                //enable/show the stopSyncButton
                stopSyncButton.show();
                stopSyncButton.prop("disabled", false);
            } else {
                //recon is complete enable/show the syncNowButton
                syncNowButton.show();
                syncNowButton.prop("disabled",false);
                //and disable/hide the stopSyncButton
                stopSyncButton.hide();
                stopSyncButton.prop("disabled", true);
            }
        },
        /**
         * This function finds the appropriate mapping name based on the click event,
         * toggles the correct buttons, and triggers a recon base on mappingName
         * which updates the recon status on the mapping card every 2 seconds until it is complete
         *
         * @param e {object} - event object
         */
        syncNow: function (e) {
            var mappingName = $(e.target).closest(".sync-now").attr("mappingName");

            e.preventDefault();

            this.toggleReconButtons(mappingName, true);

            //trigger the recon
            reconDelegate.triggerRecon(mappingName, true, (runningRecon) => {
                this.updateMappingRecon(mappingName, runningRecon);
            }, 2000).then((completedRecon) => {
                this.toggleReconButtons(mappingName);
                this.updateMappingRecon(mappingName, completedRecon);
            });
        },
        /**
         * This function is called on page load when a recon is already active.
         * It calls waitForAll on and individual recon and updates the recon status
         * on the mapping card every 2 seconds until it is complete
         *
         * @param mappingName {string} - name of the mapping to find in this.data.mappingConfig
         * @param runningReconId {string} - uuid for the active recon
         */
        updateReconStatus: function (mappingName, runningReconId) {
            this.toggleReconButtons(mappingName, true);

            reconDelegate.waitForAll([runningReconId], true, (runningRecon) => {
                this.updateMappingRecon(mappingName, runningRecon);
            }, 2000).then((completedRecon) => {
                this.toggleReconButtons(mappingName);
                this.updateMappingRecon(mappingName, completedRecon[0]);
            });
        },
        /**
         * This function finds the appropriate mapping name based on the click event,
         * stops a recon based on mappingName, toggles the correct buttons,
         * and updates the recon status on the mapping card
         *
         * @param e {object} - event object
         */
        stopSync: function(e){
            var mappingName = $(e.target).closest(".stop-sync").attr("mappingName"),
                mapping = _.findWhere(this.data.mappingConfig, { name : mappingName });

            e.preventDefault();

            reconDelegate.stopRecon(mapping.recon._id, true).then((canceledRecon) => {
                this.toggleReconButtons(mappingName);
                this.updateMappingRecon(mappingName, canceledRecon);
            });
        }
    });

    return new MappingListView();
});

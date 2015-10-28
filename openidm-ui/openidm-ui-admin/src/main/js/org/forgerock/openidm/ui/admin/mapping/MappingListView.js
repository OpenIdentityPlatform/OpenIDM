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
    "org/forgerock/openidm/ui/admin/util/AdminAbstractView",
    "org/forgerock/commons/ui/common/main/EventManager",
    "org/forgerock/openidm/ui/common/delegates/ConfigDelegate",
    "org/forgerock/commons/ui/common/util/Constants",
    "org/forgerock/openidm/ui/admin/delegates/ReconDelegate",
    "org/forgerock/commons/ui/common/util/DateUtil",
    "org/forgerock/openidm/ui/admin/delegates/SyncDelegate",
    "org/forgerock/openidm/ui/admin/util/ConnectorUtils",
    "org/forgerock/commons/ui/common/util/UIUtils",
    "jquerySortable"
], function($, _,
            AdminAbstractView,
            eventManager,
            configDelegate,
            constants,
            reconDelegate,
            dateUtil,
            syncDelegate,
            connectorUtils,
            UIUtils) {

    var MappingListView = AdminAbstractView.extend({
        template: "templates/admin/mapping/MappingListTemplate.html",
        events: {
            "click #addMapping": "addMapping",
            "click .delete-button" : "deleteMapping",
            "click .mapping-config-body": "mappingDetail"
        },
        mappingDetail: function(e){
            if(!$(e.target).closest("button").hasClass("delete-button")){
                e.preventDefault();

                eventManager.sendEvent(constants.ROUTE_REQUEST, {routeName: "propertiesView", args: [$(e.target).closest(".mapping-config-body").attr("mapping")]});
            }
        },
        render: function(args, callback) {
            var syncConfig = syncDelegate.mappingDetails(),
                mappingDetails = [],
                results,
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
                    results = arguments;

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
                    }, this);

                    this.parentRender(_.bind(function () {
                        var startIndex = null;

                        this.$el.find("#mappingConfigHolder ul").nestingSortable({
                            handle: "div",
                            items: "li",
                            toleranceElement: "ul",
                            disabledClass: "disabled",
                            placeholder: "<li class='placeholder well'></li>",
                            onMousedown: _.bind(function ($item, _super, event) {
                                startIndex = this.$el.find("#mappingConfigHolder ul li:not(.disabled)").index($item);
                                if (!event.target.nodeName.match(/^(input|select)$/i)) {
                                    event.preventDefault();
                                    return true;
                                }
                            }, this),
                            onDrop: _.bind(function ($item, container, _super, event) {
                                var endIndex = this.$el.find("#mappingConfigHolder ul li:not(.disabled)").index($item),
                                    tempRemoved;

                                _super($item, container, _super, event);

                                if (startIndex !== endIndex) {
                                    tempRemoved = this.cleanConfig.splice(startIndex, 1);
                                    this.cleanConfig.splice(endIndex, 0, tempRemoved[0]);

                                    configDelegate.updateEntity("sync", {"mappings": this.cleanConfig}).then(_.bind(function () {
                                        eventManager.sendEvent(constants.EVENT_DISPLAY_MESSAGE_REQUEST, "mappingSaveSuccess");
                                    }, this));
                                }

                            }, this)
                        });

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

            UIUtils.confirmDialog($.t("templates.mapping.confirmDeleteMapping", {"mappingName": this.cleanConfig[index].name}), "danger", _.bind(function(){
                this.cleanConfig.splice(index, 1);

                configDelegate.updateEntity("sync", {"mappings":this.cleanConfig}).then(_.bind(function() {
                    eventManager.sendEvent(constants.EVENT_DISPLAY_MESSAGE_REQUEST, "mappingDeleted");

                    if(this.cleanConfig.length === 0) {
                        this.$el.find("#noMappingsDefined").show();
                    }

                    selectedEl.remove();
                }, this));
            }, this));
        },
        showSyncStatus: function(){
            _.each(this.data.mappingConfig, function (sync){
                var el = this.$el.find("#" + sync.name + "_syncStatus"),
                    txt = $.t("templates.mapping.notYetSynced");
                if(sync.recon){
                    if(sync.recon.state === "CANCELED") {
                        txt = $.t("templates.mapping.lastSyncCanceled");
                        el.toggleClass("text-muted", false);
                        el.toggleClass("text-danger", true);
                    } else if(sync.recon.state === "ACTIVE") {
                        txt = $.t("templates.mapping.inProgress");
                        el.toggleClass("text-muted", false);
                        el.toggleClass("text-danger", true);
                    } else {
                        txt = $.t("templates.mapping.lastSynced") + " " + dateUtil.formatDate(sync.recon.ended,"MMMM dd, yyyy HH:mm");
                        el.toggleClass("text-muted", true);
                        el.toggleClass("text-danger", false);
                    }
                }

                el.text(txt);
            }, this);
        }
    });

    return new MappingListView();
});
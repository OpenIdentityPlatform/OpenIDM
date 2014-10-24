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

/*global define, $, _, Handlebars, form2js */

define("org/forgerock/openidm/ui/admin/mapping/MappingListView", [
    "org/forgerock/openidm/ui/admin/util/AdminAbstractView",
    "org/forgerock/commons/ui/common/main/EventManager",
    "org/forgerock/openidm/ui/common/delegates/ConfigDelegate",
    "org/forgerock/commons/ui/common/util/Constants",
    "org/forgerock/commons/ui/common/util/UIUtils",
    "org/forgerock/openidm/ui/admin/delegates/ReconDelegate",
    "org/forgerock/commons/ui/common/util/DateUtil",
    "org/forgerock/openidm/ui/admin/delegates/SyncDelegate"
], function(AdminAbstractView, eventManager, configDelegate, constants, uiUtils, reconDelegate, dateUtil, syncDelegate) {

    var MappingListView = AdminAbstractView.extend({
        template: "templates/admin/mapping/MappingListTemplate.html",
        events: {
            "click #addMapping": "addMapping",
            "click .delete-button" : "deleteMapping",
            "click .mapping-config-body": "mappingDetail"
        },
        mappingDetail: function(e){
            e.preventDefault();
            
            if(!$(e.target).closest("button").hasClass("delete-button")){
                eventManager.sendEvent(constants.ROUTE_REQUEST, {routeName: "propertiesView", args: [$(e.target).closest(".mapping-config-body").attr("mapping")]});
            }
        },
        render: function(args, callback) {
            var syncConfig = syncDelegate.mappingDetails();

            syncConfig.then(_.bind(function(sync){
                this.data.mappingConfig = sync.mappings;
                this.cleanConfig = _.chain(sync.mappings)
                                        .map(function(m){
                                            return _.clone(_.omit(m,"recon"));
                                        })
                                        .value();

                _.each(this.data.mappingConfig, function (sync){
                    sync.targetType = this.syncType(sync.target);

                    if(sync.targetType === "managed") {
                        sync.targetIcon = "fa-database";
                    } else {
                        sync.targetIcon = "fa-cubes";
                    }

                    sync.sourceType = this.syncType(sync.source);

                    if(sync.sourceType === "managed") {
                        sync.sourceIcon = "fa-database";
                    } else {
                        sync.sourceIcon = "fa-cubes";
                    }
                }, this);

                this.parentRender(_.bind(function () {
                    $('#mappingConfigHolder').sortable({
                        items: '.mapping-config-body',
                        start: _.bind(function(event, ui) {
                            this.startIndex = this.$el.find("#mappingConfigHolder .mapping-config-body").index(ui.item);
                        }, this),
                        stop: _.bind(function(event, ui){
                            var stopIndex = this.$el.find("#mappingConfigHolder .mapping-config-body").index(ui.item),
                                tempRemoved;
                            
                            if(this.startIndex !== stopIndex){
                                tempRemoved = this.cleanConfig.splice(this.startIndex, 1);
                                this.cleanConfig.splice(stopIndex, 0, tempRemoved[0]);
                                configDelegate.updateEntity("sync", {"mappings":this.cleanConfig}).then(_.bind(function() {
                                    eventManager.sendEvent(constants.EVENT_DISPLAY_MESSAGE_REQUEST, "mappingSaveSuccess");
                                }, this));
                            }

                        }, this)
                    });
                    
                    this.showSyncStatus();
                    
                    if(callback){
                        callback();
                    }
                }, this));
            },this));
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

            uiUtils.jqConfirm($.t("templates.mapping.confirmDeleteMapping", {"mappingName": this.cleanConfig[index].name}), _.bind(function(){
                this.cleanConfig.splice(index, 1);

                configDelegate.updateEntity("sync", {"mappings":this.cleanConfig}).then(_.bind(function() {
                    eventManager.sendEvent(constants.EVENT_DISPLAY_MESSAGE_REQUEST, "mappingDeleted");

                    if(this.cleanConfig.length === 0) {
                        this.$el.find("#noMappingsDefined").show();
                    }

                    selectedEl.remove();
                }, this));
            }, this), "550px");
        },
        showSyncStatus: function(){
            _.each(this.data.mappingConfig, function (sync){
                var el = this.$el.find("#" + sync.name + "_syncStatus"),
                    txt = $.t("templates.mapping.notYetSynced");
                if(sync.recon){
                    if(sync.recon.state === "CANCELED") {
                        txt = $.t("templates.mapping.lastSyncCanceled");
                    } else if(sync.recon.state === "ACTIVE") {
                        txt = $.t("templates.mapping.inProgress");
                    } else {
                        txt = $.t("templates.mapping.lastSynced") + " " + dateUtil.formatDate(sync.recon.ended,"MMMM dd, yyyy HH:mm");
                    }
                }
                
                el.text(txt);
            }, this);
        }
    });

    return new MappingListView();
});
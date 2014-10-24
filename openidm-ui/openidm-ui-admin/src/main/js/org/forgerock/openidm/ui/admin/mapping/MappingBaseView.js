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

/*global define, $, _, Handlebars, form2js, sessionStorage */

define("org/forgerock/openidm/ui/admin/mapping/MappingBaseView", [
    "org/forgerock/openidm/ui/admin/util/AdminAbstractView",
    "org/forgerock/commons/ui/common/main/EventManager",
    "org/forgerock/commons/ui/common/main/ValidatorsManager",
    "org/forgerock/openidm/ui/common/delegates/ConfigDelegate",
    "org/forgerock/commons/ui/common/util/UIUtils",
    "org/forgerock/commons/ui/common/util/Constants",
    "org/forgerock/openidm/ui/admin/delegates/BrowserStorageDelegate",
    "org/forgerock/commons/ui/common/components/Navigation",
    "org/forgerock/openidm/ui/admin/delegates/ReconDelegate",
    "org/forgerock/openidm/ui/admin/util/ReconProgress",
    "org/forgerock/commons/ui/common/util/DateUtil",
    "org/forgerock/openidm/ui/admin/delegates/SyncDelegate"
    ], function(AdminAbstractView, eventManager, validatorsManager, configDelegate, UIUtils, constants, browserStorageDelegate, nav, reconDelegate, reconProgress, dateUtil, syncDelegate) {

    var MappingBaseView = AdminAbstractView.extend({
        template: "templates/admin/mapping/MappingTemplate.html",
        events: {
            "click .mapping-config-body": "mappingList",
            "click #syncNowButton": "syncNow",
            "click #stopSyncButton": "stopSync",
            "click #syncStatus a": "toggleSyncDetails"
        },
        mappingList: function(e){
            e.preventDefault();
            
            if(!$(e.target).closest("button").hasClass("button") && !$(e.target).parent().hasClass("syncStatus")){
                delete this.data.mapping;
                eventManager.sendEvent(constants.ROUTE_REQUEST, {routeName: "mappingListView"});
            }
        },
        toggleSyncDetails: function(){
            this.$el.find("#syncStatusDetails").toggle();
        },
        data: {},
        setSubmenu: function(){
            _.each(nav.configuration.links.admin.urls.mapping.urls, _.bind(function(val){ 
                var url = val.url.split("/")[0];
                
                val.url = url + "/" + this.currentMapping().name + "/";
            },this));
            nav.reload();
        },
        moveSubmenu: function(){
            var submenuClone = $("#submenu").clone(true);

            $("#submenu").remove();
            this.$el.find("#submenuClone").remove();
            submenuClone.attr("id","submenuClone");
            submenuClone.insertBefore("#mappingContent", this.$el).show();
        },
        setCurrentMapping: function(mappingObj){
            browserStorageDelegate.set('currentMapping',mappingObj);
            return mappingObj;
        },
        currentMapping: function(){
            return browserStorageDelegate.get('currentMapping');
        },
        syncType: function(type) {
            var tempType = type.split("/");

            return (tempType[0] === "managed") ? "managed" : tempType[1];
        },
        runningReconProgress: function(onReady){
            var progress;
            reconDelegate.waitForAll([this.data.recon._id], true, _.bind(function (reconStatus) {
                if(this.data.recon._id === reconStatus._id && this.data.recon.mapping === this.currentMapping().name){
                    if(reconStatus.state !== "CANCELED"){
                        progress = reconProgress.init(this.$el,"#syncNowProgress","templates.mapping.syncComplete", "templates.mapping.syncInProgress");
                        this.setSyncInProgress();
                        progress.start(reconStatus);
                        this.$el.find("#stopSyncButton").prop("disabled",false);
                    } else {
                        this.data.syncCanceled = true;
                        this.data.syncStatus = $.t("templates.mapping.lastSyncCanceled");
                    }
                    this.$el.find("#syncStatusDetails").html(JSON.stringify(_.omit(reconStatus,"_id","mapping","parameters"), null, 4));
                }
            }, this)).then(_.bind(function (completedRecon) {
                if(this.data.recon._id === completedRecon[0]._id && this.data.recon.mapping === this.currentMapping().name){
                    this.$el.find("#syncNowButton").show().prop("disabled",false);
                    this.$el.find("#stopSyncButton").hide();
                    if(progress){
                        progress.end();
                    }
                    this.data.recon = completedRecon[0];
                    this.data.syncStatus = $.t("templates.mapping.lastSynced") + " " + dateUtil.formatDate(this.data.recon.ended,"MMMM dd, yyyy HH:mm");
                    onReady();
                }
            }, this));
        },
        render: function(args, callback) {
            //because there are relatively slow queries being called which would slow down the interface if they were called each time
            //decide here whether we want to render all of this view or only the child
            //if this.data.mapping does not exist we know this view has not been loaded
            //if this.data.mapping.name is set and it has a different name we want to refresh this view
            //there are rare occasions when this.data.mapping exists but it has actually not been rendered yet hence the last condition
            if(!this.data.mapping || this.data.mapping.name !== args[0] || this.$el.find("#mappingContent").length === 0){
                var syncConfig = syncDelegate.mappingDetails(args[0]);
                
                syncConfig.then(_.bind(function(sync){
                    var onReady;
                    
                    onReady = _.bind(function(runningRecon){
                        this.parentRender(_.bind(function () {
                            this.setSubmenu();
                            this.$el.find("#syncStatusDetails").html(JSON.stringify(_.omit(this.data.recon,"_id","mapping","parameters"), null, 4)).hide();
                            if(runningRecon){
                                this.runningReconProgress(onReady);
                            }
                            if(callback){
                                callback();
                            }
                        }, this));
                    }, this);
                    
                    this.data.syncConfig = { mappings: _.chain(sync.mappings)
                                                            .map(function(m){
                                                                return _.clone(_.omit(m,"recon"));
                                                            })
                                                            .value()
                                           };
                    this.data.mapping = _.filter(sync.mappings,function(m){ return m.name === args[0];})[0];
                    this.setCurrentMapping($.extend({},true,this.data.mapping));
                    this.data.syncStatus = $.t("templates.mapping.notYetSynced");
                    this.data.syncCanceled = false;
                    
                    this.data.targetType = this.syncType(this.data.mapping.target);
                    this.data.targetIcon = (this.data.targetType === "managed") ? "fa-database" : "fa-cubes";
    
                    this.data.sourceType = this.syncType(this.data.mapping.source);
                    this.data.sourceIcon = (this.data.sourceType === "managed") ? "fa-database" : "fa-cubes";
                    
                    if(this.data.mapping.recon){
                        this.data.recon = this.data.mapping.recon;
                        if (this.data.recon.ended) {
                            if(this.data.recon.state === "CANCELED"){
                                this.data.syncCanceled = true;
                                this.data.syncStatus = $.t("templates.mapping.lastSyncCanceled");
                            } else {
                                this.data.syncStatus = $.t("templates.mapping.lastSynced") + " " + dateUtil.formatDate(this.data.recon.ended,"MMMM dd, yyyy HH:mm");
                            }
                            onReady();
                        } else {
                            onReady(true);
                        }
                    } else {
                        onReady();
                    }
                }, this));
            } else {
                this.setSubmenu();
                if(callback){
                    callback();
                }
            }
        },
        setSyncInProgress: function(){
            this.$el.find("#syncNowButton").hide().prop("disabled",true);
            this.$el.find("#stopSyncButton").show().prop("disabled", true);
            this.$el.find("#syncStatus a").text($.t("templates.mapping.inProgress"));
        },
        syncNow: function(event) {
            event.preventDefault();
            this.data.reconAvailable = false;
            this.setSyncInProgress();
            
            var progress = reconProgress.init(this.$el,"#syncNowProgress","templates.mapping.syncComplete");
            
            reconDelegate.triggerRecon(this.currentMapping().name, true, _.bind(function (reconStatus) {
                progress.start(reconStatus);
                this.data.recon = reconStatus;
                this.$el.find("#stopSyncButton").prop("disabled",false);
                this.$el.find("#syncStatusDetails").html(JSON.stringify(_.omit(reconStatus,"_id","mapping","parameters"), null, 4));
            }, this)).then(_.bind(function(s){
                this.data.reconAvailable = false;
                this.data.recon = s;
                
                progress.end();
                delete this.data.mapping;
                this.child.render([this.currentMapping().name]);
            }, this));
        },
        stopSync: function(e){
            e.preventDefault();
            this.$el.find("#syncStatus a").text($.t("templates.mapping.stoppingSync"));
            this.$el.find("#stopSyncButton").hide().prop("disabled",true);
            this.$el.find("#syncNowButton").hide().prop("disabled",true);
            this.$el.find(".reconProgressContainer").hide();
            this.$el.find("#stoppingSync").show();
            reconDelegate.stopRecon(this.data.recon._id, true).then(_.bind(function(){
                delete this.data.mapping;
                this.child.render([this.currentMapping().name]);
            }, this));
        }
    });

    return new MappingBaseView();
});

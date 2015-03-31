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

/*global window,define, $, _, form2js */

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
    "org/forgerock/commons/ui/common/util/DateUtil",
    "org/forgerock/openidm/ui/admin/delegates/SyncDelegate",
    "org/forgerock/openidm/ui/admin/util/ConnectorUtils",
    "org/forgerock/openidm/ui/admin/util/ReconDetailsView",
    "bootstrap-tabdrop",
    "org/forgerock/openidm/ui/admin/util/LinkQualifierUtils"
], function(AdminAbstractView, eventManager, validatorsManager, configDelegate, UIUtils, constants, browserStorageDelegate, nav, reconDelegate, dateUtil, syncDelegate, connectorUtils, ReconDetailsView, tabdrop, LinkQualifierUtil) {

    var MappingBaseView = AdminAbstractView.extend({
        template: "templates/admin/mapping/MappingTemplate.html",
        events: {
            "click #syncNowButton": "syncNow",
            "click #stopSyncButton": "stopSync",
            "click #syncStatus": "toggleSyncDetails"
        },
        data: {},
        model: {
            syncDetails: null,
            syncOpen: false
        },
        toggleSyncDetails: function(event){
            event.preventDefault();

            this.$el.find("#syncStatus .fa").toggleClass("fa-caret-right");
            this.$el.find("#syncStatus .fa").toggleClass("fa-caret-down");

            this.$el.find("#syncStatusDetails").toggle();

            if(this.$el.find("#syncStatusDetails:visible").length) {
                this.model.syncOpen = true;
                this.loadReconDetails(this.model.syncDetails);
            } else {
                this.model.syncOpen = false;
            }
        },
        loadReconDetails: function(syncDetails) {
            ReconDetailsView.render(syncDetails);
        },
        setSubmenu: function(){
            _.each(nav.configuration.links.admin.urls.mapping.urls, _.bind(function(val){
                var url = val.url.split("/")[0];

                val.url = url + "/" + this.currentMapping().name + "/";
            },this));
            nav.reload();
        },
        moveSubmenu: function(){
            var submenuClone = $("#subNavHolder").clone(true),
                submenuList = submenuClone.find("ul");

            $("#subNavHolder").remove();
            this.$el.find("#subNavHolderClone").remove();
            submenuClone.attr({
                "id":"subNavHolderClone",
                "class": ""
            });
            submenuClone.find("#subNavBar").attr("class","");
            submenuList.attr({
                "class":"nav nav-tabs",
                "role":"tablist"
            });
            submenuList.find("a").addClass("submenuNavButton");
            submenuClone.insertBefore("#mappingContent", this.$el).show();
            this.$el.find(".nav-tabs").tabdrop();
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
            reconDelegate.waitForAll([this.data.recon._id], true, _.bind(function (reconStatus) {
                if(this.data.recon._id === reconStatus._id && this.data.recon.mapping === this.currentMapping().name){
                    if(reconStatus.state !== "CANCELED"){
                        this.setSyncInProgress();
                        this.$el.find("#stopSyncButton").prop("disabled",false);
                    } else {
                        this.data.syncCanceled = true;
                        this.data.syncStatus = $.t("templates.mapping.lastSyncCanceled");
                    }

                    this.model.syncDetails = reconStatus;

                    if(this.$el.find("#syncStatusDetails:visible").length) {
                        this.loadReconDetails(this.model.syncDetails);
                    }
                }
            }, this)).then(_.bind(function (completedRecon) {
                if(this.data.recon._id === completedRecon[0]._id && this.data.recon.mapping === this.currentMapping().name){
                    this.$el.find("#syncNowButton").show().prop("disabled",false);
                    this.$el.find("#stopSyncButton").hide();

                    this.data.recon = completedRecon[0];
                    this.data.syncLabel = $.t("templates.mapping.reconAnalysis.completed");
                    this.data.syncStatus = $.t("templates.mapping.lastSynced") + " " + dateUtil.formatDate(this.data.recon.ended,"MMMM dd, yyyy HH:mm");
                    onReady();
                }
            }, this));
        },
        render: function(args, callback) {
            var syncConfig,
                cleanName;

            if(args === "null"){
                args = UIUtils.getCurrentHash().split("/").slice(1);
            }

            this.route = { url: window.location.hash.replace(/^#/, '') };
            this.data.docHelpUrl = constants.DOC_URL;

            //because there are relatively slow queries being called which would slow down the interface if they were called each time
            //decide here whether we want to render all of this view or only the child
            //if this.data.mapping does not exist we know this view has not been loaded
            //if this.data.mapping.name is set and it has a different name we want to refresh this view
            //there are rare occasions when this.data.mapping exists but it has actually not been rendered yet hence the last condition
            if(!this.data.mapping || this.data.mapping.name !== args[0] || this.$el.find("#mappingContent").length === 0){
                this.model.syncDetails = null;
                syncConfig = syncDelegate.mappingDetails(args[0]);

                syncConfig.then(_.bind(function(sync){
                    var onReady;

                    onReady = _.bind(function(runningRecon){
                        this.parentRender(_.bind(function () {
                            this.setSubmenu();

                            if(this.model.syncOpen) {
                                $("#syncStatus").trigger("click");
                            }

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
                    this.data.syncLabel = $.t("templates.mapping.reconAnalysis.status");
                    this.data.syncStatus = $.t("templates.mapping.notYetSynced");
                    this.data.syncCanceled = false;

                    this.data.targetType = this.syncType(this.data.mapping.target);
                    this.data.sourceType = this.syncType(this.data.mapping.source);

                    connectorUtils.getMappingDetails(this.data.sourceType , this.data.targetType).then(_.bind(function (details) {
                        this.data.mapping.sourceConnector = details.sourceConnector;
                        this.data.mapping.targetConnector = details.targetConnector;
                        this.data.mapping.targetIcon = details.targetIcon.iconClass;
                        this.data.mapping.sourceIcon = details.sourceIcon.iconClass;

                        if (this.data.mapping.sourceConnector){
                            this.data.mapping.sourceConnector.displayName = $.t("templates.connector." +connectorUtils.cleanConnectorName(this.data.mapping.sourceConnector.connectorRef.connectorName));

                            cleanName = this.data.mapping.sourceConnector.config.split("/");
                            cleanName = cleanName[1] +"_" +cleanName[2];

                            this.data.mapping.sourceConnector.url = "#connectors/edit/" + cleanName +"/";
                        } else {
                            this.data.mapping.sourceConnector= {
                                "displayName" : $.t("templates.connector.managedObjectType"),
                                "url" : "#managed/edit/" +this.data.mapping.source.split("/")[1] +"/"
                            };
                        }

                        if (this.data.mapping.targetConnector){
                            this.data.mapping.targetConnector.displayName = $.t("templates.connector." +connectorUtils.cleanConnectorName(this.data.mapping.targetConnector.connectorRef.connectorName));

                            cleanName = this.data.mapping.targetConnector.config.split("/");
                            cleanName = cleanName[1] +"_" +cleanName[2];

                            this.data.mapping.targetConnector.url = "#connectors/edit/" + cleanName +"/";
                        } else {
                            this.data.mapping.targetConnector = {
                                "displayName" : $.t("templates.connector.managedObjectType"),
                                "url" : "#managed/edit/" +this.data.mapping.target.split("/")[1] +"/"
                            };
                        }

                        LinkQualifierUtil.checkLinkQualifier(this.data.mapping).then(_.bind(function(result){
                            if(this.data.mapping.recon){
                                this.data.recon = this.data.mapping.recon;
                                this.model.syncDetails = this.data.recon;

                                if (this.data.recon.ended) {
                                    if(this.data.recon.state === "CANCELED"){
                                        this.data.syncCanceled = true;
                                        this.data.syncLabel = $.t("templates.mapping.reconAnalysis.status");
                                        this.data.syncStatus = $.t("templates.mapping.lastSyncCanceled");
                                    } else {
                                        this.data.syncLabel = $.t("templates.mapping.reconAnalysis.completed");
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

                    }, this));

                }, this));
            } else {
                this.setSubmenu();

                this.parentRender();

                if(callback){
                    callback();
                }
            }
        },
        setSyncInProgress: function(){
            this.$el.find("#syncNowButton").hide().prop("disabled",true);
            this.$el.find("#stopSyncButton").show().prop("disabled", true);
        },
        syncNow: function(event) {
            var total,
                processed;

            if (event) {
                event.preventDefault();
            }

            this.data.reconAvailable = false;
            this.setSyncInProgress();

            reconDelegate.triggerRecon(this.currentMapping().name, true, _.bind(function (reconStatus) {

                if(reconStatus.progress.source.existing.total !== "?"  && reconStatus.stage === "ACTIVE_RECONCILING_SOURCE") {
                    processed = parseInt(reconStatus.progress.source.existing.processed, 10);
                    total = parseInt(reconStatus.progress.source.existing.total, 10);
                } else if(reconStatus.progress.target.existing.total !== "?" && reconStatus.stage === "ACTIVE_RECONCILING_TARGET") {
                    total = parseInt(reconStatus.progress.target.existing.total, 10);
                    processed = parseInt(reconStatus.progress.target.existing.processed, 10);
                } else {
                    total = 0;
                    processed = 0;
                }

                this.data.recon = reconStatus;
                this.$el.find("#stopSyncButton").prop("disabled",false);

                this.model.syncDetails = reconStatus;

                this.$el.find("#syncLabel").html($.t("templates.mapping.reconAnalysis.inProgress"));

                if(total !== 0 && processed !== 0) {
                    this.$el.find("#syncMessage").html(reconStatus.stageDescription + " - <span class='bold-message'>" + processed + "/" + total + "</span>");
                } else {
                    this.$el.find("#syncMessage").html(reconStatus.stageDescription);
                }

                if(this.$el.find("#syncStatusDetails:visible").length) {
                    this.loadReconDetails(this.model.syncDetails);
                }
            }, this)).then(_.bind(function(s){
                this.data.reconAvailable = false;
                this.data.recon = s;

                delete this.data.mapping;
                this.child.render([this.currentMapping().name]);
            }, this));
        },
        stopSync: function(e){
            e.preventDefault();

            this.$el.find("#syncMessage").text($.t("templates.mapping.stoppingSync"));
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
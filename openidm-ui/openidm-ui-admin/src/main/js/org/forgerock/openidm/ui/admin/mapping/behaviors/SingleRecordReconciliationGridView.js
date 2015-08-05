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

define("org/forgerock/openidm/ui/admin/mapping/behaviors/SingleRecordReconciliationGridView", [
    "jquery",
    "underscore",
    "org/forgerock/openidm/ui/admin/mapping/util/MappingAdminAbstractView",
    "org/forgerock/openidm/ui/admin/delegates/ReconDelegate",
    "org/forgerock/openidm/ui/common/delegates/SearchDelegate",
    "org/forgerock/commons/ui/common/main/Configuration",
    "org/forgerock/commons/ui/common/components/Messages"
], function ($, _,
             MappingAdminAbstractView,
             reconDelegate,
             searchDelegate,
             conf,
             messagesManager) {
    var SingleRecordReconciliationGridView = MappingAdminAbstractView.extend({
        template: "templates/admin/mapping/behaviors/SingleRecordReconciliationGridTemplate.html",
        data: {},
        element: "#testSyncGrid",
        noBaseTemplate: true,
        events: {
            "click #syncUser": "syncNow"
        },

        syncNow: function () {
            reconDelegate.triggerReconById(this.data.mapping.name, conf.globalData.testSyncSource._id).then(_.bind(function(recon) {
                this.loadData(recon[0]._id);
            }, this));
        },

        render: function (args) {

            this.data.recon = args.recon;
            this.data.sync = this.getSyncConfig();
            this.data.mapping = this.getCurrentMapping();
            this.data.mappingName = this.getMappingName();

            this.data.showChangedPropertyMessage = false;

            this.loadData();
        },

        loadData: function(newReconId) {
            var reconId = newReconId || this.data.recon._id,
                doLoad = _.bind(function() {
                    this.parentRender(_.bind(function () {
                        this.$el.find(".changed").tooltip({ position: { my: "left+5px bottom+15px", at: "left+5px bottom+15px"}});
                    }, this));
                }, this),
                targetProm = $.Deferred();

            if (conf.globalData.testSyncSource && this.data.mapping.properties.length){
                this.data.sampleSource_txt = conf.globalData.testSyncSource[this.data.mapping.properties[0].source];
                this.$el.parent().find(".sampleSourceAction").show();

                reconDelegate.getLastAuditForObjectId(reconId, "sourceObjectId", this.data.mapping.source + "/" + conf.globalData.testSyncSource._id).then(_.bind(function(audit) {
                    var targetObjectId;

                    if (audit.result.length) {
                        targetObjectId = audit.result[0].targetObjectId;
                        if (newReconId) {
                            if (audit.result[0].status === "SUCCESS") {
                                messagesManager.messages.addMessage({"message": $.t("templates.sync.testSync.singleRecordReconciliationComplete")});
                            } else {
                                if (audit.result[0].messageDetail && audit.result[0].messageDetail.message) {
                                    messagesManager.messages.addMessage({"type": "error", "message": audit.result[0].messageDetail.message});
                                } else if (audit.result[0].message && audit.result[0].message.length) {
                                    messagesManager.messages.addMessage({"type": "error", "message": audit.result[0].message});
                                } else {
                                    messagesManager.messages.addMessage({"type": "error", "message": $.t("config.messages.UserMessages.unknown")});
                                }
                            }
                        }

                        if (targetObjectId && targetObjectId.replace(this.data.mapping.target + "/", "") !== "null") {
                            searchDelegate.searchResults(this.data.mapping.target,["_id"],targetObjectId.replace(this.data.mapping.target + "/", ""),"eq").then(function(qry){
                                if(qry.length){
                                    targetProm.resolve(qry[0]);
                                }
                            });
                        } else {
                            targetProm.resolve(false);
                        }
                    } else {
                        targetProm.resolve(false);
                    }

                    targetProm.then(_.bind(function(target) {
                        this.data.showSampleSource = true;

                        this.data.propMap = _.map($.extend({},true,this.data.mapping.properties), _.bind(function(p, i) {
                            var targetBefore = "",
                                targetValue = target[p.target] || "",
                                changed = false;

                            if (newReconId) {
                                targetBefore = this.data.propMap[i].targetValue;

                                if (targetBefore !== targetValue) {
                                    changed = true;
                                    this.data.showChangedPropertyMessage = true;
                                }
                            }

                            return {
                                source : p.source,
                                sourceValue : conf.globalData.testSyncSource[p.source],
                                target : p.target,
                                targetValue : targetValue,
                                targetBefore : targetBefore,
                                changed: changed
                            };
                        }, this));

                        doLoad();
                    }, this));
                }, this));
            } else {
                this.data.showSampleSource = false;
                this.data.sampleSource_txt = "";
                this.$el.parent().find(".sampleSourceAction").hide();
                doLoad();
            }
        }
    });

    return new SingleRecordReconciliationGridView();
});

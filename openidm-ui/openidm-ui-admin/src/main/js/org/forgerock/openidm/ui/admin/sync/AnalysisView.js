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

/*global define, $, _, Handlebars */

define("org/forgerock/openidm/ui/admin/sync/AnalysisView", [
    "org/forgerock/openidm/ui/admin/util/AdminAbstractView",
    "org/forgerock/openidm/ui/admin/mapping/MappingBaseView",
    "org/forgerock/commons/ui/common/main/EventManager",
    "org/forgerock/commons/ui/common/util/Constants",
    "org/forgerock/openidm/ui/admin/delegates/ReconDelegate",
    "org/forgerock/commons/ui/common/util/DateUtil",
    "org/forgerock/openidm/ui/admin/delegates/SyncDelegate",
    "org/forgerock/openidm/ui/common/delegates/ConfigDelegate",
    "org/forgerock/openidm/ui/admin/util/MappingUtils"
], function(AdminAbstractView, MappingBaseView, eventManager, constants, reconDelegate, dateUtil, syncDelegate, configDelegate, mappingUtils) {
    var AnalysisView = AdminAbstractView.extend({
        template: "templates/admin/sync/AnalysisTemplate.html",
        element: "#analysisView",
        noBaseTemplate: true,
        events: {
            "change #situationSelection": "changeSituationView",
            "click #doSyncButton": "syncNow"
        },
        data: {},
        syncNow: function(e){
            e.preventDefault();
            $(e.target).prop('disabled',true);
            MappingBaseView.syncNow(e);
        },
        renderReconResults:  function (selectedSituation) {
                var _this = this,
                    recon = this.data.recon,
                    totalRecords = recon.statusSummary.FAILURE + recon.statusSummary.SUCCESS,
                    renderGrid = _.bind(function (container) {
                        var situations = selectedSituation || $("#situationSelection",this.$el).val().split(",");

                        if (!container.find("table.recon-grid").jqGrid('getGridParam', 'gridstate')) {
                            $("table.recon-grid", container).jqGrid({
                                jsonReader : {
                                    repeatitems: false,
                                    root: function (obj) { return obj.result[0].rows; },
                                    page: function (obj) { return obj.result[0].page; },
                                    total: function (obj) { return Math.ceil(parseInt(totalRecords, 10)/obj.result[0].limit); },
                                    records: function (obj) { return totalRecords; }
                                },
                                autowidth: true,
                                height: "100%",
                                url: "/openidm/endpoint/reconResults?_queryId=reconResults&source="+ this.mapping.source +
                                                "&target="+ this.mapping.target +
                                                "&sourceProps="+ this.data.sourceProps.join(",") +
                                                "&targetProps="+ this.data.targetProps.join(",") +
                                                "&reconId="+ recon._id +
                                                "&situations=" + situations.join(","),
                                datatype: "json",
                                rowNum: 10,
                                rowList: [10,20,50],
                                multiselect: false,
                                multiboxonly: false,
                                hoverrows: false,
                                loadError : function(xhr,st,err) { 
                                    if(xhr.status === 404){
                                        configDelegate.createEntity("endpoint/reconResults", {
                                                "context" : "endpoint/reconResults",
                                                "type" : "text/javascript",
                                                "file" : "ui/reconResults.js"
                                            }).then(function(){
                                                _.delay(function(){_this.render([_this.mapping.name]);}, 2000);
                                            });
                                    }
                                },
                                loadComplete: function(data){
                                    $(this).data("rowData",data.result[0].rows);
                                },
                                beforeRequest: function(){
                                    var params = $("table.recon-grid", container).jqGrid('getGridParam','postData');
                                    params.search = params._search;
                                    delete params._search;
                                    $("table.recon-grid", container).setGridParam({ postData: params });
                                },
                                colModel: [
                                    {"name": "sourceObjectDisplay", "jsonmap": "sourceObject", "label": $.t("templates.mapping.source"), "sortable": false,
                                        formatter : function (sourceObject) {
                                            var translatedObject;
                                            if (sourceObject) {
                                                translatedObject= syncDelegate.translateToTarget(sourceObject, _this.mapping);
                                                return  _this.buildObjectRepresentation(translatedObject, _this.data.sourceProps);
                                            } else if ($(this).attr("id") === "validGrid") {
                                                return "<span class='errorMessage'>" + $.t("salesforce.sync.analysisGridOutOfSync") + "</span>";
                                            } else {
                                                return "Not Found";
                                            }
                                        }
                                    },
                                    {"name": "targetObjectDisplay", "jsonmap": "targetObject","label": $.t("templates.mapping.target"), "sortable": false, 
                                        formatter : function (targetObject, opts, reconRecord) {
                                            if (targetObject) {
                                                return  _this.buildObjectRepresentation(targetObject, _this.data.targetProps);
                                            } else if (reconRecord.ambiguousTargetObjectIds.length) {
                                                return "Multiple Matches Found";
                                            } else {
                                                return "Not Found";
                                            }
                                        }
                                    }
                                ],
                                pager: "#" + $("div.recon-pager", container).attr("id")
                            });

                            $("table.recon-grid", container).jqGrid("filterToolbar", {
                                searchOnEnter: false
                            });

                        }
                    }, this);

                this.data.reconAvailable = true;
                this.data.allSituations = _.keys(this.data.recon.situationSummary).join(",");
                this.data.situationList = _.map(_.pairs(this.data.recon.situationSummary), function(item) { return { key: item[0], value: item[1] }; });
                
                if (recon.started) {
                    this.data.last_started = dateUtil.formatDate(recon.started,"MMMM dd, yyyy HH:mm");
                }

                this.parentRender(_.bind(function () {
                    if(selectedSituation){
                        $("#situationSelection",this.$el).val(selectedSituation.join(","));
                    }
                    renderGrid($("#analysisGridContainer", this.$el));
                }, this));

        },
        buildObjectRepresentation: function(objToRep, props){
            var propVals = [];
            
            _.each(props, _.bind(function(prop){
                var txt = "<span class='bold'>" + prop + "</span>: ";
                if(objToRep[prop]){
                    txt += Handlebars.Utils.escapeExpression(objToRep[prop]);
                }
                propVals.push(txt);
            }, this));
            
            return propVals.join("<br/>");
        },


        render: function(args, callback) {
            this.data.recon = MappingBaseView.data.recon;
            this.mapping = MappingBaseView.currentMapping();
            this.data.numRepresentativeProps = mappingUtils.numRepresentativeProps(this.mapping.name);
            this.data.sourceProps = _.pluck(this.mapping.properties,"source").slice(0,this.data.numRepresentativeProps);
            this.data.targetProps = _.pluck(this.mapping.properties,"target").slice(0,this.data.numRepresentativeProps);

            this.data.reconAvailable = false;
            this.parentRender(_.bind(function() {
                if(this.data.recon && !MappingBaseView.data.syncCanceled){
                    this.renderReconResults();
                }
                if(callback) {
                    callback();
                }
            }, this));
                
        },
        changeSituationView: function(e){
            e.preventDefault();
            this.renderReconResults($(e.target).val().split(","));
        }
    });

    return new AnalysisView();
});

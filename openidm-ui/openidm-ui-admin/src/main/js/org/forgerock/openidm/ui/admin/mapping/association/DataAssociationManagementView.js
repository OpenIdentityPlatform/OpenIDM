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

/*global define, window */

define("org/forgerock/openidm/ui/admin/mapping/association/DataAssociationManagementView", [
    "jquery",
    "underscore",
    "org/forgerock/openidm/ui/admin/mapping/util/MappingAdminAbstractView",
    "org/forgerock/commons/ui/common/main/Configuration",
    "org/forgerock/commons/ui/common/main/EventManager",
    "org/forgerock/commons/ui/common/util/Constants",
    "org/forgerock/openidm/ui/admin/delegates/ReconDelegate",
    "org/forgerock/commons/ui/common/util/DateUtil",
    "org/forgerock/openidm/ui/admin/delegates/SyncDelegate",
    "org/forgerock/openidm/ui/common/delegates/ConfigDelegate",
    "org/forgerock/openidm/ui/admin/mapping/util/MappingUtils",
    "org/forgerock/openidm/ui/admin/mapping/association/dataAssociationManagement/ChangeAssociationDialog",
    "org/forgerock/openidm/ui/admin/mapping/association/dataAssociationManagement/TestSyncDialog",
    "jqgrid"
], function($, _,
            MappingAdminAbstractView,
            conf,
            eventManager,
            constants,
            reconDelegate,
            dateUtil,
            syncDelegate,
            configDelegate,
            mappingUtils,
            changeAssociationDialog,
            TestSyncDialog) {

    var DataAssociationManagementView = MappingAdminAbstractView.extend({
        template: "templates/admin/mapping/association/DataAssociationManagementTemplate.html",
        element: "#analysisView",
        noBaseTemplate: true,
        events: {
            "change #situationSelection": "changeSituationView",
            "click #doSyncButton": "syncNow",
            "click #changeAssociation": "changeAssociation",
            "click #singleRecordSync": "singleRecordSync"
        },
        data: {},

        render: function(args, callback) {
            this.data.recon = this.getRecon();
            this.mapping = this.getCurrentMapping();
            this.mappingSync = this.getSyncNow();
            this.data.numRepresentativeProps = this.getNumRepresentativeProps();
            this.data.sourceProps = _.pluck(this.mapping.properties,"source").slice(0,this.data.numRepresentativeProps);
            this.data.targetProps = _.pluck(this.mapping.properties,"target").slice(0,this.data.numRepresentativeProps);
            this.data.hideSingleRecordReconButton = mappingUtils.readOnlySituationalPolicy(this.mapping.policies);

            this.data.reconAvailable = false;
            this.parentRender(_.bind(function() {
                if(this.data.recon && !this.getSyncCancelled()){
                    this.renderReconResults(null, callback);

                    $(window).resize(function () {
                        if(this.$el) {
                            this.$el.find("#analysisGridContainer .recon-grid").setGridWidth(this.$el.find("#analysisGridContainer").width());
                        }
                    });

                } else if(callback) {
                    callback();
                }

            }, this));
        },

        syncNow: function(e) {
            e.preventDefault();
            $(e.target).closest("button").prop('disabled',true);
            this.mappingSync(e);
        },

        singleRecordSync: function(e) {
            conf.globalData.testSyncSource = this.getSelectedRow().sourceObject;

            e.preventDefault();

            TestSyncDialog.render({
                recon: this.getRecon()
            });
        },

        changeAssociation: function(e) {
            var args,
                selectedRow = this.getSelectedRow(),
                sourceObj = selectedRow.sourceObject,
                targetObj = selectedRow.targetObject,
                translateObj = _.bind(function(obj, isSource) {
                    if (isSource) {
                        obj = syncDelegate.translateToTarget(obj, this.mapping);
                    }
                    return mappingUtils.buildObjectRepresentation(obj, this.data.targetProps);
                }, this);

            e.preventDefault();

            args = {
                selectedLinkQualifier: selectedRow.linkQualifier,
                sourceObj: sourceObj,
                sourceObjRep : translateObj(_.pick(sourceObj, this.data.sourceProps), true),
                targetObj: targetObj,
                targetObjRep: translateObj(_.pick(targetObj, this.data.targetProps)),
                targetProps: $.extend({},this.data.targetProps),
                ambiguousTargetObjectIds: selectedRow.ambiguousTargetObjectIds,
                recon: $.extend({}, this.data.recon),
                linkQualifiers : this.mapping.linkQualifiers,
                reloadAnalysisGrid: _.bind(function(){
                    this.renderReconResults(this.$el.find("#situationSelection").val().split(","), null);
                }, this)
            };

            changeAssociationDialog.render(args);
        },

        getSelectedRow: function () {
            var grid = this.$el.find(".recon-grid"),
                selRow = grid.jqGrid("getGridParam", "selrow");

            return grid.data("rowData")[selRow -1];
        },

        checkNewLinks: function(rows) {
            var count = this.data.newLinkIds.length,
                warningText;

            _.chain(rows)
                .map(_.bind(function(row) {
                    if(row.sourceObject) {
                        row._id = row.sourceObject._id;
                    } else {
                        row._id = "";
                    }
                }, this));

            if (count) {
                if (count === 1) {
                    warningText = $.t("templates.mapping.analysis.oneNewLinkWarning");
                } else {
                    warningText = $.t("templates.mapping.analysis.newLinksWarning", { "count": count});
                }
                $('#newLinksWarning').show().find("span#newLinksWarningText").html(warningText);
            } else {
                $('#newLinksWarning').hide();
            }

            return rows;
        },

        renderReconResults:  function (selectedSituation, callback) {
            var _this = this,
                recon = this.data.recon,
                totalRecords = recon.statusSummary.FAILURE + recon.statusSummary.SUCCESS,
                renderGrid = _.bind(function (container, callback) {
                    var situations = selectedSituation || $("#situationSelection",this.$el).val().split(",");

                    if (!container.find("table.recon-grid").jqGrid('getGridParam', 'gridstate')) {
                        $("table.recon-grid", container).jqGrid({
                            jsonReader : {
                                repeatitems: false,
                                root: function (obj) { return _this.checkNewLinks(obj.result[0].rows); },
                                page: function (obj) { return obj.result[0].page; },
                                total: function (obj) { return Math.ceil(parseInt(totalRecords, 10)/obj.result[0].limit); },
                                records: function (obj) { return totalRecords; }
                            },
                            autowidth:true,
                            shrinkToFit:true,
                            height: "100%",
                            url: "/openidm/endpoint/reconResults?_queryId=reconResults&source="+ this.mapping.source +
                            "&target="+ this.mapping.target +
                            "&sourceProps="+ this.data.sourceProps.join(",") +
                            "&targetProps="+ this.data.targetProps.join(",") +
                            "&reconId="+ recon._id +
                            "&situations=" + situations.join(",") +
                            "&mapping=" + this.mapping.name,
                            datatype: "json",
                            rowNum: 10,
                            rowList: [10,20,50],
                            multiselect: false,
                            multiboxonly: true,
                            hoverrows: true,
                            altRows:true,
                            altclass: "analysisGridRowAlt",
                            loadError : function(xhr,st,err) {
                                if (xhr.status === 404) {
                                    configDelegate.createEntity("endpoint/reconResults", {
                                        "context" : "endpoint/reconResults",
                                        "type" : "text/javascript",
                                        "file" : "ui/reconResults.js"
                                    }).then(function() {
                                        _.delay(function() {_this.render([_this.mapping.name]);}, 2000);
                                    });
                                }
                            },
                            loadComplete: function(data) {
                                $(this).data("rowData",data.result[0].rows);
                                _this.$el.find("td.ui-search-input input").attr("placeholder",$.t("templates.mapping.analysis.enterSearchTerms"));

                                $("table.recon-grid", container).find(".newLinkWarning").popover({
                                    content: function () { return $(this).attr("data-original-title");},
                                    trigger:'hover',
                                    placement:'top',
                                    container: 'body',
                                    html: 'true',
                                    template: '<div class="popover popover-info" role="tooltip"><div class="popover-content"></div></div>'
                                });

                                _this.$el.find(".actionButton").prop('disabled',true);

                                if (callback) {
                                    callback();
                                }
                            },
                            beforeRequest: function() {
                                var params = $("table.recon-grid", container).jqGrid('getGridParam','postData');
                                params.search = params._search;
                                delete params._search;
                                $("table.recon-grid", container).setGridParam({ postData: params });
                                if (params.search) {
                                    $("div.recon-pager div", container).hide();
                                } else {
                                    $("div.recon-pager div", container).show();
                                }
                            },
                            onSelectRow: function(id) {
                                var rowData = $("table.recon-grid", container).jqGrid("getRowData", id),
                                    disableButton = !rowData.sourceObject.length || _.contains(_this.data.newLinkIds,rowData._id);

                                _this.$el.find(".actionButton").prop('disabled',disableButton);
                            },
                            colModel: [
                                {"name":"sourceObject", "hidden": true},
                                {"name":"_id", "hidden": true},
                                {"name":"hasLink", "hidden": true},
                                {"name": "sourceObjectDisplay", "jsonmap": "sourceObject", "label": $.t("templates.mapping.source"), "sortable": false,
                                    formatter : function (sourceObject, opts, reconRecord) {
                                        var translatedObject,
                                            txt;

                                        if (sourceObject) {
                                            translatedObject= syncDelegate.translateToTarget(sourceObject, _this.mapping);
                                            txt =  mappingUtils.buildObjectRepresentation(translatedObject, _this.data.targetProps);

                                            if (_.contains(_this.data.newLinkIds,sourceObject._id)) {
                                                txt = "<span class='newLinkWarning errorMessage fa fa-exclamation-triangle' title='" + $.t("templates.mapping.analysis.newLinkCreated") + "'></span> " + txt;
                                            }

                                            return txt;
                                        } else {
                                            return "Not Found";
                                        }
                                    }
                                },
                                {"name":"linkQualifier", "label": $.t("templates.mapping.linkQualifier"), search: false, "sortable": false},
                                {"name": "targetObjectDisplay", "jsonmap": "targetObject","label": $.t("templates.mapping.target"), "sortable": false,
                                    formatter : function (targetObject, opts, reconRecord) {
                                        if (reconRecord.sourceObject && _.contains(_this.data.newLinkIds, reconRecord.sourceObject._id)) {
                                            return mappingUtils.buildObjectRepresentation(_.filter(_this.data.newLinks, function(link){
                                                return link.sourceObjectId.replace(_this.mapping.source + "/","") === reconRecord.sourceObject._id;
                                            })[0].targetObject, _this.data.targetProps);
                                        } else if (targetObject) {
                                            return  mappingUtils.buildObjectRepresentation(targetObject, _this.data.targetProps);
                                        } else if (reconRecord.ambiguousTargetObjectIds && reconRecord.ambiguousTargetObjectIds.length) {
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

            if (selectedSituation) {
                totalRecords = recon.situationSummary[selectedSituation];
            }

            this.data.reconAvailable = true;
            this.data.allSituations = _.keys(this.data.recon.situationSummary).join(",");
            this.data.situationList = _.map(_.pairs(this.data.recon.situationSummary), function(item) { return { key: item[0], value: item[1] }; });

            if (recon.started) {
                this.data.last_started = dateUtil.formatDate(recon.started,"MMMM dd, yyyy HH:mm");
            }

            reconDelegate.getNewLinksFromRecon(this.data.recon._id, this.data.recon.ended).then(_.bind(function(newLinks) {
                this.data.newLinks = newLinks;
                this.data.newLinkIds = _.chain(newLinks)
                    .pluck("sourceObjectId")
                    .map(_.bind(function(sObjId) {
                        return sObjId.replace(this.mapping.source + "/","");
                    }, this))
                    .value();
                this.parentRender(_.bind(function () {
                    if(selectedSituation){
                        $("#situationSelection",this.$el).val(selectedSituation.join(","));
                    }
                    renderGrid($("#analysisGridContainer"), callback);
                }, this));
            }, this));
        },

        changeSituationView: function(e) {
            e.preventDefault();
            this.renderReconResults($(e.target).val().split(","));
        }
    });

    return new DataAssociationManagementView();
});

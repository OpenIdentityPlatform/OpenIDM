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
 * Copyright 2015 ForgeRock AS.
 */

/*global define */

define("org/forgerock/openidm/ui/admin/mapping/association/dataAssociationManagement/ChangeAssociationDialog", [
    "jquery",
    "underscore",
    "org/forgerock/openidm/ui/admin/mapping/util/MappingAdminAbstractView",
    "org/forgerock/commons/ui/common/main/Configuration",
    "org/forgerock/commons/ui/common/util/UIUtils",
    "org/forgerock/openidm/ui/common/delegates/SearchDelegate",
    "org/forgerock/openidm/ui/admin/mapping/util/MappingUtils",
    "org/forgerock/openidm/ui/admin/delegates/SyncDelegate",
    "bootstrap-dialog",
    "org/forgerock/openidm/ui/admin/util/LinkQualifierUtils"
], function($, _,
            MappingAdminAbstractView,
            conf,
            uiUtils,
            searchDelegate,
            mappingUtils,
            syncDelegate,
            BootstrapDialog,
            LinkQualifierUtil) {

    var ChangeAssociationDialog = MappingAdminAbstractView.extend({
        template: "templates/admin/mapping/association/dataAssociationManagement/ChangeAssociationDialogTemplate.html",
        el: "#dialogs",
        events: {
            "click #targetSearchBtn": "searchTarget",
            "click #search_results li": "selectSearchResult",
            "click #linkObjectBtn": "linkObject"
        },

        selectSearchResult: function(e) {
            e.preventDefault();
            this.$el.find(".readyToLink").removeClass("readyToLink");
            $(e.target).closest("li").addClass("readyToLink").find(":radio").prop("checked",true);
            this.$el.find("#linkObjectBtn").show().prependTo($(e.target).closest("li"));
        },

        searchTarget: function (e) {
            e.preventDefault();
            var searchCriteria = this.$el.find("#targetSearchInput").val(),
                dialogData = this.data;

            dialogData.searching = true;
            dialogData.searchCriteria = searchCriteria;

            searchDelegate.searchResults(this.getCurrentMapping().target, this.data.targetProps, searchCriteria).then(_.bind(function(results) {
                dialogData.results = _.chain(results)
                    .map(_.bind(function(result) {
                        return {
                            _id: result._id,
                            objRep: mappingUtils.buildObjectRepresentation(result, this.data.targetProps)
                        };
                    },this))
                    .value();
                this.reloadData(dialogData);
            },this), _.bind(function () {
                dialogData.results = [];
                this.reloadData(dialogData);
            }, this));
        },

        linkObject: function(e) {
            var sourceId = this.$el.find("[name=sourceId]").val(),
                linkId = this.$el.find("[name=found]:checked").val(),
                mapping = this.getMappingName(),
                linkType = this.$el.find("#linkTypeSelect").val();

            e.preventDefault();

            syncDelegate.deleteLinks(mapping, sourceId, "firstId").then(_.bind(function() {
                syncDelegate.performAction(this.data.recon._id, mapping, "LINK", sourceId, linkId, linkType).then(_.bind(function() {
                    this.data.reloadAnalysisGrid();
                    this.currentDialog.close();
                }, this));
            }, this));
        },

        getAmbiguousMatches: function() {
            var ids = this.data.ambiguousTargetObjectIds.split(", "),
                prom = $.Deferred();

            _.each(ids,_.bind(function(id, i) {
                searchDelegate.searchResults(this.getCurrentMapping().target, ["_id"], id, "eq").then(_.bind(function(result) {
                    this.data.results.push({
                        _id: result[0]._id,
                        objRep: mappingUtils.buildObjectRepresentation(result[0], this.data.targetProps)
                    });
                    if (i === ids.length - 1) {
                        prom.resolve();
                    }
                }, this));
            },this));

            return prom;
        },

        reloadData: function(data) {
            uiUtils.renderTemplate("templates/admin/mapping/association/dataAssociationManagement/ChangeAssociationDialogTemplate.html", $("#changeAssociationDialog"), data);
            if (data.searchCriteria) {
                this.$el.find("#targetSearchInput").focus().val(data.searchCriteria);
                this.$el.find("#linkTypeSelect").val(this.data.selectedLinkQualifier);
            }
        },

        render: function(args, callback) {
            var _this = this;

            this.dialogContent = $('<div id="changeAssociationDialog"></div>');
            this.setElement(this.dialogContent);
            $('#dialogs').append(this.dialogContent);

            this.currentDialog = new BootstrapDialog({
                title: $.t("templates.mapping.analysis.changeAssociation"),
                type: BootstrapDialog.TYPE_DEFAULT,
                message: this.dialogContent,
                onshown : _.bind(function() {
                    uiUtils.renderTemplate(
                        this.template,
                        this.$el,
                        _.extend({}, conf.globalData, this.data),
                        _.bind(function() {
                            this.$el.find("#linkTypeSelect").val(args.selectedLinkQualifier);

                            if (callback) {
                                callback();
                            }
                        }, this),
                        "replace");
                }, _this)
            });

            _.extend(this.data, args);

            this.data.results = [];

            if (this.data.targetObj !== null && this.data.targetObj._id !== undefined){
                this.data.results.push({ _id: this.data.targetObj._id , objRep: this.data.targetObjRep });
            }

            if (this.data.ambiguousTargetObjectIds.length) {
                this.getAmbiguousMatches();
            }

            this.data.linkQualifiers = LinkQualifierUtil.getLinkQualifier(this.getMappingName());

            this.currentDialog.realize();
            this.currentDialog.open();
        }
    });

    return new ChangeAssociationDialog();
});

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
 * Copyright 2015-2016 ForgeRock AS.
 * Portions copyright 2026 3A Systems, LLC.
 */

define([
    "jquery",
    "underscore",
    "org/forgerock/openidm/ui/admin/mapping/util/MappingAdminAbstractView",
    "org/forgerock/openidm/ui/common/delegates/SearchDelegate",
    "org/forgerock/commons/ui/common/main/Configuration",
    "org/forgerock/openidm/ui/admin/mapping/util/MappingUtils",
    "org/forgerock/openidm/ui/admin/mapping/behaviors/SingleRecordReconciliationGridView"
], function ($, _,
             MappingAdminAbstractView,
             searchDelegate,
             conf,
             mappingUtils,
             SingleRecordReconciliationGridView) {

    var SingleRecordReconciliationView = MappingAdminAbstractView.extend({
        template: "templates/admin/mapping/behaviors/SingleRecordReconciliationTemplate.html",
        data: {},
        element: "#testSyncView",
        noBaseTemplate: true,
        events: {
            "click #refreshSourceRecord": "refreshSourceRecord",
            "click #removeSourceRecord": "removeSourceRecord"
        },
        refreshSourceRecord: function(e){
            e.preventDefault();

            if(conf.globalData.testSyncSource){
                searchDelegate.searchResults(this.data.mapping.source,["_id"],conf.globalData.testSyncSource._id,"eq").then(_.bind(function(qry){
                    conf.globalData.testSyncSource = qry[0];
                    this.data.showChangedPropertyMessage = false;
                    SingleRecordReconciliationGridView.loadData();
                }, this));
            }
        },
        removeSourceRecord: function(e){
            e.preventDefault();

            if(conf.globalData.testSyncSource){
                delete conf.globalData.testSyncSource;
                this.data.showChangedPropertyMessage = false;
                this.data.showSampleSource = false;
                $("#findSampleSource",this.$el).val("");
                SingleRecordReconciliationGridView.loadData();
            }
        },
        render: function (args, callback) {
            this.data.recon = args.recon;
            this.data.mapping = this.getCurrentMapping();

            this.parentRender(_.bind(function () {
                if (this.data.recon) {
                    this.setupSearch();
                    SingleRecordReconciliationGridView.render(this.data);
                } else {
                    this.$el.closest("#singleRecordRecon").hide();
                }
            }, this));

            if (callback) {
                callback();
            }
        },

        setupSearch: function(){
            // Only keep properties that actually define a "source"; otherwise the
            // first representative property may be undefined and break the sample
            // search query (empty _sortKeys= => HTTP 500). See discussion #186.
            // Use native Array filter/slice (lodash 3 _.first ignores the count arg).
            var autocompleteProps = _.pluck(this.data.mapping.properties, "source")
                .filter(function(source){ return !!source; })
                .slice(0, this.getNumRepresentativeProps());

            mappingUtils.setupSampleSearch($("#findSampleSource",this.$el), this.data.mapping, autocompleteProps, _.bind(function(item) {
                conf.globalData.testSyncSource = item;
                if (this.data.propMap) {
                    this.data.showChangedPropertyMessage = false;
                    delete this.data.propMap;
                }

                SingleRecordReconciliationGridView.loadData();
            }, this));
        }
    });

    return new SingleRecordReconciliationView();
});

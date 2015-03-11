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

/*global define, $, _, Handlebars*/

define("org/forgerock/openidm/ui/admin/sync/TestSyncView", [
    "org/forgerock/commons/ui/common/main/AbstractView",
    "org/forgerock/openidm/ui/admin/delegates/SearchDelegate",
    "org/forgerock/commons/ui/common/main/Configuration",
    "org/forgerock/openidm/ui/admin/util/MappingUtils",
    "org/forgerock/openidm/ui/admin/sync/TestSyncGridView"
], function (AbstractView, searchDelegate, conf, mappingUtils, TestSyncGridView) {
    var TestSyncView = AbstractView.extend({
        template: "templates/admin/sync/TestSyncTemplate.html",
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
                    TestSyncGridView.loadData();
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
                TestSyncGridView.loadData();
            }
        },
        render: function (args, callback) {
            
            this.data = _.extend(this.data,args);
            
            this.parentRender(_.bind(function () {
                if(this.data.recon){
                    this.setupSearch();
                    TestSyncGridView.render(this.data);
                } else {
                    this.$el.closest("#singleRecordRecon").hide();
                }
            }, this));
        },
        setupSearch: function(){
            var _this = this,
                autocompleteProps = _.pluck(this.data.mapping.properties,"source").slice(0,mappingUtils.numRepresentativeProps(this.data.mapping.name));
            
            mappingUtils.setupSampleSearch($("#findSampleSource",this.$el),this.data.mapping,autocompleteProps, _.bind(function(item){
                conf.globalData.testSyncSource = item;
                if(this.data.propMap){
                    this.data.showChangedPropertyMessage = false;
                    delete this.data.propMap;
                }
                
                TestSyncGridView.loadData();
            }, this));
        }
    }); 
    
    return new TestSyncView();
});
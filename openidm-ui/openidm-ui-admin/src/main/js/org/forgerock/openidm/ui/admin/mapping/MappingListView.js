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
    "org/forgerock/commons/ui/common/util/Constants"
    ], function(AdminAbstractView, eventManager, configDelegate, constants) {

    var MappingListView = AdminAbstractView.extend({
        template: "templates/admin/mapping/MappingListTemplate.html",
        events: {
            "click #addMapping": "addMapping"
        },
        data: {},
        render: function(args, callback) {
            var errorCallback = _.bind(function(){
                    configDelegate.createEntity("sync", { mappings: [] }).then(_.bind(function(){
                        this.render(args, callback);
                    },this));
                },this),
                syncConfig = configDelegate.readEntity("sync",null, errorCallback);
            
            syncConfig.then(_.bind(function(sync, currConnections, managed, repo){
                this.data.syncConfig = sync;
                this.parentRender(_.bind(function () {
                    this.buildMappingsGrid();
                    
                    if(callback){
                        callback();
                    }
                }, this));
            },this));
        },
        buildMappingsGrid: function(){
            var grid_id = 'MappingListGrid',
                pager_id = grid_id + '_pager',
                cols = [
                        {
                            "name": "name",
                            "label": $.t("common.user.name"),
                            "key": true
                        },
                        {
                            "name": "source",
                            "label": $.t("templates.mapping.source")
                        },
                        {
                            "name": "target",
                            "label": $.t("templates.mapping.target")
                        }
                ];
            
            $('#' + grid_id).jqGrid('GridUnload');
            $('#' + pager_id).remove();
            
            $('#' + grid_id).jqGrid( {
                data: this.data.syncConfig.mappings,
                datatype: "local",
                height: 'auto',
                width: 960,
                rowNum: 10,
                rowList: [10,20,50],
                pager: pager_id,
                hidegrid: false,
                colModel: cols,
                onSelectRow: _.bind(function(rowid){
                    this.editMapping(rowid);
                }, this),
                loadComplete: _.bind(function(data){
                    if(data.rows.length === 0){
                        this.$el.find("#noMappingsDefined").next().hide();
                        this.$el.find("#noMappingsDefined").show();
                    }
                }, this)
            });
        },
        editMapping: function(mapping) {
            if(mapping) {
                eventManager.sendEvent(constants.ROUTE_REQUEST, {routeName: "editMappingView", args: [mapping]});
            }
        },
        addMapping: function(e) {
            eventManager.sendEvent(constants.ROUTE_REQUEST, {routeName: "addMappingView"});
        }
    });

    return new MappingListView();
});

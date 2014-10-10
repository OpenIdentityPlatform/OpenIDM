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
    "org/forgerock/commons/ui/common/components/Navigation"
    ], function(AdminAbstractView, eventManager, validatorsManager, configDelegate, UIUtils, constants, browserStorageDelegate, nav) {

    var MappingBaseView = AdminAbstractView.extend({
        template: "templates/admin/mapping/MappingTemplate.html",
        events: {
            "click #updateMappingButton": "saveMapping",
            "onValidate": "onValidate",
            "click #clearChanges": "clearChanges",
            "click .mapping-config-body": "mappingList"
        },
        mappingList: function(e){
            e.preventDefault();
            
            if(!$(e.target).closest("button").hasClass("icon-button")){
                eventManager.sendEvent(constants.ROUTE_REQUEST, {routeName: "mappingListView"});
            }
        },
        data: {},
        setSubmenu: function(){
            _.each(nav.configuration.links.admin.urls.mapping.urls, _.bind(function(val){ 
                var url = val.url.split("/")[0];
                
                val.url = url + "/" + this.currentMapping().name + "/";
            },this));
            nav.reload();
        },
        setCurrentMapping: function(mappingObj){
            browserStorageDelegate.set('currentMapping',mappingObj);
            return mappingObj;
        },
        currentMapping: function(){
            return browserStorageDelegate.get('currentMapping');
        },
        checkChanges: function () {
            var currentProperties = this.currentMapping().properties,
                changedProperties = browserStorageDelegate.get(this.currentMapping().name + "_Properties") || currentProperties,
                changesPending = !_.isEqual(currentProperties,changedProperties);
            
            if(changesPending) {
                this.$el.find("#updateMappingButton").prop('disabled', false);
                this.$el.find("#clearChanges").prop('disabled', false);
                this.$el.find(".changesPending").show();
            }
            else {
                this.$el.find("#updateMappingButton").prop('disabled', true);
                this.$el.find("#clearChanges").prop('disabled', true);
                this.$el.find(".changesPending").hide();
            }
            
        },
        syncType: function(type) {
            var tempType = type.split("/");

            return (tempType[0] === "managed") ? "managed" : tempType[1];
        },
        render: function(args, child, callback) {
            var prom = $.Deferred(),
                syncConfig = configDelegate.readEntity("sync");
            
            this.child = child;
            
            syncConfig.then(_.bind(function(sync){
                this.data.syncConfig = sync;
                this.data.mapping = _.filter(sync.mappings,function(m){ return m.name === args[0];})[0];
                this.setCurrentMapping($.extend({},true,this.data.mapping));
                
                this.data.targetType = this.syncType(this.data.mapping.target);
                this.data.targetIcon = (this.data.targetType === "managed") ? "fa-database" : "fa-paper-plane";

                this.data.sourceType = this.syncType(this.data.mapping.source);
                this.data.sourceIcon = (this.data.sourceType === "managed") ? "fa-database" : "fa-paper-plane";
                
                this.parentRender(_.bind(function () {
                    this.setSubmenu();
                    this.checkChanges();
                    
                    prom.resolve();
                    if(callback){
                        callback();
                    }
                }, this));
            }, this));
            
            return prom;
        },
        saveMapping: function(event) {
            event.preventDefault();
            
            var syncMappings;

            syncMappings = _.map(this.data.syncConfig.mappings,_.bind(function(m){
                var propertyChanges = browserStorageDelegate.get(this.currentMapping().name + "_Properties");
                if(m.name === this.currentMapping().name){
                    if(propertyChanges){
                        m.properties = propertyChanges;
                        browserStorageDelegate.remove(this.currentMapping().name + "_Properties");
                    }
                }
                return m;
            }, this));
            
            configDelegate.updateEntity("sync", {"mappings" : syncMappings}).then(_.bind(function(){
                this.child.render([this.data.mapping.name]);
                eventManager.sendEvent(constants.EVENT_DISPLAY_MESSAGE_REQUEST, "mappingSaveSuccess");
            }, this));
        },
        clearChanges: function(e){
            e.preventDefault();
            browserStorageDelegate.remove(this.currentMapping().name + "_Properties");
            this.child.render([this.currentMapping().name]);
        }
    });

    return new MappingBaseView();
});

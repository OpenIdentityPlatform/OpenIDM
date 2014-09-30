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

define("org/forgerock/openidm/ui/admin/mapping/EditMappingView", [
    "org/forgerock/openidm/ui/admin/util/AdminAbstractView",
    "org/forgerock/commons/ui/common/main/EventManager",
    "org/forgerock/commons/ui/common/main/ValidatorsManager",
    "org/forgerock/openidm/ui/common/delegates/ConfigDelegate",
    "org/forgerock/commons/ui/common/util/UIUtils",
    "org/forgerock/commons/ui/common/util/Constants",
    "org/forgerock/openidm/ui/admin/mapping/PropertiesView",
    "org/forgerock/openidm/ui/admin/delegates/BrowserStorageDelegate",
    "org/forgerock/openidm/ui/admin/delegates/ConnectorDelegate"
    ], function(AdminAbstractView, eventManager, validatorsManager, configDelegate, UIUtils, constants, PropertiesView, browserStorageDelegate, connectorDelegate) {

    var EditMappingView = AdminAbstractView.extend({
        template: "templates/admin/mapping/EditMappingTemplate.html",
        events: {
            "click input[type=submit]": "formSubmit",
            "onValidate": "onValidate",
            "click #deleteMapping": "deleteMapping",
            "click #clearChanges": "clearChanges"
        },
        data: {},
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
                this.$el.find("input[type=submit]").prop('disabled', false);
                this.$el.find("#clearChanges").prop('disabled', false);
                this.$el.find(".changesPending").show();
            }
            else {
                this.$el.find("input[type=submit]").prop('disabled', true);
                this.$el.find("#clearChanges").prop('disabled', true);
                this.$el.find(".changesPending").hide();
            }
            
        },
        render: function(args, callback) {
            var syncConfig = configDelegate.readEntity("sync");
            
                syncConfig.then(_.bind(function(sync){
                    this.data.syncConfig = sync;
                    this.data.mapping = _.filter(sync.mappings,function(m){ return m.name === args[0];})[0];
                    this.setCurrentMapping($.extend({},true,this.data.mapping));
                    this.buildAvailableObjectsMap().then(_.bind(function(availableObjects){
                        browserStorageDelegate.set(this.currentMapping().name + "_AvailableObjects", availableObjects);
                        this.data.pageTitle = $.t("templates.mapping.editMapping",{name: this.data.mapping.name});
                        this.parentRender(_.bind(function () {
                            $('#mappingTabs').tabs();
                            PropertiesView.render(this);
                            this.checkChanges();
                            if(callback){
                                callback();
                            }
                        }, this));
                    }, this));
                }, this));
        },
        formSubmit: function(event) {
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
                this.render([this.data.mapping.name]);
                eventManager.sendEvent(constants.EVENT_DISPLAY_MESSAGE_REQUEST, "mappingSaveSuccess");
            }, this));
        },
        deleteMapping: function(e){
            e.preventDefault();
            
            UIUtils.jqConfirm($.t("templates.mapping.confirmDeleteMapping",{ mappingName: this.currentMapping().name}), _.bind(function(){
                var syncMappings = _.reject(this.data.syncConfig.mappings,_.bind(function(m){ return m.name === this.currentMapping().name; }, this));
                
                configDelegate.updateEntity("sync", {"mappings" : syncMappings}).then(function(){
                    eventManager.sendEvent(constants.ROUTE_REQUEST, {routeName: "mappingListView"});
                    eventManager.sendEvent(constants.EVENT_DISPLAY_MESSAGE_REQUEST, "mappingDeleted");
                });
            },this));
        },
        clearChanges: function(e){
            e.preventDefault();
            browserStorageDelegate.remove(this.currentMapping().name + "_Properties");
            this.render([this.currentMapping().name]);
        },
        buildAvailableObjectsMap: function(){
            var sourceProm = $.Deferred(),
                targetProm = $.Deferred(),
                currentConnectors = connectorDelegate.currentConnectors(),
                managedConfig = configDelegate.readEntity("managed");
            
            return $.when(currentConnectors, managedConfig).then(_.bind(function(currConnectors, managed){
                
                _.map(managed.objects,_.bind(function(o){
                    if(this.currentMapping().source === "managed/" + o.name){
                        sourceProm.resolve({ name: o.name, fullName: "managed/" + o.name });
                    }
                    if(this.currentMapping().target === "managed/" + o.name){
                        targetProm.resolve({ name: o.name, fullName: "managed/" + o.name });
                    }
                }, this));

                if(!(sourceProm.state() === "resolved" && targetProm.state() === "resolved")){
                    _(currConnectors[0])
                        .each(function(connector){
                            _.each(connector.objectTypes, function(objType){
                                var objTypeMap = {
                                        name: connector.name,
                                        fullName: "system/" + connector.name + "/" + objType
                                    },
                                    getProps = function(){
                                        return configDelegate.readEntity(connector.config.replace("config/", "")).then(function(connector){
                                            return _.keys(connector.objectTypes[objType].properties).sort();
                                        });
                                    };
                                
                                if(this.currentMapping().source === objTypeMap.fullName){
                                    getProps().then(function(props){
                                        objTypeMap.properties = props;
                                        sourceProm.resolve(objTypeMap);
                                    });
                                }
                                if(this.currentMapping().target === objTypeMap.fullName){
                                    getProps().then(function(props){
                                        objTypeMap.properties = props;
                                        targetProm.resolve(objTypeMap);
                                    });
                                }
                            }, this);
                        }, this);
                }
                
                return $.when(sourceProm,targetProm).then(function(source,target){
                    return { source: source, target: target};
                });
                
            }, this));
        }
    });

    return new EditMappingView();
});

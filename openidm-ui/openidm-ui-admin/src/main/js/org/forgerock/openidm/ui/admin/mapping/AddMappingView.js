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

/*global define, $, _, Handlebars, form2js, localStorage */

define("org/forgerock/openidm/ui/admin/mapping/AddMappingView", [
    "org/forgerock/openidm/ui/admin/util/AdminAbstractView",
    "org/forgerock/commons/ui/common/main/EventManager",
    "org/forgerock/commons/ui/common/main/ValidatorsManager",
    "org/forgerock/openidm/ui/common/delegates/ConfigDelegate",
    "org/forgerock/commons/ui/common/util/UIUtils",
    "org/forgerock/commons/ui/common/util/Constants",
    "org/forgerock/openidm/ui/admin/delegates/ConnectorDelegate"
    ], function(AdminAbstractView, eventManager, validatorsManager, configDelegate, UIUtils, constants, connectorDelegate) {

    var AddMappingView = AdminAbstractView.extend({
        template: "templates/admin/mapping/AddMappingTemplate.html",
        events: {
            "click input[type=submit]": "formSubmit",
            "onValidate": "onValidate",
            "customValidate": "customValidate"
        },
        data: {},
        customValidate: function () {
            var mapping = _.extend(this.data.mapping,form2js('mappingForm', '.', true));
            
            if(validatorsManager.formValidated(this.$el)) {
                this.$el.find("input[type=submit]").prop('disabled', false);
            }
            else {
                this.$el.find("input[type=submit]").prop('disabled', true);
            }
            
        },
        render: function(args, callback) {
            var _this = this,
                syncConfig = configDelegate.readEntity("sync"),
                availableObjectsMap = this.buildAvailableObjectsMap();
            
                $.when(syncConfig, availableObjectsMap).then(_.bind(function(sync, availableObjectsMap){
                    this.data.syncConfig = sync;
                    this.data.availableObjectsMap = availableObjectsMap;
                    UIUtils.fillTemplateWithData(
                        "templates/admin/mapping/newMapping.json", 
                        {}, 
                        function (newMappingText) {
                            _this.data.mapping = $.parseJSON(newMappingText);
                            
                            _this.parentRender(_.bind(function () {
                                validatorsManager.bindValidators(this.$el);
                                validatorsManager.validateAllFields(this.$el);
                                
                                if(callback){
                                    callback();
                                }
                            }, _this));
                        }
                    );
                },this));
        },
        formSubmit: function(event) {
            event.preventDefault();
            
            var mapping = form2js('mappingForm', '.', true);
            
            this.data.syncConfig.mappings.push(this.data.mapping);
            
            configDelegate.updateEntity("sync", {"mappings" : this.data.syncConfig.mappings}).then(_.bind(function(){
                eventManager.sendEvent(constants.ROUTE_REQUEST, {routeName: "editMappingView", args: [this.data.mapping.name]});
                eventManager.sendEvent(constants.EVENT_DISPLAY_MESSAGE_REQUEST, "newMappingAdded");
            }, this));
        },
        buildAvailableObjectsMap: function(){
            var currentConnectors = connectorDelegate.currentConnectors(),
                managedConfig = configDelegate.readEntity("managed");
            
            return $.when(currentConnectors, managedConfig).then(function(currConnections, managed){
                var objects = {
                        system: [],
                        managed: _.map(managed.objects, function(o){ return { name: o.name, fullName: "managed/" + o.name }; })
                    },
                    enabledConnectors = _.filter(currConnections[0],function(c) { return c.enabled; });
                
                if(enabledConnectors.length){
                    objects.system = _(enabledConnectors)
                        .map(function(connector){ 
                            return _.map(connector.objectTypes, function(ot){ 
                                return { 
                                            name: connector.name, 
                                            fullName: "system/" + connector.name + "/" + ot
                                        };
                            });
                        })
                        .flatten()
                        .value();
                }
                    
                return objects;
            });
        }
    });

    return new AddMappingView();
});

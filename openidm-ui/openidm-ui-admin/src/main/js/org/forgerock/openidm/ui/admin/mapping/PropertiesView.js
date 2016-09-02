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
 */

define([
    "jquery",
    "underscore",
    "org/forgerock/openidm/ui/admin/mapping/util/MappingAdminAbstractView",
    "org/forgerock/openidm/ui/admin/mapping/properties/LinkQualifiersView",
    "org/forgerock/openidm/ui/admin/mapping/properties/MappingAssignmentsView",
    "org/forgerock/openidm/ui/admin/mapping/properties/AttributesGridView",
    "org/forgerock/commons/ui/common/main/EventManager",
    "org/forgerock/commons/ui/common/util/Constants",
    "org/forgerock/openidm/ui/admin/util/LinkQualifierUtils",
    "org/forgerock/openidm/ui/admin/delegates/ConnectorDelegate",
    "org/forgerock/openidm/ui/common/delegates/ConfigDelegate",
    "org/forgerock/openidm/ui/admin/util/AdminUtils"

], function($, _,
            MappingAdminAbstractView,
            LinkQualifiersView,
            MappingAssignmentsView,
            AttributesGridView,
            EventManager,
            Constants,
            LinkQualifierUtil,
            ConnectorDelegate,
            ConfigDelegate,
            AdminUtils) {

    var PropertiesView = MappingAdminAbstractView.extend({
        template: "templates/admin/mapping/PropertiesTemplate.html",
        element: "#mappingContent",
        noBaseTemplate: true,
        data: {},

        render: function (args, callback) {
            this.data.hasLinkQualifiers = !_.isUndefined(this.getCurrentMapping().linkQualifiers);

            this.parentRender(_.bind(function () {

                LinkQualifiersView.render();

                this.renderAttributesGrid(callback);

                MappingAssignmentsView.render();

            }, this));
        },
        renderAttributesGrid: function(callback) {
            this.buildAvailableObjectsMap().then(_.bind(function(availableObjects) {
                AttributesGridView.render({
                    useDragIcon: true,
                    usesLinkQualifier: true,
                    linkQualifiers: LinkQualifierUtil.getLinkQualifier(this.getCurrentMapping().name),
                    usesDynamicSampleSource: true,
                    availableObjects: availableObjects[0],
                    requiredProperties: availableObjects[1],
                    mapping: this.getCurrentMapping(),
                    save: (mappingProperties) => {
                        var mapping = this.getCurrentMapping();

                        if (mapping.recon) {
                            delete mapping.recon;
                        }

                        if (mappingProperties) {
                            mapping.properties = mappingProperties;

                            this.AbstractMappingSave(mapping, _.bind(function() {
                                EventManager.sendEvent(Constants.EVENT_DISPLAY_MESSAGE_REQUEST, "mappingSaveSuccess");
                                this.setCurrentMapping(mapping);
                                this.render();
                            }, this));
                        }
                    },
                    numRepresentativeProps: this.getNumRepresentativeProps()

                }, _.bind(function() {
                    if (callback) {
                        callback();
                    }
                }, this));
            }, this));
        },

        buildAvailableObjectsMap: function() {
            var currentMapping = this.getCurrentMapping(),
                sourceProm = $.Deferred(),
                targetProm = $.Deferred(),
                currentConnectors = ConnectorDelegate.currentConnectors(),
                managedConfig = ConfigDelegate.readEntity("managed"),
                targetProperties = AdminUtils.findPropertiesList(currentMapping.target.split("/"));

            return $.when(currentConnectors, managedConfig, targetProperties).then(_.bind(function(currConnectors, managed, targetProps) {
                let requiredProperties = {};

                _.map(managed.objects, _.bind(function(o) {
                    if (currentMapping.source === "managed/" + o.name) {
                        sourceProm.resolve({ name: o.name, fullName: "managed/" + o.name });
                    }
                    if (currentMapping.target === "managed/" + o.name) {
                        let transformedTargetProps = _.chain(targetProps).keys().sortBy().value();
                        targetProm.resolve({ name: o.name, fullName: "managed/" + o.name, properties: transformedTargetProps });
                    }
                }, this));

                if (!(sourceProm.state() === "resolved" && targetProm.state() === "resolved")) {
                    _.each(currConnectors, function(connector) {
                        _.each(connector.objectTypes, function(objType) {
                            var objTypeMap = {
                                    name: connector.name,
                                    fullName: "system/" + connector.name + "/" + objType
                                },
                                getProps = function(){
                                    return ConfigDelegate.readEntity(connector.config.replace("config/", "")).then(function(connector) {
                                        return connector.objectTypes[objType].properties;
                                    });
                                };

                            if (currentMapping.source === objTypeMap.fullName) {
                                getProps().then(function(props){
                                    objTypeMap.properties = _.keys(props).sort();
                                    sourceProm.resolve(objTypeMap);
                                });
                            }

                            AdminUtils.findPropertiesList(currentMapping.target.split("/"), true).then(_.bind(function (properties) {
                                requiredProperties = _.keys(properties);
                                targetProm.resolve(objTypeMap);
                            }, this));

                        }, this);
                    }, this);
                }

                return $.when(sourceProm, targetProm).then(function(source, target) {
                    return [{ source: source, target: target}, requiredProperties];
                });

            }, this));
        }
    });

    return new PropertiesView();
});

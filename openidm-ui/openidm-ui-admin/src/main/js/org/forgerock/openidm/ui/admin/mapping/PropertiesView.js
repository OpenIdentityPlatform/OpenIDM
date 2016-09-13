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
            var currentMapping = this.getCurrentMapping();
            this.buildAvailableObjectsMap(currentMapping).then(_.bind(function(availableObjects) {
                AttributesGridView.render({
                    useDragIcon: true,
                    usesLinkQualifier: true,
                    linkQualifiers: LinkQualifierUtil.getLinkQualifier(currentMapping.name),
                    usesDynamicSampleSource: true,
                    availableObjects: availableObjects[0],
                    requiredProperties: availableObjects[1],
                    mapping: currentMapping,
                    save: (mappingProperties) => {
                        var mapping = currentMapping;

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

        /**
         * Takes an object with a source and target property and returns an Array.
         * The first returned parameter is an object containing name and property information about
         * the source and the target.  The second item is an Array of required target properties
         *
         * @param currentMapping {object}
         * @param currentMapping.target {string}
         * @param currentMapping.source {string}
         *
         * @returns {*}
         */
        buildAvailableObjectsMap: function(currentMapping) {
            var sourceProm = $.Deferred(),
                targetProm = $.Deferred(),

                targetNameParts = currentMapping.target.split("/"),
                sourceNameParts = currentMapping.source.split("/"),

                sourceProperties = AdminUtils.findPropertiesList(sourceNameParts),
                targetProperties = AdminUtils.findPropertiesList(targetNameParts),
                requiredProperties = AdminUtils.findPropertiesList(targetNameParts, true);

            function getManaged(name, promise, properties) {
                promise.resolve({
                    "name": name,
                    "fullName": "managed/" + name,
                    "properties": _.chain(properties).keys().sortBy().value(),
                    "schema": properties
                });
            }

            function getSystem(nameParts, promise, properties) {
                promise.resolve({
                    "name": nameParts[1],
                    "fullName":  nameParts.join("/"),
                    "properties": _.chain(properties).keys().sortBy().value(),
                    "schema": properties
                });
            }

            return $.when(targetProperties, sourceProperties, requiredProperties)
                .then(_.bind(function(targetProps, sourceProps, requiredProps) {

                    // Target is a system object,
                    // System properties are always returned as an array of objects, Managed properties are not.
                    if (targetNameParts[0] === "system") {
                        requiredProps = requiredProps[0];
                        getSystem(targetNameParts, targetProm, targetProps[0]);

                    // Target is a managed object
                    } else {
                        getManaged(targetNameParts[1], targetProm, targetProps);
                    }

                    // Source is a system object
                    if (sourceNameParts[0] === "system") {
                        getSystem(sourceNameParts, sourceProm, sourceProps[0]);

                    // Source is a managed object
                    } else {
                        getManaged(sourceNameParts[1], sourceProm, sourceProps);
                    }

                    return $.when(sourceProm, targetProm).then(function(source, target) {
                        return [{ source: source, target: target}, _.keys(requiredProps)];
                    });
                }, this));
        }
    });

    return new PropertiesView();
});

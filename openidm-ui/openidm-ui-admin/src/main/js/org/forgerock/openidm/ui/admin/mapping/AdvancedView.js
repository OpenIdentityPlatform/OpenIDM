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
    "lodash",
    "form2js",
    "org/forgerock/openidm/ui/admin/mapping/util/MappingAdminAbstractView",
    "org/forgerock/commons/ui/common/util/Constants",
    "org/forgerock/commons/ui/common/main/EventManager",
    "org/forgerock/commons/ui/common/components/ChangesPending"

], function(
        $,
        _,
        form2js,
        MappingAdminAbstractView,
        constants,
        eventManager,
        ChangesPending
    ) {
    const AdvancedView = MappingAdminAbstractView.extend({
        events: {
            "click .save-mapping": "saveMapping",
            "click .cancel-mapping-changes": "render",
            "change .advanced-mapping-panel": "checkChanges"
        },
        partials: [
            "partials/mapping/advanced/_booleanPartial.html",
            "partials/mapping/advanced/_textPartial.html"
        ],
        template: "templates/admin/mapping/AdvancedTemplate.html",
        element: "#mappingContent",
        noBaseTemplate: true,
        model: {
            DOC_URL: constants.DOC_URL + "integrators-guide/",
            TRANS_BASE: "templates.mapping.advanced."
        },

        render: function(args, callback) {

            var mapping = this.getCurrentMapping();

            this.data.panels = [
                this.createUiObj("panel", {name: "additionalOptions", mapping: mapping, helpLink: "#reconciliation-optimization", description: true, params: [
                    this.createUiObj("param", {name: "correlateEmptyTargetSet", fieldType: "boolean"}),
                    this.createUiObj("param", {name: "prefetchLinks", fieldType: "boolean"}),
                    this.createUiObj("param", {name: "allowEmptySourceSet", fieldType: "boolean", helpLink: "#preventing-accidental-deletion"}),
                    this.createUiObj("param", {name: "taskThreads", fieldType: "text", defaultValue: "10"})
                ]})
            ];

            this.parentRender(() => {
                this.data.panels.map((panel) => {
                    panel.changeWatcher = ChangesPending.watchChanges({
                        element: this.$el.find(panel.domId + " .changes-pending-container"),
                        undo: true,
                        watchedObj: _.clone(mapping),
                        watchedProperties: panel.params.map((param) => param.name),
                        undoCallback: (orig) => {
                            this.render();
                        }
                    });

                    // Overwrite `ChangesPending#isChanged` to provide functionality to ChangesPending Widget
                    panel.changeWatcher.isChanged = function() {
                        var isChanged =  _.some(this.data.watchedProperties, function (prop) {
                        // need to add `!` to the output of `compare` to return from properly from `_.some`
                            return !this.compareObjects(prop, this.data.watchedObj, this.data.changes);
                        }, this);
                        return isChanged;
                    };

                    // Overwrite `ChangesPending#compareObjects` to provide proper functionality to ChangesPending Widget
                    panel.changeWatcher.compareObjects = function(property, obj1, obj2) {
                        var val1 = _.clone(obj1[property], true),
                            val2 = _.clone(obj2[property], true),
                            deleteEmptyProperties = function (obj) {
                                _.each(obj, function(prop, key) {
                                    if (_.isEmpty(prop) && !_.isNumber(prop) && !_.isBoolean(prop)) {
                                        delete obj[key];
                                    }
                                });
                            };

                        if (_.isObject(val1) && _.isObject(val2)) {
                            deleteEmptyProperties(val1);
                            deleteEmptyProperties(val2);

                        // Need to add an explicit comparison (val1 === val2)
                        // reason: `!val1/!val2` performs type coercion
                        } else if (!val1 && !val2 && val1 === val2) {
                            return true;
                        }
                        return _.isEqual(val1, val2);
                    };
                });

                if (callback) { callback(); }
            });
        },

        checkChanges: function (event) {
            var panelId = "#" + $(event.currentTarget)[0].id,
                buttons = this.$el.find(panelId + " .advanced-mapping-button"),
                formData = this.getFormData(),
                mapping = this.getCurrentMapping(),
                panel = _.find(this.data.panels, {domId: panelId}),
                mutatedMapping = this.mutateMapping(mapping, formData);

            panel.changeWatcher.makeChanges(mutatedMapping);

            if (panel.changeWatcher.isChanged()) {
                buttons.prop('disabled', false);
            } else {
                buttons.prop('disabled', true);
            }
        },

        getFormData: function() {
            return form2js("advancedMappingConfigForm", ".", true);
        },

        saveMapping: function(event) {
            event.preventDefault();
            var mapping = this.getCurrentMapping(),
                formData = this.getFormData(),
                mutatedMapping = this.mutateMapping(mapping, formData);

            if (!_.isEqual(mutatedMapping, mapping)) {
                this.AbstractMappingSave(mutatedMapping, () => {
                    eventManager.sendEvent(constants.EVENT_DISPLAY_MESSAGE_REQUEST, "mappingSaveSuccess");
                    this.render();
                });
            } else {
                this.render();
            }

        },

        mutateMapping: function(mapping, formData) {
            var mutatedMapping = {};

            if (formData.taskThreads) {
                formData.taskThreads = Number(formData.taskThreads);
            }

            _.assign(mutatedMapping, mapping, formData);

            // Loop over mutatedMapping and handle cases
            _.forIn(mutatedMapping, (value, key) => {

                if (key === "taskThreads") {
                    // Task threads need to handle a zero case
                    if (value === 0) {
                        mutatedMapping[key] = 0;

                    // default is 10 threads so if this is selected remove from the mapping
                    } else if (value === 10) {
                        delete mutatedMapping[key];

                    // handle other falsy values
                    } else if (!value) {
                        if (mapping.taskThreads) {
                            mutatedMapping.taskThreads = mapping.taskThreads;
                        } else {
                            delete mutatedMapping.taskThreads;
                        }
                    }
                } else if (key === "prefetchLinks") {
                    // have to reverse the expression of this because it is implicitly enabled
                    // if undefined on or false on mapping delete true and send false
                    if (_.isUndefined(mapping.prefetchLinks) || mapping.prefetchLinks === false) {
                        if (mutatedMapping.prefetchLinks === true) {
                            delete mutatedMapping.prefetchLinks;
                        } else {
                            mutatedMapping.prefetchLinks = false;
                        }
                    }
                } else {
                    // If value falsy for anything else,
                    // remove it from the mapping to restore default behavior
                    if (!value) {
                        delete mutatedMapping[key];
                    }
                }
            });

            return mutatedMapping;
        },

        createUiObj: function(type, options) {
            if (!options.name) {
                throw new Error("name not specified");
            }

            var obj = {
                name: options.name,
                title: $.t(this.model.TRANS_BASE + options.name),
                helpText: $.t(this.model.TRANS_BASE + options.name + "Help"),
                getClean: function(ownProp) {
                    return this[ownProp].split('#')[1];
                }
            };

            if (options.helpLink) {
                obj.helpLink = this.model.DOC_URL + options.helpLink;
            }

            // options for panel type object
            if (type === "panel") {
                obj.domId = "#" + obj.name + "Panel";
                if (options.description) {
                    obj.description = this.helpText;
                }

                obj.params = options.params.map((param) => {

                    // Set initial values

                    // Special case for prefetchLinks
                    // prefetchLinks is implicitly enabled by default
                    if (param.name === "prefetchLinks") {
                        param.configuredValue = !options.mapping.prefetchLinks;
                    }

                    // All others should be whatever the mapping is
                    if (!_.isUndefined(options.mapping[param.name])) {
                        param.configuredValue = options.mapping[param.name];
                    }

                    // Add panel ids to configParameters
                    param.panelId = obj.domId;

                    return param;
                });

                obj.collapsed = options.collapsed;

            // options for param type object
            } else if  (type === "param") {
                switch (options.fieldType) {
                    case "boolean":
                        obj.partial = () => "mapping/advanced/_booleanPartial";
                        break;
                    case "text":
                        obj.partial = () => "mapping/advanced/_textPartial";
                        break;
                    default:
                        throw new Error("Unknown fieldType");
                }

                if (options.defaultValue) {
                    obj.defaultValue = options.defaultValue;
                }

                if (options.activatedBy) {
                    obj.activatedBy = "#" + options.activatedBy;
                }
            }

            return obj;
        }

    });

    return new AdvancedView();
});

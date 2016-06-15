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

                    // Overwrite `isChanged` to provide proper functionality to ChangesPending Widget
                    panel.changeWatcher.isChanged = function() {
                        var isChanged =  _.some(this.data.watchedProperties, function (prop) {
                            return !this.compareObjects(prop, this.data.watchedObj, this.data.changes);
                        }, this);
                        return isChanged;
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
                panel = _.find(this.data.panels, {domId: panelId});

            panel.changeWatcher.makeChanges(this.mutateMapping(mapping, formData));

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
                formData = form2js("advancedMappingConfigForm", ".", true),
                mutatedMapping = this.mutateMapping(mapping, formData);

            // Send mutatedMapping to the server
            this.AbstractMappingSave(mutatedMapping, () => {
                eventManager.sendEvent(constants.EVENT_DISPLAY_MESSAGE_REQUEST, "mappingSaveSuccess");
                this.render();
            });

        },

        mutateMapping: function(mapping, formData) {
            var mutatedMapping = {};

            _.assign(mutatedMapping, mapping, formData);

            // Loop over mutatedMapping and handle cases
            _.forIn(mutatedMapping, (value, key) => {

                // Properly add/remove the properties to the mapping obj
                if (key === "taskThreads") {
                    // Task threads need to handle a zero case
                    if (value === 0) {
                        mutatedMapping[key] = 0;
                    } else if (value === 10) {
                        // default is 10 threads so if this is selected remove from the mapping
                        delete mutatedMapping[key];
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
                    if (!_.isUndefined(options.mapping[param.name])) {

                        // Special case for enableSync
                        if (param.name.match("enableSync")) {
                            param.configuredValue = !options.mapping[param.name];
                        } else {
                            param.configuredValue = options.mapping[param.name];
                        }
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

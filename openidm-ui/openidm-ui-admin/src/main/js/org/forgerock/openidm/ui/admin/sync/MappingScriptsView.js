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

define("org/forgerock/openidm/ui/admin/sync/MappingScriptsView", [
    "org/forgerock/openidm/ui/admin/util/AdminAbstractView",
    "org/forgerock/commons/ui/common/main/EventManager",
    "org/forgerock/commons/ui/common/util/Constants",
    "org/forgerock/openidm/ui/common/delegates/ConfigDelegate",
    "org/forgerock/openidm/ui/admin/util/ScriptEditor",
    "org/forgerock/openidm/ui/admin/util/InlineScriptEditor"
], function(AdminAbstractView,
            eventManager,
            constants,
            ConfigDelegate,
            ScriptEditor,
            InlineScriptEditor) {
    var MappingScriptsView = AdminAbstractView.extend({
        template: "templates/admin/sync/MappingScriptsTemplate.html",
        baseTemplate: "templates/admin/AdminBaseTemplate.html",

        addScript: function() {
            var event = this.$el.find(".scriptEvents option:selected"),
                defaultScript = null;

            this.$el.find(".scriptContainer").append("<div class='scriptEditor'></div>");

            if (_.has(this.model.mapping, event.val())) {
                defaultScript = this.model.mapping[event.val()];
            }

            this.$el.find(".scriptContainer").append("<div class='scriptEditor'></div>");

            this.model.scriptEditors[event.val()] = ScriptEditor.generateScriptEditor({
                "element": this.$el.find(".scriptContainer .scriptEditor").last(),
                "eventName": event.val(),
                "deleteCallback": _.bind(function(script) {
                    this.model.scripts.push(script.eventName);
                    delete this.model.scriptEditors[script.eventName];
                    this.updateScripts();
                }, this),
                "scriptData": defaultScript
            });

            this.model.scripts.splice(this.model.scripts.indexOf(event.val()), 1);
            this.updateScripts();
            event.remove();
        },

        loadDefaultScripts: function() {
            _.each(_.clone(this.model.scripts), function(script) {
                if (_.has(this.model.mapping, script)) {
                    this.$el.find(".scriptEvents").val(script);
                    this.addScript();
                }
            }, this);
        },

        init: function() {
            if (this.model.scripts.length > 1) {
                this.updateScripts();
                this.loadDefaultScripts();

            } else if (this.model.scripts.length === 1) {
                var eventName = this.model.scripts[0],
                    defaultScript = null;

                this.model.singleScript = true;
                this.$el.find(".addScriptContainer").hide();

                if (_.has(this.model.mapping, eventName)) {
                    defaultScript = this.model.mapping[eventName];
                }

                this.model.scriptEditors[eventName]  = InlineScriptEditor.generateScriptEditor({
                    "element": this.$el.find(".scriptContainer"),
                    "eventName": eventName,
                    "noValidation": true,
                    "scriptData": defaultScript
                });

            }
        },

        updateScripts: function() {
            this.$el.find(".scriptEvents").empty();

            _.chain(this.model.scripts)
                .sortBy()
                .each(function(script) {
                    this.$el.find(".scriptEvents").append("<option value='" + script + "'>" + script + "</option>");
                }, this);

            if (this.model.scripts.length === 0) {
                this.$el.find(".addScript").prop('disabled', true);
                this.$el.find(".scriptEvents").prop('disabled', true);
                this.$el.find(".allAdded").show();
            } else {
                this.$el.find(".addScript").prop('disabled', false);
                this.$el.find(".scriptEvents").prop('disabled', false);
                this.$el.find(".allAdded").hide();
            }
        },

        saveScripts: function(e) {
            e.preventDefault();

            var scriptHook = null;

            // Update the mapping with the script editors, remove any instances of uncompleted editors.
            _.each(this.model.scriptEditors, function(scriptEditor, eventName) {
                if (this.model.singleScript) {
                    scriptHook = scriptEditor.generateScript();
                } else {
                    scriptHook = scriptEditor.getScriptHook().script;
                }

                if (! _.isNull(scriptHook)) {
                    this.model.mapping[eventName] = scriptHook;
                } else if (_.has(this.model.mapping, eventName)) {
                    delete this.model.mapping[eventName];
                }
            }, this);

            // Remove any mapping instances of scripts that are not added
            if (!this.model.singleScript) {
                _.each(this.model.scripts, function(script) {
                    if (_.has(this.model.mapping, script)) {
                        delete this.model.mapping[script];
                    }
                }, this);
            }

            // Update the sync object
            _.each(this.model.sync.mappings, function(map, key) {
                if (map.name === this.model.mappingName) {
                    this.model.sync.mappings[key] = this.model.mapping;
                }
            }, this);

            // Save the Sync object
            ConfigDelegate.updateEntity("sync", this.model.sync).then(_.bind(function() {
                eventManager.sendEvent(constants.EVENT_DISPLAY_MESSAGE_REQUEST, this.model.successMessage);
            }, this));
        }
    });

    return MappingScriptsView;
});

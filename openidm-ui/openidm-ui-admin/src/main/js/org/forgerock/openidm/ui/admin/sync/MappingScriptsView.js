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
    "org/forgerock/openidm/ui/admin/util/InlineScriptEditor",
    "org/forgerock/openidm/ui/admin/delegates/BrowserStorageDelegate",
    "org/forgerock/openidm/ui/admin/util/ScriptList"
], function(AdminAbstractView,
            eventManager,
            constants,
            ConfigDelegate,
            InlineScriptEditor,
            BrowserStorageDelegate,
            ScriptList) {
    var MappingScriptsView = AdminAbstractView.extend({
        template: "templates/admin/sync/MappingScriptsTemplate.html",

        init: function() {
            var addedEvents = _.keys(_.pick(this.model.mapping,this.model.scripts)),
                eventName,
                defaultScript;

            this.model.availableScripts = _.clone(this.model.scripts);
            this.model.scriptEditors = [];

            if (this.model.scripts.length > 1) {

                ScriptList.generateScriptList({
                    element: this.$el.find(".scriptContainer"),
                    label: "",
                    selectEvents: _.difference(this.model.availableScripts, addedEvents),
                    addedEvents: addedEvents,
                    eventHooks: this.model.scriptEditors,
                    currentObject: this.model.mapping,
                    hasWorkflow: true
                });

            } else if (this.model.scripts.length === 1) {
                eventName = this.model.scripts[0];
                defaultScript = null;

                this.model.singleScript = true;
                this.$el.find(".addScriptContainer").hide();

                if (_.has(this.model.mapping, eventName)) {
                    defaultScript = this.model.mapping[eventName];
                }

                this.model.scriptEditors[eventName]  = InlineScriptEditor.generateScriptEditor({
                    "element": this.$el.find(".scriptContainer"),
                    "eventName": eventName,
                    "disableValidation": false,
                    "validationCallback": _.bind(function(valid) {
                        if (valid) {
                            this.$el.find(".saveScripts").prop("disabled", false);
                        } else {
                            this.$el.find(".saveScripts").prop("disabled", true);
                        }
                    }, this),
                    "scriptData": defaultScript,
                    "hasWorkflow": true
                });

            }
        },

        saveScripts: function(e) {
            e.preventDefault();

            var scriptHook = null,
                tmpEditor,
                eventName,
                currentScripts,
                scriptsToDelete,
                addRemoveFromMapping = _.bind(function(){
                    if (! _.isNull(scriptHook)) {
                        this.model.mapping[eventName] = scriptHook;
                    } else if (_.has(this.model.mapping, eventName)) {
                        delete this.model.mapping[eventName];
                    }
                },this);

            if(this.model.singleScript){
                tmpEditor = this.model.scriptEditors.result;
                scriptHook = tmpEditor.generateScript();
                eventName = tmpEditor.model.eventName;
                addRemoveFromMapping();
            } else {
                currentScripts = _.map(this.model.scriptEditors,function(editor) { return editor.getScriptHook().eventName; });
                scriptsToDelete = _.difference(this.model.availableScripts,currentScripts);

                // Update the mapping with the script editors, remove any instances of uncompleted editors.
                _.each(this.model.scriptEditors, function(scriptEditor) {
                    eventName = scriptEditor.getScriptHook().eventName;
                    scriptHook = scriptEditor.getScriptHook().script;
                    addRemoveFromMapping();
                }, this);

                // Remove any mapping instances of scripts that are not added
                _.each(scriptsToDelete, function(script) {
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
                BrowserStorageDelegate.set("currentMapping", _.extend(this.model.mapping, this.model.recon));
            }, this));
        }
    });

    return MappingScriptsView;
});

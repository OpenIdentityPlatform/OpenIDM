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
 * Copyright 2015 ForgeRock AS.
 */

/*global define */

define("org/forgerock/openidm/ui/admin/mapping/util/MappingScriptsView", [
    "underscore",
    "org/forgerock/openidm/ui/admin/mapping/util/MappingAdminAbstractView",
    "org/forgerock/commons/ui/common/main/EventManager",
    "org/forgerock/commons/ui/common/util/Constants",
    "org/forgerock/openidm/ui/admin/util/InlineScriptEditor",
    "org/forgerock/openidm/ui/admin/util/ScriptList"
], function( _,
            MappingAdminAbstractView,
            eventManager,
            constants,
            InlineScriptEditor,
            ScriptList) {

    var MappingScriptsView = MappingAdminAbstractView.extend({
        template: "templates/admin/mapping/util/MappingScriptsTemplate.html",

        init: function(args) {
            this.model.availableScripts = _.clone(this.model.scripts);
            this.model.scriptEditors = [];
            this.model.sync = this.getSyncConfig();
            this.model.mapping = this.getCurrentMapping();
            this.model.mappingName = this.getMappingName();
            this.model.hasWorkflow = true;

            if(!_.isUndefined(args) && args.hasWorkFlow === false) {
                this.model.hasWorkflow = false;
            }

            var addedEvents = _.keys(_.pick(this.model.mapping, this.model.scripts)),
                eventName,
                defaultScript;

            if (this.model.scripts.length > 1) {

                this.model.scriptList = ScriptList.generateScriptList({
                    element: this.$el.find(".scriptContainer"),
                    label: "",
                    selectEvents: _.difference(this.model.availableScripts, addedEvents),
                    addedEvents: addedEvents,
                    currentObject: this.model.mapping,
                    hasWorkflow: this.model.hasWorkflow
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

            if (this.model.singleScript) {
                tmpEditor = this.model.scriptEditors.result;
                scriptHook = tmpEditor.generateScript();
                eventName = tmpEditor.model.eventName;
                addRemoveFromMapping();

            } else {
                currentScripts = this.model.scriptList.getScripts();
                scriptsToDelete = _.difference(this.model.availableScripts, _.keys(currentScripts));

                _.extend(this.model.mapping, currentScripts);

                // Remove any mapping instances of scripts that are not added
                _.each(scriptsToDelete, function(script) {
                    if (_.has(this.model.mapping, script)) {
                        delete this.model.mapping[script];
                    }
                }, this);
            }

            this.AbstractMappingSave(this.model.mapping, _.bind(function() {
                eventManager.sendEvent(constants.EVENT_DISPLAY_MESSAGE_REQUEST, this.model.successMessage);
            }, this));
        }
    });

    return MappingScriptsView;
});

/**
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2015 ForgeRock AS. All rights reserved.
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

/*global define, $, _, Handlebars */

define("org/forgerock/openidm/ui/admin/util/ScriptList", [
    "org/forgerock/commons/ui/common/main/AbstractView",
    "org/forgerock/openidm/ui/admin/util/ScriptDialog",
    "org/forgerock/commons/ui/common/util/UIUtils"
], function(AbstractView, ScriptDialog, UIUtils) {
    var scriptListInstance = {},
        ScriptList = AbstractView.extend({
            template: "templates/admin/util/ScriptList.html",
            noBaseTemplate: true,
            events: {
                "click .addScriptButton" : "addScript",
                "click .edit-btn" : "editScriptHook",
                "click .script-remove" : "removeScript",
                "click .event-hook-empty" : "addScriptHook"
            },

            render: function (args, callback) {
                var tempWorkflow,
                    tempScriptData;

                this.element = args.element;
                this.data = _.clone(args, true);

                this.data.eventHooks = {};

                _.each(this.data.addedEvents, _.bind(function(event) {
                    if (_.has(this.data.currentObject, event)) {
                        tempWorkflow = null;
                        tempScriptData = {};

                        if (this.data.hasWorkflow && this.data.currentObject[event].globals && this.data.currentObject[event].globals.workflowReadable) {
                            tempWorkflow = this.data.currentObject[event].globals.workflowReadable;
                        } else {
                            tempScriptData = this.data.currentObject[event];
                        }

                        this.data.eventHooks[event] = {
                            "workflow": tempWorkflow,
                            "scriptData": tempScriptData
                        };
                    }
                }, this));

                this.data.addedEvents = _.sortBy(this.data.addedEvents);

                this.parentRender(function() {
                    if (this.data.addedEvents.length) {
                        this.$el.find(".scripts-container").show();
                    }

                    if (callback) {
                        callback();
                    }
                });
            },

            reRender: function() {
                this.render(this.data);

                if(this.$el.find(".scriptSelection option").length === 0) {
                    this.$el.find(".scriptSelection").prop('disabled', true);
                    this.$el.find(".addScriptButton").prop('disabled', true);
                }
            },

            addScript: function() {
                var selectedEvent = this.$el.find(".scriptSelection").val();

                ScriptDialog.render({
                    "element": this.data.element,
                    "disableValidation" : false,
                    "eventName": selectedEvent,
                    "scriptData": null,
                    "saveCallback": _.bind(function() {
                        this.data.addedEvents.push(selectedEvent);
                        this.data.selectEvents.splice(_.indexOf(this.data.selectEvents, selectedEvent), 1);
                        this.data.currentObject[selectedEvent] = ScriptDialog.generateScript();
                        this.reRender();
                    }, this),
                    "hasWorkflow": this.data.hasWorkflow,
                    "workflowContext": this.data.workflowContext
                });
            },

            removeScript: function(event) {
                UIUtils.jqConfirm($.t("templates.scriptEditor.deleteMsg"), _.bind(function() {
                    var selectedEvent = this.$el.find(event.currentTarget).closest(".event-hook").attr("data-script-type"),
                        index = _.indexOf(this.data.addedEvents, selectedEvent);

                    this.data.addedEvents.splice(index, 1);
                    this.data.selectEvents.push(selectedEvent);
                    delete this.data.currentObject[selectedEvent];

                    if (!this.data.addedEvents.length) {
                        this.$el.find(".scripts-container").hide();
                    }

                    this.reRender();
                },this), "290px");
            },

            editScriptHook: function(event) {
                var selectedEvent = this.$el.find(event.currentTarget).closest(".event-hook").attr("data-script-type");

                ScriptDialog.render({
                    "element": this.data.element,
                    "disableValidation" : false,
                    "eventName": selectedEvent,
                    "scriptData": this.data.currentObject[selectedEvent],
                    "saveCallback": _.bind(function() {
                        this.data.currentObject[selectedEvent] = ScriptDialog.generateScript();
                        this.reRender();
                    }, this),
                    "hasWorkflow": this.data.hasWorkflow,
                    "workflowContext": this.data.workflowContext
                });
            },

            getScripts: function() {
                var scripts =  _.map(this.data.addedEvents, function(event) {
                    if (_.has(this.data.currentObject, event)) {
                        return this.data.currentObject[event];
                    }
                }, this);

                return _.object(this.data.addedEvents, scripts);
            }
        });

    /* Params:
         element - the element that will receive the scriptList
         label - the label text that shows up above the events/hooks grid
         selectEvents - events to display in the initial dropdown
         addedEvents - events that are already in use
         currentObject - the object containing the current eventHooks
         hasWorkflow - if this instance of the editor supports workflows,
         workflowContext - the context of the workflows if any
     */

    scriptListInstance.generateScriptList = function(params) {
        var scriptList = {};

        $.extend(true, scriptList, new ScriptList());

        scriptList.render(params);

        return scriptList;
    };

    return scriptListInstance;
});


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

/*global define, $, _, Handlebars, form2js */

define("org/forgerock/openidm/ui/admin/util/ScriptList", [
    "org/forgerock/commons/ui/common/main/AbstractView",
    "org/forgerock/openidm/ui/admin/util/ScriptEditor"
], function(AbstractView, ScriptEditor) {
    var scriptListInstance = {},
        ScriptList = AbstractView.extend({
            template: "templates/admin/util/ScriptList.html",
            noBaseTemplate: true,
            events: {
                "click .addScriptButton" : "addScript"
            },

            render: function (args, callback) {
                var tempEvent,
                    scriptOption;

                this.element = args.element;
                this.data.label = args.label;
                this.data.selectEvents = args.selectEvents;
                this.data.addedEvents = args.addedEvents;
                this.eventHooks = args.eventHooks;
                this.currentObject = args.currentObject;
                this.hasWorkflow = args.hasWorkflow || false;
                this.workflowContext = args.workflowContext || "";

                this.parentRender(function() {
                    _.each(this.$el.find(".scripts-container .event-hook"), function(eventHook){
                        tempEvent = $(eventHook).attr("data-script-type");

                        if(_.isUndefined(this.currentObject[tempEvent])) {
                            scriptOption = null;
                        } else {
                            scriptOption = this.currentObject[tempEvent];
                        }

                        this.eventHooks.push(ScriptEditor.generateScriptEditor({
                            "element": $(eventHook),
                            "scriptData": scriptOption,
                            "eventName": tempEvent,
                            "hasWorkflow": this.hasWorkflow,
                            "workflowContext": this.workflowContext,
                            "deleteCallback": _.bind(this.removeScript, this)
                        }));

                        this.$el.find(".scripts-container").show();
                    }, this);

                    if(callback) {
                        callback();
                    }
                });
            },

            addScript: function() {
                var selectedEvent = this.$el.find(".scriptSelection").val(),
                    createdElement = "<tr data-script-type='" +selectedEvent +"' class='event-hook'></tr>";

                this.$el.find(".scripts-container table").append(createdElement);

                this.eventHooks.push(ScriptEditor.generateScriptEditor({
                            "element": this.$el.find(".scripts-container .group-field-block .event-hook:last"),
                            "eventName": selectedEvent,
                            "hasWorkflow": this.hasWorkflow,
                            "workflowContext": this.workflowContext,
                            "deleteCallback": _.bind(this.removeScript, this)
                        },
                        _.bind(function() { this.$el.find("tr[data-script-type=" +selectedEvent +"]").find(".edit-btn").click(); }, this)
                    )
                );

                this.$el.find(".scriptSelection option:selected").remove();

                if(this.$el.find(".scriptSelection option").length === 0) {
                    this.$el.find(".scriptSelection").prop('disabled', true);
                    this.$el.find(".addScriptButton").prop('disabled', true);
                }

                this.$el.find(".scripts-container").show();
            },

            removeScript: function(scriptObj) {
                var tempHooks = _.clone(this.eventHooks);

                _.each(tempHooks, function(hook, index){
                    if(hook.getEventName() === scriptObj.eventName) {
                        this.eventHooks.splice(index, 1);
                        this.$el.find(".scriptSelection").append("<option value='" +scriptObj.eventName +"'>" +scriptObj.eventName +"</option>");
                        this.$el.find(".scriptSelection").prop('disabled', false);
                        this.$el.find(".addScriptButton").prop('disabled', false);
                        if(!this.eventHooks.length) {
                            this.$el.find(".scripts-container").hide();
                        }
                    }
                }, this);
            }
        });

    /*Params that need to be passed into this widget
     element - the element that will recieve the scriptList
     label - the label text that shows up above the events/hooks grid
     selectEvents - events to display in the initial dropdown
     addedEvents - events that are already in use
     eventHooks - an empty array used to hold the currently used event objects
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


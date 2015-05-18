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

define("org/forgerock/openidm/ui/admin/util/ScriptEditor", [
    "org/forgerock/commons/ui/common/main/AbstractView",
    "org/forgerock/commons/ui/common/util/Constants",
    "org/forgerock/openidm/ui/admin/util/ScriptDialog",
    "org/forgerock/commons/ui/common/util/UIUtils"
], function(AbstractView, constants, ScriptDialog, uiUtils) {
    var seInstance = {},
        ScriptEditor = AbstractView.extend({
            template: "templates/admin/util/ScriptEditor.html",
            noBaseTemplate: true,
            events: {
                "click .edit-btn" : "editScriptHook",
                "click .script-remove" : "deleteScriptHook",
                "click .event-hook-empty" : "addScriptHook"
            },
            deleteElement: true,

            render: function (args, callback) {
                this.element = args.element;

                if(args.scriptData) {
                    this.data.scriptData = args.scriptData;
                } else {
                    this.data.scriptData = null;
                }

                if(args.eventName){
                    this.data.eventName = args.eventName;
                } else {
                    this.data.eventName = "Event";
                }

                if(args.deleteCallback){
                    this.deleteCallback = args.deleteCallback;
                }

                if(args.deleteElement === false) {
                    this.deleteElement = args.deleteElement;
                }

                if (args.saveCallback) {
                    this.saveCallback = args.saveCallback;
                } else {
                    this.saveCallback = _.noop;
                }

                if (args.hasWorkflow) {
                    if (this.data.scriptData && this.data.scriptData.globals) {
                        this.data.workflow = this.data.scriptData.globals.workflowReadable;
                    } else {
                        this.data.workflow = null;
                    }
                    this.hasWorkflow = args.hasWorkflow || false;
                }
                this.workflowContext = args.workflowContext || "";

                this.parentRender(callback);
            },

            editScriptHook: function() {
                if (this.data.scriptData !== null && this.data.scriptData.file === "workflow/triggerWorkflowGeneric.js") {
                    this.hasWorkflow = true;
                }

                ScriptDialog.render({
                    "element":this.element,
                    "disableValidation" : false,
                    "eventName":this.data.eventName,
                    "scriptData":this.data.scriptData,
                    "setScript": _.bind(this.setScriptHook, this),
                    "saveCallback": this.saveCallback,
                    "hasWorkflow": this.hasWorkflow,
                    "workflowContext": this.workflowContext
                });
            },

            deleteScriptHook: function() {
                uiUtils.jqConfirm("Are you sure you want to delete this script?", _.bind(function(){

                    if(this.deleteCallback){
                        this.deleteCallback({"element":this.element, "eventName":this.data.eventName});
                    }

                    if(this.deleteElement) {
                        this.$el.remove();
                    }
                },this), "290px");
            },

            addScriptHook: function() {
                ScriptDialog.render({
                    "element": this.element,
                    "disableValidation" : false,
                    "eventName": this.data.eventName,
                    "scriptData": null,
                    "setScript": _.bind(this.setScriptHook, this),
                    "saveCallback": _.bind(this.saveCallback, this),
                    "hasWorkflow": this.hasWorkflow,
                    "workflowContext": this.workflowContext
                });
            },

            setScriptHook: function(args) {
                this.render({"element":this.element, "eventName":this.data.eventName, "scriptData":args.scriptObject, "hasWorkflow": this.hasWorkflow, "workflowContext": this.workflowContext});
            },

            getScriptHook: function() {
                return {"script" :this.data.scriptData, "eventName" : this.data.eventName};
            },

            clearScriptHook: function() {
                this.render({"element":this.element, "eventName":this.data.eventName, "hasWorkflow": this.hasWorkflow, "workflowContext": this.workflowContext});
            },

            getEventName: function() {
                return this.data.eventName;
            }
        });

    seInstance.generateScriptEditor = function(loadingObject,callback) {
        var editor = {};

        $.extend(true, editor, new ScriptEditor());

        editor.render(loadingObject,callback);

        return editor;
    };

    return seInstance;
});
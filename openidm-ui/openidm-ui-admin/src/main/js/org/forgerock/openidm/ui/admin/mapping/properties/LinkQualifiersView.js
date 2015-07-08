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

define("org/forgerock/openidm/ui/admin/mapping/properties/LinkQualifiersView", [
    "org/forgerock/openidm/ui/admin/mapping/util/MappingAdminAbstractView",
    "org/forgerock/openidm/ui/common/delegates/ConfigDelegate",
    "org/forgerock/commons/ui/common/util/Constants",
    "org/forgerock/commons/ui/common/main/EventManager",
    "org/forgerock/openidm/ui/admin/util/InlineScriptEditor",
    "org/forgerock/openidm/ui/admin/delegates/ScriptDelegate",
    "org/forgerock/openidm/ui/admin/util/LinkQualifierUtils"
], function(MappingAdminAbstractView,
            ConfigDelegate,
            Constants,
            EventManager,
            inlineScriptEditor,
            ScriptDelegate,
            LinkQualifierUtils) {

    var LinkQualifiersView = MappingAdminAbstractView.extend({
        template: "templates/admin/mapping/properties/LinkQualifiersTemplate.html",
        element: "#mappingLinkQualifiers",
        noBaseTemplate: true,
        events: {
            "click .removeLinkQualifier": "removeLinkQualifier",
            "click .addLinkQualifier": "addLinkQualifier",
            "submit form": "addLinkQualifier",
            "click #linkQualifierTabs .btn" : "sectionControl",
            "click .linkQualifierSave" : "save"
        },
        model: {},
        data: {},

        render: function (args, callback) {
            this.model.mappingName = this.getMappingName();
            this.model.mapping = this.getCurrentMapping();

            this.data.linkQualifiers = this.model.mapping.linkQualifiers || [];

            if (this.data.linkQualifiers.length === 0 || this.data.linkQualifiers.type !== undefined) {
                this.data.linkQualifiers = ["default"];
                this.data.doNotDelete = true;
            } else if (this.data.linkQualifiers.length === 1) {
                this.data.doNotDelete = true;
            } else {
                this.data.doNotDelete = false;
            }

            this.parentRender(function () {
                if (this.$el.find("#scriptLinkQualifierBody").length === 0) {
                    return;
                }
                var scriptData = "",
                    linkQualifiers;

                if(this.model.mapping.linkQualifiers !== undefined && this.model.mapping.linkQualifiers.type !== undefined) {
                    scriptData = this.model.mapping.linkQualifiers;

                    this.$el.find("#linkQualifierTabs").find('.active').removeClass('active');
                    this.$el.find("#linkQualifierTabBodies").find('.active').removeClass('active');


                    this.$el.find("#scriptQualifierTab").toggleClass('active', true);
                    this.$el.find("#scriptLinkQualifier").toggleClass('active', true);

                    linkQualifiers = LinkQualifierUtils.getLinkQualifier(this.model.mappingName);

                    this.populateScriptLinkQualifier(linkQualifiers);
                }

                this.linkQualifierScript = inlineScriptEditor.generateScriptEditor({
                        "element": this.$el.find("#scriptLinkQualifierBody"),
                        "eventName": "linkQualifierScript",
                        "scriptData": scriptData,
                        "disablePassedVariable": false,
                        "disableValidation" : false,
                        "placeHolder" : "['test', 'default']"
                    },
                    _.bind(function(){
                        if(callback) {
                            callback();
                        }
                    }, this));

                $("#linkQualifierPanel").on('shown.bs.collapse', _.bind(function () {
                    this.linkQualifierScript.refresh();
                }, this));
            });
        },

        populateScriptLinkQualifier : function (data) {
            if(_.isArray(data) === true) {
                _.each(data, function(linkQualifier){
                    this.$el.find("#scriptLinkQualifierList").append('<button disabled="true" type="button" class="removeLinkQualifier btn btn-primary">'
                    + '<span class="linkQualifier">' +linkQualifier  +'</span>'
                    + '</button>');
                }, this);

                this.$el.find("#badLinkQualifierScript").hide();
            } else {
                this.$el.find("#badLinkQualifierScript .message").html($.t("templates.mapping.badScript"));
                this.$el.find("#badLinkQualifierScript").show();
            }
        },

        sectionControl: function(event) {
            var selected = $(event.target),
                currentTab = selected.prop("id");

            selected.parent().find('.active').removeClass('active');

            selected.toggleClass('active', true);
        },

        removeLinkQualifier: function(e) {
            e.preventDefault();

            if (!this.data.doNotDelete) {
                this.data.linkQualifiers = _.without(this.data.linkQualifiers, $(e.target).closest(".removeLinkQualifier").find(".linkQualifier").text());
                $(e.target).closest(".removeLinkQualifier").remove();

                if(this.$el.find("#staticLinkQualifierList .removeLinkQualifier").length === 1) {
                    this.data.doNotDelete = true;
                    this.$el.find("#staticLinkQualifierList .removeLinkQualifier").prop("disabled", true);
                }
            }
        },

        addLinkQualifier: function(e) {
            e.preventDefault();
            var toAdd = this.$el.find(".newLinkQualifier").val().trim();

            if (this.isValid(toAdd) && toAdd.length > 0 ) {
                this.data.linkQualifiers.push(toAdd);

                this.$el.find("#staticLinkQualifierList").append('<button type="button" class="removeLinkQualifier btn btn-primary">'
                + '<span class="linkQualifier">' +toAdd  +'</span>'
                + '<i class="fa fa-times fa-lg"></i>'
                + '</button>');

                this.$el.find(".newLinkQualifier").val("");

                if(this.$el.find("#staticLinkQualifierList .removeLinkQualifier").length > 1) {
                    this.$el.find(".removeLinkQualifier").prop("disabled", false);
                    this.data.doNotDelete = false;
                }
            }
        },

        isValid: function(toAdd) {
            if (this.data.linkQualifiers.indexOf(toAdd) === -1 ) {
                this.$el.find(".notValid").hide();
                return true;
            } else {
                this.$el.find(".notValid").show();
                return false;
            }
        },

        save: function() {
            var currentTab = this.$el.find("#linkQualifierTabs .active").prop("id");

            this.model.scriptError = false;

            if(currentTab === "staticQualifierTab") {
                this.saveDeclarative();
            } else {
                this.saveScript();
            }
        },

        saveDeclarative: function() {
            this.model.mapping.linkQualifiers = this.data.linkQualifiers;

            LinkQualifierUtils.setLinkQualifier(this.data.linkQualifiers, this.model.mappingName);

            this.AbstractMappingSave(this.model.mapping, _.bind(function() {
                EventManager.sendEvent(Constants.EVENT_DISPLAY_MESSAGE_REQUEST, "linkQualifierSaveSuccess");
                EventManager.sendEvent(Constants.EVENT_QUALIFIER_CHANGED, this.model.mappingName);
            }, this));
        },

        saveScript: function() {
            var scriptDetails;

            scriptDetails = this.linkQualifierScript.generateScript();

            if(scriptDetails !== null) {
                ScriptDelegate.evalLinkQualifierScript(scriptDetails).then(_.bind(function (result) {
                    if(_.isArray(result)) {
                        _.each(result, function(item) {
                            if(!_.isString(item)) {
                                this.model.scriptError = true;
                            }
                        }, this);
                    } else {
                        this.model.scriptError = true;
                    }

                    if(!this.model.scriptError) {
                        this.model.scriptResult = result;

                        this.model.mapping.linkQualifiers = this.linkQualifierScript.generateScript();
                        LinkQualifierUtils.setLinkQualifier(this.model.scriptResult, this.model.mappingName);

                        this.AbstractMappingSave(this.model.mapping, _.bind(function() {
                            EventManager.sendEvent(Constants.EVENT_DISPLAY_MESSAGE_REQUEST, "linkQualifierSaveSuccess");
                            EventManager.sendEvent(Constants.EVENT_QUALIFIER_CHANGED, this.model.mappingName);
                        }, this));

                    } else {
                        this.showErrorMessage($.t("templates.mapping.validLinkQualifierScript"));
                    }
                }, this),
                _.bind(function (result) {
                    this.model.scriptError = true;
                    this.showErrorMessage(result.responseJSON.message);
                }, this));
            } else {
                this.showErrorMessage($.t("templates.mapping.validLinkQualifierScript"));
            }
        },

        showErrorMessage: function(message) {
            this.$el.find("#badLinkQualifierScript .message").html(message);
            this.$el.find("#badLinkQualifierScript").show();
        }
    });

    return new LinkQualifiersView();
});
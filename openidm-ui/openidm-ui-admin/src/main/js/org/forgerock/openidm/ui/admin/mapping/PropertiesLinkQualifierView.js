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

define("org/forgerock/openidm/ui/admin/mapping/PropertiesLinkQualifierView", [
    "org/forgerock/commons/ui/common/main/AbstractView",
    "org/forgerock/openidm/ui/common/delegates/ConfigDelegate",
    "org/forgerock/commons/ui/common/util/Constants",
    "org/forgerock/commons/ui/common/main/EventManager",
    "org/forgerock/openidm/ui/admin/delegates/BrowserStorageDelegate",
    "org/forgerock/openidm/ui/admin/mapping/MappingBaseView",
    "org/forgerock/openidm/ui/admin/util/InlineScriptEditor",
    "org/forgerock/openidm/ui/admin/delegates/ScriptDelegate",
    "org/forgerock/openidm/ui/admin/util/LinkQualifierUtils"
], function(AbstractView,
            ConfigDelegate,
            Constants,
            EventManager,
            BrowserStorageDelegate,
            MappingBaseView,
            inlineScriptEditor,
            ScriptDelegate,
            LinkQualifierUtils) {

    var PropertiesLinkQualifierView = AbstractView.extend({
        template: "templates/admin/mapping/PropertiesLinkQualifierTemplate.html",
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

        render: function (args) {
            this.model.mappingName = args;
            this.model.mapping = MappingBaseView.currentMapping();

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
                        "onBlurPassedVariable" : _.bind(this.showLinkQualifier, this),
                        "onKeypress" : _.bind(function(){
                            this.$el.find(".linkQualifierSave").prop("disabled", true);
                        }, this),
                        "eventName": "linkQualifierScript",
                        "scriptData": scriptData,
                        "disablePassedVariable": false,
                        "onBlur" : _.bind(this.showLinkQualifier, this),
                        "onDeletePassedVariable": _.bind(this.showLinkQualifier, this),
                        "onAddPassedVariable": _.bind(this.showLinkQualifier, this),
                        "disableValidation" : false,
                        "placeHolder" : "['test', 'default']"
                    },
                    _.bind(function(){}, this));

                $("#linkQualifierPanel").on('shown.bs.collapse', _.bind(function () {
                    this.linkQualifierScript.refresh();
                }, this));
            });
        },

        showLinkQualifier: function(event) {
            var scriptDetails,
                validationResults =  this.linkQualifierScript.getValidation();

            if(validationResults) {
                scriptDetails = this.linkQualifierScript.generateScript();

                if(scriptDetails !== null) {
                    ScriptDelegate.evalLinkQualifierScript(scriptDetails).then(_.bind(function (result) {
                            this.model.scriptError = false;

                            this.$el.find("#scriptLinkQualifierList").empty();

                            this.model.scriptResult = result;

                            this.$el.find(".linkQualifierSave").prop("disabled", false);

                            this.populateScriptLinkQualifier(result);
                        }, this),
                        _.bind(function (result) {
                            this.model.scriptError = true;
                            this.$el.find(".linkQualifierSave").prop("disabled", true);
                            this.$el.find("#badLinkQualifierScript .message").html(result.responseJSON.message);
                            this.$el.find("#badLinkQualifierScript").show();
                        }, this));
                }
            } else {
                this.$el.find("#scriptLinkQualifierList").empty();
                this.$el.find("#badLinkQualifierScript .message").html($.t("templates.mapping.validLinkQualifierScript"));
                this.$el.find("#badLinkQualifierScript").show();
                this.$el.find(".linkQualifierSave").prop("disabled", true);
            }
        },

        populateScriptLinkQualifier : function (data) {
            if(_.isArray(data) === true) {
                _.each(data, function(linkQualifier){
                    this.$el.find("#scriptLinkQualifierList").append('<button disabled="true" type="button" class="removeLinkQualifier btn btn-primary">'
                        + '<span class="linkQualifier">' +linkQualifier  +'</span>'
                        + '</button>');
                }, this);


                this.$el.find(".linkQualifierSave").prop("disabled", false);
                this.$el.find("#badLinkQualifierScript").hide();
            } else {
                this.$el.find(".linkQualifierSave").prop("disabled", true);
                this.$el.find("#badLinkQualifierScript .message").html($.t("templates.mapping.badScript"));
                this.$el.find("#badLinkQualifierScript").show();
            }
        },

        sectionControl: function(event) {
            var selected = $(event.target),
                currentTab = selected.prop("id");

            selected.parent().find('.active').removeClass('active');

            selected.toggleClass('active', true);

            if(currentTab === "staticQualifierTab") {
                this.$el.find(".linkQualifierSave").prop("disabled", false);
            } else {
                if(this.model.scriptError === true) {
                    this.$el.find(".linkQualifierSave").prop("disabled", true);
                }
            }
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
            var sync = MappingBaseView.data.syncConfig,
                mapping,
                currentTab = this.$el.find("#linkQualifierTabs .active").prop("id");

            _.each(sync.mappings, function (map, key) {
                if (map.name === this.model.mappingName) {

                    if(currentTab === "staticQualifierTab") {
                        sync.mappings[key].linkQualifiers = this.data.linkQualifiers;
                        mapping = sync.mappings[key];
                        LinkQualifierUtils.setLinkQualifier(this.data.linkQualifiers, this.model.mappingName);

                    } else {
                        sync.mappings[key].linkQualifiers = this.linkQualifierScript.generateScript();
                        mapping = sync.mappings[key];
                        LinkQualifierUtils.setLinkQualifier(this.model.scriptResult, this.model.mappingName);
                    }
                }
            }, this);


            ConfigDelegate.updateEntity("sync", sync).then(_.bind(function() {
                EventManager.sendEvent(Constants.EVENT_DISPLAY_MESSAGE_REQUEST, "linkQualifierSaveSuccess");
                BrowserStorageDelegate.set("currentMapping", mapping);

                EventManager.sendEvent(Constants.EVENT_QUALIFIER_CHANGED, this.model.mappingName);
            }, this));
        }
    });

    return new PropertiesLinkQualifierView();
});
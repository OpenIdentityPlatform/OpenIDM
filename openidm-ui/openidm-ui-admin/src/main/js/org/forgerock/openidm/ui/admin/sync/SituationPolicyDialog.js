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

/*global define, $, _, require, window */

define("org/forgerock/openidm/ui/admin/sync/SituationPolicyDialog", [
    "org/forgerock/commons/ui/common/main/AbstractView",
    "org/forgerock/commons/ui/common/main/Configuration",
    "org/forgerock/commons/ui/common/util/Constants",
    "org/forgerock/commons/ui/common/main/EventManager",
    "org/forgerock/commons/ui/common/util/UIUtils",
    "org/forgerock/openidm/ui/common/delegates/ConfigDelegate",
    "org/forgerock/openidm/ui/admin/delegates/BrowserStorageDelegate"
], function(AbstractView, conf, constants, eventManager, uiUtils, ConfigDelegate, BrowserStorageDelegate) {
    var SituationPolicyDialog = AbstractView.extend({
        template: "templates/admin/sync/SituationPolicyDialogTemplate.html",
        el: "#dialogs",
        events: {
            "change #policyPatterns": "setPolicies",
            "change .action": "updatePattern"
        },

        render: function(mapping, pattern, allPatterns, callback) {
            var btns = [];

            this.mapping = mapping;
            this.pattern = pattern;
            this.allPatterns = allPatterns;
            this.callback = callback;
            this.recon = mapping.recon;

            this.currentDialog = $('<div id="situationPolicyDialog"></div>');
            this.setElement(this.currentDialog);
            $('#dialogs').append(this.currentDialog);

            btns.push({
                text: $.t("common.form.cancel"),
                click: function() {
                    if (callback) {
                        callback(false);
                    }
                    $("#situationPolicyDialog").dialog('destroy').remove();
                }
            });
            btns.push({
                text: $.t("common.form.save"),
                click: _.bind(function() {
                    var definedPolicies = this.getPolicies(),
                        mapping;

                    ConfigDelegate.readEntity("sync").then(_.bind(function(data) {
                        _(data.mappings).each(function(map, index) {
                            if (map.name === this.mapping.name) {
                                data.mappings[index].policies = definedPolicies.policies;
                                mapping = map;
                            }
                        }, this);

                        ConfigDelegate.updateEntity("sync", data).then(_.bind(function() {
                            BrowserStorageDelegate.set("currentMapping", _.extend(mapping,this.recon));
                            eventManager.sendEvent(constants.EVENT_DISPLAY_MESSAGE_REQUEST, "syncPolicySaveSuccess");
                        }, this));

                        if (callback) {
                            callback(definedPolicies);
                        }
                    }, this));

                    $("#situationPolicyDialog").dialog('destroy').remove();
                }, this)
            });

            this.currentDialog.dialog({
                title: $.t("templates.situationalPolicies.dialogTitle"),
                modal: true,
                resizable: false,
                draggable: true,
                dialogClass: "situationPolicyDialog",
                width: 420,
                height: 630,
                position: { my: "center", at: "center", of: window },
                buttons: btns,
                close: _.bind(function () {
                    if (this.currentDialog) {
                        this.currentDialog.dialog('destroy').remove();
                    }
                }, this)
            });

            uiUtils.renderTemplate(
                this.template,
                this.$el,
                _.extend({}, conf.globalData, this.data),
                _.bind(function() {
                    this.loadPatterns();
                }, this),
                "replace"
            );
        },

        /**
         *  Populated the Policies section with the default IDM policies
         *  then sets their values based off of the current mapping's policy
         */
        loadPatterns: function() {
            _(this.allPatterns).each(function(pattern, key) {
                $("#policyPatterns")
                    .append($("<option></option>")
                        .attr("value", key)
                        .text(key));
            }, this);

            // Add default data
            _(this.allPatterns["Default Actions"].policies).each(function(policy) {
                var newPolicy = $("#situationCopy").clone();
                newPolicy.find(".situation").html(policy.situation);
                newPolicy.find(".action").val(policy.action);
                newPolicy.find(".action").find(":selected").html(policy.action + " &#9733;");

                // Removes all options that are not viable for a given situation
                _(newPolicy.find(".action option")).filter(function(option) {
                    if (_(policy.options).indexOf($(option).val()) === -1) {
                        $(option).remove();
                        return false;
                    }
                }, this);

                newPolicy.removeAttr("id");
                newPolicy.addClass(policy.situation);
                $("#situationCopy").after(newPolicy);
            }, this);

            $("#policyPatterns").val(this.pattern).change();
        },

        /**
         * When the select box for policy patterns changes the values below change to reflect those changes
         */
        setPolicies: function() {
            var policies;

            if ($("#policyPatterns").val() === "Custom") {
                policies = this.mapping.policies;
            } else {
                policies = this.allPatterns[$("#policyPatterns").val()].policies;
            }

            // First reset to defaults
            _(this.allPatterns["Default Actions"].policies).each(function(policy) {
                $("."+policy.situation + " .action").val(policy.action);
            }, this);

            // Apply differences from defaults and the supplied policy
            _(policies).each(function(policy) {
                $("."+policy.situation + " .action").val(policy.action);
            }, this);

            $("#patternDescription").text(this.allPatterns[$("#policyPatterns").val()].description);
        },

        /**
         * Return the array of policies currently set and the pattern name
         */
        getPolicies: function() {
            var policies = [],
                patternName = "Custom",
                patternDescription = "",
                currentPattern = [],
                patternFound = false;

            function policySorter(policy) {
                return policy.situation;
            }

            _($(".situationRow").not("#situationCopy")).each(function(row) {
                policies.push({
                    "situation" : $(row).find(".situation").text(),
                    "action" : $(row).find(".action").val()
                });
            }, this);


            policies = _.sortBy(policies, policySorter);

            _(this.allPatterns).each(_.bind(function(pattern, name) {
                currentPattern = _.chain(pattern.policies)
                    .map(function(policy) {
                        return _.omit(policy, "options");
                    })
                    .sortBy(policySorter)
                    .value();

                if (_(currentPattern).isEqual(policies)) {
                    patternName = name;
                    patternDescription = pattern.description;
                    patternFound = true;
                    return false;
                }
            },this));

            if (!patternFound) {
                patternName = "Custom";
                patternDescription = this.allPatterns.Custom.description;
            }

            return {policies: policies, patternName: patternName, patternDescription: patternDescription};
        },

        updatePattern: function(e) {
            var currentPolicies = this.getPolicies();

            $("#policyPatterns").val(currentPolicies.patternName);
        }
    });

    return new SituationPolicyDialog();
});


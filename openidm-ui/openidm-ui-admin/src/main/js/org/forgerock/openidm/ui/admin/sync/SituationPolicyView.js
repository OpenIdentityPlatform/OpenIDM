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

define("org/forgerock/openidm/ui/admin/sync/SituationPolicyView", [
    "org/forgerock/commons/ui/common/main/AbstractView",
    "org/forgerock/commons/ui/common/main/Configuration",
    "org/forgerock/commons/ui/common/util/Constants",
    "org/forgerock/commons/ui/common/main/EventManager",
    "org/forgerock/commons/ui/common/util/UIUtils",
    "org/forgerock/openidm/ui/common/delegates/ConfigDelegate",
    "org/forgerock/openidm/ui/admin/delegates/BrowserStorageDelegate"
], function(AbstractView, conf, constants, eventManager, uiUtils, ConfigDelegate, BrowserStorageDelegate) {
    var SituationPolicyView = AbstractView.extend({

        element: "#policyPattern",
        noBaseTemplate: true,
        template: "templates/admin/sync/SituationPolicyViewTemplate.html",
        events: {
            "change #policyPatterns": "setPolicies",
            "change .action": "updatePattern",
            "click .savePolicy": "save"
        },
        data: {
            star: "&#9733;",
            hollowStar: "&#9734;"
        },
        model: {
            successMessage: "triggeredBySituationSaveSuccess",
            allPatterns: [],
            currentPattern: "",
            lookup: {
                situations: {
                    "SOURCE_MISSING": "Source Missing",
                    "ALL_GONE": "All Gone",
                    "SOURCE_IGNORED": "Source Ignored",
                    "UNQUALIFIED": "Unqualified",
                    "AMBIGUOUS": "Ambiguous",
                    "FOUND_ALREADY_LINKED": "Found Already Linked",
                    "CONFIRMED": "Confirmed",
                    "UNASSIGNED": "Unassigned",
                    "LINK_ONLY": "Link Only",
                    "TARGET_IGNORED": "Target Ignored",
                    "MISSING": "Missing",
                    "ABSENT": "Absent",
                    "FOUND": "Found"
                },
                actions: {
                    "IGNORE": "Ignore",
                    "DELETE": "Delete",
                    "UNLINK": "Unlink",
                    "EXCEPTION": "Exception",
                    "REPORT": "Report",
                    "NOREPORT": "No Report",
                    "ASYNC": "Async",
                    "CREATE": "Create",
                    "UPDATE": "Update"
                }

            }
        },

        /***
         * @param args {object}
         *      sync {object}
         *      mapping {object}
         *      mappingName {string}
         *
         */
        render: function(args, callback) {
            this.model.sync = args.sync;
            this.model.mapping = args.mapping;
            this.model.mappingName = args.mappingName;
            this.data.docHelpUrl = constants.DOC_URL;
            this.model.saveCallback = args.saveCallback;

            this.parentRender(function () {
                this.getPatterns().then(function() {
                    if (callback) {
                        callback();
                    }
                });
            });
        },

        getPatterns: function() {
            var currentPattern = [],
                currentPolicy = [],
                patternFound = false;

            function policySorter(policy) {
                return policy.situation;
            }

            return $.getJSON("templates/admin/sync/situationalPolicyPatterns.json", _.bind(function(patterns) {
                this.model.allPatterns = patterns;

                _(patterns).each(_.bind(function(pattern, name) {
                    currentPattern = _.chain(pattern.policies)
                        .map(function(policy) {
                            return _.omit(policy, "options", "color", "note");
                        })
                        .sortBy(policySorter)
                        .value();

                    currentPolicy = _.sortBy(this.model.mapping.policies, policySorter);

                    this.$el.find("#policyPatterns")
                        .append($("<option></option>")
                            .attr("value", name)
                            .text(name));

                    if (_(currentPattern).isEqual(currentPolicy)) {
                        patternFound = true;
                        this.model.currentPattern = name;
                    }
                }, this));

                if (!patternFound) {
                    this.model.currentPattern = "Custom";
                }

                // Add default data
                _(this.model.allPatterns["Default Actions"].policies).each(function(policy) {
                    var newPolicy = $("#situationCopy").clone();
                    newPolicy.find(".situation span").html(this.model.lookup.situations[policy.situation]);
                    newPolicy.find(".action").val(policy.action);
                    newPolicy.find(".action").find(":selected").html(this.model.lookup.actions[policy.action] + " " + this.data.star);

                    switch (policy.color) {
                        case "red":
                            newPolicy.find(".color").addClass("failure-display");
                            break;
                        case "yellow":
                            newPolicy.find(".color").addClass("warning-display");
                            break;
                        case "green":
                            newPolicy.find(".color").addClass("success-display");
                            break;
                    }

                    newPolicy.find(".info").attr("data-title", $.t(policy.note));

                    // Stars the more acceptable choices
                    _(newPolicy.find(".action option")).filter(function(option) {
                        if (_(policy.options).indexOf($(option).val()) >= 0) {
                            $(option).html($(option).text() + " " + this.data.hollowStar);

                            return false;
                        }
                    }, this);

                    newPolicy.removeAttr("id");
                    newPolicy.addClass(policy.situation);
                    this.$el.find("#situationCopy").before(newPolicy);
                }, this);

                this.$el.find("#policyPatterns").val(this.model.currentPattern).change();

                this.$el.find(".info").popover({
                    content: function () { return $(this).attr("data-title");},
                    trigger:'hover click',
                    placement:'top',
                    html: 'true',
                    template: '<div class="popover popover-info" role="tooltip"><div class="popover-content"></div></div>'
                });
            }, this));
        },

        /**
         * When the select box for policy patterns changes the values below change to reflect those changes
         */
        setPolicies: function() {
            var policies,
                newPattern = this.$el.find("#policyPatterns").val();

            if (newPattern === "Custom") {
                policies = this.model.mapping.policies;
            } else {
                policies = this.model.allPatterns[newPattern].policies;
            }

            // First reset to defaults
            _(this.model.allPatterns["Default Actions"].policies).each(function (policy) {
                $("." + policy.situation + " .action").val(policy.action);
            }, this);

            // Apply differences from defaults and the supplied policy
            _(policies).each(function (policy) {
                $("." + policy.situation + " .action").val(policy.action);
            }, this);

            this.$el.find("#patternDescription").text(this.model.allPatterns[newPattern].description);
        },

        /**
         * When the select box for a situation changes
         */
        updatePattern: function(e) {
            var currentPolicies = this.getPolicies();

            this.$el.find("#policyPatterns").val(currentPolicies.patternName);
        },

        /**
         * Return the array of policies currently set and the pattern name
         */
        getPolicies: function() {
            var policies = [],
                patternName = "",
                patternDescription = "",
                currentPattern = [],
                patternFound = false;

            function policySorter(policy) {
                return policy.situation;
            }

            _(this.$el.find(".situationRow").not("#situationCopy")).each(function(row) {
                policies.push({
                    "situation" : _.invert(this.model.lookup.situations)[$(row).find(".situation span").text()],
                    "action" : $(row).find(".action").val()
                });
            }, this);

            policies = _.sortBy(policies, policySorter);

            _(this.model.allPatterns).each(_.bind(function(pattern, name) {
                currentPattern = _.chain(pattern.policies)
                    .map(function(policy) {
                        return _.omit(policy, "options", "color", "note");
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
                patternDescription = this.model.allPatterns.Custom.description;
            }

            return {policies: policies, patternName: patternName, patternDescription: patternDescription};
        },


        save: function() {
            var definedPolicies = this.getPolicies(),
                mapping,
                _this = this;

            ConfigDelegate.readEntity("sync").then(_.bind(function(data) {
                _(data.mappings).each(function(map, index) {
                    if (map.name === this.model.mapping.name) {
                        data.mappings[index].policies = definedPolicies.policies;
                        mapping = map;
                    }
                }, this);

                ConfigDelegate.updateEntity("sync", data).then(function() {
                    BrowserStorageDelegate.set("currentMapping", _.extend(mapping, this.recon));
                    eventManager.sendEvent(constants.EVENT_DISPLAY_MESSAGE_REQUEST, "syncPolicySaveSuccess");
                    _this.model.saveCallback();
                });
            }, this));
        }
    });

    return new SituationPolicyView();
});
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
 * Copyright 2015-2016 ForgeRock AS.
 */

define([
    "jquery",
    "underscore",
    "org/forgerock/openidm/ui/admin/settings/audit/AuditAdminAbstractView",
    "org/forgerock/openidm/ui/admin/settings/audit/AuditFilterPoliciesDialog",
    "org/forgerock/commons/ui/common/components/ChangesPending"
], function($, _,
            AuditAdminAbstractView,
            AuditFilterPoliciesDialog,
            ChangesPending) {

    var AuditFilterPoliciesView = AuditAdminAbstractView.extend({
        template: "templates/admin/settings/audit/AuditFilterPoliciesTemplate.html",
        element: "#AuditFilterPoliciesView",
        noBaseTemplate: true,
        events: {
            "click .add-filter": "addFilter",
            "click .edit-filter": "editFilter",
            "click .delete-filter": "deleteFilter"
        },

        render: function (args, callback) {
            this.data = {};
            this.model = {
                filterPolicies: {}
            };

            if (!_.has(args, "model")) {
                this.model.auditData = this.getAuditData();

                if (_.has(this.model.auditData, "auditServiceConfig") && _.has(this.model.auditData.auditServiceConfig, "filterPolicies")) {
                    _.extend(this.model.filterPolicies, this.model.auditData.auditServiceConfig.filterPolicies);
                }

            } else {
                this.model = args.model;
            }

            this.formatData();

            this.parentRender(_.bind(function() {
                if (!_.has(this.model, "changesModule")) {
                    this.model.changesModule = ChangesPending.watchChanges({
                        element: this.$el.find(".audit-filter-alert"),
                        undo: true,
                        watchedObj: _.clone(this.model.auditData.auditServiceConfig, true),
                        watchedProperties: ["filterPolicies"],
                        undoCallback: _.bind(function (original) {
                            this.model.filterPolicies = original.filterPolicies;
                            this.reRender();
                        }, this)
                    });
                } else {
                    this.model.changesModule.reRender(this.$el.find(".audit-filter-alert"));
                    if (args && args.saved) {
                        this.model.changesModule.saveChanges();
                    }
                }

                if (callback) {
                    callback();
                }
            }, this));
        },

        /**
         * Sets the filters to the global config and checks for changes, then rerenders.
         */
        reRender: function() {
            this.setFilterPolicies(this.model.filterPolicies);
            this.model.changesModule.makeChanges({"filterPolicies": this.model.filterPolicies});
            this.render({model: this.model});
        },

        /**
         *  Converts the Audit.json format for filters into a flat array renderable by Handlebars
         *
         *   FROM:
         *   "filterPolicies" : {
         *       "field" : {
         *           "excludeIf" : [ ],
         *           "includeIf" : [
         *               "/access/filter/field"
         *           ]
         *       },
         *       "value" : {
         *           "excludeIf" : [
         *               "/access/filter/value"
         *           ],
         *           "includeIf" : [ ]
         *       }
         *   }
         *
         *   TO:
         *   [
         *      {"type": "Field", "typeLiteral": "field", "includeExclude": "Include", "includeExcludeLiteral": "includeIf", "location": "/access/filter/field"},
         *      {"type": "Value", "typeLiteral": "value", "includeExclude": "Exclude", "includeExcludeLiteral": "excludeIf", "location": "/access/filter/value"}
         *   ]
         */
        formatData: function() {
            this.data.filters = [];
            var tempLocation;

            function addFilter(type, includeExclude) {
                _.each(this.model.filterPolicies[type][includeExclude], function(location) {
                    tempLocation = location.split("/");

                    this.data.filters.push({
                        "type": $.t("templates.audit.filterPolicies." + type),
                        "typeLiteral": type,
                        "includeExclude": $.t("templates.audit.filterPolicies." + includeExclude),
                        "includeExcludeLiteral": includeExclude,
                        "topic": tempLocation[1] || "",
                        "location": tempLocation.splice(2).join("/") || location
                    });
                }, this);
            }

            if (_.has(this.model.filterPolicies, "field")) {
                if (_.has(this.model.filterPolicies.field, "excludeIf")) {
                    _.bind(addFilter, this)("field", "excludeIf");
                }

                if (_.has(this.model.filterPolicies.field, "includeIf")) {
                    _.bind(addFilter, this)("field", "includeIf");
                }
            }

            if (_.has(this.model.filterPolicies, "value")) {
                if (_.has(this.model.filterPolicies.value, "excludeIf")) {
                    _.bind(addFilter, this)("value", "excludeIf");
                }

                if (_.has(this.model.filterPolicies.value, "includeIf")) {
                    _.bind(addFilter, this)("value", "includeIf");
                }
            }
        },

        /**
         * On click the selected row is identified and the location corresponding to that row is spliced out.
         * @param e
         */
        deleteFilter: function(e) {
            e.preventDefault();

            var selected = $(e.currentTarget).closest(".filter")[0],
                selectedFilter = {},
                filterLocation;

            _.each(this.$el.find(".filters .filter"), _.bind(function(row, index) {
                if (row === selected) {
                    selectedFilter = this.data.filters[index];
                }
            }, this));

            filterLocation = this.model.filterPolicies[selectedFilter.typeLiteral][selectedFilter.includeExcludeLiteral].indexOf("/" + selectedFilter.topic + "/" + selectedFilter.location);
            this.model.filterPolicies[selectedFilter.typeLiteral][selectedFilter.includeExcludeLiteral].splice(filterLocation, 1);

            this.reRender();
        },

        /**
         * Opens an editor preloaded with the existing configuration of the selected filter.
         * Upon submiting the dialog the old filter is removed and the new one is added.
         * @param e
         */
        editFilter: function(e) {
            e.preventDefault();

            var selected = $(e.currentTarget).closest(".filter")[0],
                selectedFilter = {},
                filterLocation;

            _.each(this.$el.find(".filters .filter"), _.bind(function(row, index) {
                if (row === selected) {
                    selectedFilter = this.data.filters[index];
                }
            }, this));

            AuditFilterPoliciesDialog.render(
                {
                    "newFilter": false,
                    "filter": selectedFilter,
                    "saveCallback": _.bind(function(type, includeExclude, location) {
                        filterLocation = this.model.filterPolicies[selectedFilter.typeLiteral][selectedFilter.includeExcludeLiteral].indexOf("/" + selectedFilter.topic + "/" + selectedFilter.location);
                        _.bind(this.saveFilter, this)(type, includeExclude, location, filterLocation);
                    }, this)
                }, _.noop
            );
        },

        saveFilter: function(type, includeExclude, location, filterLocation) {
            if (_.has(this.model.filterPolicies, type)) {
                if (_.has(this.model.filterPolicies[type], includeExclude)) {
                    if (_.isNumber(filterLocation) && filterLocation >= 0) {
                        this.model.filterPolicies[type][includeExclude].splice(filterLocation, 1, location);
                    } else {
                        this.model.filterPolicies[type][includeExclude].push(location);
                    }
                } else {
                    this.model.filterPolicies[type][includeExclude] = [location];
                }
            } else {
                this.model.filterPolicies[type] = {};
                this.model.filterPolicies[type][includeExclude] = [location];
            }

            this.reRender();
        },

        /**
         * Creates a new filter and pushes it to the appropriate array
         */
        addFilter: function(e) {
            e.preventDefault();

            AuditFilterPoliciesDialog.render(
                {
                    "newFilter": true,
                    "saveCallback": _.bind(function(type, includeExclude, location) {
                        this.saveFilter(type, includeExclude, location);
                    }, this)
                }, _.noop
            );
        }
    });

    return new AuditFilterPoliciesView();
});

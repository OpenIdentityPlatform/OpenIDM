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
    "handlebars",
    "org/forgerock/openidm/ui/admin/mapping/util/MappingAdminAbstractView",
    "org/forgerock/commons/ui/common/main/EventManager",
    "org/forgerock/commons/ui/common/util/Constants",
    "org/forgerock/openidm/ui/admin/mapping/util/QueryFilterEditor"
], function($,
            _,
            Handlebars,
            MappingAdminAbstractView,
            eventManager,
            constants,
            QueryFilterEditor) {

    var ReconQueryFilterEditor = QueryFilterEditor.extend({
            // template: "templates/admin/mapping/util/MappingSetupFilter.html",
            showLegendAndRadios() {
                this.data.hidden = _.isEmpty(this.data.filterString);
            },
            renderExpressionTree(callback) {
                this.showLegendAndRadios();
                QueryFilterEditor.prototype.renderExpressionTree.call(this, callback);
            }

        }),
        ReconciliationQueryFiltersView = MappingAdminAbstractView.extend({
            element: "#reconQueriesView",
            template: "templates/admin/mapping/association/ReconciliationQueryFiltersTemplate.html",
            noBaseTemplate: true,
            events: {
                "click input[type=submit]": "saveQueryFilters"
            },
            model: {
                queryEditors: [
                    {
                        "type": "source"
                    },
                    {
                        "type": "target"
                    }
                ]
            },

            render: function() {
                this.model.sync = this.getSyncConfig();
                this.model.mapping = this.getCurrentMapping();
                this.model.mappingName = this.getMappingName();

                this.parentRender(_.bind(function () {
                    this.model.queryEditors = _.map(this.model.queryEditors, function (qe) {
                        qe.query = qe.type + "Query";
                        qe.resource = this.model.mapping[qe.type];
                        qe.editor = this.renderEditor(qe.query, this.model.mapping[qe.query], qe.resource);
                        return qe;
                    }, this);
                }, this));
            },

            renderEditor: function (element, query, resource) {
                var editor = new ReconQueryFilterEditor(),
                    fullEntry = this.model.mapping[element + "FullEntry"],
                    filter;

                if (query !== undefined) {
                    filter = query._queryFilter || query.queryFilter;
                } else {
                    filter = "";
                }

                if (_.isUndefined(fullEntry)) {
                    fullEntry = "default";
                } else {
                    fullEntry = fullEntry.toString();
                }
                editor.render({
                    "queryFilter": filter,
                    "element": "#" + element,
                    "resource": resource,
                    data: {
                        element,
                        legend: $.t("templates.correlation." + element),
                        hidden: true,
                        fullEntry
                    }
                });

                return editor;
            },

            saveQueryFilters: function (e) {
                var queries;

                e.preventDefault();

                this.model.mapping = this.getCurrentMapping();

                queries =  _.chain(this.model.queryEditors)
                    .filter(function (qe) {
                        return _.has(qe, "editor");
                    })
                    .map(function (qe) {
                        var filterString = qe.editor.getFilterString();
                        if (filterString.length) {
                            return [
                                qe.query,
                                {
                                    "_queryFilter": filterString
                                }
                            ];
                        } else {
                            return null;
                        }
                    })
                    .filter(function (qe) {
                        return qe !== null;
                    })
                    .object()
                    .value();

                _.each(this.model.queryEditors, function (qe) {
                    let fullEntryName = qe.query + "FullEntry",
                        fullEntryValue = this.$el.find("input[name='" + fullEntryName + "Radios']:checked").val();
                    // set query filter value
                    this.model.mapping[qe.query] = queries[qe.query];

                    // set flag for full entry
                    if (_.isUndefined(fullEntryValue) || fullEntryValue.match("default") || _.isEmpty(queries[qe.query])) {
                        this.model.mapping = _.omit(this.model.mapping, fullEntryName);
                    } else {
                        if (fullEntryValue.match("true")) {
                            this.model.mapping = _.set(this.model.mapping, fullEntryName, true);
                        } else if (fullEntryValue.match("false")) {
                            this.model.mapping = _.set(this.model.mapping, fullEntryName, false);
                        }
                    }

                }, this);

                this.AbstractMappingSave(this.model.mapping, _.bind(function() {
                    eventManager.sendEvent(constants.EVENT_DISPLAY_MESSAGE_REQUEST, "reconQueryFilterSaveSuccess");
                }, this));
            }
        });

    Handlebars.registerHelper ("setInitialRadioChecked", function (value, currentValue) {
        if ( value === currentValue ) {
            return "checked";
        } else {
            return "";
        }
    });

    return new ReconciliationQueryFiltersView();
});

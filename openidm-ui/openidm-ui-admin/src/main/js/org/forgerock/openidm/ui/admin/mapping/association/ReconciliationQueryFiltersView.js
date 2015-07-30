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

/*global define */

define("org/forgerock/openidm/ui/admin/mapping/association/ReconciliationQueryFiltersView", [
    "underscore",
    "org/forgerock/openidm/ui/admin/mapping/util/MappingAdminAbstractView",
    "org/forgerock/commons/ui/common/main/EventManager",
    "org/forgerock/commons/ui/common/util/Constants",
    "org/forgerock/openidm/ui/admin/mapping/util/QueryFilterEditor"
], function(_,
            MappingAdminAbstractView,
            eventManager,
            constants,
            QueryFilterEditor) {

    var ReconciliationQueryFiltersView = MappingAdminAbstractView.extend({
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
            var editor = new QueryFilterEditor(),
                filter;

            if (query !== undefined) {
                filter = query._queryFilter || query.queryFilter;
            } else {
                filter = "";
            }

            editor.render({
                "queryFilter": filter,
                "element": "#" + element,
                "resource": resource
            });

            return editor;
        },

        saveQueryFilters: function (e) {
            var queries;

            e.preventDefault();

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
                this.model.mapping[qe.query] = queries[qe.query];
            }, this);

            this.AbstractMappingSave(this.model.mapping, _.bind(function() {
                eventManager.sendEvent(constants.EVENT_DISPLAY_MESSAGE_REQUEST, "reconQueryFilterSaveSuccess");
            }, this));
        }
    });

    return new ReconciliationQueryFiltersView();
});

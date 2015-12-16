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
 * Copyright 2011-2015 ForgeRock AS.
 */

/*global define */

define("org/forgerock/openidm/ui/admin/util/BackgridUtils", [
    "underscore",
    "backbone",
    "org/forgerock/commons/ui/common/backgrid/Backgrid",
    "org/forgerock/commons/ui/common/util/BackgridUtils"

], function (_, Backbone, Backgrid, commonBackgridUtils) {
    var obj = _.extend({}, commonBackgridUtils);

    obj.queryFilter = function (data) {
        if(data === undefined) { data = {}; }

        var params = [],
            additionalFilters = data._queryFilter || [],
            getFilter = (function () {
                return function (filterName, filterQuery) {
                        return filterName + ' sw "' + filterQuery.replace(/"/g, '\\"') + '"';
                    };
            }());

        _.each(this.state.filters, function (filter) {
            if (filter.query() !== '') {
                params.push(getFilter(filter.name, filter.query()));
            }
        });
        params = params.concat(additionalFilters);

        return params.length === 0 ? true : params.join(" AND ");
    };
    
    obj.getQueryParams = function (data, isSystemResource) {
        data = data || {};
        var queryParams = {
                _sortKeys: this.sortKeys,
                _queryFilter: function () {
                    return obj.queryFilter.call(this, { _queryFilter: data._queryFilter });
                },
                _fields: data._fields || "",
                pageSize: "_pageSize",
                _pagedResultsOffset: this.pagedResultsOffset,
                _totalPagedResultsPolicy: "ESTIMATE"
            };
        
        if (isSystemResource) {
            delete queryParams._fields;
        }

        return queryParams;
    };

    obj.getState = function (sortCol, data) {
        var state = {
            pageSize: 50,
            sortKey: sortCol
        };

        if (data && typeof data === 'object') {
            _.extend(state, data);
        }
        return state;
    };
    
    obj.escapedStringCell = function (prop) {
        return Backgrid.Cell.extend({
            render: function () {
                if (this.model.get(prop)) {
                    this.$el.html(this.model.escape(prop));
                } else {
                    this.$el.html("");
                }
                return this;
            }
        });
    };
    
    return obj;

});

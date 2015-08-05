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
 * Portions copyright 2011-2015 ForgeRock AS.
 */

/*global define, sessionStorage */

define("org/forgerock/openidm/ui/common/util/JQGridUtil", [
    "underscore",
    "jquery",
    "org/forgerock/commons/ui/common/util/DateUtil",
    "jqgrid"
], function (_, $, dateUtil) {
    var obj = {};

    // TODO: Remove when jqgrid has been replaced by backgrid
    obj.buildJQGrid = function (view, id, options, additional, callback) {
        options = options ? options : {};

        if (!id || !view || !options.url) {
            return null;
        }

        var grid = view.$el.find('#' + id),
            cm = options.colModel,
            showSearch,
            saveColumnState = function (perm) {
                var colModel = this.jqGrid('getGridParam', 'colModel'), i, l = colModel.length, colItem, cmName,
                    postData = this.jqGrid('getGridParam', 'postData'),
                    gridState = {
                        search: this.jqGrid('getGridParam', 'search'),
                        rowNum: this.jqGrid('getGridParam', 'rowNum'),
                        page: this.jqGrid('getGridParam', 'page'),
                        sortname: this.jqGrid('getGridParam', 'sortname'),
                        sortorder: this.jqGrid('getGridParam', 'sortorder'),
                        permutation: perm,
                        colStates: {}
                    },
                    colStates = gridState.colStates;

                for (i = 0; i < l; i++) {
                    colItem = colModel[i];
                    cmName = colItem.name;
                    if (cmName !== 'rn' && cmName !== 'cb' && cmName !== 'subgrid') {
                        colStates[cmName] = {
                            width: colItem.width,
                            hidden: colItem.hidden
                        };
                    }
                }
                sessionStorage.setItem(additional.storageKey + '-grid-state', JSON.stringify(gridState));
            },
            gridState,
            restoreColumnState = function (colModel) {
                var colItem, i, l = colModel.length, colStates, cmName,
                    gridState = JSON.parse(sessionStorage.getItem(additional.storageKey + '-grid-state'));

                if (gridState) {
                    colStates = gridState.colStates;
                    for (i = 0; i < l; i++) {
                        colItem = colModel[i];
                        cmName = colItem.name;
                        if (cmName !== 'rn' && cmName !== 'cb' && cmName !== 'subgrid') {
                            colModel[i] = $.extend(true, {}, colModel[i], colStates[cmName]);
                        }
                    }
                }
                return gridState;
            },
            defaultOptions = {
                datatype: "json",
                loadBeforeSend: function (jqXHR) {
                    jqXHR.setRequestHeader('Accept-API-Version', 'protocol=1.0,resource=1.0');
                },
                colNames: [],
                colModel: [],
                height: 'auto',
                width: 'none',
                jsonReader: {
                    root: function (obj) {
                        return obj.result;
                    },
                    total: function (obj) {  // total number of pages
                        var postedData = grid.jqGrid('getGridParam', 'postData'),
                            records = postedData._pagedResultsOffset + obj.remainingPagedResults + obj.resultCount,
                            pageSize = postedData._pageSize,
                            pages = Math.floor(records / pageSize);

                        if (records % pageSize > 0) {
                            pages += 1;
                        }

                        sessionStorage.setItem(additional.storageKey + '-pages-number', pages);
                        return pages;
                    },
                    records: function (obj) {  // total number of records
                        return grid.jqGrid('getGridParam', 'postData')._pagedResultsOffset + obj.remainingPagedResults +
                            obj.resultCount;
                    },
                    userdata: function (obj) {
                        return { remaining: obj.remainingPagedResults };
                    },
                    repeatitems: false
                },
                prmNames: {
                    nd: null,
                    sort: '_sortKeys',
                    search: '_queryFilter',
                    rows: '_pageSize' // number of records to fetch
                },
                serializeGridData: function (postedData) {
                    var i, length, filter = '', colNames, postedFilters, filterDataToDate,
                        searchOperator = additional.searchOperator || "co";

                    if (additional.serializeGridData) {
                        filter = additional.serializeGridData.call(this, postedData);
                    }

                    colNames = _.pluck(grid.jqGrid('getGridParam', 'colModel'), 'name');
                    _.each(colNames, function (element, index, list) {
                        if (postedData[element]) {
                            if (filter.length > 0) {
                                filter += ' AND ';
                            }
                            filter = filter.concat(element, ' ' + searchOperator + ' "', postedData[element], '"');
                        }
                        delete postedData[element];
                    });

                    if (additional.searchFilter) {
                        for (i = 0, length = additional.searchFilter.length; i < length; i++) {
                            if (filter.length > 0) {
                                filter += ' AND ';
                            }
                            filter = filter.concat(additional.searchFilter[i].field, ' ', additional.searchFilter[i].op, ' "', additional.searchFilter[i].val, '"');
                        }
                    }

                    // search window filters
                    if (postedData.filters) {
                        postedFilters = JSON.parse(postedData.filters);
                        for (i = 0, length = postedFilters.rules.length; i < length; i++) {
                            if (postedFilters.rules[i].data) {
                                if (filter.length > 0) {
                                    filter += ' AND ';
                                }
                                filterDataToDate = new Date(postedFilters.rules[i].data);
                                if (dateUtil.isDateValid(filterDataToDate)) {
                                    filter = filter.concat(postedFilters.rules[i].field, ' ', postedFilters.rules[i].op, ' ', filterDataToDate.getTime().toString());
                                } else {
                                    filter = filter.concat(postedFilters.rules[i].field, ' ', postedFilters.rules[i].op, ' "*', postedFilters.rules[i].data, '*"');
                                }
                            }
                        }
                    }

                    postedData._queryFilter = filter === '' ? true : filter;

                    postedData._pagedResultsOffset = postedData._pageSize * (postedData.page - 1);
                    delete postedData.page;

                    if (postedData._sortKeys) {
                        if (postedData.sord === 'desc') {
                            postedData._sortKeys = '-' + postedData._sortKeys;
                        }
                    }
                    delete postedData.sord;

                    return $.param(postedData);
                },
                loadComplete: function (data) {
                    saveColumnState.call( grid, grid[0].p.permutation);
                    //because of the bug in the used version of jquery.jqGrid-4.5.4-min.js we need to set selected option manually
                    view.$el.find(".ui-pg-selbox option[value=" + grid[0].p.rowNum + "]").prop("selected", true);
                    _.extend(view.data[id], data);
                },
                onPaging: function () {
                    var totalPagesNum = JSON.parse(sessionStorage.getItem(additional.storageKey + '-pages-number')),
                        inputVal = $($(this).jqGrid('getGridParam', 'pager')).find('input').val();
                    if (totalPagesNum !== null && /[0-9]+/.test(inputVal) && totalPagesNum < parseInt(inputVal, 10)) {
                        $(this).trigger('reloadGrid', {page: 1});
                        return 'stop';
                    }
                },
                pager: null,
                rowNum: 10,
                page: 1,
                viewrecords: true,
                rowList: [10, 20, 30]
            };
        gridState = restoreColumnState(cm);

        $.extend(true, defaultOptions, options);
        if (gridState) {
            $.extend(true, defaultOptions, gridState);
        }
        grid.jqGrid(defaultOptions);

        if (additional.search) {
            grid.jqGrid('filterToolbar', {searchOnEnter: false, defaultSearch: 'eq'});
        }

        showSearch = !!options.search;
        grid.navGrid(options.pager, {edit: false, add: false, del: false, search: showSearch, refresh: false},
                     {},{},{},{multipleSearch: true, closeOnEscape: true, closeAfterSearch: true});

        if(!additional.suppressColumnChooser){
            grid.navButtonAdd(options.pager,{
                caption:"Columns",
                buttonicon:"ui-icon-add",
                position: "first",
                onClickButton: function(){
                    grid.jqGrid('columnChooser', {
                        modal : true,
                        width : additional.columnChooserOptions.width, height : additional.columnChooserOptions.height,
                        done: function (perm){
                            if (perm) {
                                saveColumnState.call(this, perm);
                            }
                            grid.trigger('jqGridAfterLoadComplete.setFrozenColumns');
                        }});
                }
            });
        }

        grid.on("jqGridAfterGridComplete", function () {
            if (callback) {
                callback();
            }
        });

        grid.on('jqGridAfterLoadComplete.setFrozenColumns', function () {
            var table = view.$el.find('#' + id), row, height;
            view.$el.find('#' + id + '_frozen').find('tr').each(function () {
                if ($.jgrid.jqID(this.id)) {
                    row = table.find("#" + $.jgrid.jqID(this.id));
                    height = row.outerHeight();
                    $(this).find('td').height(height);
                    row.find('td').height(height);
                }
            });
        });

        return grid;
    };



    return obj;
});

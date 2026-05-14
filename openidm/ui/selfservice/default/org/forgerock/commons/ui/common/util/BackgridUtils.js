"use strict";

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
 * Copyright 2011-2016 ForgeRock AS.
 */

define(["jquery", "lodash", "org/forgerock/commons/ui/common/backgrid/Backgrid", "org/forgerock/commons/ui/common/util/DateUtil", "org/forgerock/commons/ui/common/util/UIUtils", "org/forgerock/commons/ui/common/util/AutoScroll", "moment", "dragula", "org/forgerock/commons/ui/common/backgrid/extension/ThemeableServerSideFilter"], function ($, _, Backgrid, DateUtil, UIUtils, AutoScroll, moment, dragula) {
    /**
     * @exports org/forgerock/commons/ui/common/util/BackgridUtils
     */
    var obj = {};

    /**
     * Makes the provided table drag and droppable
     *
     * @param {Object} data
     * @param {array} data.containers - an array of container elements
     * @params {array} data.rows - array of the rows in the table
     * @params {string} data.handlesClassname - the classname of the handles a use must click on to drag, omit property
     *                                          if the whole element should be selectable
     * @params {boolean} data.autoScroll [true] - if the grid should auto scroll when a row is dragged out of bounds
     * @param {Object} callback - called on row drop
     *
     * @returns {array} rows - The rows array in the new order
     *
     * @example
     * BackgridUtils.sortable({
     *   "containers": [$("#leftContainer")[0], $("#rightContainer")[0]],
     *   "rows": _.clone(this.model.mappingProperties, true),
     *   "handlesClassName": "fa fa-arrows"
     * }, _.bind(this.setMappingProperties, this));
     *
     */
    obj.sortable = function (data, callback) {
        var start,
            dragDropInstance = dragula(data.containers, {
            moves: function moves(el, container, handle) {
                if (_.has(data, "handlesClassname")) {
                    return handle.className.indexOf(data.handlesClassname) > -1;
                } else {
                    return true;
                }
            }
        });

        if (_.isUndefined(data.autoScroll)) {
            data.autoScroll = true;
        }

        dragDropInstance.on("cloned", _.bind(function (clone, original) {
            _.each(original.children, function (child, index) {
                $(clone).children().eq(index).css("width", $(child).css("width"));
                $(clone).children().eq(index).css("padding", $(child).css("padding"));
            });
        }, this));

        dragDropInstance.on("drag", _.bind(function (el, container) {
            start = _.indexOf($(container).find("tr"), el);

            if (data.autoScroll) {
                AutoScroll.startDrag();
            }
        }, this));

        dragDropInstance.on("drop", _.bind(function (el, container) {
            var stop = _.indexOf($(container).find("tr"), el),
                tempCopy = data.rows[start];

            if (data.autoScroll) {
                AutoScroll.endDrag();
            }

            data.rows.splice(start, 1);
            data.rows.splice(stop, 0, tempCopy);

            if (callback) {
                callback(data.rows);
            }
        }, this));
    };

    obj.formatDate = function (date) {
        var returnDate = "";
        if (date) {
            returnDate = DateUtil.formatDate(date, "MMM dd, yyyy") + " <small class='text-muted'>" + DateUtil.formatDate(date, "h:mm:ss TT") + "</small>";
        }

        return returnDate;
    };

    /**
     * The date cell will search the model attributes for the provided property and format that into a standard date.
     * @param {string} dateProperty TODO Add parameter description
     * @returns {*} TODO Add returns description
     */
    obj.DateCell = function (dateProperty) {
        var _this = this;
        return Backgrid.Cell.extend({
            render: function render() {
                if (this.model.get(dateProperty)) {
                    this.$el.html(_this.formatDate(this.model.get(dateProperty)));
                } else {
                    this.$el.html("");
                }
                return this;
            }
        });
    };

    /**
     * Datetime Ago Cell Renderer
     * <p>
     * Displays human friendly date time text (e.g. 4 "hours ago") with a tooltip of the exact time
     */
    obj.DatetimeAgoCell = Backgrid.Cell.extend({
        className: "date-time-ago-cell",
        formatter: {
            fromRaw: function fromRaw(rawData) {
                return moment(rawData).fromNow();
            }
        },
        render: function render() {
            Backgrid.Cell.prototype.render.apply(this);
            this.$el.attr("title", moment(this.model.get(this.column.get("name"))).format("Do MMMM YYYY, h:mm:ssa"));
            return this;
        }
    });

    /**
     * The button cell allows you to define an array of icons to insert into a single cell.
     * The icons will be given the class name and will execute the callback on click.
     *
     * @param {Object[]} buttons TODO Add parameter description
     * @param {Function} renderCallback TODO Add parameter description
     * @returns {Backgrid.Cell} TODO Add returns description
     * @example
     * cell: CustomCells.ButtonCell([{
     *   className: "fa fa-pencil grid-icon",
     *   callback: function(){alert(this.model.get("createTime"));}
     * }, {
     *   className: "fa fa-plus grid-icon",
     *   callback: function(){alert(this.model.get("assignee"));}
     * }])
     */
    obj.ButtonCell = function (buttons, renderCallback) {
        var events = {},
            html = "";

        _.each(buttons, function (button, index) {
            if (button.href) {
                html += "<a href=\"" + button.href + "\"><i class=\"button-" + index + " " + button.className + "\"></i></a>";
            } else {
                events["click .button-" + index] = button.callback;
                html += "<i class=\"button-" + index + " " + button.className + "\"></i>";
            }
        });

        return Backgrid.Cell.extend({
            events: events,

            render: function render() {
                this.$el.html(html);
                this.delegateEvents();

                if (renderCallback) {
                    _.bind(renderCallback, this)();
                }
                return this;
            }
        });
    };

    /**
     * In the case that a grid needs to sort on a property other than the one displayed, use this custom cell.
     * @param {string} displayProperty TODO Add parameter description
     * @returns {Backgrid.Cell} TODO Add return description
     * @example
     * // Will sort on "taskName" and display "name"
     * {
     *   label: "Task",
     *   name: "taskName",
     *   cell: CustomCells.DisplayNameCell(name),
     *   sortable: true,
     *   editable: false
     * }
     */
    obj.DisplayNameCell = function (displayProperty) {
        return Backgrid.Cell.extend({
            render: function render() {
                this.$el.text(this.model.get(displayProperty));
                return this;
            }
        });
    };

    /**
     * addSmallScreenCell creates a hidden column with a custom "smallScreenCell" that
     * will be displayed as a replacement for the full grid on small screens. It takes
     * an array of Backgrid column definitions, loops over them, adds a vertical
     * representation of how the cell is rendered for the current column to the
     * smallScreenCell's html, then adds the newly created column definition to the
     * originally defined grid columns.
     *
     * the "hideColumnLabel" param can be passed in to display the cell with no label
     * for the associated value
     * @param {Object[]} cols TODO Add parameter description
     * @param {boolean} hideColumnLabels TODO Add parameter description
     * @returns {Object[]} TODO Add returns description
     */
    obj.addSmallScreenCell = function (cols, hideColumnLabels) {
        var smallScreenCell = Backgrid.Cell.extend({
            className: "smallScreenCell",
            events: {},
            render: function render() {
                var filteredCols = _.reject(cols, function (c) {
                    return c.name === "smallScreenCell";
                });

                _.each(filteredCols, _.bind(function (col) {
                    var cellView,
                        label = "<span class='text-muted'>" + col.label + ":</span> ",
                        cellWrapper;

                    if (_.isObject(col.cell)) {
                        cellView = new col.cell({ model: this.model, column: col });
                        cellView.$el = $("<span>");
                        cellView.render();

                        if (!_.isEmpty(_.omit(cellView.events, "click"))) {
                            cellWrapper = $("<p class='pull-right show'></p>");

                            if (cellView.$el.html().length && !hideColumnLabels && col.label) {
                                cellWrapper.append(label);
                            }

                            cellWrapper.append(cellView.$el);

                            this.$el.prepend(cellWrapper);
                        } else {
                            cellWrapper = $("<p>");

                            if (cellView.$el.html().length && !hideColumnLabels && col.label) {
                                cellWrapper.append(label);
                            }

                            cellWrapper.append(cellView.$el);

                            this.$el.append(cellWrapper);
                        }
                    } else {
                        cellWrapper = $("<p>");
                        if (this.model.get(col.name) && this.model.get(col.name).length && !hideColumnLabels && col.label) {
                            cellWrapper.append(label);
                        }

                        cellWrapper.append(this.model.get(col.name));

                        this.$el.append(cellWrapper);
                    }
                }, this));

                return this;
            }
        }),
            newCol = {
            name: "smallScreenCell",
            editable: false,
            sortable: false,
            cell: smallScreenCell
        };

        cols.push(newCol);

        return cols;
    };

    /**
     * Handlebars Template Cell Renderer
     * <p>
     * You must extend this renderer and specify a "template" attribute
     * @example
     * BackgridUtils.TemplateCell.extend({
     *   template: "templates/MyTemplate.html"
     * });
     */
    obj.TemplateCell = Backgrid.Cell.extend({
        className: "template-cell",

        events: {
            "click": "_onClick"
        },

        render: function render() {
            this.$el.html(UIUtils.fillTemplateWithData(this.template, this.model.attributes));

            if (this.additionalClassName) {
                this.$el.addClass(this.additionalClassName);
            }

            if (this.callback) {
                this.callback();
            }

            this.delegateEvents();

            return this;
        },

        _onClick: function _onClick(e) {
            if (this.onClick) {
                this.onClick(e, this.model.id);
            }
        }
    });

    /**
     * Object Cell
     * <p>
     * Displays cell content as a definition list. Used for cells which values are objects
     */
    obj.ObjectCell = Backgrid.Cell.extend({
        className: "object-formatter-cell",

        render: function render() {
            this.$el.empty();

            var object = this.model.get(this.column.attributes.name),
                result = "<dl class='dl-horizontal'>",
                prop;

            for (prop in object) {
                if (_.isString(object[prop])) {
                    result += "<dt>" + prop + "</dt><dd>" + object[prop] + "</dd>";
                } else {
                    result += "<dt>" + prop + "</dt><dd>" + JSON.stringify(object[prop]) + "</dd>";
                }
            }
            result += "</dl>";

            this.$el.append(result);

            this.delegateEvents();
            return this;
        }
    });

    /**
     * Array Cell
     * <p>
     * Displays cell content as an unordered list. Used for cells which values are arrays
     */
    obj.ArrayCell = Backgrid.Cell.extend({
        className: "array-formatter-cell",

        buildHtml: function buildHtml(arrayVal) {
            var result = "<ul>",
                i = 0;

            for (; i < arrayVal.length; i++) {
                if (_.isString(arrayVal[i])) {
                    result += "<li>" + arrayVal[i] + "</li>";
                } else {
                    result += "<li>" + JSON.stringify(arrayVal[i]) + "</li>";
                }
            }
            result += "</ul>";

            return result;
        },

        render: function render() {
            this.$el.empty();

            var arrayVal = this.model.get(this.column.attributes.name);
            this.$el.append(this.buildHtml(arrayVal));

            this.delegateEvents();
            return this;
        }
    });

    obj.FilterHeaderCell = Backgrid.HeaderCell.extend({
        className: "filter-header-cell",
        events: {
            "click a": "preventSortOnClear"
        },
        preventSortOnClear: function preventSortOnClear(e) {
            if ($(e.target).data().backgridAction === "clear") {
                e.preventDefault();
            } else {
                this.onClick(e);
            }
        },
        render: function render() {
            var filter = new Backgrid.Extension.ThemeableServerSideFilter({
                name: this.column.get("name"),
                placeholder: $.t("common.form.filter"),
                collection: this.collection
            });

            /**
             * this setting tells the ThemeableServerSideFilter both to search on keyup and to only
             * search if the minimum number of search characters are entered in the filter field
             */
            filter.minimumSearchChars = this.column.attributes.minimumSearchChars;

            if (this.addClassName) {
                this.$el.addClass(this.addClassName);
            }

            this.collection.state.filters = this.collection.state.filters ? this.collection.state.filters : [];
            this.collection.state.filters.push(filter);
            obj.FilterHeaderCell.__super__.render.apply(this);
            this.$el.prepend(filter.render().el);
            return this;
        }
    });

    /**
     * Clickable Row
     * <p>
     * You must extend this row and specify a "callback" attribute e.g.
     * <p>
     * MyRow = BackgridUtils.ClickableRow.extend({
     *     callback: myCallback
     * });
     */
    obj.ClickableRow = Backgrid.Row.extend({
        events: {
            "click": "onClick"
        },

        onClick: function onClick(e) {
            if (this.callback) {
                this.callback(e);
            }
        }
    });

    obj.sortKeys = function () {
        return this.state.order === 1 ? "-" + this.state.sortKey : this.state.sortKey;
    };

    // FIXME: Workaround to fix "Double sort indicators" issue
    // @see https://github.com/wyuenho/backgrid/issues/453
    obj.doubleSortFix = function (model) {
        // No ids so identify model with CID
        var cid = model.cid,
            filtered = model.collection.filter(function (model) {
            return model.cid !== cid;
        });

        _.each(filtered, function (model) {
            model.set("direction", null);
        });
    };

    obj.pagedResultsOffset = function () {
        return (this.state.currentPage - 1) * this.state.pageSize;
    };

    return obj;
});

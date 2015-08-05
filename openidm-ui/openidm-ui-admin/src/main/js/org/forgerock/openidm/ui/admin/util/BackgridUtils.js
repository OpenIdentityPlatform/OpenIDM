/**
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2011-2015 ForgeRock AS. All rights reserved.
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

define("org/forgerock/openidm/ui/admin/util/BackgridUtils", [
    "jquery",
    "underscore",
    "backgrid",
    "org/forgerock/commons/ui/common/util/DateUtil"

], function ($, _, Backgrid, DateUtil) {
    var obj = {};

    obj.formatDate = function(date) {
        var returnDate = "";
        if(date) {
            returnDate = DateUtil.formatDate(date, "MMM dd, yyyy") +
            " <small class='text-muted'>" +
            DateUtil.formatDate(date, "h:mm:ss TT") +
            "</small>";
        }

        return returnDate;
    };

    /**
     * The date cell will search the model attributes for the provided property and format that into a standard date.
     * @param dateProperty{string}
     * @returns {*}
     * @constructor
     */
    obj.DateCell = function (dateProperty) {
        var _this = this;
        return Backgrid.Cell.extend({
            render: function () {
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
     * The button cell allows you to define an array of icons to insert into a single cell.
     * The icons will be given the class name and will execute the callback on click.
     *
     * @param buttons {array}
     *      EXAMPLE:
     *       cell: CustomCells.ButtonCell([{
     *           className: "fa fa-pencil grid-icon",
     *           callback: function(){alert(this.model.get("createTime"));}
     *       }, {
     *           className: "fa fa-plus grid-icon",
     *           callback: function(){alert(this.model.get("assignee"));}
     *       }])
     * @returns {Backgrid.Cell}
     * @constructor
     */
    obj.ButtonCell = function (buttons, renderCallback) {
        var events = {},
            html = "";

        _.each(buttons, function(button, index) {
            if(button.href) {
                html += ("<a href=\"" +button.href +"\"><i class=\"button-" + index + " " + button.className +  "\"></i></a>");
            } else {
                events["click .button-"+index] = button.callback;
                html += ("<i class=\"button-" + index + " " + button.className +  "\"></i>");
            }
        });

        return Backgrid.Cell.extend({
            events: events,

            render: function () {
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
     * EXAMPLE: Will sort on "taskName" and display "name"
     *    {
     *        label: "Task",
     *        name: "taskName",
     *        cell: CustomCells.DisplayNameCell(name),
     *        sortable: true,
     *        editable: false
     *    }
     * @param displayProperty
     * @returns {*}
     * @constructor
     */
    obj.DisplayNameCell = function (displayProperty) {
        return Backgrid.Cell.extend({
            render: function () {
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
     *
     * @param cols {array}
     * @param hideColumnLabels {boolean}
     * @returns {array}
     * @constructor
     *
     */

    obj.addSmallScreenCell = function (cols, hideColumnLabels) {
        var smallScreenCell = Backgrid.Cell.extend({
            className: "smallScreenCell",
            events: {},
            render: function () {
                var html = "",
                    filteredCols = _.reject(cols, function (c) {
                        return c.name === "smallScreenCell";
                    });

                _.each(filteredCols, _.bind(function (col) {
                    var cellView,
                        label = "<span class='text-muted'>" + col.label + ":</span> ",
                        cellWrapper;

                    if (_.isObject(col.cell)) {
                        cellView = new col.cell({ model: this.model, column: col});
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

    return obj;

});

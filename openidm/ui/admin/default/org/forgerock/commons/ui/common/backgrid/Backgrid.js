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
 * Copyright 2015-2016 ForgeRock AS.
 */

define(["jquery", "backgrid"], function ($, Backgrid) {
    /**
     * Fixed behavior, when Backgrid automatically puts the name of the field as a class in the header.
     * Default behavior is bad, because those classes are not prefixed/suffixed and can clash with a css class name
     * which is in use. For example we just had a field called "active" which was then being styled by bootstraps.css.
     * The data-attribute is used instead of class name in the overrided version.
     * @override
     * */
    Backgrid.HeaderCell.prototype.render = function () {
        this.$el.empty();
        var column = this.column,
            sortable = Backgrid.callByNeed(column.sortable(), column, this.collection),
            label;
        if (sortable) {
            label = $("<a>").text(column.get("label")).append("<b class='sort-caret'></b>");
        } else {
            label = document.createTextNode(column.get("label"));
        }

        this.$el.append(label);
        /* override code start */
        // updated to using the data-field attribute instead of class
        this.$el.attr("data-field", column.get("name"));
        /* override code end */
        this.$el.addClass(column.get("direction"));
        this.delegateEvents();
        return this;
    };

    /**
     * @exports org/forgerock/commons/ui/common/backgrid/Backgrid
     */
    return Backgrid;
});

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

/**
 * Themeable extension to <code>Backgrid.Extension.Paginator</code>.
 * <p>
 * The defaults provide automatic integration with Bootstrap 3.
 * @module org/forgerock/commons/ui/common/backgrid/extension/ThemeablePaginator
 * @extends Backgrid.Extension.Paginator
 * @see {@link http://backgridjs.com/ref/extensions/paginator.html|Backgrid.Extension.Paginator}
 * @example
 * // Use RequireJS argument name...
 * new ThemeablePaginator({ ... });
 * // ...or the reference on Backgrid.Extension
 * new Backgrid.Extension.ThemeablePaginator({ ... });
 */
define(["jquery", "backgrid.paginator", "org/forgerock/commons/ui/common/backgrid/Backgrid"], function ($, BackgridPaginator, Backgrid) {
    Backgrid.Extension.ThemeablePaginator = Backgrid.Extension.Paginator.extend({
        /**
         * @default
         */
        className: "text-center",

        /**
         * @inheritdoc
         */
        controls: {
            rewind: {
                label: "&laquo;",
                title: $.t("common.grid.pagination.first")
            },
            back: {
                label: "&lsaquo;",
                title: $.t("common.grid.pagination.previous")
            },
            forward: {
                label: "&rsaquo;",
                title: $.t("common.grid.pagination.next")
            },
            fastForward: {
                label: "&raquo;",
                title: $.t("common.grid.pagination.last")
            }
        },

        /**
         * @property CSS class name to add to <code>ul</code> element
         * @default
         */
        ulClassName: "pagination",

        /**
         * @inheritdoc
         */
        render: function render() {
            Backgrid.Extension.Paginator.prototype.render.call(this);

            if (this.ulClassName) {
                this.$el.find("ul").addClass(this.ulClassName);
            }

            return this;
        }
    });

    return Backgrid.Extension.ThemeablePaginator;
});

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
 * Themeable extension to <code>Backgrid.Extension.SelectAll</code>.
 * <p>
 * The defaults provide automatic integration with Bootstrap 3.
 * @module org/forgerock/commons/ui/common/backgrid/extension/ThemeableSelectAllCell
 * @extends Backgrid.Extension.SelectRowCell
 * @see {@link http://backgridjs.com/ref/extensions/select-all.html|Backgrid.Extension.SelectAll}
 * @example
 * // Use RequireJS argument name...
 * new ThemeableSelectAllCell({ ... });
 * // ...or the reference on Backgrid.Extension
 * new Backgrid.Extension.ThemeableSelectAllCell({ ... });
 * // Use in a Backgrid column
 * {
 *   cell: ThemeableSelectAllCell,
 *   headerCell: "select-all"
 * }
 */
define(["backgrid-selectall", "org/forgerock/commons/ui/common/backgrid/Backgrid"], function (BackgridSelectAll, Backgrid) {
    Backgrid.Extension.ThemeableSelectAllCell = Backgrid.Extension.SelectRowCell.extend({
        /**
         * @inheritdoc
         */
        onChange: function onChange() {
            var checked = this.$el.find("input[type=checkbox]").prop("checked");
            this.$el.parent().toggleClass("info", checked);
            this.model.trigger("backgrid:selected", this.model, checked);
        }
    });

    return Backgrid.Extension.ThemeableSelectAllCell;
});

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
 * Themeable extension to <code>Backgrid.Extension.ServerSideFilter</code>.
 * <p>
 * The defaults provide automatic integration with Bootstrap 3.
 * @module org/forgerock/commons/ui/common/backgrid/extension/ThemeableServerSideFilter
 * @extends Backgrid.Extension.ServerSideFilter
 * @see {@link http://backgridjs.com/ref/extensions/filter.html|Backgrid.Extension.ServerSideFilter}
 * @example
 * // Use RequireJS argument name...
 * new ThemeableServerSideFilter({ ... });
 * // ...or the reference on Backgrid.Extension
 * new Backgrid.Extension.ThemeableServerSideFilter({ ... });
 */
define(["jquery", "underscore", "backgrid-filter", "org/forgerock/commons/ui/common/backgrid/Backgrid"], function ($, _, BackgridFilter, Backgrid) {
  Backgrid.Extension.ThemeableServerSideFilter = Backgrid.Extension.ServerSideFilter.extend({
    /**
     * Overriding the "keyup input[type=search]" event on ServerSideFilter here
     * to accommodate the ability to filter as input is typed into the filter field
     */
    events: _.extend(Backgrid.Extension.ServerSideFilter.prototype.events, {
      "keyup input[type=search]": "keyupSearch"
    }),
    keyupSearch: function keyupSearch(e) {
      e.preventDefault();

      /**
       * showClearButtonMaybe is the default action of
       * "keyup input[type=search]" in ServerSideFilter
       */
      this.showClearButtonMaybe(e);
      /*
       * if there is no minimumSearchChars setting stick with the default behavior
       * of searching only on submit (a.k.a. clicking the enter button)
       */
      if (this.minimumSearchChars && $(e.target).val().length >= this.minimumSearchChars) {
        this.search(e);
      }
    },

    /**
     * @default
     */
    className: "form-group has-feedback",

    /**
     * @inheritdoc
     */
    template: function template(data) {
      return '<input class="form-control input-sm" type="search" ' + (data.placeholder ? 'placeholder="' + data.placeholder + '"' : '') + ' name="' + data.name + '" ' + (data.value ? 'value="' + data.value + '"' : '') + '/>' + '<a class="fa fa-times form-control-feedback" data-backgrid-action="clear" role="button" href="#"></a>';
    }
  });

  return Backgrid.Extension.ThemeableServerSideFilter;
});

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
 * Copyright 2016 ForgeRock AS.
 */

define(["lodash", "react", "react-dom"], function (_, React, ReactDOM) {
  /**
   * Given a component, render it into the given DOM element.
   * @exports org/forgerock/commons/ui/common/util/reactify
   * @param  {ReactElement} component The React element to render
   * @param  {jQuery} el A jQuery object containing a collection of DOM elements
   * @return {ReactComponent} Rendered component
   * @example reactify(<Title>My Text</Title>, this.$el.find("[data-title]"));
   */
  var exports = function exports(component, el) {
    return ReactDOM.render(component, el[0]);
  };

  return exports;
});

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

define(["jquery", "lodash"], function ($, _) {
    /**
     * @exports org/forgerock/commons/ui/common/util/AutoScroll
     */
    var obj = {},
        scrollBuffer = 20,
        scrollDirection = null;

    function scroll() {
        if (!_.isNull(scrollDirection)) {
            window.scrollBy(0, scrollBuffer * scrollDirection);

            var scrollPosition = window.pageYOffset,
                maxScrollPosition = $(document).height() - $(window).height();

            // If the scroll bar is not at the top nor bottom continue scrolling
            if (scrollDirection === -1 && scrollPosition > 0 || scrollDirection === 1 && scrollPosition < maxScrollPosition) {
                _.delay(function () {
                    scroll();
                }, 50);
            }
        }
    }

    /**
     * Call startDrag on Dragula.on("drag", function() {});
     */
    obj.startDrag = function () {
        $("body").on("mousemove", _.throttle(function (e) {
            scrollDirection = null;
            var mousePosition = e.pageY,
                windowHeight = $(window).height(),
                scrollTop = $(document).scrollTop();

            // SCROLL DOWN
            if (mousePosition >= windowHeight + scrollTop - scrollBuffer) {
                scrollDirection = 1;

                // SCROLL UP
            } else if (mousePosition - scrollTop <= scrollBuffer) {
                scrollDirection = -1;
            }

            scroll();
        }, 50));
    };

    /**
     * Call endDrag on Dragula.on("drop", function() {});
     */
    obj.endDrag = function () {
        $("body").off("mousemove");
        scrollDirection = null;
    };

    return obj;
});

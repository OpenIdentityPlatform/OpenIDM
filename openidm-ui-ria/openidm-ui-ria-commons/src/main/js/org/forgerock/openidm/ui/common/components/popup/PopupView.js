/*
 * @license DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright © 2011-2012 ForgeRock AS. All rights reserved.
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

/*global $, define*/

/**
 * @author mbilski
 */
define("org/forgerock/openidm/ui/common/components/popup/PopupView", [
], function() {

    var obj = {};

    obj.init = function() {
        $("#popup").on('mouseleave', function(event) {
            obj.hide();
        });
    };

    obj.setContent = function(content) {
        $("#popupContent").html(content);
    };

    obj.setPositionBy = function(element) {
        var ph, left = $(element).position().left, top = $(element).position().top, w = $(element).width(), h = $(element).height();

        $("#popup").css('left', left);
        $("#popup").css('top', top);

        $("#popup").css('height', h);
        $("#popupContent").css("margin-left", w + 10);

        ph = $("#popupContent").height();
        $("#popupContent").css("margin-top", -ph / 2);
    };

    obj.show = function() {
        $("#popup").show();		
    };

    obj.hide = function() {
        $("#popup").hide();
    };

    return obj;

});



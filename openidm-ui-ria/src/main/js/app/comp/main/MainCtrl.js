/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright © 2011 ForgeRock AS. All rights reserved.
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
 * @author yaromin
 */
define(["./Configuration",
        "app/comp/common/eventmanager/EventManager",
        "app/util/Constants"], 
        function (configuration, eventManager, constants) {
    var obj = {};

    obj.clearContent = function() {
        $("#content").fadeOut(100, function() {
            $("#content").html("<div id='contentMainImage'><img src='images/main.jpg' width='592' height='442' alt='' /></div>");
            $(this).fadeIn();
        });
    };

    return obj;

});



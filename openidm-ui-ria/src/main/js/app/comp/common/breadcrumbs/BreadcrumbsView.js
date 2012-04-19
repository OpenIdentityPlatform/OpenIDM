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
 * @author mbilski
 */

define("app/comp/common/breadcrumbs/BreadcrumbsView",[],
        function() {
    var obj = {};

    obj.setCrumb = function(pageName) {
        $("#nav-content span:last").html(pageName);
    };

    obj.getHomeButton = function() {
        return $("#home_link");
    };

    obj.top = function() {
        return $("#nav-content a").last();
    };

    obj.pushPath = function(path) {
        $("#nav-content a").last().after(' <a href="#" class="orange">' + path + '</a>');
        $("#nav-content a").last().before('<img src="images/navi-next.png" width="3" height="5" alt="" align="absmiddle" class="navi-next" /><span></span>');
        return $("#nav-content a").last();
    };

    obj.popPath = function() {
        $("#nav-content a").last().remove();
        $("#nav-content img").last().remove();
        $("#nav-content span").last().prev().remove();
    };

    return obj;

});



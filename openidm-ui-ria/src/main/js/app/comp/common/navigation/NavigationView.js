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

define("app/comp/common/navigation/NavigationView",
        ["app/util/UIUtils"],
        function(UIUtils) {

    var obj = {};

    obj.show = function(callback) {
        console.log("showing navigation");
        UIUtils.fillTemplateWithData("templates/common/NavigationTemplate.html", null, function(data) {
            $("#menu").html(data);
            callback();
        });
    };

    obj.addLink = function(id, name) {
        $("#menu ul").append('<li><a href="#" id="'+ id +'">'+ name +'</a></li>');

        return $("#"+id);
    };

    obj.addOuterLink = function(href, name) {
        $("#menu ul").append('<li><a href="'+ href +'" title="'+ name +'" target="_blank">'+ name +'</a></li>');
    };

    obj.getHomeLink = function() {
        return $("#homeNavigatonLink");
    };

    return obj;

});



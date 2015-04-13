/**
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2014-2015 ForgeRock AS. All Rights Reserved
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

/*global require, define*/
define([

    "text!css/config/adminConfig.less",
    "text!css/config/adminStyle.less",
    "text!css/config/resourcesView.less",

    "text!css/fontawesome/less/forgerock-variables.less",
    "text!css/fontawesome/less/mixins.less",
    "text!css/fontawesome/less/path.less",
    "text!css/fontawesome/less/core.less",
    "text!css/fontawesome/less/larger.less",
    "text!css/fontawesome/less/fixed-width.less",
    "text!css/fontawesome/less/list.less",
    "text!css/fontawesome/less/bordered-pulled.less",
    "text!css/fontawesome/less/spinning.less",
    "text!css/fontawesome/less/rotated-flipped.less",
    "text!css/fontawesome/less/stacked.less",
    "text!css/fontawesome/less/icons.less"
], function () {

    /* an unfortunate need to duplicate the file names here, but I haven't
     yet found a way to fool requirejs into doing dynamic dependencies */
    var staticFiles = [

            "css/config/adminConfig.less",
            "css/config/adminStyle.less",
            "css/config/resourcesView.less",

            "css/fontawesome/less/forgerock-variables.less",
            "css/fontawesome/less/mixins.less",
            "css/fontawesome/less/path.less",
            "css/fontawesome/less/core.less",
            "css/fontawesome/less/larger.less",
            "css/fontawesome/less/fixed-width.less",
            "css/fontawesome/less/list.less",
            "css/fontawesome/less/bordered-pulled.less",
            "css/fontawesome/less/spinning.less",
            "css/fontawesome/less/rotated-flipped.less",
            "css/fontawesome/less/stacked.less",
            "css/fontawesome/less/icons.less"
        ],
        deps = arguments;

    return function (server) {

        _.each(staticFiles, function (file, i) {
            server.respondWith(
                "GET",
                new RegExp(file.replace(/([\/\.\-])/g, "\\$1") + "$"),
                [
                    200,
                    { },
                    deps[i]
                ]
            );
        });
        
    };
});
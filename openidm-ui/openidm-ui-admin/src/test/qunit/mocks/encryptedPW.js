/**
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2014 ForgeRock AS. All Rights Reserved
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
    "text!templates/admin/AdminBaseTemplate.html",
    "text!templates/admin/DashboardTemplate.html"
], function () {

    /* an unfortunate need to duplicate the file names here, but I haven't
     yet found a way to fool requirejs into doing dynamic dependencies */
    var staticFiles = [
            "templates/admin/AdminBaseTemplate.html",
            "templates/admin/DashboardTemplate.html"
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
            
        server.respondWith(
            "GET",   
            "/openidm/info/login",
            [
                200, 
                { },
                "{\"authorizationId\":{\"id\":\"openidm-admin\",\"component\":\"repo/internal/user\",\"roles\":[\"openidm-admin\",\"openidm-authorized\"]},\"parent\":{\"id\":\"f4ffdaa2-6cb1-47cb-a359-e0959086bd61\",\"parent\":null,\"class\":\"org.forgerock.json.resource.RootContext\"},\"class\":\"org.forgerock.json.resource.SecurityContext\",\"authenticationId\":\"openidm-admin\"}"
            ]
        );
    
        server.respondWith(
            "GET",   
            "/openidm/repo/internal/user/openidm-admin",
            [
                200, 
                { },
                "{\"_id\":\"openidm-admin\",\"_rev\":\"2\",\"roles\":\"openidm-admin,openidm-authorized\",\"userName\":\"openidm-admin\",\"password\":{\"$crypto\":{\"type\":\"x-simple-encryption\",\"value\":{\"data\":\"3h8qdTITjZ8yDjW0IR3kRA==\",\"cipher\":\"AES/CBC/PKCS5Padding\",\"iv\":\"s8q+XIxDsAUNIeXTZaITVA==\",\"key\":\"openidm-sym-default\"}}}}"
            ]
        );
    };

});

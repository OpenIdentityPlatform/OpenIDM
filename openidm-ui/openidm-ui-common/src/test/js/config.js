/**
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2015 ForgeRock AS.
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

/*global $, require, QUnit */

require.config({
    baseUrl: '../www',
    paths: {
        text: "../test/text",
        jquery: "libs/jquery-2.1.1-min",
        underscore: "libs/lodash-2.4.1-min",
        sinon: "libs/sinon-1.12.2"
    },
    shim: {
        underscore: {
            exports: "_"
        },
        sinon: {
            exports: "sinon"
        }
    }
});

require([
    "../test/tests/mocks/systemInit",
    "underscore",
    "sinon"
], function (systemInit, _, sinon) {

    sinon.FakeXMLHttpRequest.useFilters = true;
    sinon.FakeXMLHttpRequest.addFilter(function (method, url, async, username, password) {
        if (/((translation\.json)|(less-1\.5\.1-min\.js))$/.test(url)) {
            return false;
        }

        return /((\.html)|(\.css)|(\.less)|(\.json))$/.test(url);
    });

    var server = sinon.fakeServer.create();
    server.autoRespond = true;
    systemInit(server);

    require(['../www/main','../test/run'], function (appMain, run) {
        run(server);
    });

    return server;
});

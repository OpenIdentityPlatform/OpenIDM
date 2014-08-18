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

/*global $, require, QUnit */

require.config({
    paths: {
        sinon: "../test/libs/sinon-1.9.1",
        text: "../test/text"
    },
    shim: {
        sinon: {
            exports: "sinon"
        }
    }
});

require([
    "org/forgerock/commons/ui/common/util/Constants",
    "org/forgerock/commons/ui/common/main/EventManager",
    "org/forgerock/commons/ui/common/main/ViewManager",
    "org/forgerock/commons/ui/common/main/Configuration",
    "sinon",
    "../test/tests/mocks/systemInit",
    "../test/tests/common",
    "../test/tests/specific"
], function (constants, eventManager, viewManager, conf, sinon, systemInit, commonTests, specificTests) {

    var server = sinon.fakeServer.create();
    server.autoRespond = true;
    systemInit(server);

    eventManager.registerListener(constants.EVENT_APP_INTIALIZED, function () {

        // delayed testing start gives the app time to stablize during startup
        _.delay(function () {

            QUnit.start();

            commonTests.executeAll(server);
            specificTests.executeAll(server);

        }, 100);

    });

});
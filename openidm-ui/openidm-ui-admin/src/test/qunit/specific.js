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

/*global require, define, QUnit, $ */

define([
    "./mocks/adminInit",
    "./managedobjects/managedObjectsTest",
    "./resources/resourceTest",
    "./mapping/addMappingTest",
    "./mapping/reconTests",
    "./mapping/propertyMappingTest",
    "./mapping/correlationTest"
], function (adminInit, moTest, resourceTest, addMappingTest, reconTests, propertyMappingTest, correlationTest) {

    return {
        executeAll: function (server) {
            QUnit.moduleDone(function() {
                $(".bootstrap-dialog").remove();
            });

            adminInit(server);
            addMappingTest.executeAll(server);
            resourceTest.executeAll(server);
            reconTests.executeAll(server);
            correlationTest.executeAll(server);
            moTest.executeAll(server);
            propertyMappingTest.executeAll(server);
        }
    };
});
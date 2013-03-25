/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2013 ForgeRock AS. All Rights Reserved
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
package org.forgerock.openidm.recon

/**
 * A org.forgerock.openidm.recon.DataProvider does ...
 *
 * @author Laszlo Hordos
 */
class DataProvider {

    static void initDataOTOSimple(openidm) {

        def user1 = openidm.create("/managed/user", "user1", [userName: "user1", "active": true])
        def user2 = openidm.create("/managed/user", "user2", [userName: "user2", "active": true])
        def user3 = openidm.create("/managed/user", "user3", [userName: "user3", "active": true])
        def user4 = openidm.create("/managed/user", "user4", [userName: "user4", "active": true])

        def account3 = openidm.create("/system/OpenDJ/account", "user3", [userName: "user3"])
        def account4 = openidm.create("/system/OpenDJ/account", "user4", [userName: "user4"])
        def account5 = openidm.create("/system/OpenDJ/account", "user5", [userName: "user5"])
        def account6 = openidm.create("/system/OpenDJ/account", "user6", [userName: "user6"])


        def linkMember1 = openidm.create("/repo/link/account", null, [firstId: user1._id, secondId: "NONE_T1"])

        def linkMember3 = openidm.create("/repo/link/account", null, [firstId: user3._id, secondId: account3._id])

        def linkMember5 = openidm.create("/repo/link/account", null, [firstId: "NONE_S5", secondId: account5._id])

        def linkMember7 = openidm.create("/repo/link/account", null, [firstId: "NONE_S7", secondId: "NONE_T7"])


        def user = openidm.create("/system/AD/account", "daffy_duck", ["userName": "daffy_duck"]);
        assert user._id != null
    }
}

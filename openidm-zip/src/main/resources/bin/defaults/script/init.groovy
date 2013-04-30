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

package bin.defaults.script

import org.forgerock.openidm.core.ServerConstants

def response = [status: "ok", version: ServerConstants.getVersion(), revision: ServerConstants.getRevision()]

try {

    /*

    Gremlin Example
    def g = openidm.graph()

	g.getRawGraph().createVertexType("Product");
	g.getRawGraph().createVertexType("Customer");
	g.getRawGraph().createEdgeType("Sell");

    def v = g.addVertex("class:managed_user", [userName: "john_doe", active: true])
    def u = g.addVertex("class:managed_user", [name: "jdoe", active: true])
    g.addEdge(v, u, "hasAccount", [created: true])
    g.commit()
    */

    def org000 = openidm.create("/repo/managed/organization", "forgerock", [displayName: "ForgeRock"])
    def org100 = openidm.create("/repo/managed/organization", "oslo", [displayName: "ForgeRock Oslo"])
    def org110 = openidm.create("/repo/managed/organization", "bristol", [displayName: "ForgeRock Bristol"])
    def org120 = openidm.create("/repo/managed/organization", "grenoble", [displayName: "ForgeRock Grenoble"])
    def org200 = openidm.create("/repo/managed/organization", "san_francisco", [displayName: "ForgeRock San Francisco"])
    def org210 = openidm.create("/repo/managed/organization", "vancouver", [displayName: "ForgeRock Vancouver"])


    def role000 = openidm.create("/repo/managed/role", "employee", [displayName: "Default Employee Role"])
    def role010 = openidm.create("/repo/managed/role", "employee_oslo", [displayName: "Oslo Employee Role"])
    def role020 = openidm.create("/repo/managed/role", "employee_bristol", [displayName: "Bristol Employee Role"])
    def role030 = openidm.create("/repo/managed/role", "employee_grenoble", [displayName: "Grenoble Employee Role"])
    def role040 = openidm.create("/repo/managed/role", "employee_san_francisco", [displayName: "San Francisco Employee Role"])
    def role050 = openidm.create("/repo/managed/role", "employee_vancouver", [displayName: "Vancouver Employee Role"])

    def role100 = openidm.create("/repo/managed/role", "contractor", [displayName: "Default Contractor"])
    def role101 = openidm.create("/repo/managed/role", "red_badge", [displayName: "Red Badge Contractor"])


    def system000 = openidm.create("/repo/managed/system", "google_apps", [displayName: "GoogleApps"])
    def system010 = openidm.create("/repo/managed/system", "salesforce", [displayName: "Salesforce"])
    def system020 = openidm.create("/repo/managed/system", "webex", [displayName: "Webex"])



    def linkORG01 = openidm.create("/repo/link/parent_of", null, [firstId: org000._vertex, secondId: org100._vertex])
    def linkORG02 = openidm.create("/repo/link/parent_of", null, [firstId: org000._vertex, secondId: org110._vertex])
    def linkORG03 = openidm.create("/repo/link/parent_of", null, [firstId: org000._vertex, secondId: org120._vertex])
    def linkORG04 = openidm.create("/repo/link/parent_of", null, [firstId: org000._vertex, secondId: org200._vertex])
    def linkORG05 = openidm.create("/repo/link/parent_of", null, [firstId: org000._vertex, secondId: org210._vertex])


    def linkROLE_PERM01 = openidm.create("/repo/link/allow", null, [firstId: org000._vertex, secondId: role000._vertex])
    def linkROLE_PERM02 = openidm.create("/repo/link/allow", null, [firstId: org000._vertex, secondId: role100._vertex])

    def linkROLE_PERM10 = openidm.create("/repo/link/allow", null, [firstId: org100._vertex, secondId: role010._vertex])
    def linkROLE_PERM11 = openidm.create("/repo/link/allow", null, [firstId: org110._vertex, secondId: role020._vertex])
    def linkROLE_PERM12 = openidm.create("/repo/link/allow", null, [firstId: org120._vertex, secondId: role030._vertex])
    def linkROLE_PERM20 = openidm.create("/repo/link/allow", null, [firstId: org200._vertex, secondId: role040._vertex])
    def linkROLE_PERM21 = openidm.create("/repo/link/allow", null, [firstId: org210._vertex, secondId: role050._vertex])



    def user1 = openidm.create("/repo/managed/user", "user1", [userName: "user1"])
    def user2 = openidm.create("/repo/managed/user", "user2", [userName: "user2"])
    def user3 = openidm.create("/repo/managed/user", "user3", [userName: "user3"])
    def user4 = openidm.create("/repo/managed/user", "user4", [userName: "user4"])
    def user5 = openidm.create("/repo/managed/user", "user5", [userName: "user5"])

    def linkManager1 = openidm.create("/repo/link/manage", null, [firstId: user1._vertex, secondId: org100._vertex])

    def linkMember1 = openidm.create("/repo/link/member", null, [firstId: user1._vertex, secondId: org100._vertex])
    def linkMember2 = openidm.create("/repo/link/member", null, [firstId: user2._vertex, secondId: org100._vertex])
    def linkMember3 = openidm.create("/repo/link/member", null, [firstId: user3._vertex, secondId: org100._vertex])
    def linkMember4 = openidm.create("/repo/link/member", null, [firstId: user4._vertex, secondId: org100._vertex])
    def linkMember5 = openidm.create("/repo/link/member", null, [firstId: user5._vertex, secondId: org100._vertex])



    def account001 = openidm.create("/repo/managed/account", "user1_googleapps", [externalId: "user1@forgerock.com", created: false])
    def linkAccountSystem1 = openidm.create("/repo/link/contain", null, [firstId: system000._vertex, secondId: account001._vertex])
    def linkAccountUser1 = openidm.create("/repo/link/owns", null, [firstId: user1._vertex, secondId: account001._vertex])

    def account002 = openidm.create("/repo/managed/account", "user1_salesforce", [externalId: "user1@forgerock.com", created: false])
    def linkAccountSystem2 = openidm.create("/repo/link/contain", null, [firstId: system010._vertex, secondId: account002._vertex])
    def linkAccountUser2 = openidm.create("/repo/link/owns", null, [firstId: user1._vertex, secondId: account002._vertex])


} catch (org.forgerock.json.resource.ResourceException e) {
    response = e.toJsonValue().getObject()
}
response

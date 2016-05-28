/**
 * The contents of this file are subject to the terms of the Common Development and
 * Distribution License (the License). You may not use this file except in compliance with the
 * License.
 *
 * You can obtain a copy of the License at legal/CDDLv1.0.txt. See the License for the
 * specific language governing permission and limitations under the License.
 *
 * When distributing Covered Software, include this CDDL Header Notice in each file and include
 * the License file at legal/CDDLv1.0.txt. If applicable, add the following below the CDDL
 * Header, with the fields enclosed by brackets [] replaced by your own identifying
 * information: "Portions copyright [year] [name of copyright owner]".
 *
 * Copyright 2016 ForgeRock AS.
 */

// add new test modules here:
define([
    //idm common tests
    "./org/forgerock/openidm/ui/common/UserModelTest",
    "./org/forgerock/openidm/ui/common/workflow/tasks/TaskDetailsViewTest",

    //idm enduser tests
    "./org/forgerock/openidm/ui/dashboard/DashboardTest",
    "./org/forgerock/openidm/ui/user/anonymousProcess/PasswordResetViewTest",
    "./org/forgerock/openidm/ui/user/anonymousProcess/SelfRegistrationViewTest",
    "./org/forgerock/openidm/ui/util/delegates/SiteConfigurationDelegateTest"
]);

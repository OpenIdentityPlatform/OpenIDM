/**
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2011-2015 ForgeRock AS. All rights reserved.
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

/*global define*/

define("org/forgerock/openidm/ui/common/main", [
    "./MandatoryPasswordChangeDialog",
    
    "./resource/ListResourceView",
    "./resource/EditResourceView",
    "./resource/GenericEditResourceView",
    "./resource/ResourceEditViewRegistry",
    "./resource/ResourceCollectionArrayView",
    
    "./delegates/ConfigDelegate",
    "./delegates/InternalUserDelegate",
    "./delegates/PolicyDelegate",
    "./delegates/SiteConfigurationDelegate",
    "./delegates/OpenAMProxyDelegate",
    "./delegates/ResourceDelegate",

    "./login/InternalLoginHelper",
    "./login/AuthenticationUnavailable",
    
    "./util/Constants",
    "./util/AMLoginUtils",
    "./util/JSONEditorSetupUtils",
    
    "./dashboard/DashboardWidgetLoader",
    "./dashboard/widgets/MemoryUsageWidget",
    "./dashboard/widgets/ReconProcessesWidget",
    "./dashboard/widgets/CPUUsageWidget",

    "./notifications/NotificationDelegate",
    "./notifications/NotificationsView",
    "./notifications/NotificationViewHelper",

    "./workflow/FormManager",
    "./workflow/WorkflowDelegate",

    "./workflow/tasks/TaskDetailsView",
    "./workflow/tasks/AbstractTaskForm",
    "./workflow/tasks/customview/ApplicationAcceptanceTask",
    "./workflow/tasks/TasksMenuView",
    "./workflow/tasks/TasksDashboard",
    "./workflow/tasks/TemplateTaskForm",

    "./workflow/processes/AbstractProcessForm",
    "./workflow/processes/StartProcessDashboardView",
    "./workflow/processes/StartProcessView",
    "./workflow/processes/TemplateStartProcessForm",
    "./workflow/processes/customview/SendNotificationProcess",

    "./linkedView/LinkedView"
]);
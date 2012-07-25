/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright © 2011-2012 ForgeRock AS. All rights reserved.
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

define("app/util/Constants",
        [],
        function () {
    var obj = {};

    obj.host = "";

    obj.OPENIDM_HEADER_PARAM_PASSWORD = "X-OpenIDM-Password";
    obj.OPENIDM_HEADER_PARAM_USERNAME = "X-OpenIDM-Username";
    obj.OPENIDM_HEADER_PARAM_LOGOUT = "X-OpenIDM-Logout";
    obj.OPENIDM_HEADER_PARAM_NO_SESION = "X-OpenIDM-NoSession";

    obj.OPENIDM_ANONYMOUS_USERNAME = "anonymous";
    obj.OPENIDM_ANONYMOUS_PASSWORD = "anonymous";

    obj.MODE_ADMIN = 'admin';
    obj.MODE_USER = 'user';

    obj.EVENT_PROFILE_INITIALIZATION = "user.profile.EVENT_PROFILE_INITIALIZATION";

    //service invoker
    obj.EVENT_END_REST_CALL = "common.delegate.EVENT_END_REST_CALL";
    obj.EVENT_START_REST_CALL = "common.delegate.EVENT_START_REST_CALL";
    obj.EVENT_REST_CALL_ERROR = "common.delegate.EVENT_REST_CALL_ERROR";

    //breadcrumbs
    obj.EVENT_BREADCRUMBS_CHANGE_REQUEST = "common.breadcrumbs.EVENT_CHANGE_BREADCRUMBS";
    obj.EVENT_BREADCRUMBS_HOME_CLICKED = "common.breadcrumbs.EVENT_BREADCRUMBS_HOME_CLICKED";

    //login
    obj.EVENT_SUCCESFULY_LOGGGED_IN = "user.login.EVENT_SUCCESFULY_LOGGGED_IN";
    obj.EVENT_LOGIN_FAILED = "user.login.EVENT_LOGIN_FAILED";
    obj.EVENT_LOGOUT = "user.login.EVENT_LOGOUT";
    obj.EVENT_SELF_REGISTRATION_REQUEST = "user.login.EVENT_SELF_REGISTRATION_REQUEST";
    obj.EVENT_LOGIN_REQUEST = "user.login.EVENT_LOGIN_REQUEST";
    obj.EVENT_AUTHENTICATED = "user.login.EVENT_AUTHENTICATED";
    obj.EVENT_OPENIDM_ADMIN_LOGGED_IN = "user.login.EVENT_OPENIDM_ADMIN_LOGGED_IN";

    //profile
    obj.EVENT_SHOW_PROFILE_REQUEST = "user.profile.EVENT_SHOW_PROFILE_REQUEST";
    obj.EVENT_USER_PROFILE_UPDATE_FAILED = "user.profile.EVENT_USER_PROFILE_UPDATE_FAILED";
    obj.EVENT_USER_PROFILE_UPDATED_SUCCESSFULY = "user.profile.EVENT_USER_PROFILE_UPDATED_SUCCESSFULY";
    obj.EVENT_USERNAME_UPDATED_SUCCESSFULY = "user.profile.EVENT_USERNAME_UPDATED_SUCCESSFULY";
    obj.EVENT_PROFILE_DELETE_USER_REQUEST = "user.profile.EVENT_PROFILE_DELETE_USER_REQUEST";
    obj.EVENT_GO_BACK_REQUEST = "user.profile.EVENT_GO_BACK_REQUEST";

    //forgottenPassword
    obj.EVENT_FORGOTTEN_SHOW_REQUEST = "user.forgottenpassword.EVENT_FORGOTTEN_SHOW_REQUEST";

    //registration
    obj.EVENT_USER_SUCCESSFULY_REGISTERED = "user.registration.EVENT_USER_SUCCESSFULY_REGISTERED";
    obj.EVENT_USER_REGISTRATION_ERROR = "user.registration.EVENT_USER_REGISTRATION_ERROR";

    //admin
    obj.EVENT_ADMIN_ADD_USER_REQUEST = "admin.usermanagement.EVENT_ADMIN_ADD_USER_REQUEST";
    obj.EVENT_USER_LIST_DELETE_USER_REQUEST = "admin.usermanagement.EVENT_USER_LIST_DELETE_USER_REQUEST";
    obj.EVENT_ADMIN_SHOW_PROFILE_REQUEST = "admin.usermanagement.EVENT_ADMIN_SHOW_PROFILE_REQUEST";
    obj.EVENT_ADMIN_AUTHENTICATED = "admin.EVENT_ADMIN_AUTHENTICATED";

    obj.EVENT_NAVIGATION_HOME_REQUEST = "common.navigation.EVENT_NAVIGATION_HOME_REQUEST";
    obj.EVENT_SWITCH_VIEW_REQUEST = "common.navigation.EVENT_SWITCH_VIEW_REQUEST";

    obj.FORGOTTEN_PASSWORD_CHANGED_SUCCESSFULLY = "user.forgottenpassword.FORGOTTEN_PASSWORD_CHANGED_SUCCESSFULLY";

    //configuration
    obj.EVENT_CONFIGURATION_CHANGED = "main.Configuration.EVENT_CONFIGURATION_CHANGED";

    //serviceinvoker
    obj.EVENT_AUTHENTICATION_DATA_CHANGED = "common.delegate.EVENT_AUTHENTICATION_DATA_CHANGED";

    obj.EVENT_APP_INTIALIZED = "main.EVENT_APP_INTIALIZED";
    obj.EVENT_DEPENDECIES_LOADED = "main.EVENT_DEPENDECIES_LOADED";
    
    return obj;
});


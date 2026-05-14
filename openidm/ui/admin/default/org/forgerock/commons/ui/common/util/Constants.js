"use strict";

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
 * Copyright 2011-2016 ForgeRock AS.
 */

define([], function () {
    var obj = {};

    obj.host = "";

    obj.EVENT_SERVICE_UNAVAILABLE = "error.SERICE_UNAVAILABLE";

    obj.EVENT_PROFILE_INITIALIZATION = "user.profile.EVENT_PROFILE_INITIALIZATION";

    //service invoker
    obj.EVENT_END_REST_CALL = "common.delegate.EVENT_END_REST_CALL";
    obj.EVENT_START_REST_CALL = "common.delegate.EVENT_START_REST_CALL";
    obj.EVENT_REST_CALL_ERROR = "common.delegate.EVENT_REST_CALL_ERROR";

    //dialog
    obj.EVENT_DIALOG_CLOSE = "dialog.EVENT_DIALOG_CLOSE";
    obj.EVENT_SHOW_DIALOG = "dialog.EVENT_SHOW_DIALOG";
    obj.EVENT_CHANGE_VIEW = "view.EVENT_CHANGE_VIEW";
    obj.EVENT_UNAUTHORIZED = "view.EVENT_UNAUTHORIZED";
    obj.EVENT_UNAUTHENTICATED = "view.EVENT_UNAUTHENTICATED";
    obj.EVENT_SHOW_LOGIN_DIALOG = "dialog.EVENT_SHOW_LOGIN_DIALOG";

    //login
    obj.EVENT_SUCCESFULY_LOGGGED_IN = "user.login.EVENT_SUCCESFULY_LOGGGED_IN";
    obj.EVENT_LOGIN_FAILED = "user.login.EVENT_LOGIN_FAILED";
    obj.EVENT_LOGOUT = "user.login.EVENT_LOGOUT";
    obj.EVENT_SELF_REGISTRATION_REQUEST = "user.login.EVENT_SELF_REGISTRATION_REQUEST";
    obj.EVENT_LOGIN_REQUEST = "user.login.EVENT_LOGIN_REQUEST";
    obj.EVENT_AUTHENTICATED = "user.login.EVENT_AUTHENTICATED";
    obj.EVENT_ADMIN_LOGGED_IN = "user.login.EVENT_ADMIN_LOGGED_IN";

    //profile
    obj.EVENT_SHOW_PROFILE_REQUEST = "user.profile.EVENT_SHOW_PROFILE_REQUEST";
    obj.EVENT_USER_PROFILE_UPDATE_FAILED = "user.profile.EVENT_USER_PROFILE_UPDATE_FAILED";
    obj.EVENT_USER_PROFILE_UPDATED_SUCCESSFULY = "user.profile.EVENT_USER_PROFILE_UPDATED_SUCCESSFULY";
    obj.EVENT_USERNAME_UPDATED_SUCCESSFULY = "user.profile.EVENT_USERNAME_UPDATED_SUCCESSFULY";
    obj.EVENT_PROFILE_DELETE_USER_REQUEST = "user.profile.EVENT_PROFILE_DELETE_USER_REQUEST";
    obj.EVENT_GO_BACK_REQUEST = "user.profile.EVENT_GO_BACK_REQUEST";
    obj.EVENT_SECURITY_DATA_CHANGE_REQUEST = "user.profile.EVENT_SECURITY_DATA_CHANGE_REQUEST";
    obj.EVENT_SITE_IDENTIFICATION_CHANGE_REQUEST = "user.profile.EVENT_SITE_IDENTIFICATION_CHANGE_REQUEST";
    obj.EVENT_ENTER_OLD_PASSWORD_REQUEST = "user.profile.EVENT_ENTER_OLD_PASSWORD_REQUEST";

    //admin
    obj.EVENT_ADMIN_USERS = "admin.usermanagement.EVENT_ADMIN_USERS";
    obj.EVENT_ADMIN_ADD_USER_REQUEST = "admin.usermanagement.EVENT_ADMIN_ADD_USER_REQUEST";
    obj.EVENT_USER_LIST_DELETE_USER_REQUEST = "admin.usermanagement.EVENT_USER_LIST_DELETE_USER_REQUEST";
    obj.EVENT_ADMIN_SHOW_PROFILE_REQUEST = "admin.usermanagement.EVENT_ADMIN_SHOW_PROFILE_REQUEST";
    obj.EVENT_ADMIN_CHANGE_USER_PASSWORD = "admin.usermanagement.EVENT_ADMIN_CHANGE_USER_PASSWORD";

    obj.EVENT_NAVIGATION_HOME_REQUEST = "common.navigation.EVENT_NAVIGATION_HOME_REQUEST";
    obj.EVENT_SWITCH_VIEW_REQUEST = "common.navigation.EVENT_SWITCH_VIEW_REQUEST";
    obj.EVENT_HANDLE_DEFAULT_ROUTE = "common.navigation.EVENT_HANDLE_DEFAULT_ROUTE";

    //configuration
    obj.EVENT_CONFIGURATION_CHANGED = "main.Configuration.EVENT_CONFIGURATION_CHANGED";

    //serviceinvoker
    obj.EVENT_AUTHENTICATION_DATA_CHANGED = "common.delegate.EVENT_AUTHENTICATION_DATA_CHANGED";

    obj.EVENT_APP_INITIALIZED = "main.EVENT_APP_INITIALIZED";
    obj.EVENT_READ_CONFIGURATION_REQUEST = "main.EVENT_READ_CONFIGURATION_REQUEST";
    obj.EVENT_DEPENDENCIES_LOADED = "main.EVENT_DEPENDENCIES_LOADED";
    obj.EVENT_DEPENDECIES_LOADED = "main.EVENT_DEPENDENCIES_LOADED"; // deprecated typo

    obj.ROUTE_REQUEST = "view.ROUTE_REQUEST";
    obj.EVENT_CHANGE_BASE_VIEW = "view.EVENT_CHANGE_BASE_VIEW";

    //notifications
    obj.EVENT_NOTIFICATION_DELETE_FAILED = "notification.EVENT_NOTIFICATION_DELETE_FAILED";
    obj.EVENT_GET_NOTIFICATION_FOR_USER_ERROR = "notification.EVENT_GET_NOTIFICATION_FOR_USER_ERROR";

    obj.EVENT_DISPLAY_MESSAGE_REQUEST = "messages.EVENT_DISPLAY_MESSAGE_REQUEST";

    obj.EVENT_USER_APPLICATION_DEFAULT_LNK_CHANGED = "messages.EVENT_USER_APPLICATION_DEFAULT_LNK_CHANGED";
    obj.EVENT_REQUEST_RESEND_REQUIRED = "messages.EVENT_REQUEST_RESEND_REQUIRED";

    //user application link states
    obj.USER_APPLICATION_STATE_APPROVED = "B65FA6A2-D43D-49CB-BEA0-CE98E275A8CD";
    obj.USER_APPLICATION_STATE_PENDING = "B65FA6A2-D43D-49CD-BEA0-CE98E275A8CD";

    obj.DEFAULT_LANGUAGE = "en";

    obj.ANONYMOUS_USERNAME = "anonymous";
    obj.ANONYMOUS_PASSWORD = "anonymous";

    obj.HEADER_PARAM_PASSWORD = "X-Password";
    obj.HEADER_PARAM_USERNAME = "X-Username";
    obj.HEADER_PARAM_NO_SESSION = "X-NoSession";

    obj.SELF_SERVICE_CONTEXT = "selfservice/";

    obj.EVENT_SELECT_KBA_QUESTION = "user.selfservice.kba.EVENT_SELECT_KBA_QUESTION";
    obj.EVENT_DELETE_KBA_QUESTION = "user.selfservice.kba.EVENT_DELETE_KBA_QUESTION";

    return obj;
});

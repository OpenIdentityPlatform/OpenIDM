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


// A configuration for allowed HTTP requests. Each entry in the configuration contains a pattern
// to match against the incoming request ID and, in the event of a match, the associated roles,
// methods, and actions that are allowed for requests on that particular pattern.
//
// pattern:  A pattern to match against an incoming request's resource ID
// roles:  A comma separated list of allowed roles
// methods:  A comma separated list of allowed methods
// actions:  A comma separated list of allowed actions
// customAuthz: A custom function for additional authorization logic/checks (optional)
// excludePatterns: A comma separated list of patterns to exclude from the pattern match (optional)
//
// A single '*' character indicates all possible values.  With patterns ending in "/*", the "*"
// acts as a wild card to indicate the pattern accepts all resource IDs "below" the specified
// pattern (prefix).  For example the pattern "managed/*" would match "managed/user" or anything
// starting with "managed/".  Note: it would not match "managed", which would need to have its
// own entry in the config.

/*jslint vars:true*/

var httpAccessConfig =
{
    "configs" : [
         // proxy back to configured OpenAM server endpoints
        {
            "pattern"    : "endpoint/openam/*",
            "roles"      : "*",
            "methods"    : "*",
            "actions"    : "*"
        },
        // Anyone can read from these endpoints
        {
           "pattern"    : "info/*",
           "roles"      : "*",
           "methods"    : "read",
           "actions"    : "*"
        },
        {
           "pattern"    : "identityProviders",
           "roles"      : "*",
           "methods"    : "read,action",
           "actions"    : "getauthtoken"
        },
        {
            "pattern"    : "config/ui/themeconfig",
            "roles"      : "*",
            "methods"    : "read",
            "actions"    : "*"
        },
        {
           "pattern"    : "config/ui/configuration",
           "roles"      : "*",
           "methods"    : "read",
           "actions"    : "*"
        },
        {
           "pattern"    : "config/selfservice/kbaConfig",
           "roles"      : "*",
           "methods"    : "read",
           "actions"    : "*",
           "customAuthz": "checkIfUIIsEnabled('selfRegistration') || checkIfUIIsEnabled('passwordReset')"
        },
        {
            "pattern"    : "config/ui/dashboard",
            "roles"      : "openidm-authorized",
            "methods"    : "read",
            "actions"    : "*"
        },
        // externally-visisble Self-Service endpoints
        {
           "pattern"    : "selfservice/registration",
           "roles"      : "*",
           "methods"    : "read,action",
           "actions"    : "submitRequirements",
           "customAuthz" : "checkIfUIIsEnabled('selfRegistration')"
        },

        {
           "pattern"    : "selfservice/reset",
           "roles"      : "*",
           "methods"    : "read,action",
           "actions"    : "submitRequirements",
           "customAuthz" : "checkIfUIIsEnabled('passwordReset')"
        },

        {
           "pattern"    : "selfservice/username",
           "roles"      : "*",
           "methods"    : "read,action",
           "actions"    : "submitRequirements",
           "customAuthz" : "checkIfUIIsEnabled('forgotUsername')"
        },

        {
            "pattern"   : "policy/managed/user",
            "roles"     : "*",
            "methods"   : "read",
            "actions"   : "",
            "customAuthz" : "checkIfUIIsEnabled('selfRegistration') || checkIfUIIsEnabled('passwordReset')"
        },
        {
            "pattern"   : "policy/managed/user/-",
            "roles"     : "*",
            "methods"   : "action",
            "actions"   : "validateObject",
            "customAuthz" : "checkIfUIIsEnabled('selfRegistration') || checkIfUIIsEnabled('passwordReset')"
        },

        {
           "pattern"    : "selfservice/kba",
           "roles"      : "openidm-authorized",
           "methods"    : "read",
           "actions"    : "*",
           "customAuthz" : "checkIfUIIsEnabled('kbaEnabled')"
        },

        // rules governing requests originating from forgerock-selfservice
        {
            "pattern"   : "managed/user",
            "roles"     : "openidm-reg",
            "methods"   : "create",
            "actions"   : "*",
            "customAuthz" : "checkIfUIIsEnabled('selfRegistration') && isSelfServiceRequest() && onlyEditableManagedObjectProperties('user')"
        },
        {
            "pattern"   : "managed/user",
            "roles"     : "*",
            "methods"   : "query",
            "actions"   : "*",
            "customAuthz" : "(checkIfUIIsEnabled('forgotUsername') || checkIfUIIsEnabled('passwordReset')) && isSelfServiceRequest()"
        },
        {
            "pattern"   : "managed/user/*",
            "roles"     : "*",
            "methods"   : "read",
            "actions"   : "*",
            "customAuthz" : "(checkIfUIIsEnabled('forgotUsername') || checkIfUIIsEnabled('passwordReset')) && isSelfServiceRequest()"
        },
        {
            "pattern"   : "managed/user/*",
            "roles"     : "*",
            "methods"   : "patch,action",
            "actions"   : "patch",
            "customAuthz" : "checkIfUIIsEnabled('passwordReset') && isSelfServiceRequest() && onlyEditableManagedObjectProperties('user')"
        },
        {
            "pattern"   : "external/email",
            "roles"     : "*",
            "methods"   : "action",
            "actions"   : "send",
            "customAuthz" : "(checkIfUIIsEnabled('forgotUsername') || checkIfUIIsEnabled('passwordReset') || checkIfUIIsEnabled('selfRegistration')) && isSelfServiceRequest()"
        },

        // openidm-admin can request nearly anything (except query expressions on repo endpoints)
        {
            "pattern"   : "*",
            "roles"     : "openidm-admin",
            "methods"   : "*", // default to all methods allowed
            "actions"   : "*", // default to all actions allowed
            "customAuthz" : "disallowQueryExpression()",
            "excludePatterns": "repo,repo/*"
        },
        // additional rules for openidm-admin that selectively enable certain parts of system/
        {
            "pattern"   : "system/*",
            "roles"     : "openidm-admin",
            "methods"   : "create,read,update,delete,patch,query", // restrictions on 'action'
            "actions"   : "",
            "customAuthz" : "disallowQueryExpression()"
        },
        // Allow access to custom scripted endpoints
        {
            "pattern"   : "system/*",
            "roles"     : "openidm-admin",
            "methods"   : "script",
            "actions"   : "*"
        },
        // Note that these actions are available directly on system as well
        {
            "pattern"   : "system/*",
            "roles"     : "openidm-admin",
            "methods"   : "action",
            "actions"   : "test,testConfig,createconfiguration,liveSync,authenticate"
        },
        // Disallow command action on repo
        {
            "pattern"   : "repo",
            "roles"     : "openidm-admin",
            "methods"   : "*", // default to all methods allowed
            "actions"   : "*", // default to all actions allowed
            "customAuthz" : "disallowCommandAction()"
        },
        {
            "pattern"   : "repo/*",
            "roles"     : "openidm-admin",
            "methods"   : "*", // default to all methods allowed
            "actions"   : "*", // default to all actions allowed
            "customAuthz" : "disallowCommandAction()"
        },
        //allow the ability to delete links for a specific mapping
        {
            "pattern"   : "repo/links",
            "roles"     : "openidm-admin",
            "methods"   : "action",
            "actions"   : "command",
            "customAuthz" : "request.additionalParameters.commandId === 'delete-mapping-links'"
        },

        // Additional checks for authenticated users
        {
            "pattern"   : "policy/*",
            "roles"     : "openidm-authorized", // openidm-authorized is logged-in users
            "methods"   : "read,action",
            "actions"   : "*"
        },
        {
            "pattern"   : "config/ui/*",
            "roles"     : "openidm-authorized",
            "methods"   : "read",
            "actions"   : "*"
        },
        {
            "pattern"   : "authentication",
            "roles"     : "openidm-authorized",
            "methods"   : "action",
            "actions"   : "reauthenticate"
        },

        // This rule is primarily controlled by the ownDataOnly function - that will only allow
        // access to the endpoint from which the user originates
        // (For example a managed/user with the _id of bob will only be able to access managed/user/bob)

        {
            "pattern"   : "*",
            "roles"     : "openidm-authorized",
            "methods"   : "read",
            "actions"   : "*",
            "customAuthz" : "ownDataOnly()"
        },
        {
            "pattern"   : "*",
            "roles"     : "openidm-authorized",
            "methods"   : "update,patch,action",
            "actions"   : "patch",
            "customAuthz" : "ownDataOnly() && onlyEditableManagedObjectProperties('user')"
        },
        {
            "pattern"   : "selfservice/user/*",
            "roles"     : "openidm-authorized",
            "methods"   : "patch,action",
            "actions"   : "patch",
            "customAuthz" : "(request.resourcePath === 'selfservice/user/' + context.security.authorization.id) && onlyEditableManagedObjectProperties('user')"
        },

        // enforcement of which notifications you can read and delete is done within the endpoint
        {
            "pattern"   : "endpoint/usernotifications",
            "roles"     : "openidm-authorized",
            "methods"   : "read",
            "actions"   : "*"
        },
        {
            "pattern"   : "endpoint/usernotifications/*",
            "roles"     : "openidm-authorized",
            "methods"   : "delete",
            "actions"   : "*"
        },

        // Workflow-related endpoints for authorized users

        {
            "pattern"   : "endpoint/getprocessesforuser",
            "roles"     : "openidm-authorized",
            "methods"   : "read",
            "actions"   : "*"
        },
        {
            "pattern"   : "endpoint/gettasksview",
            "roles"     : "openidm-authorized",
            "methods"   : "query",
            "actions"   : "*"
        },
        {
            "pattern"   : "workflow/taskinstance/*",
            "roles"     : "openidm-authorized",
            "methods"   : "action",
            "actions"   : "complete",
            "customAuthz" : "isMyTask()"
        },
        {
            "pattern"   : "workflow/taskinstance/*",
            "roles"     : "openidm-authorized",
            "methods"   : "read,update",
            "actions"   : "*",
            "customAuthz" : "canUpdateTask()"
        },
        {
            "pattern"   : "workflow/processinstance",
            "roles"     : "openidm-authorized",
            "methods"   : "create",
            "actions"   : "*",
            "customAuthz": "isAllowedToStartProcess()"
        },
        {
            "pattern"   : "workflow/processdefinition/*",
            "roles"     : "openidm-authorized",
            "methods"   : "*",
            "actions"   : "read",
            "customAuthz": "isOneOfMyWorkflows()"
        },
        // Clients authenticated via SSL mutual authentication
        {
            "pattern"   : "managed/user",
            "roles"     : "openidm-cert",
            "methods"   : "patch,action",
            "actions"   : "patch",
            "customAuthz" : "isQueryOneOf({'managed/user': ['for-userName']}) && restrictPatchToFields(['password'])"
        },
        // Security Management
        {
            "pattern"   : "security/*",
            "roles"     : "openidm-admin",
            "methods"   : "read,create,update,delete",
            "actions"   : ""
        }
    ]
};

// Additional custom authorization functions go here

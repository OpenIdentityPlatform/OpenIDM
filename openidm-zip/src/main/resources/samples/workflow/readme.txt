These are some basic sample workflows you can copy into your base workflow folder to try them out.

One note - to use the SendNotification.bar workflow, you'll have to adjust the settings in access.js to
allow access to the query-all query for authorized users, like so:

        {   
            "pattern"   : "*",
            "roles"     : "openidm-authorized",
            "methods"   : "*",
            "actions"   : "*",
            "customAuthz" : "(ownDataOnly() && managedUserRestrictedToAllowedRoles('openidm-authorized')) || isQueryOneOf({'managed/user/': ['query-all']})"
        },

    /*
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
     * Portions copyright 2011-2016 ForgeRock AS.
     */

The files in this directory contain fragments to add to provisioner
configuration to enable the sync-failure handling supported by OpenIDM.

Configuring the provisioner to use sync-failure handling
--------------------------------------------------------

For projects using the syncFailureHandler for live-sync, add the stanza

    "syncFailureHandler" : {
        "maxRetries" : <number of retries>,
        "postRetryAction" : <retry strategy>
    },

to `provisioner.openicf-*.json`.  Number of retries is an integer.
Set to -1 or omit `maxRetries` for "infinite" retries and no sync
failure handler.  OpenIDM will employ the `postRetryAction` after
the number of failures specified by "maxRetries" are exhausted.
The possibilities for retry strategy are

    "postRetryAction" : "logged-ignore"

to log and ignore the failure;

    "postRetryAction" : "dead-letter-queue"

to save the failure detail in the repository target
`synchronisation/deadLetterQueue/<name>`, where <name> is the name
of your provisioner;

or

    "script" : {
        "type" : "text/javascript",
        "file" : "script/onSyncFailure.js"
    }

to execute a custom script on liveSync failures.  In the case of using
the scripted live sync failure handler, the file property specifies the
location of the javascript to execute.  The data available to the
javascript function is as follows:

    {
        "syncFailure" : {
            "token" : integer token id,
            "systemIdentifier" : string identifer, matches "name" property in provisioner.openicf.json,
            "objectType" : string object type being synced, one of the keys in "objectTypes" property in provisioner.openicf.json,
            "uid" : string the object's uid, e.g. uid=joe,ou=People,dc=example,dc=com,
            "failedRecord", string the record that failed to sync
        },
        "failureCause" : Exception that caused the original sync failure; call .toString, .getCause, .getMessage, etc,
        "failureHandlers" : {
            "loggedIgnore" : Internal log/ignore handler,
            "deadLetterQueue" : Internal dead-letter-queue handler
        }
    }

To use any of the `syncFailure` data, use

    syncFailure.<fieldname>

in your javascript code.

To invoke one of built-in sync failure handlers from your javascript, call

    failureHandlers.deadLetterQueue.invoke(context, syncFailure, failureCause);


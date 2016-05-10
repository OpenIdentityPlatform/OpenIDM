/*
 * The contents of this file are subject to the terms of the Common Development and
 * Distribution License (the License). You may not use this file except in compliance
 * with the License.
 *
 * You can obtain a copy of the License at legal/CDDLv1.0.txt. See the License for
 * the specific language governing permission and limitations under the License.
 *
 * When distributing Covered Software, include this CDDL Header Notice in each file
 * and include the License file at legal/CDDLv1.0.txt. If applicable, add the following
 * below the CDDL Header, with the fields enclosed by brackets [] replaced by your
 * own identifying information: "Portions copyright [year] [name of copyright owner]".
 *
 * Copyright 2016 ForgeRock AS.
 */

package samples.scriptedssh.tools

class ErrorCodes {

    static String E_USAGE = "invalid command syntax"
    static String E_BAD_ARG = "invalid argument to option"
    static String E_PW_UPDATE = "can't update password file"

    // Error codes returned with useradd
    static def useradd = [
            "1" : E_PW_UPDATE,
            "2" : E_USAGE,
            "3" : E_BAD_ARG,
            "4" : "UID already in use",
            "6" : "specified group doesn't exist",
            "9" : "user name already in use",
            "10": "can't update group file",
            "12": "can't create home directory",
            "14": "can't update SELinux user mapping"
    ]

    // Error codes returned with groupadd
    static def groupadd = [
            "2" : E_USAGE,
            "3" : E_BAD_ARG,
            "4" : "GID already in use",
            "9" : "group name already in use",
            "10": "can't update group file",
    ]

    // Error codes returned with usermod
    static def usermod = [
            "1" : E_PW_UPDATE,
            "2" : E_USAGE,
            "3" : E_BAD_ARG,
            "4" : "UID already in use",
            "6" : "specified user/group doesn't exist",
            "8" : "user to modify is logged in",
            "9" : "user name already in use",
            "10": "can't update group file",
            "12": "can't move home directory",
            "13": "can't update SELinux user mapping",
            "16": "can't update the subordinate uid file",
            "18": "can't update the subordinate gid file"
    ]

    // Error codes returned with groupmod
    static def groupmod = [
            "2" : E_USAGE,
            "3" : E_BAD_ARG,
            "4" : "gid already in use",
            "6" : "specified group doesn't exist",
            "9" : "group name already in use",
            "10": "can't update group file"
    ]

    // Error codes returned with userdel
    static def userdel = [
            "1" : E_PW_UPDATE,
            "2" : E_USAGE,
            "6" : "specified user doesn't exist",
            "8" : "user currently logged in",
            "10": "can't update group file",
            "12": "can't remove home directory"
    ]

    // Error codes returned with groupdel
    static def groupdel = [
            "2" : E_USAGE,
            "6" : "specified group doesn't exist",
            "8" : "can't remove user's primary group",
            "10": "can't update group file",
    ]

    // Error codes returned with passwd
    static def passwd = [
            "1": "permission denied",
            "2": E_USAGE,
            "3": "unexpected failure, nothing done",
            "4": "unexpected failure, passwd file missing",
            "5": "passwd file busy, try again later",
            "6": E_BAD_ARG
    ]
}
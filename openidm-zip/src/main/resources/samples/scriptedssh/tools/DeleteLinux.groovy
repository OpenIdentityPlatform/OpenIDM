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
 * Copyright 2016 ForgeRock AS.
 */

package samples.scriptedssh.tools

import org.forgerock.openicf.connectors.ssh.SSHConfiguration
import org.forgerock.openicf.connectors.ssh.SSHConnection
import org.forgerock.openicf.misc.scriptedcommon.OperationType
import org.identityconnectors.common.logging.Log
import org.identityconnectors.framework.common.exceptions.ConnectorException
import org.identityconnectors.framework.common.exceptions.UnknownUidException
import org.identityconnectors.framework.common.objects.ObjectClass
import org.identityconnectors.framework.common.objects.OperationOptions

def operation = operation as OperationType
def configuration = configuration as SSHConfiguration
def connection = connection as SSHConnection
def log = log as Log
def objectClass = objectClass as ObjectClass
def options = options as OperationOptions
def uid = uid.getUidValue() as String

// SSH Connector specific bindings

//setTimeout <value> : defines global timeout (ms) on expect/send actions
//setTimeoutSec <value> : defines global timeout (sec) on expect/send actions
//send <command> : sends a String or GString of commands
//sendln <command> : sends a String or GString of commands + \r
//sudo <command>: mock the sudo command, using sudo cmd, sudo prompt and user password defined in the configuration
//sendControlC: sends a Ctrl-C interrupt sequence
//sendControlD: sends a Ctrl-D sequence
//promptReady <prompt> <retry>: force the connection to be in prompt ready mode. Returns true if success, false if failed
//expect <pattern>: expect a match pattern from the Read buffer
//expect <pattern>, <Closure>: expect a match pattern from the Read buffer and associate a simple Closure to be performed on pattern match.
//expect <List of matches>: expect a list of different match pattern
//match: defines a global match pattern and a Closure within a call to expect<List>
//regexp: defines a Perl5 style regular expression and a Closure within a call to expect<List>
//timeout: defines a local timeout and a Closure within a call to expect
// The following constants: TIMEOUT_FOREVER, TIMEOUT_NEVER, TIMEOUT_EXPIRED, EOF_FOUND

log.info("Entering {0} script", operation);
assert operation == OperationType.DELETE, 'Operation must be a DELETE'

def prompt = configuration.getPrompt()
def command = ""
def message = ""
def exception
def success = false

// The prompt is the first thing we should expect from the connection
if (!promptReady(2)) {
    throw new ConnectorException("Can't get the session prompt")
}
log.info("Prompt ready...")

// Prepare the command to execute
switch (objectClass.getObjectClassValue()) {
    case ObjectClass.ACCOUNT_NAME:
        command = "/usr/sbin/userdel " + uid
        break
    case ObjectClass.GROUP_NAME:
        command = "/usr/sbin/groupdel " + uid
        break
}

sudo command
expect prompt, { sendln "echo \$?" }// Check returned code

switch (objectClass.getObjectClassValue()) {
    case ObjectClass.ACCOUNT_NAME:
        expect(
                [
                        match("0") {
                            success = true
                        },
                        match("6") {
                            message = ErrorCodes.userdel."6"
                            exception = new UnknownUidException(message)
                        },
                        regexp("8|10|12|1|2") {
                            message = ErrorCodes.userdel[it.getMatch()]
                            exception = new ConnectorException(message)
                        }
                ]
        )
        break
    case ObjectClass.GROUP_NAME:
        expect(
                [
                        match("0") {
                            success = true
                        },
                        match("6") {
                            message = ErrorCodes.groupdel."6"
                            exception = new UnknownUidException(message)
                        },
                        regexp("2|8|10") {
                            message = ErrorCodes.groupdel[it.getMatch()]
                            exception = new ConnectorException(message)
                        }
                ]
        )
        break
}

if (!success) {
    log.info("Delete of $uid failed: $message")
    throw exception
}
log.info("Delete of $uid successful")
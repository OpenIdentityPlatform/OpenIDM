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

import org.forgerock.openicf.connectors.ssh.CommandLineBuilder
import org.forgerock.openicf.connectors.ssh.SSHConfiguration
import org.forgerock.openicf.connectors.ssh.SSHConnection
import org.forgerock.openicf.misc.scriptedcommon.OperationType
import org.identityconnectors.common.logging.Log
import org.identityconnectors.framework.common.exceptions.AlreadyExistsException
import org.identityconnectors.framework.common.exceptions.ConnectorException
import org.identityconnectors.framework.common.exceptions.InvalidAttributeValueException
import org.identityconnectors.framework.common.objects.Attribute
import org.identityconnectors.framework.common.objects.AttributesAccessor
import org.identityconnectors.framework.common.objects.ObjectClass
import org.identityconnectors.framework.common.objects.OperationOptions

import static org.identityconnectors.common.security.SecurityUtil.decrypt

def operation = operation as OperationType
def attributes = attributes as Set<Attribute>
def configuration = configuration as SSHConfiguration
def connection = connection as SSHConnection
def id = id as String
def log = log as Log
def objectClass = objectClass as ObjectClass
def options = options as OperationOptions
def attrs = new AttributesAccessor(attributes as Set<Attribute>)

// SSH Connector specific bindings

//setTimeout <value> : defines global timeout (ms) on expect/send actions
//setTimeoutSec <value> : defines global timeout (sec) on expect/send actions
//send <command> : sends a String or GString of commands
//sendln <command> : sends a String or GString of commands + \r
//sendControlC: sends a Ctrl-C interrupt sequence
//sendControlD: sends a Ctrl-D sequence
//sudo <command>: mock the sudo command, using sudo cmd, sudo prompt and user password defined in the configuration
//promptReady <prompt> <retry>: force the connection to be in prompt ready mode. Returns true if success, false if failed
//expect <pattern>: expect a match pattern from the Read buffer
//expect <pattern>, <Closure>: expect a match pattern from the Read buffer and associate a simple Closure to be performed on pattern match.
//expect <List of matches>: expect a list of different match pattern
//match: defines a global match pattern and a Closure within a call to expect<List>
//regexp: defines a Perl5 style regular expression and a Closure within a call to expect<List>
//timeout: defines a local timeout and a Closure within a call to expect
// The following constants: TIMEOUT_FOREVER, TIMEOUT_NEVER, TIMEOUT_EXPIRED, EOF_FOUND

log.info("Entering {0} script", operation);
assert operation == OperationType.CREATE, 'Operation must be a CREATE'

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

switch (objectClass.getObjectClassValue()) {
    case ObjectClass.ACCOUNT_NAME:
        // /usr/sbin/useradd -c 'description' -d 'home' -g 'group' -s 'shell' -u 'uid' <login>
        command = new CommandLineBuilder("/usr/sbin/useradd")
                .c(attrs.findString("description"))
                .e(attrs.findString("expireDate"))
                .d(attrs.findString("home"))
                .g(attrs.findString("group"))
                .s(attrs.findString("shell"))
                .u(attrs.findString("uid"))
                .append(id)
                .build()
        break
    case ObjectClass.GROUP_NAME:
        // /usr/sbin/groupadd <groupname>
        command = "/usr/sbin/groupadd " + id
        break
}
log.info("Create command is: $command")

if (!sudo(command)) {
    throw new ConnectorException("Failed to run sudo $command")
}
expect prompt, { sendln "echo \$?" }// Check returned code

switch (objectClass.getObjectClassValue()) {
    case ObjectClass.ACCOUNT_NAME:
        expect(
                [
                        match("0") {
                            success = true
                        },
                        match("3") {
                            message = ErrorCodes.useradd."3"
                            exception = new InvalidAttributeValueException(message)
                        },
                        regexp("4|9") {
                            message = ErrorCodes.useradd[it.getMatch()]
                            exception = new AlreadyExistsException(message)
                        },
                        regexp("(10|12|14|1|2|6)\r\n") {
                            message = ErrorCodes.useradd[it.getMatch(1)]
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
                        match("3") {
                            message = ErrorCodes.groupadd."3"
                            exception = new InvalidAttributeValueException(message)
                        },
                        regexp("4|9") {
                            message = ErrorCodes.groupadd[it.getMatch()]
                            exception = new AlreadyExistsException(message)
                        },
                        regexp("(2|10)") {
                            message = ErrorCodes.groupadd[it.getMatch()]
                            exception = new ConnectorException(message)
                        }
                ]
        )
        break
}

if (!success) {
    log.info("Create of $id failed: $message")
    throw exception
}

// Need to deal with password if any provided for user creation
if (objectClass.getObjectClassValue() == ObjectClass.ACCOUNT_NAME) {
    if (attrs.findString("password") != null && promptReady(2)) {
        log.info "Setting password for $id"
        success = false
        sudo "/usr/bin/passwd $id"
        expect "Enter new UNIX password: ", { sendln decrypt(attrs.findString("password")) }
        expect "Retype new UNIX password: ", { sendln decrypt(attrs.findString("password")) }
        expect prompt, { sendln "echo \$?" }         // Check returned code
        expect(
                [
                        match("0") {
                            success = true
                        },
                        match("6") {
                            message = ErrorCodes.passwd."6"
                            exception = new InvalidAttributeValueException(message)
                        },
                        regexp("([1-5])\r\n") {
                            message = ErrorCodes.passwd[it.getMatch(1)]
                            exception = new ConnectorException(message)
                        }
                ]
        )
    }
}

if (!success) {
    log.info("Create of $id succeeded but unable to set password: $message")
    throw exception
}
log.info("Create of $id successful")
return id

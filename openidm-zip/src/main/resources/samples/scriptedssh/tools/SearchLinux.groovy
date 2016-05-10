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

import org.forgerock.openicf.connectors.ssh.SSHConfiguration
import org.forgerock.openicf.connectors.ssh.SSHConnection
import org.forgerock.openicf.misc.scriptedcommon.OperationType
import org.identityconnectors.common.logging.Log
import org.identityconnectors.framework.common.exceptions.ConnectorException
import org.identityconnectors.framework.common.objects.Name
import org.identityconnectors.framework.common.objects.ObjectClass
import org.identityconnectors.framework.common.objects.OperationOptions
import org.identityconnectors.framework.common.objects.Uid
import org.identityconnectors.framework.common.objects.filter.EqualsFilter
import org.identityconnectors.framework.common.objects.filter.Filter

def operation = operation as OperationType
def configuration = configuration as SSHConfiguration
def connection = connection as SSHConnection
def filter = filter as Filter
def query = query as Map
def log = log as Log
def objectClass = objectClass as ObjectClass
def options = options as OperationOptions
def attributesToGet = options.getAttributesToGet() as String[]

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
assert operation == OperationType.SEARCH, 'Operation must be a SEARCH'

def prompt = configuration.getPrompt()

// We do not want __UID__ and __NAME__ being part of the attributes to get
if (attributesToGet != null) {
    attributesToGet = attributesToGet - [Uid.NAME] - [Name.NAME] as String[]
}

// The prompt is the first thing we should expect from the connection
if (!promptReady(2)) {
    throw new ConnectorException("Can't get the session prompt")
}
log.info("Prompt ready...")

switch (objectClass.getObjectClassValue()) {
    case ObjectClass.ACCOUNT_NAME:
        if (filter == null) {
            // Get logon:uid:gid:description:home:shell
            sendln "/usr/bin/cut -d: -f1,3,4,5,6,7 /etc/passwd"
        } else if (filter instanceof EqualsFilter && ((EqualsFilter) filter).getAttribute().is(Uid.NAME)) {
            def username = ((EqualsFilter) filter).getAttribute().getValue().get(0)
            sendln "/usr/bin/cut -d: -f1,3,4,5,6,7 /etc/passwd | /bin/grep \"^$username:\""
        }
        expect prompt, {
            if (it.getMatchedWhere() > 0) {
                def list = it.getBuffer().substring(0, it.getMatchedWhere()).split("\r\n")
                list.each() { entry ->
                    def attrs = entry.split(":")
                    handler {
                        uid attrs[0]
                        id attrs[0]
                        if ("uid" in attributesToGet && attrs.size() > 1) {
                            attribute "uid", attrs[1]
                        }
                        if ("group" in attributesToGet && attrs.size() > 2) {
                            attribute "group", attrs[2]
                        }
                        if ("description" in attributesToGet && attrs.size() > 3) {
                            attribute "description", attrs[3]
                        }
                        if ("home" in attributesToGet && attrs.size() > 4) {
                            attribute "home", attrs[4]
                        }
                        if ("shell" in attributesToGet && attrs.size() > 5) {
                            attribute "shell", attrs[5]
                        }
                    }
                }
            }
        }
        break
    case ObjectClass.GROUP_NAME:
        if (filter == null) {
            // Get name:gid:member
            sendln "/usr/bin/cut -d: -f1,3,4 /etc/group"
        } else if (filter instanceof EqualsFilter && ((EqualsFilter) filter).getAttribute().is(Uid.NAME)) {
            def groupname = ((EqualsFilter) filter).getAttribute().getValue().get(0)
            sendln "/usr/bin/cut -d: -f1,3,4 /etc/group | /bin/grep \"^$groupname:\""
        }
        expect prompt, {
            if (it.getMatchedWhere() > 0) {
                def list = it.getBuffer().substring(0, it.getMatchedWhere()).split("\r\n")
                list.each() { entry ->
                    def attrs = entry.split(":")
                    handler {
                        uid attrs[0]
                        id attrs[0]
                        if ("gid" in attributesToGet && attrs.size() > 1) {
                            attribute "gid", attrs[1]
                        }
                        if ("members" in attributesToGet && attrs.size() > 2) {
                            attribute "members", attrs[2].split(",")
                        }
                    }
                }
            }
        }
        break
}

// Make sure we leave the connection in prompt ready mode.
sendln ""
/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2014 ForgeRock AS. All Rights Reserved
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


import org.forgerock.json.fluent.JsonValue
import org.forgerock.json.resource.ActionRequest
import org.forgerock.json.resource.Connection
import org.forgerock.json.resource.Requests
import org.forgerock.json.resource.RootContext
import org.forgerock.openicf.connectors.scriptedcrest.ScriptedCRESTConfiguration
import org.forgerock.openicf.misc.scriptedcommon.OperationType
import org.identityconnectors.common.logging.Log
import org.identityconnectors.framework.common.objects.OperationOptions

def operation = operation as OperationType
def configuration = configuration as ScriptedCRESTConfiguration
def connection = connection as Connection
def log = log as Log
def options = options as OperationOptions
def scriptArguments = scriptArguments as Map
def scriptLanguage = scriptLanguage as String
def scriptText = scriptText as String

if ("crest".equalsIgnoreCase(scriptLanguage)) {
    ActionRequest request = Requests.newActionRequest("users", scriptText)
    request.setContent(new JsonValue(scriptArguments))
    def response = connection.action(new RootContext(), request)
    return response.getObject()
} else {

    ActionRequest request = Requests.newActionRequest("system/ldap/account", "script")
    request.setAdditionalParameter("_scriptId", "")
    if ("resource".equalsIgnoreCase(scriptLanguage)) {
        request.setAdditionalParameter("_scriptExecuteMode", "resource")
    }
    if (null != options.options.variablePrefix) {
        request.setAdditionalParameter("_scriptVariablePrefix", options.options.variablePrefix)
    }


    scriptArguments.each { String key, Object value ->
        if (!key.startsWith('_')) {
            if (value instanceof String) {
                request.setAdditionalParameter(key, value as String)
            } else {
                log.warn("Argument parameter ${key} is ignored because its type ${value?.class} is not supported.")
            }
        }
    }

    def result = connection.action(new RootContext(), request)
    def returnValue = []


    result.actions.each { key, value ->
        if ("error".equals(key)) {
            throw new ConnectException(value as String)
        } else {
            returnValue.add(value)
        }
    }

    return returnValue as Object[]
}

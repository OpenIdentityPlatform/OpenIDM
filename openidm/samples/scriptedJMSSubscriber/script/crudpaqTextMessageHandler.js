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

/**
 * This script expects the "message" variable to be populated with a JMS TextMessage where the
 * content is JSON that can then be used to invoke CRUDPAQ calls on the 'openidm' exposed object.
 * The operations are based on the exposed functions of the openidm object as described in the
 * IDM integrators guide, Appendix F.1. Function Reference.
 * 
 * The expected JSON should be one of the following formats:
 *
 *  {
 *      "operation" : "CREATE",
 *      "resourceName" : "/managed/user",
 *      "newResourceId" : "",
 *      "content" : {},
 *      "params" : {},
 *      "fields" : [ ]
 *  }
 *  or
 *  {
 *      "operation" : "READ",
 *      "resourceName" : "",
 *      "params" : {},
 *      "fields" : [ ]
 *  }
 *  or
 *  {
 *      "operation" : "UPDATE",
 *      "resourceName" : "",
 *      "rev" : "",
 *      "value" : {},
 *      "params" : {},
 *      "fields" : [ ]
 *  }
 *  or
 *  {
 *      "operation" : "DELETE",
 *      "resourceName" : "",
 *      "rev" : "",
 *      "params" : {},
 *      "fields" : [ ]
 *  }
 *  or
 *  {
 *      "operation" : "PATCH",
 *      "resourceName" : "",
 *      "rev" : "",
 *      "value" : {},
 *      "params" : {},
 *      "fields" : [ ]
 *  }
 *  or
 *  {
 *      "operation" : "ACTION",
 *      "resourceName" : "",
 *      "actionName": "",
 *      "content" : {},
 *      "params" : {},
 *      "fields" : [ ]
 *  }
 *  or
 *  {
 *      "operation" : "QUERY",
 *      "resourceName" : "",
 *      "params" : {},
 *      "fields" : [ ]
 *  }
 */
(function () {
    try {
        console.log("**************request received*************");
        var payload = adaptMessageToCrudpaqPayload(message);

        console.log("Parsed JMS JSON =\n" + JSON.stringify(payload, null, 4));
        var response;
        switch (payload.operation) {
            case ("CREATE") :
                response = openidm.create(payload.resourceName, payload.newResourceId, payload.content,
                    payload.params, payload.fields);
                break;
            case("READ") :
                response = openidm.read(payload.resourceName, payload.params, payload.fields);
                break;
            case("UPDATE") :
                response = openidm.update(payload.resourceName, payload.rev, payload.value, payload.params,
                    payload.fields);
                break;
            case("DELETE") :
                response = openidm.delete(payload.resourceName, payload.rev, payload.params, payload.fields);
                break;
            case("PATCH") :
                response = openidm.patch(payload.resourceName, payload.rev, payload.value, payload.params,
                    payload.fields);
                break;
            case("ACTION") :
                response = openidm.action(payload.resourceName, payload.actionName, payload.content, payload.params,
                    payload.fields);
                break;
            case("QUERY") :
                response = openidm.query(payload.resourceName, payload.params, payload.fields);
                break;
            default :
                throw new Error("Invalid CRUDPAQ operation in message payload: " + payload.operation);
        }
        console.log("Message response is... ");
        console.log(JSON.stringify(response, null, 4));
        console.log("**************END MESSAGE*************");
    } catch (error) {
        console.log(error);
        throw error; // Throwing the error will cause the message to remain in the queue.
    }


    /**
     * Adapts the JMS message into a JSON object that maps to CRUDPAQ operations.
     *
     * @param message The received Message Object.
     */
    function adaptMessageToCrudpaqPayload(message) {
        // This impl is expecting a JMS TextMessage where the content is JSON. It is therefore simple to parse the
        // text. Other impl messages might be binary and need more parsing to extract the data to invoke a request.
        return JSON.parse(message.text);  // Expecting a JMS TextMessage.
    }
}());


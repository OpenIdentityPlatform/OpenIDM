/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright © 2011 ForgeRock AS. All rights reserved.
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

/*global $, define*/

/**
 * @author yaromin
 */
define("app/comp/common/eventmanager/EventManager",
        ["app/util/Constants"],
        function(constants) {
    
    /**
     * listenerProxyMap - Association of real listeners and proxies which transforms parameter set
     */
    var obj = {}, listenerProxyMap = [];
    

    obj.sendEvent = function (eventId, event) {
        console.debug("sending event eventId=" + eventId);
        $(document).trigger(eventId, event);
    };

    obj.registerListener = function (eventId, callback) {
        var proxyFunction = function(element, event) {
            console.debug("Handiling event");
            callback(event);
        };
        console.debug("registering event listener eventId=" + eventId);
        listenerProxyMap[callback] = proxyFunction;
        $(document).on(eventId, proxyFunction);
    };

    obj.unregisterListener = function (eventId, callback) {
        var proxyFunction;
        console.debug("unregistering event listener eventId=" + eventId);
        proxyFunction = listenerProxyMap[callback];
        $(document).off(proxyFunction);
    };

    return obj;
});
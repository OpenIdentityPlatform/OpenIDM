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
 * @author mbilski
 */

define("app/comp/common/messages/MessagesCtrl",
        ["app/comp/common/messages/MessagesView"], 
        function (messagesView, eventManager) {
    var obj = {};

    obj.view = messagesView;

    obj.messages = [];
    obj.types = [];
    obj.numberOfMessages = 0;


    obj.displayMessage = function(type, message) {
        console.info('displaing message');

        if( obj.numberOfMessages === 0 ) {
            if (type === 'info') {
                obj.view.addInfoMessage(message, "#messages", obj.messagesLoop);
            } else {
                obj.view.addErrorMessage(message, "#messages", obj.messagesLoop);
            }

            obj.numberOfMessages++;
        } else {
            var i = obj.numberOfMessages;
            obj.numberOfMessages++;

            obj.messages[i] = message;
            obj.types[i] = type;
        }
    };

    obj.messagesLoop = function() {
        obj.numberOfMessages--;

        if( obj.numberOfMessages > 0 ) {
            var i = obj.numberOfMessages;

            if (obj.types[i] === 'info') {
                obj.view.addInfoMessage(obj.messages[i], "#messages", obj.messagesLoop);
            } else {
                obj.view.addErrorMessage(obj.messages[i], "#messages", obj.messagesLoop);
            }			
        }
    };

    obj.displayMessageOn = function(type, message, divid) {
        console.info('displaing message');

        if (type === 'info') {
            obj.view.addInfoMessage(message, divid);
        } else {
            obj.view.addErrorMessage(message, divid);
        }
    };


    return obj;
});


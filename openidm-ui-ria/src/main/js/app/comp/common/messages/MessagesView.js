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

define("app/comp/common/messages/MessagesView",
        [],
        function() {
    var obj = {};

    obj.addErrorMessage = function(msg, divid, callback) {
        var msgData = {
                type : 'errorMessage',
                img : '<img src="images/span_error.png" width="14" height="14" alt="error" align="top" />',
                msg : msg,
                divid : divid
        };

        obj.addMessage(msgData, callback);
    };

    obj.addInfoMessage = function(msg, divid, callback) {
        var msgData = {
                type : 'confirmMessage',
                img: '<img src="images/span_ok.png" width="14" height="14" alt="ok" align="top" />',
                msg : msg,
                divid : divid
        };

        obj.addMessage(msgData, callback);
    };

    obj.addMessage = function(msgData, callback) {
        $(msgData.divid).append("<div class='" + msgData.type + " radious'><span>" + msgData.img + "</span>" + msgData.msg + "</div>");
        $(msgData.divid+" > div:last").fadeIn(500).delay(1000).fadeOut(500, function() {
            if( callback !== undefined ) {
                callback();
            }
        });
    };

    return obj;
});


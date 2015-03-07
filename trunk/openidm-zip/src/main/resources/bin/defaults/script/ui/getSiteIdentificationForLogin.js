/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2011-2012 ForgeRock AS. All rights reserved.
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

/**
 * @author mbilski
 * 
 * This scripts returns site image and pass phrase for specified user.
 * If such user does not exists, it returns random data due to security reasons.
 * 
 * For given non-existing login it always returns the same data
 * 
 * This endpoint expects these parameters:
 * 
 * login userName of user
 * 
 */

if (request.method !== "query") {
    throw { 
        "code" : 403,
        "message" : "Access denied"
    };
}

(function () {
    
    var params = {
            "_queryId": "for-userName",
            "uid": request.additionalParameters.login
        }, 
        passPhrases = [
           "human",
           "letter",
           "bird"
       ],
       res, ret, code;
          
    ret = openidm.query("managed/user", params);
    
    if(ret && ret.result && ret.result[0] && ret.result[0].passPhrase && ret.result[0].siteImage) {
        res = {
            "passPhrase": ret.result[0].passPhrase, 
            "siteImage": ret.result[0].siteImage
        };
    } else {
        code = new java.lang.String(request.additionalParameters.login).hashCode();
        code = java.lang.Math.abs(code);        
    
        ret = openidm.read("config/ui/configuration");
    
        res = {
            "siteImage": ret.configuration.siteImages[code % ret.configuration.siteImages.length],
            "passPhrase": passPhrases[code % passPhrases.length]
        };
    }
    
    return [res];

}());
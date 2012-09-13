/*
 * @license DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright © 2011-2012 ForgeRock AS. All rights reserved.
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

/*global define*/

define("org/forgerock/openidm/ui/common/util/ObjectUtil", [
    "org/forgerock/openidm/ui/common/util/DateUtil"                                                           
], function (dateUtil) {
    var obj = {};

    obj.copyObject = function (o) {
        var result, oneAttribute;
        if (!o) { 
            return null;
        }

        if (o instanceof Date) {
            result = dateUtil.currentDate();
            result.setTime(o.getTime());
            return result;
        }

        if (o instanceof Array) {
            result = [];
//          for (var i = 0, var len = o.length; i < len; ++i) {
//          result[i] = obj.copyObject(o[i]);
//          }
            return result;
        }

        if (o instanceof Object) {
            result = {};
            for (oneAttribute in o) {
                if (o.hasOwnProperty(oneAttribute)) {
                    obj.copyObject[oneAttribute] = obj.copyObject(o[oneAttribute]);
                }
            }
            return result;
        }

        throw new Error("Can't copy the object");
    };

    obj.patchObject = function (original, patch, mode) {
        var oneAttribute, result = {};
        for (oneAttribute in patch) {
            if (!(original.hasOwnProperty(oneAttribute) && original[oneAttribute] instanceof Array && patch[oneAttribute] instanceof Array)) {
                original[oneAttribute] = patch[oneAttribute];
            } //else {
//              for (var i = 0, var len = patch[oneAttribute].length; i < len; ++i) {
//              original[oneAttribute][i] = obj.copyObject(o[i]);
//              }                
            //}
        }
        return result;
    };

    return obj;
});

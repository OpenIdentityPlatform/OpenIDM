/** 
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2014 ForgeRock AS. All rights reserved.
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

/*
 * This script merges an attribute value with the target. Returns an object
 * containing the new value for the attribute and (optionally) an updated
 * attributesInfo object.
 * 
 * The following variables are supplied: 
 *   targetObject, sourceObject, existingTargetObject, attributeName, attributeValue, attributesInfo
 */

function mergeValues(target, name, value) {
    if (target[name] != null && target[name] instanceof Array) {
        for (var x = 0; x < value.length; x++) {
            if (target[name].indexOf(value[x]) == -1) {
                target[name].push(value[x]);
            }
        }
    } else if (target[name] != null && target[name] instanceof Object) {
        var obj = target[name];
        for (var key in value) {
            obj[key] = value[key];
        }
    } else {
        target[name] = value;
    }
}

if (existingTargetObject != null && existingTargetObject[attributeName] !== null) {
    mergeValues(targetObject, attributeName, existingTargetObject[attributeName]);
}

mergeValues(targetObject, attributeName, attributeValue);

//Return the result object
var result = {
    "value" : targetObject[attributeName]
};

result;
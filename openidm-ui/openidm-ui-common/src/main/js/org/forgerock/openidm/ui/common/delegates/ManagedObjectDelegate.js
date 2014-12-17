/**
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2011-2014 ForgeRock AS. All rights reserved.
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

/*global $, define, _ */

/**
 * @author huck.elliott
 */
define("org/forgerock/openidm/ui/common/delegates/ManagedObjectDelegate", [
    "org/forgerock/commons/ui/common/util/Constants",
    "org/forgerock/commons/ui/common/main/AbstractDelegate",
    "org/forgerock/openidm/ui/common/delegates/ConfigDelegate"
], function(constants, AbstractDelegate, configDelegate) {

    var obj = new AbstractDelegate(constants.host + "/openidm/managed");
    
    obj.getSchema = function(objectName){
        //overriding this value here so it will not have to be changed
        //on calls to createEntity, patchEntity, deleteEntity
        obj.serviceUrl = "/" + constants.context + "/managed/" + objectName;
        
        return configDelegate.readEntity("managed").then(function(managed){
            var managedObject = _.findWhere(managed.objects,{ name: objectName });
            
            if(managedObject){
                if(managedObject.schema){
                    return managedObject.schema;
                } else {
                    return false;
                }
            } else {
                return "invalidObject";
            }
        });
    };
    
    return obj;
});




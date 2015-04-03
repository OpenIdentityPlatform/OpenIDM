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

/*global define  _ */

/**
 * @author huck.elliott
 */
define("org/forgerock/openidm/ui/admin/ManagedObjectNavigation", [
    "org/forgerock/commons/ui/common/components/Navigation",
    "org/forgerock/openidm/ui/common/delegates/ConfigDelegate"
], function(nav, configDelegate) {
    var obj = {};
    
    obj.build = function(){

        nav.addUserBarLink(
            {
                "id": "admin",
                "href": "../admin",
                "i18nKey": "openidm.admin.label"
            }, 
            'top'
        );

        return configDelegate.readEntity("managed").then(function(managed){
            _.chain(managed.objects)
                .reject(function(o){ return !o.schema || _.isEmpty(o.schema.properties) || o.name === "user" || o.name === "role";  })
                .each(function(o){
                    nav.configuration.links.admin.urls[o.name] = {
                        "url": "#resource/managed/" + o.name + "/list/",
                        "name": o.schema.title || o.name,
                        "icon": "glyph-icon-star-empty",
                        "inactive": false
                            
                    };
                });
            nav.reload();
        });
    };
    
    return obj;
});



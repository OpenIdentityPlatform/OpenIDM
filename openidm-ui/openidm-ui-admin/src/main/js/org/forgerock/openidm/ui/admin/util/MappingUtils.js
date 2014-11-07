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

/*global $, define, _, Handlebars */

/**
 * @author huck.elliott
 */
define("org/forgerock/openidm/ui/admin/util/MappingUtils", [
    "org/forgerock/openidm/ui/admin/delegates/BrowserStorageDelegate"
],
  function(browserStorageDelegate) {

    var obj = {};
    
    obj.numRepresentativeProps = function(mapping){
        var defaultNumProps = 4;
        
        return browserStorageDelegate.get(mapping + "_numRepresentativeProps",true) || defaultNumProps;
    };

    obj.buildObjectRepresentation = function(objToRep, props){
        var propVals = [];
        
        _.each(props, _.bind(function(prop, i){
            var txt,
                objRepEl = $("<span>"),
                wrapper = $("<div>");
            if(objToRep[prop]){
                objRepEl.text(Handlebars.Utils.escapeExpression(objToRep[prop])).attr("title", prop);
            }
            if(i === 0){
                objRepEl.addClass("objectRepresentationHeader");
            } else {
                objRepEl.addClass("objectRepresentation");
            }
            wrapper.append(objRepEl);
            propVals.push(wrapper.html());
        }, this));
        
        return propVals.join("<br/>");
    };

    return obj;
});

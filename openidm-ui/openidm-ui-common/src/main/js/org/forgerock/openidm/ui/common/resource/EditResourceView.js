/**
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2011-2015 ForgeRock AS. All rights reserved.
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

/*global define, $, _, JSONEditor */

/**
 * @author huck.elliott
 */
define("org/forgerock/openidm/ui/common/resource/EditResourceView", [
    "org/forgerock/commons/ui/common/main/AbstractView",
    "org/forgerock/openidm/ui/common/resource/ResourceEditViewRegistry"
    
], function(AbstractView, ResourceEditViewRegistry) {
    var EditResourceView = AbstractView.extend({
        events: {},
        render: function(args, callback) {
            var view,
                resource = args[1];
            
            if (args[0] === "system") {
                resource += "/" + args[2];
            }
            
            view = ResourceEditViewRegistry.getEditViewModule(resource);
            
            view.render(args, callback);
        }
    }); 
    
    return new EditResourceView();
});



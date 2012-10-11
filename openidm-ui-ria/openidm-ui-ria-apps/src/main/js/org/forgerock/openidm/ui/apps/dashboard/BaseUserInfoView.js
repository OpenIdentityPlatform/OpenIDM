/**
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

/*global define, $ */

/**
 * @author jdabrowski
 */
define("org/forgerock/openidm/ui/apps/dashboard/BaseUserInfoView", [
    "org/forgerock/commons/ui/common/main/AbstractView",
    "org/forgerock/commons/ui/common/main/Configuration"
], function(AbstractView, conf) {
    var BaseUserInfoView = AbstractView.extend({
        template: "templates/apps/dashboard/BaseUserInfoViewTemplate.html",
        element: "#profileInfo",
        noBaseTemplate: true,
        
        render: function() {
            this.parentRender(function() {                
                if (conf.loggedUser) {
                    this.$el.find("label[name=userName]").text(conf.loggedUser.givenName + " " + conf.loggedUser.familyName);
                    this.$el.find("label[name=position]").text(conf.loggedUser.email);
                    this.$el.find("label[name=skype]").text(conf.loggedUser.email);
                    this.$el.find("label[name=google]").text(conf.loggedUser.email);
                }
            });
        }
    
    }); 
    
    return new BaseUserInfoView();
});
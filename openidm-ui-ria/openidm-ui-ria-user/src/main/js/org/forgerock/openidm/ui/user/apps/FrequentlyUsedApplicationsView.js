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

/*global define, _, $ */

/**
 * @author jdabrowski
 */
define("org/forgerock/openidm/ui/user/apps/FrequentlyUsedApplicationsView", [
    "org/forgerock/openidm/ui/user/BaseApplicationsView",
    "org/forgerock/openidm/ui/user/delegates/UserDelegate",
    "org/forgerock/openidm/ui/common/main/Configuration",
    "org/forgerock/openidm/ui/common/main/EventManager",
    "org/forgerock/openidm/ui/common/util/Constants"
], function(BaseApplicationsView, userDelegate, conf, eventManager, constants) {
    
    var FrequentlyUsedApplicationsView = BaseApplicationsView.extend({
        
        installAdditionalFunctions: function() {
        },
        
        noItemsMessage: function(item) {
            return "Your have no frequently used apps. Click <a href='#applications/addmore/'> here<a/> to add.";
        },
        
        isLinkValidToShow: function(itemLink) {
            if (itemLink.state === constants.USER_APPLICATION_STATE_APPROVED) {
                return true;
            } else {
                return false;
            }
        },
        
        sort: function() {
            this.items.sort( function(app1, app2) {
                if (app1.lastTimeUsed < app2.lastTimeUsed) {
                    return 1;
                }
                if (app1.lastTimeUsed > app2.lastTimeUsed){
                    return -1;
                }
                return 0;
            });
        }
        
    });
    
    return FrequentlyUsedApplicationsView;
});
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

/*global define, _, $ */

/**
 * @author jdabrowski
 */
define("org/forgerock/openidm/ui/apps/BaseApplicationsView", [
    "org/forgerock/commons/ui/common/components/GridTableView",
    "org/forgerock/openidm/ui/apps/delegates/ApplicationDelegate",
    "org/forgerock/commons/ui/user/delegates/UserDelegate",
    "org/forgerock/commons/ui/common/main/Configuration",
    "org/forgerock/commons/ui/common/main/UniversalCachedDelegate",
    "org/forgerock/commons/ui/common/util/UIUtils"
], function(GridTableView, applicationDelegate, userDelegate, conf, universalCachedDelegate, uiUtils) {
    
    var BaseApplicationsView = GridTableView.extend({
        
        events: {            
        },
        
        generateItemView: function(itemObject) {
            var privilagesClass = '', item, data, tpl;
            
            if (itemObject.applicationId) {
                item = applicationDelegate.getApplicationDetails(itemObject.applicationId);
            } else {
                item = itemObject;
            }

            if (this.shouldApplicationBeVisible(item) && (!itemObject.applicationId || this.isLinkValidToShow(itemObject)) ) {
                data = {
                        closable: this.closable(),
                        privilagesClass: !this.hideApprovalInfo() && item.needsApproval ? 'ui-state-needsApproval' : '',
                        item: item,
                        itemObject: itemObject,
                        clickable: this.isClickable && itemObject.state === 'B65FA6A2-D43D-49CB-BEA0-CE98E275A8CD'
                };
                
                tpl = uiUtils.fillTemplateWithData("templates/apps/application.html", data);
                
                return tpl;                
            } else { 
                return '';
            }
            
        },
        
        hideApprovalInfo: function() {
            return true;
        },
        
        getItems: function() {
            var optionTexts = [], value;
            this.$el.find("ul li").each(function() { 
                value = $(this).find("input").val();
                if (value) {
                    optionTexts.push(value);
                }
            });
            return optionTexts;
        },
        
        orderChanged: function() {
        },
        
        closable: function() {
            return false;
        },
        
        noItemsMessage: function(item) {
            return "Your have no apps here";
        },
        
        appExists : function(appId) {
            var i;
            for (i = 0; i < this.items.length; i++) {
                if (this.items[i].applicationId === appId) {
                    return true;
                }
            }
            return false;
        },
        
        shouldApplicationBeVisible: function(item) {
            return true;
        },
        
        isLinkValidToShow: function(item) {
            return true;
        }
        
    
    });
    
    return BaseApplicationsView;
});
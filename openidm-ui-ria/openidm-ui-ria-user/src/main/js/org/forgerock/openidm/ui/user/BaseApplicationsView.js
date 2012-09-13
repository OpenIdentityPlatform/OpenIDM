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
define("org/forgerock/openidm/ui/user/BaseApplicationsView", [
    "org/forgerock/openidm/ui/common/components/GridTableView",
    "org/forgerock/openidm/ui/user/delegates/ApplicationDelegate",
    "org/forgerock/openidm/ui/user/delegates/UserDelegate",
    "org/forgerock/openidm/ui/common/main/Configuration",
    "org/forgerock/openidm/ui/common/main/UniversalCachedDelegate"
], function(GridTableView, applicationDelegate, userDelegate, conf, universalCachedDelegate) {
    
    var BaseApplicationsView = GridTableView.extend({
        
        generateItemView: function(itemObject) {
            var privilagesClass = '', item, closableTag, stateClass = '', lnkIdTag = '', stateTag = '';
            
            if (itemObject.applicationId) {
                item = applicationDelegate.getApplicationDetails(itemObject.applicationId);
                lnkIdTag = '<input type="hidden" name="lnkId" value='+itemObject._id+' />';
                stateTag = '<input type="hidden" name="state" value='+itemObject.state+' />';
            } else {
                item = itemObject;
            }
            
            if (this.shouldApplicationBeVisible(item) && (!itemObject.applicationId || this.isLinkValidToShow(itemObject)) ) {
                closableTag = this.closable() ? '<div class="ui-state-close">x</div>' : '';
                privilagesClass = !this.hideApprovalInfo() && item.needsApproval ? 'ui-state-needsApproval' : '';
                return '<li class="ui-state-default ' + privilagesClass + ' ' +  stateClass + '">'
                          + '<a href="#" class="ui-item-href">'
                              + closableTag
                              + '<div class="ui-state-item gradientLinearGray" >' 
                                  + '<img src="'+item.iconPath+'"/>'
                                  + '<span class="link">'+item.description+'</span>'
                                  + '<input type="hidden" name="id" value='+item._id+' />'
                                  + stateTag
                                  + lnkIdTag
                              + '</div>'
                          + '</a>'
                     + '</li>';
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
        
        onClick: function(event) {
            event.preventDefault();
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
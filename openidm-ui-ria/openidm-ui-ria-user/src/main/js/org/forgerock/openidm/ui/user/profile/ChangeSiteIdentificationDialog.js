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

/*global define, $, _, ContentFlow */

/**
 * @author mbilski
 */
define("org/forgerock/openidm/ui/user/profile/ChangeSiteIdentificationDialog", [
    "org/forgerock/openidm/ui/common/components/Dialog",
    "org/forgerock/openidm/ui/common/main/ValidatorsManager",
    "org/forgerock/openidm/ui/common/main/Configuration",
    "org/forgerock/openidm/ui/common/main/Router",
    "org/forgerock/openidm/ui/user/delegates/UserDelegate",
    "org/forgerock/openidm/ui/common/main/EventManager",
    "org/forgerock/openidm/ui/common/util/Constants"
], function(Dialog, validatorsManager, conf, router, userDelegate, eventManager, constants) {
    var ChangeSiteIdentificationDialog = Dialog.extend({    
        contentTemplate: "templates/user/ChangeSiteIdentificationDialogTemplate.html",
        
        data: {         
            width: 800,
            height: 350
        },
        
        siteImageFlow:{},
        
        events: {
            "click input[type=submit]": "formSubmit",
            "click .dialogCloseCross img": "close",
            "click input[name='close']": "close",
            "click": "close",
            "onValidate": "onValidate",
            "click .dialogContainer": "stop"
        },
        
        formSubmit: function(event) {
            event.preventDefault();
            
            if(validatorsManager.formValidated(this.$el)) {            
                var self = this, patchDefinition = [{replace: "siteImage", value: this.$el.find("input[name='siteImage']").val()}, {replace: "passPhrase", value: this.$el.find("input[name=passPhrase]").val()}];
    
                userDelegate.patchSelectedUserAttributes(conf.loggedUser.userName,  patchDefinition,
                        function(r) {
                    eventManager.sendEvent(constants.EVENT_DISPLAY_MESSAGE_REQUEST, "siteIdentificationChanged");
                    
                    //updating in profile
                    conf.loggedUser.siteImage = self.$el.find("input[name='siteImage']").val();
                    conf.loggedUser.passPhrase = self.$el.find("input[name=passPhrase]").val();
                }, function(r) {
                    eventManager.sendEvent(constants.EVENT_DISPLAY_MESSAGE_REQUEST, "unknown");
                });
                
                this.close();            
            }
        },
        
        render: function() {
            this.actions = {};
            this.addAction("Save", "submit");
            this.show(_.bind(function() {
                validatorsManager.bindValidators(this.$el); 

                this.siteImageCounter = 0;
                $("#siteImageFlow img").load(_.bind(this.refreshFlow, this));
                
                this.$el.find("input[name=oldSiteImage]").val(conf.loggedUser.siteImage);                
                this.$el.find("input[name=passPhrase]").val(conf.loggedUser.passPhrase);
                this.$el.find("input[name=oldPassPhrase]").val(conf.loggedUser.passPhrase);
                
                validatorsManager.validateAllFields(this.$el);
            }, this));            
        },
        
        refreshFlow: function() {
            this.siteImageCounter++;
            
            if( this.siteImageCounter === $("#siteImageFlow img").length ) {
                console.log("Refreshing flow");
                
                this.siteImageFlow = new ContentFlow('siteImageFlow', { reflectionHeight: 0, circularFlow: false } );
                this.siteImageFlow._init();
                this.siteImageCounter = 0;

                this.siteImageFlow.conf.onclickActiveItem = function(){};
                this.siteImageFlow.conf.onMoveTo = _.bind(this.setImage, this);
                
                this.siteImageFlow.moveTo($("#siteImageFlow").find("[data-site-image='"+conf.loggedUser.siteImage+"']").parent().index());
                $("#siteImageFlow img").show();
            }  
        },
        
        setImage: function(item) {
            this.siteImage = $(item.element).children().attr("data-site-image");
            this.$el.find("input[name='siteImage']").val(this.siteImage);
            this.$el.find("input[name='siteImage']").trigger("change");
        }
    }); 
    
    return new ChangeSiteIdentificationDialog();
});
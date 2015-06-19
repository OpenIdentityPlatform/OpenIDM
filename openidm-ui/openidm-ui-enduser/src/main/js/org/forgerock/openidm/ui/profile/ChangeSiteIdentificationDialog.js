/**
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2011-2013 ForgeRock AS. All rights reserved.
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
define("org/forgerock/openidm/ui/profile/ChangeSiteIdentificationDialog", [
    "org/forgerock/commons/ui/common/components/Dialog",
    "org/forgerock/commons/ui/common/main/ValidatorsManager",
    "org/forgerock/commons/ui/common/main/Configuration",
    "org/forgerock/commons/ui/common/main/Router",
    "UserDelegate",
    "AuthnDelegate",
    "org/forgerock/commons/ui/common/main/EventManager",
    "org/forgerock/commons/ui/common/util/Constants"
], function(Dialog, validatorsManager, conf, router, userDelegate, authnDelegate, eventManager, constants) {
    var ChangeSiteIdentificationDialog = Dialog.extend({    
        contentTemplate: "templates/profile/ChangeSiteIdentificationDialogTemplate.html",
        
        data: {         
            width: 800,
            height: 350
        },
        
        siteImageFlow:{},
        
        events: {
            "click input[type=submit]": "formSubmit",
            "click .dialogCloseCross img": "close",
            "click input[name='close']": "close",
            "onValidate": "onValidate",
            "click .modal-content": "stop"
        },
        
        formSubmit: function(event) {
            event.preventDefault();
            event.stopPropagation();
            
            if(validatorsManager.formValidated(this.$el)) {
                var self = this, 
                    patchDefinition = [
                        {"operation": "replace", "field": "/siteImage", "value": this.$el.find("input[name='siteImage']").val()}, 
                        {"operation": "replace", "field": "/passPhrase", "value": this.$el.find("input[name=passPhrase]").val()}
                    ];
                
                userDelegate.patchSelectedUserAttributes(conf.loggedUser._id, conf.loggedUser._rev,  patchDefinition, _.bind(function(r) {
                    eventManager.sendEvent(constants.EVENT_DISPLAY_MESSAGE_REQUEST, "siteIdentificationChanged");
                    
                    //updating in profile
                    conf.loggedUser.siteImage = self.$el.find("input[name='siteImage']").val();
                    conf.loggedUser.passPhrase = self.$el.find("input[name=passPhrase]").val();
                    this.close();

                    return authnDelegate.getProfile()
                        .then(function(user) {
                            conf.loggedUser = user;
                            return user;
                        });
                    
                }, this));
            }
        },
        
        render: function() {
            this.actions = [];
            this.addAction($.t("common.form.save"), "submit");
            this.addTitle($.t("common.user.siteImage"));

            this.show(_.bind(function() {
                this.siteImageCounter = 0;
                $("#siteImageFlow img").load(_.bind(this.refreshFlow, this));
                validatorsManager.bindValidators(this.$el, userDelegate.baseEntity + "/" + conf.loggedUser._id, _.bind(function () {
                    
                    this.$el.find("input[name=oldSiteImage]").val(conf.loggedUser.siteImage);                
                    this.$el.find("input[name=passPhrase]").val(conf.loggedUser.passPhrase);
                    this.$el.find("input[name=oldPassPhrase]").val(conf.loggedUser.passPhrase);
                    
                    validatorsManager.validateAllFields(this.$el);
                }, this)); 
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

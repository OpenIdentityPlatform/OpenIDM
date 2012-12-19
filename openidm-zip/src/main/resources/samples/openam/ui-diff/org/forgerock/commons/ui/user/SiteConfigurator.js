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

/*global $, define, require */

/**
* @author mbilski
*/
define("org/forgerock/commons/ui/user/SiteConfigurator", [
   "org/forgerock/commons/ui/common/main/AbstractConfigurationAware",
   "org/forgerock/commons/ui/common/util/Constants", 
   "org/forgerock/commons/ui/common/main/EventManager",
   "org/forgerock/commons/ui/common/main/Configuration",
   "org/forgerock/commons/ui/user/delegates/SiteConfigurationDelegate",
   "org/forgerock/commons/ui/common/main/i18nManager",
   "org/forgerock/openidm/ui/admin/notifications/NotificationViewHelper"
], function(AbstractConfigurationAware, constants, eventManager, conf, configurationDelegate, i18nManager, notificationViewHelper) {
   var obj = new AbstractConfigurationAware();
   
   obj.initialized = false;
   
   $(document).on(constants.EVENT_READ_CONFIGURATION_REQUEST, function() {
       
       if(!conf.globalData) {
           conf.setProperty('globalData', {});
       }

       console.log("READING CONFIGURATION");
       if(obj.configuration && obj.initialized === false) {
           obj.initialized = true;
           
           i18nManager.setLanguage(constants.DEFAULT_LANGUAGE);
           
           if(obj.configuration.remoteConfig === true) {
               configurationDelegate.getConfiguration(function(config) {
                   obj.processConfiguration(config); 
                   eventManager.sendEvent(constants.EVENT_APP_INTIALIZED);
               }, function() {
                   eventManager.sendEvent(constants.EVENT_APP_INTIALIZED);                   
               });
           } else {
               obj.processConfiguration(obj.configuration); 
               eventManager.sendEvent(constants.EVENT_APP_INTIALIZED);
           }          
       }
   });
   
   obj.processConfiguration = function(config) {
       var router, changeSecurityDataDialog;
       
       router = require("org/forgerock/commons/ui/common/main/Router");
      
       if (config.hasOwnProperty('openamBaseURL')) {
           conf.globalData.openamBaseURL = config.openamBaseURL;
       } 

       if(config.selfRegistration === true) {
           conf.globalData.selfRegistration = true;
       } else {               
           if(router.configuration && router.configuration.routes.register) {
               console.log("Removing register route.");
               delete router.configuration.routes.register;
           }
       }
       
       if(config.securityQuestions === true) {
           conf.globalData.securityQuestions = true;             
       } else {
           changeSecurityDataDialog = require("org/forgerock/commons/ui/user/profile/ChangeSecurityDataDialog");
           changeSecurityDataDialog.data.height = 260;
       }
       
       
       if(config.securityQuestions === true) {
           conf.globalData.securityQuestions = true;             
       } else {
           if(router.configuration && router.configuration.routes.forgottenPassword) {
               console.log("Removing forgottenPassword route.");
               delete router.configuration.routes.forgottenPassword;
           } 
       }
       
       if(config.siteIdentification === true) {
           conf.globalData.siteIdentification = true;
       } else {
           if(router.configuration && router.configuration.routes.siteIdentification) {
               console.log("Removing siteIdentification route.");
               delete router.configuration.routes.siteIdentification;
           }
       }
       
       if(config.siteImages) {
           conf.globalData.siteImages = config.siteImages;
       }
       
       if (config.language) {
           i18nManager.setLanguage(config.language);
       } else {
           i18nManager.setLanguage(constants.DEFAULT_LANGUAGE);
       }
       
       if(config.roles) {
           conf.globalData.userRoles = config.roles;
       }
       
       if(config.notificationTypes) {
           notificationViewHelper.notificationTypes = config.notificationTypes;
       }
       
       if(config.defaultNotificationType) {
           notificationViewHelper.defaultType = config.defaultNotificationType;
       }
       
   };
   
   return obj;
});

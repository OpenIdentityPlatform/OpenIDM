/*
 * @license DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright © 2011-2012 ForgeRock AS. All rights reserved.
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

/*global define, Backbone, _, $, window */

/**
 * @author mbilski
 */
define("org/forgerock/openidm/ui/common/main/AbstractView", [
    "org/forgerock/openidm/ui/common/util/UIUtils",
    "org/forgerock/openidm/ui/common/main/ValidatorsManager",
    "org/forgerock/openidm/ui/common/util/ValidatorsUtils",
    "org/forgerock/openidm/ui/common/main/Configuration",
    "org/forgerock/openidm/ui/common/main/EventManager", 
    "org/forgerock/openidm/ui/common/util/Constants"
], function(uiUtils, validatorsManager, validatorsUtils, conf, eventManager, constants) {
    var View = Backbone.View.extend({

        /**
         * This params should be passed when creating new object, for example:
         * new View({el: "#someId", template: "templates/main.html"});
         */
        element: "#content",
        
        baseTemplate: "templates/user/DefaultBaseTemplate.html",
        
        /**
         * View mode: replace or append
         */
        mode: "replace",

        /**
         * Change content of 'el' element with 'viewTpl', 
         * which is compiled using 'data' attributes.
         */
        parentRender: function(callback) {   
            this.callback = callback;
            
            if(conf.baseTemplate !== this.baseTemplate && !this.noBaseTemplate) {
                uiUtils.renderTemplate(this.baseTemplate, $("#wrapper"), this.data, _.bind(this.loadTemplate, this), "replace");
            } else {
                this.loadTemplate();
            }
            
            $(window).scrollTop(0);
        },
        
        loadTemplate: function() {
            this.setElement($(this.element));
            this.$el.unbind();
            this.delegateEvents();
            
            if(conf.baseTemplate !== this.baseTemplate && !this.noBaseTemplate) {
                conf.setProperty("baseTemplate", this.baseTemplate);
                eventManager.sendEvent(constants.EVENT_CHANGE_BASE_VIEW);
            }
            
            if(this.callback) {
                uiUtils.renderTemplate(this.template, this.$el, this.data, _.bind(this.callback, this), this.mode);
            } else {
                uiUtils.renderTemplate(this.template, this.$el, this.data, null, this.mode);
            }
        },
        
        rebind: function() {
            this.setElement($(this.element));
            this.$el.unbind();
            this.delegateEvents();
        },
        
        render: function(args, callback) {
            this.parentRender(callback);
        },
        
        reload: function() {},
        
        /**
         * Perform only view changes: displays tick, message and
         * change color of submit button.
         */
        onValidate: function(event, input, msg, validatorType) {           
            if(msg === "inProgress") {
                //TODO spinner
                //console.log("in progress..");
                return;
            }
            
            if(validatorsManager.formValidated(this.$el)) {
                this.$el.find("input[type=submit]").removeClass('gray').addClass('orange');
                this.$el.find(".inputValidationMessage").hide();
            } else {
                this.$el.find("input[type=submit]").removeClass('orange').addClass('gray');
                this.$el.find(".inputValidationMessage").show();
            }
            
            if(msg === "disabled") {
                validatorsUtils.hideValidation(input);
                return;
            } else {
                validatorsUtils.showValidation(input);
            }
            
            if($(input).nextAll("span")) {
                validatorsUtils.setTick(input, msg ? true : false);
            }
            
            if($(input).nextAll("div.validationMessage:first")) {
                $(input).nextAll("div.validationMessage:first").html(msg ? msg : '');
            }
            
            if(validatorType) {
                validatorsUtils.setErrors(this.$el, validatorType, msg);
            }    
        }
    });

    return View;
});
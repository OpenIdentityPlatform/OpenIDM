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

/*global define, $, form2js, _, ContentFlow */

/**
 * @author mbilski
 */
define("org/forgerock/openidm/ui/admin/users/AdminUserRegistrationView", [
    "org/forgerock/commons/ui/common/main/AbstractView",
    "org/forgerock/commons/ui/common/main/ValidatorsManager",
    "org/forgerock/commons/ui/common/util/UIUtils",
    "UserDelegate",
    "org/forgerock/commons/ui/common/main/EventManager",
    "org/forgerock/commons/ui/common/util/Constants",
    "org/forgerock/commons/ui/common/main/Configuration",
    "org/forgerock/commons/ui/common/main/Router",
    "org/forgerock/openidm/ui/user/delegates/RoleDelegate"
], function(AbstractView, validatorsManager, uiUtils, userDelegate, eventManager, constants, conf, router, roleDelegate) {
    var AdminUserRegistrationView = AbstractView.extend({
        template: "templates/admin/AdminUserRegistrationTemplate.html",
        delegate: userDelegate,
        events: {
            "click input[type=submit]": "formSubmit",
            "onValidate": "onValidate",
            "click input[name=backButton]": "back"
        },
        
        formSubmit: function(event) {
            event.preventDefault();
            var _this = this, data = form2js(this.$el.attr("id")), element;
            if(validatorsManager.formValidated(this.$el) && !this.isFormLocked()) {
                this.lock();
                
                data.roles = this.$el.find("input[name=roles]:checked").map(function(){return $(this).val();}).get();
                delete data.terms;
                delete data.passwordConfirm;
                
                this.delegate.createEntity(null, data, function(user) {
                    eventManager.sendEvent(constants.EVENT_USER_SUCCESSFULLY_REGISTERED, { user: data, autoLogin: false });
                    router.navigate(router.configuration.routes.adminUsers.url, {trigger: true});
                }, function() {
                    _this.unlock();
                });
            }
        },
        
        render: function() {
            roleDelegate.getAllRoles().then(_.bind(function (roles) {

                var managedRoleMap = _.chain(roles.result)
                                      .map(function (r) { return [r._id, r.properties.name || r._id]; })
                                      .object()
                                      .value();

                this.data.roles = _.extend({}, conf.globalData.userRoles, managedRoleMap);
                
                this.parentRender(function() {
                    validatorsManager.bindValidators(this.$el, this.delegate.baseEntity + "/*", _.bind(function () {
                        this.$el.find(':input[name=roles][value=openidm-authorized]').prop("checked", true);
                        validatorsManager.validateAllFields(this.$el);
                        this.unlock();
                    }, this));
                });

            }, this));
        },
        
        back: function() {
            router.routeTo(router.configuration.routes.adminUsers, {trigger: true});
        }    
    }); 
    
    return new AdminUserRegistrationView();
});



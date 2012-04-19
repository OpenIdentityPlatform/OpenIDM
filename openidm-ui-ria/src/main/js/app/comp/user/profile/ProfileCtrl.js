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

/*global $, define, require*/

define("app/comp/user/profile/ProfileCtrl",[ 
                                            "app/comp/user/profile/ProfileView", 
                                            "app/comp/user/delegates/UserDelegate",
                                            "app/comp/common/eventmanager/EventManager", 
                                            "app/comp/common/dialog/ConfirmationDialogCtrl", 
                                            "app/comp/user/changesecurityinfo/SecurityDialogCtrl",
                                            "app/comp/user/changepassword/ChangePasswordDialogCtrl",
                                            "app/comp/user/securitypicture/SelectPictureDialogCtrl",
                                            "app/util/Validators",
                                            "app/util/Constants",
                                            "app/comp/main/Configuration",
                                            "app/util/Validator",
                                            "app/util/Condition"],
                                            function(profileView, userDelegate, eventManager, confirmationDialogCtrl, securityDialogCtrl, changePasswordDialogCtrl, selectPictureDialogCtrl, validators, constants, globalConfiguration, Validator, Condition)  {

    var obj = {};

    obj.view = profileView;
    obj.delegate = userDelegate;

    obj.validators = [];

    obj.user = null;

    obj.setUser = function(u) {
        obj.user = u;
        obj.reloadUser();
        obj.validate();
    };

    obj.reloadUser = function() {

        obj.view.setUserName(obj.user.firstname + " " + obj.user.lastname);
        obj.view.getEmailInput().val(obj.user.email);

        obj.view.getFirstNameInput().val(obj.user.firstname);
        obj.view.getLastNameInput().val(obj.user.lastname);

        obj.view.getAddress1Input().val(obj.user.address1);
        obj.view.getAddress2Input().val(obj.user.address2);

        obj.view.getCityInput().val(obj.user.city);
        obj.view.getPostalCodeInput().val(obj.user.postalcode);
        obj.view.getPhoneNumberInput().val(obj.user.phonenumber);

        obj.setCountryAndStateProvince(obj.user.country, obj.user.state_province);
        obj.setSecurityQuestionSelect(obj.user.securityquestion);

        obj.view.getSecurityQuestion().val(obj.user.securityquestion);
        obj.view.getSecurityAnswer().val(obj.user.securityanswer);

        if( obj.mode === constants.MODE_ADMIN ) {
            //obj.view.getPasswordAttemptsInput().val(obj.user.passwordAttempts);
            var curr_date, curr_month, curr_year, d = new Date(obj.user.lastPasswordSet);
            curr_date = d.getDate();
            curr_month = d.getMonth();
            curr_year = d.getFullYear();

            obj.view.getLastPasswordSetInput().val(curr_year + "/" + curr_month + "/" + curr_date);
            obj.view.getAccountStatusInput().val(obj.user.accountStatus);
        } else {
            require("app/comp/user/login/LoginCtrl").setUserName(obj.user.email);
        }

        obj.view.getUserProfileHeadingLabel().html(obj.user.firstname+" "+obj.user.lastname+"'s Profile");
    };

    obj.getUser = function() {
        return obj.user;
    };

    obj.init = function(mode, profileName, user, callback) {
        console.log("ProfileCtrl.init()");
        if(!profileName) {
            profileName = 'My profile'; 
        }
        obj.mode = (mode) ? mode : constants.MODE_USER;

        eventManager.sendEvent(constants.EVENT_PROFILE_INITIALIZATION, { profileName: profileName});

        obj.view.show(function() {
            obj.registerListeners();

            if( obj.mode === 'user' ) {
                $("#securityDialogLink").on(
                        'click',
                        function(event) {
                            if( obj.mode === constants.MODE_USER ) {
                                securityDialogCtrl.init(obj.getUser().userName, constants.MODE_USER, obj);
                            } else {
                                securityDialogCtrl.init(obj.getUser().userName, constants.MODE_ADMIN, obj);
                            }
                        });
            } else {			
                $("#passwordChangeLink").bind('click', function(event) {
                    changePasswordDialogCtrl.init(obj.getUser().userName);
                });
            }

            obj.setUser((user) ? user : globalConfiguration.loggedUser);

            obj.registerValidators();
            obj.validate();

            $("#authMethodLink").off().on('click', function(){ obj.authMethodDialog(); });
            $("#passphraseLink").off().on('click', function(){ obj.passphraseDialog(); });

            if(callback) {
                callback();
            }
        }, mode);				

    };

    obj.authMethodDialog = function() {
        confirmationDialogCtrl.init("Select adaptive auth method", "<form><div style='float: left;'><input type='radio' name='auth' value='SMS' checked='checked'/>SMS</div><div style='float: left;'><input type='radio' name='auth' value='OAuth' disabled='disabled'/>OAuth</div></form>", "Update", function() {

        }, 400, 70);

    };

    obj.passphraseDialog = function() {
        selectPictureDialogCtrl.init(obj);
    };

    obj.registerListeners = function() {
        var self = this;

        console.log("ProfileCtrl.registerListeners()");

        obj.view.getSaveButton().off().on('click', function(event) {
            event.preventDefault();
            obj.saveUser();
        });

        obj.view.getCountryInput().change(obj.adjustStateProvinceDropdown).change();
        obj.view.getStateProvinceInput().change(function(event){
            if(obj.view.getStateProvinceInput().val()!==""){
                obj.view.getFirstStateProvinceOption().text("");
            }else{
                obj.view.getFirstStateProvinceOption().text("Please Select");
            }
        });

        if( obj.mode === constants.MODE_ADMIN) {
            obj.view.getDeleteButton().on('click', function(event) {
                event.preventDefault();
                confirmationDialogCtrl.init("Delete user", obj.user.email + " account will be deleted.", "Delete", function() {
                    self.deleteUser();
                });
            });

            obj.view.getBackButton().on('click', function(event) {
                event.preventDefault();
                eventManager.sendEvent(constants.EVENT_GO_BACK_REQUEST);
            });
        }
    };

    obj.registerValidators = function() {
        var self = this;

        obj.validators[0] = new Validator([ obj.view
                                                    .getFirstNameInput() ], [ new Condition('letters-only',
                                                            validators.nameValidator) ], 'keyup', 'simple',
                                                            self.validateForm);
        obj.validators[1] = new Validator(
                [ obj.view.getLastNameInput() ], [ new Condition(
                        'letters-only', validators.lastnameValidator) ],
                        'keyup', 'simple', self.validateForm);
        obj.validators[2] = new Validator([ obj.view
                                                    .getPhoneNumberInput() ], [ new Condition(
                                                            'letters-only', validators.phoneNumberValidator) ],
                                                            'keyup', 'simple', self.validateForm);

        obj.validators[3] = new Validator([obj.view.getEmailInput()], 
                [new Condition('email', validators.emailValidator), 
                 new Condition('unique', function(inputs, self) {
                     if( obj.user.email !== inputs[0].val() ) {
                         userDelegate.checkUserNameAvailability(inputs[0].val(), function(available) {
                             if(!available) {
                                 self.simpleAddError(inputs[0], "Email address already exists.");
                                 self.addError(inputs[0]);
                             } else {
                                 self.simpleRemoveError(inputs[0]);
                                 self.removeError(inputs[0]);	
                             }
                         });
                     }
                 })], 'change', 'simple', obj.validateForm);		
    };

    obj.validate = function() {
        var i;
        for (i = 0; i < obj.validators.length; i++) {
            obj.validators[i].validate();
        }
    };

    obj.validateForm = function() {
        var i, allOk = true;
        for (i = 0; i < obj.validators.length; i++) {
            if (obj.validators[i].isOk() === false) {
                allOk = false;
                break;
            }
        }

        if (allOk) {
            console.log('enable');
            obj.view.enableSaveButton();
        } else if (!allOk) {
            console.log('disable');
            obj.view.disableSaveButton();
        }

        return allOk;
    };

    obj.deleteUser = function() {
        eventManager.sendEvent(constants.EVENT_PROFILE_DELETE_USER_REQUEST, { userId: obj.user._id, successCallback: obj.afterUserDelete, errorCallback: obj.afterUserDelete});
    };

    obj.afterUserDelete = function() {
        confirmationDialogCtrl.close();
    };

    obj.saveUser = function() {
        var selfProfileUpdate = false;
        if(obj.user._id === globalConfiguration.loggedUser._id) {
            selfProfileUpdate = true;
        }
        obj.delegate.patchUserDifferences(obj.user, obj.view.getUser(), function() {
            obj.delegate.readEntity(obj.user._id, function(user) {
                obj.setUser(user);
                if(selfProfileUpdate) {
                    globalConfiguration.loggedUser = user;
                }
                eventManager.sendEvent(constants.EVENT_USER_PROFILE_UPDATED_SUCCESSFULY, { user: user });
            }, function() {
                eventManager.sendEvent(constants.EVENT_USER_PROFILE_UPDATE_FAILED);
            });
        }, function() {
            eventManager.sendEvent(constants.EVENT_USER_PROFILE_UPDATE_FAILED);
            obj.reloadUser();
        }, function() {
            obj.reloadUser();
        });
    };

    obj.setStates = function(country,stateProvince) {
        var self = this;

        $.ajax({
            type : "GET",
            url : "data/" + country + ".json",
            dataType : "json",
            success : function(data) {
                data = [ {
                    "key" : "",
                    "value" : "Please Select"
                } ].concat(data);
                obj.view.getStateProvinceInput().loadSelect(data);
                if(stateProvince && stateProvince!==""){
                    obj.view.getStateProvinceInput().val(stateProvince);
                    obj.view.getFirstStateProvinceOption().text("");
                }else{
                    obj.view.getFirstStateProvinceOption().text("Please Select");
                }

                obj.checkSelectors();				
            },
            error : function(xhr) {
                console.log('Error: ' + xhr.status + ' ' + xhr.statusText);
            }
        });
    };

    obj.setCountryAndStateProvince = function(country, stateProvince) {
        var self = this;

        $.ajax({
            type : "GET",
            url : "data/countries.json",
            dataType : "json",
            success : function(data) {
                data = [ {
                    "key" : "",
                    "value" : "Please Select"
                } ].concat(data);
                obj.view.getCountryInput().loadSelect(data);
                if(country && country!==""){
                    obj.view.getCountryInput().val(country);
                    obj.view.getFirstCoutryOption().text("");
                }
                obj.adjustStateProvinceDropdown(null,stateProvince);

                obj.checkSelectors();
            },
            error : function(xhr) {
                console.log('Error: ' + xhr.status + ' ' + xhr.statusText);
            }
        });
    };

    obj.setSecurityQuestionSelect = function(question) {
        var self = this;

        $.ajax({
            type : "GET",
            url : "data/secquestions.json",
            dataType : "json",
            success : function(data) {
                obj.view.getSecurityQuestion().loadSelect(data);
                obj.view.getSecurityQuestion().val(question);

                obj.checkSelectors();
            },
            error : function(xhr) {
                console.log('Error: ' + xhr.status + ' ' + xhr.statusText);
            }
        });
    };


    obj.adjustStateProvinceDropdown = function(event,stateProvince) {
        var country = obj.view.getCountryInput().val();
        if(!country || country===""){
            console.log("Removing all states/provinces");
            obj.view.getStateProvinceInput().emptySelect();
            if(country===""){
                obj.view.getFirstCoutryOption().text("Please Select");
            }
        }else{
            obj.view.getFirstCoutryOption().text("");
            console.log("Getting data from server for "+country);
            obj.setStates(country,stateProvince);
        }
    };

    obj.checkSelectors = function() { 
        var self = this;

        obj.view.getSelects().each(function() {
            if( self.editMode === false && $(this).val() === '' ) {
                self.view.setEditMode(false);
            }
        });		
    };

    console.log("ProfileCtrl created");

    return obj;
});


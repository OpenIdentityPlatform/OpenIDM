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

/*global $, define, Mustache*/

define("app/comp/user/profile/ProfileView",
        [],
        function () {
    var obj = {};

    obj.getUser = function() {
        var user = {
                userName : obj.getEmailInput().val().toLowerCase(),
                firstname : obj.getFirstNameInput().val(),
                lastname : obj.getLastNameInput().val(),
                email : obj.getEmailInput().val(),
                address1 : obj.getAddress1Input().val(),
                address2 : obj.getAddress2Input().val(),
                city : obj.getCityInput().val(),
                postalcode : obj.getPostalCodeInput().val(),
                phonenumber : obj.removeInvalidCharsFromPhoneNmber(obj.getPhoneNumberInput().val()),
                country : obj.getCountryInput().val(),
                state_province : (obj.getStateProvinceInput().val()) ? obj.getStateProvinceInput().val() : ""
        };

        if( obj.getAccountStatusInput().length !== 0 ) {
            user.accountStatus = obj.getAccountStatusInput().val();
        }

        if( obj.getPasswordAttemptsInput().length !== 0 ) {
            user.passwordAttempts = obj.getPasswordAttemptsInput().val();
        }

        return user;
    };

    obj.removeInvalidCharsFromPhoneNmber = function(phoneNumber){
        return phoneNumber.split(' ').join('').split('-').join('').split('(').join('').split(')').join('');
    };

    obj.setUserName = function(name) {
        $("#profileInfo h1").html(name);
    };

    obj.getEmailInput = function() {
        return $("#profileData input[name='email']");
    };

    obj.getPasswordInput = function() {
        return $("#profileData input[name='password']");
    };

    obj.getCountryInput = function() {
        return $('#profile select[name="country"]');
    };

    obj.getStateProvinceInput = function() {
        return $("#profile select[name='stateProvince']");
    };

    obj.getFirstCoutryOption = function() {
        return $("#profile select[name='country'] > option:first");
    };

    obj.getFirstStateProvinceOption = function() {
        return $("#profile select[name='stateProvince'] > option:first");
    };

    obj.show = function(showCallback, mode) {
        console.log("showing profile form");

        var view = null;
        if( mode !== undefined && mode === 'admin' ) {
            view = {admin: true, user: false};
        } else {
            view = {admin: false, user: true};
        }

        $.ajax({
            type : "GET",
            url : "templates/user/ProfileTemplate.html",
            dataType : "html",
            success : function(template) {
                $("#content").fadeOut(100, function() {					
                    var html = Mustache.to_html(template, view);

                    $(this).html(html);
                    $(this).fadeIn(100);
                    $("#actions").fadeIn(200);

                    showCallback();
                });
            },
            error : showCallback
        });
    };

    obj.getUserProfileHeadingLabel = function() {
        return $("#userProfileHeadingLabel");
    };

    obj.getResetButton = function() {
        return $("#profile input[name='resetButton']");
    };

    obj.getSaveButton = function() {
        return $("#profile input[name='saveButton']");
    };

    obj.getFirstNameInput = function() {
        return $("#profile input[name='firstName']");
    };

    obj.getLastNameInput = function() {
        return $("#profile input[name='lastName']");
    };

    obj.getAddress1Input = function() {
        return $("#profile input[name='address1']");
    };

    obj.getAddress2Input = function() {
        return $("#profile input[name='address2']");
    };

    obj.getCityInput = function() {
        return $("#profile input[name='city']");
    };

    obj.getPostalCodeInput = function() {
        return $("#profile input[name='postalCode']");
    };

    obj.getPhoneNumberInput = function() {
        return $("#profile input[name='phoneNumber']");
    };

    obj.getPasswordAttemptsInput = function() {
        return $("#profile input[name='passwordAttempts']");
    };

    obj.getLastPasswordSetInput = function() {
        return $("#profile input[name='lastPasswordSet']");
    };

    obj.getUserPrincipleInput = function() {
        return $("#profile input[name='userPrinciple']");
    };

    obj.getAccountStatusInput = function() {
        return $("#profile select[name='accountStatus']");
    };

    obj.getSecurityQuestion = function() {
        return $("#profile select[name='securityQuestion']");
    };

    obj.getSecurityAnswer = function() {
        return $("#profile input[name='securityAnswer']");
    };

    obj.getDeleteButton = function() {
        return $("#profile input[name='deleteButton']");
    };

    obj.getBackButton = function() {
        return $("#profile input[name='backButton']");
    };

    obj.enableSaveButton = function() {
        obj.getSaveButton().removeClass('ashy').addClass('orange');
        obj.getSaveButton().removeAttr('disabled');
    };

    obj.disableSaveButton = function() {
        obj.getSaveButton().removeClass('orange').addClass('ashy');
        obj.getSaveButton().attr('disabled', 'disabled');
    };

    obj.getSelects = function() {
        return $("#profile select");		
    };

    return obj;

});

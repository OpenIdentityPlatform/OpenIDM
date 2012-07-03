/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
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

/*global $, define, Mustache*/

/**
 * @author mbilski
 */

define("app/comp/user/registration/RegistrationView",
        ["app/comp/common/dialog/DialogsCtrl"],
        function(dialogsCtrl) {
    var obj = {};

    obj.dialogs = dialogsCtrl;
    obj.mode = null;
    obj.image = null;

    obj.getRegisterButton = function() {
        return $("#registration input[name='register']");
    };

    obj.getUser = function() {
        var user = {
                userName : $("#registration input[name='email']").val(),
                firstname : $("#registration input[name='firstName']").val(),
                lastname : $("#registration input[name='lastName']").val(),
                email : $("#registration input[name='email']").val(),
                password : $("#registration input[name='password']").val(),				
                //TODO: Until 'replace' won't add new values
                address1 : "",
                address2 : "",
                city : "",
                postalcode : "",
                phonenumber : $("#registration input[name='phone']").val(),			
                country : "",
                state_province : "",
                passwordAttempts: "",
                accountStatus: "active",
                lastPasswordSet: new Date(),
                image: obj.getSelectedImageValue(),
                securityquestion : $("#registration select[name='securityQuestion']").val(),
                securityanswer : $("#registration input[name='securityAnswer']").val(),
                passphrase : $("#registration input[name='passphrase']").val()
        };

        if( obj.mode === "admin" ) {
            user.passphrase = "";
            user.image = "mail.png";
            user.securityquestion = obj.getSecurityQuestionOrZeroIfNotSet();
            user.securityanswer = obj.getSecurityAnswerOrRandomIfNotSet();
        }

        return user;
    };
    
    obj.getSecurityQuestionOrZeroIfNotSet = function(){
        var securityQuestion = obj.getSecurityQuestion().val();
        if(securityQuestion){
            console.log("Setting security question:" + securityQuestion);
            return securityQuestion;
        } else {
            console.log("Setting security question as 0");
            return "0";
        }
    };
    
    obj.getSecurityAnswerOrRandomIfNotSet = function(){
        var securityAnswer = obj.getSecurityAnswer().val();
        if(securityAnswer){
            return securityAnswer;
        } else {
            securityAnswer = "";
            securityAnswer += String.fromCharCode((Math.floor((Math.random() * 100)) % 26) + 97);
            securityAnswer += String.fromCharCode((Math.floor((Math.random() * 100)) % 26) + 97);
            securityAnswer += String.fromCharCode((Math.floor((Math.random() * 100)) % 26) + 97);
            securityAnswer += String.fromCharCode((Math.floor((Math.random() * 100)) % 26) + 97);
            return securityAnswer.toLowerCase();
        }
    };
    
    obj.getFirstNameInput = function() {
        return $("#registration input[name='firstName']");
    };

    obj.getLastNameInput = function() {
        return $("#registration input[name='lastName']");
    };

    obj.getPhoneInput = function() {
        return $("#registration input[name='phone']");
    };

    obj.getEmailInput = function() {
        return $("#registration input[name='email']");
    };

    obj.getPasswordInput = function() {
        return $("#registration input[name='password']");
    };

    obj.getPasswordConfirmInput = function() {
        return $("#registration input[name='passwordConfirm']");
    };

    obj.getTermsOfUseCheckbox = function() {
        return $("#registration input[name='terms']");
    };

    obj.areTermsOfUseAgreed = function() {
        return $("#registration input[name='terms']").is(':checked');
    };

    obj.getSecurityQuestion = function() {
        return $("#registration select[name='securityQuestion']");
    };

    obj.getSecurityAnswer = function() {
        return $("#registration input[name='securityAnswer']");
    };

    obj.getPassphrase = function() {
        return $("#registration input[name='passphrase']");
    };

    obj.getSelectPictureLink = function() {
        return $("#selectPictureLink");
    };

    obj.show = function(callback, mode) {
        var view;
        console.log("showing registration form");

        if( mode === 'admin' ) {
            obj.mode = 'admin';
            view = {'admin': true, 'user': false};
        } else {
            obj.mode = 'user';
            view = {'admin': false, 'user': true};
        }

        $.ajax({
            type : "GET",
            url : "templates/user/RegistrationTemplate.html",
            dataType : "html",
            success : function(template) {
                $("#content").fadeOut(100, function() {
                    var html = Mustache.to_html(template, view);

                    $(this).html(html);
                    $(this).fadeIn(100);
                    callback();
                });
            },
            error : callback
        });

        $('#nav-content > span').html('');
    };

    obj.showTermsOfUseDialog = function() {
        var self = this;

        $.get("templates/user/TermOfUseTemplate.html", function(data) {
            self.dialogs.setWidth(800);
            self.dialogs.setHeight(500);
            self.dialogs.setContent(data);
            self.dialogs.setActions("<input type='button' name='dialogClose' id='dialogClose' class='button gray floatRight' value='Close' />");
            $("#dialogClose").on('click', function(event) {
                self.dialogs.close();
            });
            self.dialogs.show();
        });		
    };

    obj.enableRegisterButton = function() {
        obj.getRegisterButton().removeClass('gray').addClass('orange');
    };

    obj.disableRegisterButton = function() {
        obj.getRegisterButton().removeClass('orange').addClass('gray');
    };

    //pasphrase

    obj.selectImage = function(image) {
        $("#passphrasePictures img").removeClass('pictureSelected');
        $("#passphrasePictures img").addClass('pictureNotSelected');

        $(image).removeClass('pictureNotSelected');
        $(image).addClass('pictureSelected');

        obj.image = obj.getValueOfImage(image);
    };

    obj.getSelectedImageValue = function(image) {
        return $("#passphrasePictures img.pictureSelected").next().val();
    };

    obj.getValueOfImage = function(image) {
        return $(image).next().val();
    };

    obj.selectImageByValue = function(value) {
        obj.selectImage( $("#passphrasePictures input[value='"+ value +"']").prev() );
    };

    obj.getImages = function() {
        return $("#passphrasePictures img");
    };

    obj.getForgottenPasswordDialog = function() {
        return $("#frgtPasswrdSelfReg");
    };

    return obj;
});

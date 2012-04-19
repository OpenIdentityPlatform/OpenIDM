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

/*global $, define*/

define("app/comp/user/changesecurityquestion/ChangeSecurityQuestionDialogCtrl",
        ["app/comp/user/changesecurityquestion/ChangeSecurityQuestionDialogView",
         "app/comp/user/delegates/UserDelegate",
         "app/comp/common/messages/MessagesCtrl", 
         "app/util/Validator",
         "app/util/Condition",
         "app/util/Validators"], 
         function(changeSecurityQuestionDialogView, userDelegate, messagesCtrl, Validator, Condition, validators) {
    var obj = {}, enableSaveButton, disableSaveButton;

    obj.view = changeSecurityQuestionDialogView;
    obj.delegate = userDelegate;
    obj.messages = messagesCtrl;

    obj.validators = [];
    obj.user = null;
    obj.password = null;
    obj.profileCtrl = null;

    obj.init = function(id, password, profileCtrl) {
        obj.user = id;
        obj.profileCtrl = profileCtrl;
        obj.password = password;

        obj.view.show(function() {
            obj.registerValidators();

            obj.view.getCloseButton().on('click', function(event) {
                obj.view.close();
            });

            obj.setSecurityQuestionSelect("");
        });
    };

    enableSaveButton = function() {
        obj.view.getSaveButton().off('click').on('click', function(event) {
            event.preventDefault();
            obj.afterSaveButtonClicked();
            return false;
        });
        obj.view.enableSaveButton();

    };

    disableSaveButton = function() {
        obj.view.getSaveButton().off('click');
        obj.view.disableSaveButton();
    };

    obj.setSecurityQuestionSelect = function(question) {
        $.ajax({
            type : "GET",
            url : "data/secquestions.json",
            dataType : "json",
            success : function(data) {
                obj.view.getQuestion().loadSelect(data);
                obj.view.getQuestion().val(question);

            },
            error : function(xhr) {
                console.log('Error: ' + xhr.status + ' ' + xhr.statusText);
            }
        });
    };

    obj.getUser = function() {
        return obj.user;		
    };

    obj.registerValidators = function() {

        console.log("register dialog validators");
        obj.validators[0] = new Validator([obj.view.getAnswer()], [new Condition('not-empty', validators.notEmptyValidator)], 'change', 'simple', obj.validateForm);

        obj.validators[1] = new Validator([obj.view.getQuestion()], [new Condition('not-empty', validators.notEmptyValidator)], 'change', 'simple', obj.validateForm);

        obj.validators[2] = new Validator([obj.view.getPassword()], [new Condition('correctPassword', function(input) {
            if( input[0].val() !== obj.password ) {
                return "Invalid password";
            }
        })], 'change', 'simple', obj.validateForm);

    };

    obj.validateForm = function() {
        console.log('validate all form');

        var i, allOk = true;

        for (i = 0; i < obj.validators.length; i++) {
            if (obj.validators[i].isOk() === false) {
                allOk = false;
                break;
            }
        }

        if (allOk) {
            enableSaveButton();
        } else if (!allOk) {
            disableSaveButton();
        }

        return allOk;
    };
//  TODO not in use anymore
//  obj.afterSaveButtonClicked = function() {
//  for (k = 0; k < obj.validators.length; k++) {
//  obj.validators[k].validate();
//  }

//  var flds = new Array("securityquestion","securityanswer");
//  var vals = new Array(obj.view.getQuestion().val(), obj.view.getAnswer().val());


//  if (obj.validateForm() == true) {
//  obj.delegate.changeFields(obj.user, flds, vals,
//  function(r) {
//  obj.messages.displayMessage('info','Security question has been changed');
//  //updating in profile
//  obj.profileCtrl.getUser().securityquestion = obj.view.getQuestion().val();
//  obj.profileCtrl.getUser().securityanswer = obj.view.getAnswer().val();
//  obj.view.close();
//  }, function(r) {
//  obj.messages.displayMessage('error', 'Unknown error');
//  obj.view.close();
//  });
//  }
//  }
    return obj;
});


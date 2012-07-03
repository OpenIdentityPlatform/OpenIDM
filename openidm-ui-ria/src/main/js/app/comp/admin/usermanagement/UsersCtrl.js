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

/*global $, define */
/*jslint devel: true*/

/**
 * @author mbilski
 */

define("app/comp/admin/usermanagement/UsersCtrl",[
                                                  "app/comp/admin/usermanagement/UsersView",
                                                  "app/comp/user/delegates/UserDelegate",
                                                  "app/comp/common/breadcrumbs/BreadcrumbsCtrl",
                                                  "app/comp/user/profile/ProfileCtrl",
                                                  "app/comp/common/popup/PopupCtrl",
                                                  "app/comp/common/dialog/ConfirmationDialogCtrl",
                                                  "app/comp/common/messages/MessagesCtrl",
                                                  "app/comp/common/eventmanager/EventManager",
                                                  "app/util/Constants",
                                                  "app/util/Comparators"],
function(usersView, userDelegate, breadcrumbsCtrl, profileCtrl, popupCtrl, confirmationDialogCtrl, messagesCtrl, eventManager, constants, comparators) {
	
	var obj = {};
	
	obj.view = usersView;
	obj.delegate = userDelegate;
	obj.breadcrumbs = breadcrumbsCtrl;
	
	obj.profileCtrl = profileCtrl;
	
	obj.allUsers = null;
	obj.editLock = false;
	
	obj.init = function() {
		console.log("UsersCtrl.init()");
		
		obj.breadcrumbs.removePath();
		obj.breadcrumbs.set("Users");
		
		obj.view.init();
		obj.view.show(function() {
			obj.findUsersMatchingFilterInput();
			obj.view.getFilterInput().bind('keyup', obj.findUsersMatchingFilterInput);		
			
			obj.view.getAddUserButton().unbind();
			obj.view.getAddUserButton().bind('click', function(event) {
				event.preventDefault();
				eventManager.sendEvent(constants.EVENT_ADMIN_ADD_USER_REQUEST);
			});
		});		
	};
	
	obj.registerListener = function(userNumber) {
		var editButton = obj.view.getEditButton(userNumber);
		
		$(editButton).unbind();
		$(editButton).bind('click', function(event) {
			event.preventDefault();

			var n = obj.view.getUserNumberByEditButton(this);
			obj.editUserAction(obj.allUsers[n]._id);
		});		
	};
	
	obj.editUserAction = function(userId) {
		if( obj.editLock === true ) {
			return;
		}
		
		obj.editLock = true;
					
		obj.delegate.readEntity(userId, function(user) {
			eventManager.sendEvent(constants.EVENT_ADMIN_SHOW_PROFILE_REQUEST, { user: user, callback: function() {obj.editLock = false;} });
		}, function(user) {
			obj.editLock = false;	
		});
	};
	
	obj.findUsersMatchingFilterInput = function() {
		var i, k = 0, limit = 2, indexes = [], usersToAdd = [], registerListenerForUser, filter = obj.view.getFilterInput().val();
		
		if(obj.allUsers){
			for(i = 0; i < obj.allUsers.length; i++ ) {
				obj.view.removeUser(i);
			}
			obj.view.getRemainingUsers().html('0');
		}
		
		obj.delegate.getUsersCountByMatchingNames(filter, function(result) {
			if( result.count - limit > 0 ) {
				obj.view.getRemainingUsers().html( result.count - limit );
			} else {
				obj.view.getRemainingUsers().html('0');
			}
		}, function(r) {
			console.log('error:' + r);
		}); 
		
		
		obj.delegate.getUsersByMatchingNamesWithLimit(filter, limit, function(r) {
			var i;
			obj.allUsers = r;
			r = r.sort(comparators.userComparator);
		
			for(i = 0; i < obj.allUsers.length; i++ ) {
				indexes[k] = i;
				usersToAdd[k] = obj.allUsers[i];
				k++;
			}
			
			registerListenerForUser = function() {
		           obj.registerListener(indexes[i]);
		       };  
			
			for(i = 0; i < obj.allUsers.length; i++ ) {
				obj.view.addUser(usersToAdd[i], indexes[i], registerListenerForUser);			
			}
			
			obj.registerPopups();
			
		}, function(r) {
			console.log('error:' + r);
		}); 
	};
	
	obj.registerPopups = function() {
		obj.view.getActionLinks().unbind().bind('mouseenter', function() {
		    var n;
			popupCtrl.showBy(obj.view.getActionMenu(), this);
			
			n = obj.view.getUserNumberByActionLink(this);
			
			obj.registerActionLinks(obj.allUsers[n]._id, obj.allUsers[n].userName, n);
		});
				
		obj.view.getUserPrincipleLinks().unbind().bind('mouseenter', function() {
			var current = this, userId = obj.view.getUserIdByPrincipleLink(this);
			
			userDelegate.readEntity(obj.allUsers[userId]._id, function(user) {
				obj.view.getUserBriefInfo(user, function(data) {
					popupCtrl.showBy(data, current);
				});
			}, function() {
				console.log('error');
			});
		});		
	};
	
	obj.registerActionLinks = function(userId, userName, number) {
		obj.view.getDeleteUserLink().unbind().bind('click', function() {
			confirmationDialogCtrl.init("Delete user", userName + " account will be deleted.", "Delete", function() {
				obj.deleteUser(userId, number);
			});
		});
		
		obj.view.getEditUserLink().unbind().bind('click', function() {
			obj.editUserAction(userId);
		});
	};
	
	obj.deleteUser = function(userId, number) {
		eventManager.sendEvent(constants.EVENT_USER_LIST_DELETE_USER_REQUEST, { userId: userId, successCallback: function () { obj.afterUserDeletedSuccesfully(number); } , errorCallback: obj.afterUserDeleteFailed()});
	};
	
	obj.afterUserDeletedSuccesfully = function (number) {
		confirmationDialogCtrl.close();
		obj.view.removeUser(number);
		obj.findUsersMatchingFilterInput();
	};
	
	obj.afterUserDeleteFailed = function () {
		confirmationDialogCtrl.close();
	};
	
	obj.includeUser = function(user, filter) {
	    var i, exp;
		if( filter === "" ) {
			return true;
		}
		
		exp = filter.split(" ");
				
		for(i = 0; i < exp.length; i++ ) {
			if( user.lastname.toLowerCase().indexOf(exp[i].toLowerCase()) === -1 && 
				user.email.toLowerCase().indexOf(exp[i].toLowerCase()) === -1 ) {
				return false;
			}
		}
		
		return true;
	};
	
	return obj;
});

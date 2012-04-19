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

/*global $, define */

/**
 * @author mbilski
 */

define("app/comp/admin/usermanagement/UsersView", ["app/util/UIUtils"],function(UIUtils) {
	
	var obj = {};
	
	obj.maxUsers = 10;
	obj.users = 0;
	
	obj.init = function() {
		obj.users = 0;
	};
	
	obj.show = function(showCallback) {
		console.log("showing users");

		$.ajax({
			type : "GET",
			url : "templates/admin/UsersTemplate.html",
			dataType : "html",
			success : function(data) {
				$("#content").fadeOut(100, function() {
					$(this).html(data);
					$(this).fadeIn(100);
					showCallback();
				});
			},
			error : showCallback
		});
	};
	
	/**
	 * userNumber - user index in UserCtrl's users array
	 */
	obj.addUser = function(user, userNumber, callback) {
		if( obj.users < obj.maxUsers && !obj.hasUser(userNumber) ) {			
			var i = 0, u, userRow; 
			
			u = $("#usersTable").find("input[type='hidden']").first();
			
			userRow = obj.getUserRow(user, userNumber);
			
			if( u.length === 0 ) {
				console.log('adding user at the beggining');
				$("#usersTable").append(userRow);
			} else {
				while( u.length !== 0 ) {
					if( $(u).val() > userNumber ) {
						console.log('adding user before' + $(u).val());
						$(u).parent().parent().after(userRow);
						break;
					}
					
					i++;
					u = $("#usersTable").find("input[type='hidden']").filter(":gt(" + i + ")").first();
				}
				
				if( u.length === 0 ) {
					console.log('adding at the end');
					$("#usersTable").append(userRow);
				}
			}
			
			obj.users++;
		}
		
		if( callback !== undefined ) {
			callback();
		}
	};
	
	obj.getUserRow = function(user, userNumber) {
		return "<tr><td width='245'><a href='#'>" + user.email + "</a></td>" +
			"<td width='210'>" + user.lastname + "</td>" +
			"<td width='110'>" + user.firstname + "</td>" +
			"<td style='width: auto; text-align: center;'><input type=\"hidden\" name=\"number\" value=\"" + userNumber +"\">" +
					"<a href='#'>show</a></td></tr>";
	};
	
	obj.removeUser = function(userNumber) {
		if( obj.hasUser(userNumber) ) {
			console.log('removing user from users list');
			$("#usersTable").find("input[value='" + userNumber + "']").parent().parent().remove();
			obj.users--;
		}
	};
	
	obj.hasUser = function(userNumber) {
		if( $("#usersTable").find("input[value='" + userNumber + "']").length !== 0 ) {
			return true;
		}
		
		return false;
	};
	
	obj.getFilterInput = function() {
		return $("#users").find("input[name='filter']").first();
	};
	
	obj.getEditButton = function(userNumber) {
		return $("#users").find("input[value='" + userNumber + "']").parent().parent();
	};
	
	obj.getUserNumberByEditButton = function(editButton) {
		return $(editButton).find("input[type='hidden']").val();
	};
	
	obj.getRemoveButton = function(userId) {
				
	};
	
	obj.getAddUserButton = function() {
		return $("#addUserButton");
	};
	
	obj.getMaxUsers = function() {
		return obj.maxUsers;
	};
	
	obj.setMaxUsers = function(max) {
		obj.maxUsers = max;
	};
	
	obj.getUsers = function() {
		return obj.users;
	};
	
	obj.getRemainingUsers = function() {
		return $("#remainingUsers");
	};
	
	obj.getActionLinks = function() {
		return $('#usersTable a:odd');
	};
	
	obj.getUserPrincipleLinks = function() {
		return $("#usersTable a:even");
	};
	
	obj.getUserIdByPrincipleLink = function(link) {
		return $(link).parent().parent().find('input[type=hidden]').first().val();
	};
	
	obj.getActionMenu = function() {
		return "<ul><li><a href='#' id='deleteUserLink'>Delete this user</a></li><li><a href='#' id='editUserLink'>Edit this user's details</a></li></ul>";
	};
	
	obj.getUserNumberByActionLink = function(link) {
		return $(link).prev().val();
	};
	
	obj.getDeleteUserLink = function() {
		return $("#deleteUserLink");
	};
	
	obj.getEditUserLink = function() {
		return $("#editUserLink");
	};
	
	obj.getUserBriefInfo = function(user, callback) {
	    UIUtils.fillTemplateWithData("templates/admin/UserBriefInfoTemplate.html", user, function(data) {
			callback(data);
		});
	};
	
	return obj;
	
});

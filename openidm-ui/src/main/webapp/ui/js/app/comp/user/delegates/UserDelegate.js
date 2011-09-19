/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
registerNS("openidm.app.components.data");

openidm.app.components.data.UserDelegate = function() {

	var getUser = function(id, successCallback, errorCallback) {
		console.info("getting user[id=" + id + "]");

		$.ajax({
			url: util.constants.host + "openidm/managed/user/" + id,
			dataType: 'json',
			success: successCallback,
			error: errorCallback
		});
	};

	var addUser = function(user, successCallback, errorCallback) {
		console.info("adding user[" + JSON.stringify(user) + "]");

		$.ajax({
			type: "PUT",
			url: util.constants.host + "openidm/managed/user/" + user.firstname,
			dataType: "json",
			contentType: 'application/json; charset=utf-8',
			data: JSON.stringify(user),
			success: successCallback,
			error: errorCallback
		});
	};

	var updateUser = function(user, successCallback, errorCallback) {
		addUser(user, successCallback, errorCallback);
	};

	return {
		getUser: getUser,
		addUser: addUser,
		updateUser: updateUser
	}
} ();


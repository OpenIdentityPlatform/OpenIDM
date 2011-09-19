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
registerNS("openidm.app.components.nav");

openidm.app.components.nav.MessagesView = function() {

	this.addErrorMessage = function(msg) {
		var msgData = {
			type: 'errorMessage',
			msg: msg
		};

		this.addMessage(msgData);
	};

	this.addInfoMessage = function(msg) {
		var msgData = {
			type: 'confirmMessage',
			msg: msg
		};

		this.addMessage(msgData);
	};

	this.addMessage = function(msgData) {
		$("#messages").append("<div class='" + msgData.type + " radious'>" + msgData.msg + "</div>");
		$("#messages > div:last").fadeIn(500).delay(2000).fadeOut(500);
	};

	return this;
} ();


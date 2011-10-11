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
define(["app/comp/main/views/DialogsView"], function (dialogsView) {
	var obj = {}

	obj.view = dialogsView;

	obj.show = function() {
		console.log("show dialog");
		obj.view.show();
	}

	obj.close = function() {
		console.log("close dialog");
		obj.view.close();
	}

	obj.setContent = function(content) {
		obj.view.setContent(content);
	}

	obj.setActions = function(actions) {
		obj.view.setActions(actions)
	}

	obj.setWidth = function(width) {
		obj.view.setWidth(width);
	}

	obj.setHeight = function(height) {
		obj.view.setHeight(height);
	}

	return obj;
})


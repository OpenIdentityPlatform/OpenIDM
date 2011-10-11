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
define(function() {
	var obj = {};

	obj.show = function() {
		$("#dialog").fadeIn(300, function() {
			$(this).show();
		});
	}

	obj.close = function() {
		$("#dialog").fadeOut(300, function() {
			$(this).hide();
		});
	}

	obj.setContent = function(content) {
		$("#dialogContent").html(content);
	}

	obj.setActions = function(actions) {
		$("#dialogActions").html(actions);
	}

	obj.setWidth = function(width) {
		$("#dialogContainer").css('width', width);
	}

	obj.setHeight = function(height) {
		$("#dialogContainer").css('height', height + 50);
		$("#dialogContent").css('height', height);
	}

	return obj;
});


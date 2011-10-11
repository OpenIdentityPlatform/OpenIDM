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
//function creating a namespace
registerNS = function() {
	var a = arguments, o = null, i, j, d;
	for (i = 0; i < a.length; i = i + 1) {
		d = a[i].split(".");
		o = window;
		for (j = 0; j < d.length; j = j + 1) {
			o[d[j]] = o[d[j]] || {};
			o = o[d[j]];
		}
	}
	return o;
};

// function creating main namespace
function createMainNamespace() {
	console.log("main namespace registration");
	if (typeof openidm == "undefined" || !openidm) {
		registerNS("openidm");
	}
	;
};
createMainNamespace();

// function supporting inheritance
openidm.extend = function(subClass, baseClass) {
	function inheritance() {
	}
	inheritance.prototype = baseClass.prototype;

	subClass.prototype = new inheritance();
	subClass.prototype.constructor = subClass;
	subClass.baseConstructor = baseClass;
	subClass.superClass = baseClass.prototype;
};

openidm.fillTemplateWithData = function(templateUrl, data,callback) {
	$.ajax({
		type: "GET",
		url: templateUrl,
		dataType: "html",
		success: function(template) {
			if(data == 'unknown' || data == null) {
				//don't fill the template
				callback(template);
			} else {
				//fill the template
				callback(Mustache.to_html(template,data));
			}
		},
		error: callback
	});
}

$.fn.emptySelect = function() {
	return this.each(function() {
		if (this.tagName == 'SELECT') {
			this.options.length = 0;
		}
	});
}

$.fn.loadSelect = function(optionsDataArray) {
	return this.emptySelect().each(function() {
		if (this.tagName == 'SELECT') {
			var selectElement = this;
			for(var i=0;i<optionsDataArray.length;i++){
				var option = new Option(optionsDataArray[i].value, optionsDataArray[i].key);
				if ($.browser.msie) {
					selectElement.add(option);
				} else {
					selectElement.add(option, null);
				}
			}
		}
	});
}

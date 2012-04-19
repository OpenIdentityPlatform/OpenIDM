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

/*global $, define, window , Mustache*/

define("app/util/UIUtils",
        [],
        function () {
    var obj = {};

    obj.getUrl = function() {
        return window.location.href;	
    };

    obj.getCurrentUrlBasePart = function() {
        return window.location.protocol + "//" + window.location.host;	
    };

    obj.getCurrentUrlQueryParameters = function() {
        return window.location.search.substr(1,window.location.search.lenght);	
    };

    obj.getCurrentPathName = function() {
        return window.location.pathname;
    };

    obj.setUrl = function(url) {
        window.location.href = url;	
    };

    obj.convertCurrentUrlToJSON = function() {
        var result = {}, parsedQueryParams;

        result.url = obj.getCurrentUrlBasePart();
        result.pathName = obj.getCurrentPathName();
        result.pathname = obj.getCurrentPathName();
        parsedQueryParams = decodeURI(obj.getCurrentUrlQueryParameters().replace("/&/g", "\",\"").replace("/=/g","\":\""));
        if(parsedQueryParams) {
            result.params = JSON.parse('{"' + parsedQueryParams + '"}');
        }
        return result;
    };

    obj.fillTemplateWithData = function(templateUrl, data,callback) {
        $.ajax({
            type: "GET",
            url: templateUrl,
            dataType: "html",
            success: function(template) {
                if(data === 'unknown' || data === null) {
                    //don't fill the template
                    callback(template);
                } else {
                    //fill the template
                    callback(Mustache.to_html(template,data));
                }
            },
            error: callback
        });
    };

    $.fn.emptySelect = function() {
        return this.each(function() {
            if (this.tagName === 'SELECT') {
                this.options.length = 0;
            }
        });
    };

    $.fn.loadSelect = function(optionsDataArray) {
        return this.emptySelect().each(function() {
            if (this.tagName === 'SELECT') {
                var i, option, selectElement = this;
                for(i=0;i<optionsDataArray.length;i++){
                    option = new Option(optionsDataArray[i].value, optionsDataArray[i].key);
                    if ($.browser.msie) {
                        selectElement.add(option);
                    } else {
                        selectElement.add(option, null);
                    }
                }
            }
        });
    };
    
    return obj;
});
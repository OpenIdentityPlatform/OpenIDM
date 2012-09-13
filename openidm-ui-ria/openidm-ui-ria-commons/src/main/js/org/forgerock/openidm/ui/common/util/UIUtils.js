/*
 * @license DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
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

/*global $, define, window , Mustache*/

define("org/forgerock/openidm/ui/common/util/UIUtils", [
	"org/forgerock/openidm/ui/common/util/typeextentions/String"
], function () {
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

    obj.normalizeSubPath = function(subPath) {
        if(subPath.endsWith('/')) {
            return subPath.removeLastChars();
        }
        return subPath;
    };
    
    obj.convertCurrentUrlToJSON = function() {
        var result = {}, parsedQueryParams;

        result.url = obj.getCurrentUrlBasePart();
        result.pathName = obj.normalizeSubPath(obj.getCurrentPathName());
        
        result.params = obj.convertQueryParametersToJSON(obj.getCurrentUrlQueryParameters());
        return result;
    };
    
    obj.convertQueryParametersToJSON = function(queryParameters) {
        var parsedQueryParams;
        
        if(queryParameters) {
            parsedQueryParams = decodeURI(queryParameters.replace(/&/g, "\",\"").replace(/\=/g,"\":\""));
            return JSON.parse('{"' + parsedQueryParams + '"}');
        }
        return null;
    };
    
    obj.templates = {};

    obj.renderTemplate = function(templateUrl, el, data, clb, mode) {
        obj.fillTemplateWithData(templateUrl, data, function(tpl) {
            if(mode === "append") {
                el.append(tpl);
            } else {
                el.html(tpl);
            }

            if(clb) {
                clb();
            }
        });
    };

    obj.fillTemplateWithData = function(templateUrl, data, callback) {
        if(templateUrl) {
            if (obj.templates[templateUrl]) {
                callback(Mustache.to_html(obj.templates[templateUrl], data));
            } else {               
                $.ajax({
                    type: "GET",
                    url: templateUrl,
                    dataType: "html",
                    success: function(template) {
                        if(data === 'unknown' || data === null) {
                            //don't fill the template
                            callback(template);
                        } else {
                            obj.templates[templateUrl] = template;

                            //fill the template
                            callback(Mustache.to_html(template, data));
                        }
                    },
                    error: callback
                });
            }
        }
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
    
    obj.loadSelectOptions = function(url, el, empty, callback) {
        $.ajax({
            type : "GET",
            url : url,
            dataType : "json",
            success : function(data) {                
                if( empty === undefined || empty === true ) {
                    data = [ {
                        "key" : "",
                        "value" : "Please Select"
                    } ].concat(data);
                }
                
                el.loadSelect(data);
                
                if(callback) {
                    callback(data);
                }
            },
            error : function(xhr) {
                console.log('Error: ' + xhr.status + ' ' + xhr.statusText); 
            }
        });
    };
    
    return obj;
});
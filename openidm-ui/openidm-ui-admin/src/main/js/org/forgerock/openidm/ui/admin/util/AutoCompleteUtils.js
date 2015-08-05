/**
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2014 ForgeRock AS. All rights reserved.
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

/*global define */

define("org/forgerock/openidm/ui/admin/util/AutoCompleteUtils", [
    "jquery",
    "jqueryui"
], function($) {

    var obj = {},
        createShowAllItemsButton = function(input){
                var wasOpen = false,
                showAllBtn = $( "<a>" )
                .css('height','21px')
                .attr( "tabIndex", -1 )
                .attr( "title", $.t("common.form.showAllItems") )
                .tooltip({
                    open: function (event, ui) {
                        wasOpen = input.autocomplete( "widget" ).is( ":visible" );
                        if (wasOpen) {
                            $(this).tooltip('close');
                            return false;
                        }
                    }
                })
                .button({
                  icons: {
                    primary: "ui-icon-triangle-1-s"
                  },
                  text: false
                })
                .removeClass( "ui-corner-all" )
                .addClass( "ui-corner-right ui-combobox-toggle" )
                .mousedown(function() {
                  wasOpen = input.autocomplete( "widget" ).is( ":visible" );
                })
                .click(function() {
                    input.focus();
                    $(this).tooltip('close');
                  // Close if already visible
                  if ( wasOpen ) {
                    return;
                  }

                  // Pass empty string as value to search for, displaying all results
                  input.autocomplete( "search", "" );
                });
            return showAllBtn;
        };

    obj.selectionSetup = function(input,source,hideValue){

        if (source.length && !input.data("uiAutocomplete")) {
            input.autocomplete({
                minLength: 0,
                source: source,
                focus: function () {
                    return false;
                }
              });
              if(source && typeof(source[0]) !== 'string'){
                  input.data( "uiAutocomplete" )._renderItem = function( ul, item ) {
                        var display;
                        if (hideValue) {
                            display =  "<a>" + item.label + "</a>";
                        } else {
                            display =  "<a>" + item.value + "<br>" + item.label + "</a>";
                        }
                        return $( "<li>" ).append( display ).appendTo( ul );
                  };
              }


            var showAllBtn = createShowAllItemsButton(input);

            input.after(showAllBtn);
        }
    };

    return obj;
});

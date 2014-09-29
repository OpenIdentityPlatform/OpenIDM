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

/*global define, $, _, Handlebars, form2js */
/*jslint evil: true */

define("org/forgerock/openidm/ui/admin/mapping/PropertiesView", [
    "org/forgerock/openidm/ui/admin/util/AdminAbstractView",
    "org/forgerock/commons/ui/common/main/EventManager",
    "org/forgerock/commons/ui/common/main/Configuration",
    "org/forgerock/commons/ui/common/util/UIUtils",
    "org/forgerock/commons/ui/common/util/Constants",
    "org/forgerock/openidm/ui/admin/delegates/SessionStorageDelegate"
    ], function(AbstractView, eventManager, conf, UIUtils, constants, sessionStorageDelegate) {

    var PropertiesView = AbstractView.extend({
        template: "templates/admin/mapping/PropertiesTemplate.html",
        element: "#propertyMapping",
        noBaseTemplate: true,
        events: {
            "click .addProperty": "addProperty",
            "click .removePropertyBtn": "removeProperty",
            "click .attrTabBtn": "openPropertyEditTab"
        },
        addProperty: function (e) {
            e.preventDefault();

            eventManager.sendEvent(constants.ROUTE_REQUEST, {routeName: "addMappingProperty", args: [this.parent.currentMapping().name]});
        },
        removeProperty: function (e) {
            e.preventDefault();
            
            UIUtils.jqConfirm($.t("templates.mapping.confirmRemoveProperty",{property: $(e.target).attr('target')}),_.bind(function(){
                var mapProps = sessionStorageDelegate.get(this.parent.currentMapping().name + "_Properties") || this.data.mapProps;
                sessionStorageDelegate.set(this.parent.currentMapping().name + "_Properties",_.reject(mapProps, function (p) { return p.target === $(e.target).attr('target'); }));
                this.parent.checkChanges();
               this.render(this.parent);
            },this));
        },
        openPropertyEditTab: function(e){
            var id = $(e.target).attr('rowId'),
                tab = $(e.target).attr('tab');
            
            eventManager.sendEvent(constants.ROUTE_REQUEST, {routeName: "editMappingProperty", args: [this.parent.currentMapping().name, id]});
            if(tab === 'condition'){
                $("[href=#Condition_Script]").click();
            }
            else if(tab === 'transform'){
                $("[href=#Transformation_Script]").click();
            }
            else if(tab === 'default'){
                $("[href=#Default_Values]").click();
            }
        },
        loadMappingPropertiesGrid: function() {
            var _this = this,
                mapProps = sessionStorageDelegate.get(this.parent.currentMapping().name + "_Properties") || sessionStorageDelegate.get("currentMapping").properties,
                sampleSource = conf.globalData.sampleSource || {},
                gridFromMapProps = function (props) {
                    return _.chain(props)
                            .map(function (prop) {
                                var sampleData = null,
                                    type = "",
                                    source = {},
                                    sourceProp = "",
                                    required = false,
                                    def = "",
                                    script = "",
                                    object = {},
                                    hasConditionScript = false;
                                
                                if(typeof(prop.condition) === "object" && prop.condition.type === "text/javascript" &&
                                        typeof(prop.condition.source) === "string"){
                                        
                                        hasConditionScript = true;
                                        object = sampleSource;
                                        
                                        try {
                                            if(!eval(prop.condition.source) && prop.condition.source.length > 0){
                                                sampleData = "WILL NOT UPDATE";
                                            }
                                        } catch (e) {
                                            sampleData = "ERROR WITH CONDITION";
                                        }
                                }

                                if (typeof(prop.transform) === "object" && prop.transform.type === "text/javascript" &&
                                        typeof (prop.transform.source) === "string" && sampleData === null) {

                                    if (typeof(prop.source) !== "undefined" && prop.source.length) {
                                        source = sampleSource[prop.source];
                                    } else {
                                        source = sampleSource;
                                    }
                                    try {
                                        sampleData = eval(prop.transform.source); // references to "source" variable expected within this string
                                    } catch (err) {
                                        sampleData = "ERROR WITH SCRIPT";
                                    }
                                    script = prop.transform.source;
                                    if (typeof(prop.source) !== "undefined" && prop.source.length) {
                                        sourceProp = prop.source;
                                    }
                                    
                                } else if (typeof(prop.source) !== "undefined" && prop.source.length) {
                                    
                                    if(sampleData === null){
                                        sampleData = sampleSource[prop.source];
                                    }
                                    sourceProp = prop.source;
                                    
                                }
                                
                                if (typeof(prop["default"]) !== "undefined" && prop["default"].length) {
                                    
                                    if (sampleData === null || sampleData === undefined) {
                                        sampleData = prop["default"];
                                    }

                                    def = prop["default"];

                                }
                                                            
                                
                                return {
                                    "target": Handlebars.Utils.escapeExpression(prop.target), 
                                    "source": Handlebars.Utils.escapeExpression(sourceProp), 
                                    "default": Handlebars.Utils.escapeExpression(def), 
                                    "script": Handlebars.Utils.escapeExpression(script), 
                                    "sample": Handlebars.Utils.escapeExpression(decodeURIComponent((sampleData || ""))),
                                    "required": required,
                                    "hasConditionScript": hasConditionScript
                                };
                        }).value();
                };
                
            this.data.mapProps = mapProps;
            
            /*if (!$("#findSampleUser").hasClass("ui-autocomplete-input")) {
                $("#findSampleUser").autocomplete({
                    delay: 500,
                    minLength: 2,
                    select: function (event, ui) {
                        conf.globalData.sampleSource = ui.item;
                        sampleSource = ui.item;
                        $("#findSampleUser").val(ui.item.cn);
                        

                        $('#mappingTable').jqGrid('setGridParam', {
                            datatype: 'local',
                            data: gridFromMapProps(mapProps)
                        }).trigger('reloadGrid');

                        return false;
                    },
                    source: function (request, response) {
                        searchDelegate.searchResults("account", "cn", request.term).always(response);
                    }
                }).data( "ui-autocomplete" )._renderItem = function (ul, item) {
                    return $( "<li>" )
                        .append( "<a>" + Handlebars.Utils.escapeExpression(item.cn) + "<br>" + Handlebars.Utils.escapeExpression(item.sAMAccountName) + " / " + Handlebars.Utils.escapeExpression(item.mail) + "</a>" )
                        .appendTo( ul );
                };
            }*/                


            //$('#mappingTable').after('<div id="mappingTable_pager"></div>');
            $('#mappingTable').jqGrid( {
                datatype: "local",
                data: gridFromMapProps(mapProps),
                height: 'auto',
                width: 910,
                rowNum: mapProps.length,
                pager: 'mappingTable_pager',
                hidegrid: false,
                colModel: [
                    {  
                        "name": "required",
                        "label": "&nbsp;",
                        "width": "25px",
                        "align": "center",
                        "title": false,
                        "formatter": function(required,opt,row){
                            return (!required) ? '<button target="' + row.target + '" type="button" title="' + $.t("common.form.removeAttribute") + ': ' + row.target + '" class="glyph-icon glyph-icon-minus-sign removePropertyBtn" style="margin-top:4px;">&nbsp;&nbsp;&nbsp;</button>' : '';
                         }
                    },
                    {
                        "name": "target",
                        "label": $.t("templates.mapping.target"),
                        "width": "175px",
                        "key": true
                    },
                    {  
                        "name": "source",
                        "label": $.t("templates.mapping.source"),
                        "width": "150px"
                    },
                    {  
                        "name": "default",
                        "label": "Default",
                        "width": "115px",
                        "formatter": function(def,opt,row){
                            return (def.length > 0) ? '<span class="attrTabBtn" style="cursor:pointer;" rowId="' + opt.rowId + '" tab="default">' + def + '</span>' : '';
                         }
                    },       
                    {  
                        "name": "script",
                        "label": "Transform Script",
                        "width": "150px",
                        "formatter": function(script,opt,row){
                            return (script.length > 0) ? '<span class="attrTabBtn" style="cursor:pointer;" rowId="' + opt.rowId + '" tab="transform">' + script + '</span>' : '';
                         }
                    }, 
                    {  
                        "name": "hasConditionScript",
                        "label": "&nbsp;",
                        "width": "25px",
                        "align": "center",
                        "title": false,
                        "formatter": function(hasConditionScript,opt,row){
                            return (hasConditionScript) ? '<button class="glyph-icon glyph-icon-filter attrTabBtn" style="height:17px;margin-bottom:4px;" rowId="' + opt.rowId + '" title="' + $.t('templates.mapping.conditionScriptApplied') + '"  tab="condition">&nbsp;&nbsp;&nbsp;</button>' : '';
                         }
                    }/*,
                    {  
                        "name": "sample",
                        "label": "Sample",
                        "width": "175px"
                    }*/
                ],
                cmTemplate: {sortable:false},
                onSelectRow: function(id){
                    if(id !== "blankRow"){
                        eventManager.sendEvent(constants.ROUTE_REQUEST, {routeName: "editMappingProperty", args: [_this.parent.currentMapping().name, id]});
                    }
                },
                beforeSelectRow: function(rowid, e) {
                    //this prevents the row from being selected if the remove button is clicked
                    var btn = $('.removeBtn', e.target);
                    if (e.target.tagName.toUpperCase() === "BUTTON" || btn.length > 0) {
                        return false;
                    }
                    return true;
                },
                loadComplete: function(data){
                    if (!data.rows.length) {
                        $('#mappingTable').addRowData("blankRow", {"required":true,"target":$.t("templates.mapping.noPropertiesMapped"), "default":"", "script":"", "hasConditionScript":false});
                        _this.parent.$el.find("#findSampleSource").parent().hide();
                   }
                }
            }).jqGrid('sortableRows',{
                update: function(ev,ui){
                    var item = ui.item[0],
                        index = item.rowIndex;
                        
                }
            });
        },
        checkAvailableProperties: function(){
            var _this = this,
                availableProps;
            
            availableProps = sessionStorageDelegate.get(this.parent.currentMapping().name + "_AvailableObjects").target.properties || [];
            
            if(!availableProps.length || _.difference(availableProps,_.pluck(_this.data.mapProps,"target")).length) {
                _this.$el.find('.addProperty').removeProp('disabled');
                _this.$el.find('#allPropertiesMapped').hide();
            } else {
                _this.$el.find('.addProperty').prop('disabled',true);
                _this.$el.find('#allPropertiesMapped').show();
            }
        },
        render: function(parent, callback) {
            this.parent = parent;
            
            this.parentRender(_.bind(function () {
                this.loadMappingPropertiesGrid();
                this.checkAvailableProperties();
                if(callback){
                    callback();
                }
            }, this));
        }
    });

    return new PropertiesView();
});

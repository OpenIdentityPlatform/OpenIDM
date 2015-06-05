/**
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2011-2015 ForgeRock AS. All rights reserved.
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

/*global define, $, _, JSONEditor */

/**
 * @author huck.elliott
 */
define("org/forgerock/openidm/ui/common/resource/GenericEditResourceView", [
    "org/forgerock/commons/ui/common/main/AbstractView",
    "org/forgerock/commons/ui/common/main/EventManager",
    "org/forgerock/commons/ui/common/util/Constants",
    "org/forgerock/commons/ui/common/util/UIUtils",
    "org/forgerock/openidm/ui/common/delegates/ResourceDelegate",
    "org/forgerock/openidm/ui/common/delegates/SearchDelegate",
    "org/forgerock/commons/ui/common/components/Messages",
    "org/forgerock/openidm/ui/common/resource/ResourceCollectionArrayView",
    "org/forgerock/openidm/ui/common/resource/ResourceCollectionRelationshipsView",
    "org/forgerock/openidm/ui/common/util/ResourceCollectionUtils",
    "org/forgerock/openidm/ui/common/linkedView/LinkedView",
    "org/forgerock/commons/ui/common/main/Router"
], function(
        AbstractView, 
        eventManager, 
        constants, 
        uiUtils, 
        resourceDelegate, 
        searchDelegate, 
        messagesManager, 
        ResourceCollectionArrayView, 
        ResourceCollectionRelationshipsView, 
        resourceCollectionUtils, 
        LinkedView, 
        router
    ) {
    var EditResourceView = AbstractView.extend({
        template: "templates/admin/resource/EditResourceViewTemplate.html",
        
        events: {
            "click #saveBtn": "save",
            "click #backBtn": "backToList",
            "click #deleteBtn": "deleteObject",
            "click #resetBtn": "reset"
        },
        render: function(args, callback) {
            var resourceReadPromise,
                schemaPromise = resourceDelegate.getSchema(args),
                objectId = (args[0] === "managed") ? args[2] : args[3],
                displayField;
            
            this.data.args = args;
            
            this.data.objectType = args[0];
            this.isSystemResource = false;
            this.objectName = args[1];
            this.data.serviceUrl = resourceDelegate.getServiceUrl(args);

            if(objectId){
                resourceReadPromise = resourceDelegate.readResource(this.data.serviceUrl, objectId);
                this.objectId = objectId;
                this.data.newObject = false;
            } else {
                resourceReadPromise = $.Deferred().resolve({});
                this.data.newObject = true;
            }
            
            if (this.data.objectType === "system") {
                this.isSystemResource = true;
                this.objectName += "/" + args[2];
            }
            
            $.when(resourceReadPromise, schemaPromise).then(_.bind(function(resource, schema){
                this.data.objectTitle = schema.title || this.objectName;
                
                this.data.schema = schema;
                
                if(this.isSystemResource) {
                    this.data.objectTitle = this.objectName;
                }
                
                if(!this.data.newObject) {
                    if(this.isSystemResource) {
                        displayField = _.chain(schema.properties)
                                        .map(function(val, key) { val.name = key; return val; })
                                        .where({ nativeName: "__NAME__" })
                                        .value();
                        
                        if(displayField) {
                            displayField = displayField[0].name;
                        } else {
                            displayField = _.keys(schema.properties)[0];
                        }
                    } else {
                        displayField = schema.order[0];
                    }
                    
                    this.data.objectDisplayText = resource[0][displayField];
                }
                
                this.data.backBtnText = $.t("templates.admin.ResourceEdit.backToList",{ objectTitle: this.data.objectTitle });
                
                this.parentRender(function(){
                    this.setupEditor(resource, schema);
                    
                    if(!this.data.newObject) {
                        this.linkedView = new LinkedView();
                        this.linkedView.element = "#linkedView";

                        this.linkedView.render({id: resource[0]._id, resourcePath: this.data.objectType + "/" + this.objectName + "/" });
                    }
                    
                    if(callback) {
                        callback();
                    }
                });
            },this));
        },
        setupEditor: function(resource, schema){
            var propCount = 0,
                filteredProperties,
                filteredObject = resource[0];
            
            this.oldObject = $.extend(true, {}, filteredObject);
            
            filteredProperties = _.omit(schema.properties,function(p) { return !p.viewable; });
            
            if(!_.isEmpty(filteredProperties)){
                filteredObject = _.pick(filteredObject, _.keys(filteredProperties));
            }
            
            JSONEditor.defaults.options = {
                    theme: "bootstrap3",
                    iconlib: "fontawesome4",
                    disable_edit_json: true,
                    disable_array_reorder: true,
                    disable_collapse: true,
                    disable_properties: true,
                    show_errors: "never",
                    formHorizontal: true
            };
            
            if(schema.order){
                _.each(schema.order, _.bind(function(prop){
                    schema.properties[prop].propertyOrder = propCount++;
                    if(schema.properties[prop].viewable && !_.has(filteredObject, prop)){
                        filteredObject[prop] = null;
                    }
                }, this));
            }
            
            if (this.isSystemResource) {
                schema.title = this.data.objectTitle;
                if (this.data.newObject) {
                    _.each(schema.properties,function(p) {
                        p.required = true;
                    });
                }
            }

            this.editor = new JSONEditor(document.getElementById("resource"), { schema: schema });
            this.editor.setValue(filteredObject);
            this.addTooltips();
            this.convertResourceCollectionFields(filteredObject,schema);
            
            this.editor.on('change', _.bind(function() {
                this.showPendingChanges();
            }, this));
        },
        showPendingChanges : function() {
            var changedFields = [],
                newValue = _.extend({},this.oldObject, this.getFormValue());
            
            if(_.isEqual(newValue, this.oldObject)) {
                this.$el.find("#saveBtn").attr("disabled", true);
                this.$el.find("#resetBtn").attr("disabled", true);
                this.$el.find("#resourceChangesPending").hide();
            } else {
                if(!this.data.newObject) {
                    _.each(newValue, _.bind(function(val,key) {
                        if((!this.oldObject[key] && val.length) || (this.oldObject[key] && !_.isEqual(this.oldObject[key], val))) {
                            if(this.data.schema.properties && this.data.schema.properties[key] && this.data.schema.properties[key].title && this.data.schema.properties[key].title.length) {
                                changedFields.push(this.data.schema.properties[key].title);
                            } else {
                                changedFields.push(key);
                            }
                        }
                    }, this));
                    
                    if(changedFields.length) {
                        this.$el.find("#changedFields").html("<br/>- " + changedFields.join("<br/>- "));

                        this.$el.find("#saveBtn").removeAttr("disabled");
                        this.$el.find("#resetBtn").removeAttr("disabled");
                        
                        this.$el.find("#resourceChangesPending").show();
                    } else {
                        this.$el.find("#resourceChangesPending").hide();
                    }
                } else {
                    this.$el.find("#saveBtn").removeAttr("disabled");
                    this.$el.find("#resetBtn").removeAttr("disabled");
                    
                }
            }
        },
        /* To accommodate a popover the addTooltips function transforms the following html:
         * 
         * <div class=" form-group">
         *      <label class=" control-label">Username</label>
         *      <input type="text" class="form-control" name="root[userName]">
         *      <p class="help-block">The Username</p>
         * </div>
         * 
         * into:
         * 
         * <div class=" form-group">
         *      <label class=" control-label">Username</label> <i class="fa fa-info-circle info" title="" data-original-title="The Username"></i>
         *      <input type="text" class="form-control" name="root[userName]">
         *      <p class="help-block"></p>
         * </div>
         * 
         */
        addTooltips: function(){
            var propertyDescriptionSpan = this.$el.find("p.help-block"),
                objectHeader = this.$el.find("#resource").find("h3:eq(0)"),
                objectDescriptionSpan = objectHeader.next();
            
            $.each(propertyDescriptionSpan, function(){
                $(this).parent().find("label").after(' <i class="fa fa-info-circle info" title="' + $(this).text() + '"/>');
                $(this).empty();
            });
            
            if(objectDescriptionSpan.text().length > 0){
                objectHeader.append('<i class="fa fa-info-circle info" title="' + objectDescriptionSpan.text() + '"/>');
                objectDescriptionSpan.empty();
            }
            
            this.$el.find(".info").popover({
                content: function () { return $(this).attr("data-original-title");},
                trigger:'hover',
                placement:'top',
                container: 'body',
                html: 'true',
                template: '<div class="popover popover-info" role="tooltip"><div class="popover-content"></div></div>'
            });
        },
        getFormValue : function() {
            var formVal = this.editor.getValue();
            
            if(!this.data.newObject){
                /*
                The following _.each() was placed here to account for JSONEditor.setValue() 
                turning a property that exists but has a null value into an empty text field. 
                Upon calling JSONEditor.getValue() the previously null property will be set to and empty string.
                
                This loop filters out previously null values that have not been changed.
                */
                _.each(_.keys(formVal), function(key){
                    if(!_.has(this.oldObject, key) && (!formVal[key] || !formVal[key].length)){
                        delete formVal[key];
                    }
                }, this);
                
                _.each(this.$el.find(".resourceCollectionArrayValue"), function(element) {
                    var propName = $(element).attr("propName"),
                        propVal = $(element).val().split(",");
                    
                    if($(element).val().length) {
                        formVal[propName] = propVal;
                    } else {
                        formVal[propName] = [];
                    }
                }); 
            }
            
            return formVal;
        },
        save: function(e, callback){
            var formVal = this.getFormValue(),
                successCallback = _.bind(function(editedObject){
                    var msg = (this.data.newObject) ? "templates.admin.ResourceEdit.addSuccess" : "templates.admin.ResourceEdit.editSuccess",
                        editRouteName = (!this.isSystemResource) ? "adminEditManagedObjectView" : "adminEditSystemObjectView";
                    
                    messagesManager.messages.addMessage({"message": $.t(msg,{ objectTitle: this.data.objectTitle })});
                    this.data.editedObject = editedObject;
                    
                    if(this.data.newObject) {
                        this.data.args.push(editedObject._id);
                        eventManager.sendEvent(constants.EVENT_CHANGE_VIEW, {route: router.configuration.routes[editRouteName], args: this.data.args, callback: callback});
                    } else {
                        this.render(this.data.args,callback);
                    }
                }, this);
            
            if(e) {
                e.preventDefault();
            }
            
            if(this.data.newObject){
                resourceDelegate.createResource(this.data.serviceUrl, formVal._id, formVal, successCallback);
            } else { 
                if (!this.isSystemResource) {
                    resourceDelegate.patchResourceDifferences(this.data.serviceUrl, {id: this.oldObject._id, rev: this.oldObject._rev}, this.oldObject, formVal, successCallback);
                } else {
                    resourceDelegate.updateResource(this.data.serviceUrl, this.oldObject._id, formVal, successCallback);
                }
            }
        },
        backToList: function(e){
            var routeName = (!this.isSystemResource) ? "adminListManagedObjectView" : "adminListSystemObjectView";
            
            if(e){
                e.preventDefault();
            }
            
            eventManager.sendEvent(constants.ROUTE_REQUEST, {routeName: routeName, args: this.data.args});
        },
        reset: function(e){
            e.preventDefault();

            this.render(this.data.args);
        },
        deleteObject: function(e, callback){
            if (e) {
                e.preventDefault();
            }
            
            uiUtils.jqConfirm($.t("templates.admin.ResourceEdit.confirmDelete",{ objectTitle: this.data.objectTitle }), _.bind(function(){
                resourceDelegate.deleteResource(this.data.serviceUrl, this.objectId, _.bind(function(){
                    messagesManager.messages.addMessage({"message": $.t("templates.admin.ResourceEdit.deleteSuccess",{ objectTitle: this.data.objectTitle })});
                    this.backToList();
                    if (callback) {
                        callback();
                    }
                }, this));
            }, this));
        },
        convertResourceCollectionFields: function(filteredObject,schema){
                var _this = this,
                    getFields,
                    convertField,
                    convertArrayField,
                    showRelationships,
                    addTab;
                
                getFields = function(properties, parent){
                    _.each(properties,function(prop,key){
                        prop.propName = key;
                        if(prop.type === "object"){
                            if(parent){
                                parent += "\\." + key;
                            } else {
                                parent = "\\." + key;
                            }
                            getFields(prop.properties, parent);
                        }
                        
                        if(parent){
                            prop.selector =  parent + "\\." + key;
                        } else {
                            prop.selector = "\\." + key;
                        }
                        
                        if(prop.type === "array") {
                            if(prop.items.resourceCollection && _.has(filteredObject,key)) {
                                prop.value = filteredObject[key];
                                convertArrayField(prop);
                            }
                        }
                        if(prop.resourceCollection){
                            convertField(prop);
                            
                            if(_this.data.objectType + "/" + _this.objectName === prop.resourceCollection.path && prop.resourceCollection.label && prop.resourceCollection.label.length) {
                                prop.parentId = prop.resourceCollection.path + "/" + _this.objectId;
                                prop.parentValue = _this.oldObject;
                                showRelationships(prop);
                            }
                        }
                    });
                };
                
                convertField = function(field){
                    var el = $("#0\\.root" + field.selector),
                        autocompleteID = "JSONEditorAutocomplete_" + field.selector.replace("\\",""),
                        autocompleteField = $('<select class="form-control selectize" type="text" style="display:none !important;" id="' + autocompleteID + '"></select>'),
                        onChange = function (value) {
                            var readPath = field.resourceCollection.path + "/" + value;
                            _this.editor.getEditor("root" + field.selector.replace("\\","")).setValue(readPath);
                        };
                    
                    el.attr("style","display: none !important").after(autocompleteField);
                    
                    resourceCollectionUtils.setupAutocompleteField(autocompleteField, field, { onChange: onChange });
                    
                    if(!_this.data.newObject && el.val().length){
                        resourceDelegate.readResource("/" + constants.context,el.val()).then(function(result){
                            autocompleteField[0].selectize.addOption(result);
                            autocompleteField[0].selectize.setValue(result._id);
                        });
                    }
                };
                
                convertArrayField = function(prop) {
                    _this.editor.getEditor('root' + prop.selector.replace("\\","")).destroy();
                    addTab(prop, {
                        templateId : "tabContentTemplate",
                        tabView: new ResourceCollectionArrayView(),
                        viewId: "resourceCollectionArray-" + prop.propName,
                        contentId: "resource-" + prop.propName,
                        contentClass: "resourceCollectionArray",
                        headerText: prop.title,
                        onChange: _.bind(_this.showPendingChanges, _this)
                    });
                };
                
                showRelationships = function(prop) {
                    addTab(prop, {
                        templateId : "relationshipsTemplate",
                        tabView: new ResourceCollectionRelationshipsView(),
                        viewId: "resourceCollectionRelationship-" + prop.propName,
                        contentId: "relationship-" + prop.propName,
                        contentClass: "resourceCollectionRelationships",
                        headerText: prop.resourceCollection.label
                    });
                };
                
                addTab = function(prop, opts) {
                    var tabHeader = _this.$el.find("#tabHeaderTemplate").clone(),
                        tabContent = _this.$el.find("#" + opts.templateId).clone();
                    
                    if(!_this.data.newObject) {
                        tabHeader.attr("id", "tabHeader_" + opts.contentId);
                        tabHeader.find("a").attr("href","#" + opts.contentId).text(opts.headerText);
                        
                        tabContent.attr("id",opts.contentId);
                        tabContent.find("." + opts.contentClass).attr("id", opts.viewId);
                        
                        _this.$el.find("#linkedSystemsTabHeader").before(tabHeader);
                        _this.$el.find("#resource-linkedSystems").before(tabContent);
                        
                        opts.tabView.render({ element: "#" + opts.viewId, prop: prop, schema: schema, onChange: opts.onChange });
                    }
                };
                
                getFields(schema.properties);
        }
    }); 
    
    return new EditResourceView();
});



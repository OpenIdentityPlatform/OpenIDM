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
    "org/forgerock/commons/ui/common/components/Messages"
], function(AbstractView, eventManager, constants, uiUtils, resourceDelegate, messagesManager) {
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
                objectId = (args[0] === "managed") ? args[2] : args[3];
            
            this.data.args = args;
            
            this.data.objectType = args[0];
            this.isSystemResource = false;
            this.objectName = args[1];

            if(objectId){
                resourceReadPromise = resourceDelegate.readEntity(objectId);
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
                
                if(this.isSystemResource) {
                    this.data.objectTitle = this.objectName;
                }
                
                this.parentRender(function(){
                    this.setupEditor(resource, schema);
                });
            },this));
        },
        setupEditor: function(resource, schema){
            var propCount = 0,
                filteredProperties,
                filteredObject = resource[0];
            
            this.oldObject = filteredObject;
            
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
                    show_errors: "never"
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
        },
        /* To accomodate a popover the addTooltips function transforms the following html:
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
        save: function(e){
            var formVal = this.editor.getValue(),
                successCallback = _.bind(function(){
                    var msg = (this.data.newObject) ? "templates.admin.ResourceEdit.addSuccess" : "templates.admin.ResourceEdit.editSuccess";
                    messagesManager.messages.addMessage({"message": $.t(msg,{ objectTitle: this.data.objectTitle })});
                    this.backToList();
                }, this);
            
            e.preventDefault();
            
            if(this.data.newObject){
                resourceDelegate.createEntity(formVal._id, formVal, successCallback);
            } else {
                /*
                The following _.each() was placed here to account for JSONEditor.setValue() 
                turning a property that exists but has a null value into an empty text field. 
                Upon calling JSONEditor.getValue() the previously null property will be set to and empty string.
                
                This loop filters out previously null values that have not been changed.
                */
                _.each(_.keys(formVal), function(key){
                    if(!_.has(this.oldObject, key) && !formVal[key].length){
                        delete formVal[key];
                    }
                }, this);
                
                if (!this.isSystemResource) {
                    resourceDelegate.patchEntityDifferences({id: this.oldObject._id, rev: this.oldObject._rev}, this.oldObject, formVal, successCallback);
                } else {
                    resourceDelegate.updateEntity(this.oldObject._id, formVal, successCallback);
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
        deleteObject: function(e){
            e.preventDefault();
            
            uiUtils.jqConfirm($.t("templates.admin.ResourceEdit.confirmDelete",{ objectTitle: this.data.objectTitle }), _.bind(function(){
                resourceDelegate.deleteEntity(this.objectId, _.bind(function(){
                    messagesManager.messages.addMessage({"message": $.t("templates.admin.ResourceEdit.deleteSuccess",{ objectTitle: this.data.objectTitle })});
                    this.backToList();
                }, this));
            }, this));
        }
    }); 
    
    return new EditResourceView();
});



/**
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2011-2014 ForgeRock AS. All rights reserved.
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
define("org/forgerock/openidm/ui/common/managed/EditManagedObjectView", [
    "org/forgerock/commons/ui/common/main/AbstractView",
    "org/forgerock/commons/ui/common/main/EventManager",
    "org/forgerock/commons/ui/common/util/Constants",
    "org/forgerock/commons/ui/common/util/UIUtils",
    "org/forgerock/openidm/ui/common/delegates/ManagedObjectDelegate",
    "org/forgerock/commons/ui/common/components/Messages"
], function(AbstractView, eventManager, constants, uiUtils, managedObjectDelegate, messagesManager) {
    var EditManagedObjectView = AbstractView.extend({
        template: "templates/admin/managed/EditManagedObjectViewTemplate.html",
        
        events: {
            "click #saveBtn": "save",
            "click #backBtn": "backToList",
            "click #deleteBtn": "deleteObject",
            "click #resetBtn": "reset"
        },
        render: function(args, callback) {
            var managedObjectPromise,
                schemaPromise = managedObjectDelegate.getSchema(args[0]);

            if(args[1]){
                managedObjectPromise = managedObjectDelegate.readEntity(args[1]);
                this.objectId = args[1];
                this.data.newObject = false;
            } else {
                managedObjectPromise = {};
                this.data.newObject = true;
            }
            this.objectName = args[0];
            
            $.when(managedObjectPromise, schemaPromise).then(_.bind(function(managedObject, schema){
                this.data.objectTitle = schema.title || this.objectName;
                this.parentRender(function(){
                    this.setupEditor(managedObject, schema);
                });
            },this));
        },
        setupEditor: function(managedObject, schema){
            var propCount = 0,
                filteredProperties,
                filteredObject = managedObject[0];
            
            this.oldObject = filteredObject;
            
            filteredProperties = _.omit(schema.properties,function(p) { return !p.viewable; });
            
            if(!_.isEmpty(filteredProperties)){
                filteredObject = _.pick(filteredObject, _.keys(filteredProperties));
            }
            
            JSONEditor.defaults.options = {
                    disable_edit_json: true,
                    disable_array_reorder: true,
                    disable_collapse: true,
                    disable_properties: true,
                    show_errors: "never",
                    template: 'handlebars',
                    theme: 'jqueryui'
            };
            
            if(schema.order){
                _.each(schema.order, _.bind(function(prop){
                    schema.properties[prop].propertyOrder = propCount++;
                    if(schema.properties[prop].viewable && !_.has(filteredObject, prop)){
                        filteredObject[prop] = null;
                    }
                }, this));
            }
            
            this.editor = new JSONEditor(document.getElementById("managedObject"), { schema: schema });
            this.editor.setValue(filteredObject);
            this.addTooltips();
        },
        addTooltips: function(){
            var propertyDescriptionSpan = this.$el.find(".form-control span"),
                objectHeader = this.$el.find("#managedObject").find("h3:eq(0)"),
                objectDescriptionSpan = objectHeader.next();
            
            $.each(propertyDescriptionSpan, function(){
                $(this).after('<i class="fa fa-info-circle info" title="' + $(this).text() + '"/>');
                $(this).empty();
            });
            
            if(objectDescriptionSpan.text().length > 0){
                objectHeader.append('<i class="fa fa-info-circle info" title="' + objectDescriptionSpan.text() + '"/>');
                objectDescriptionSpan.empty();
            }
            
            this.$el.find(".info").tooltip({
                tooltipClass: "idm-tooltip",
                position : { my: 'right center', at: 'left-35 center' }
            });
        },
        save: function(e){
            var formVal = this.editor.getValue();
            
            e.preventDefault();
            
            if(this.data.newObject){
                managedObjectDelegate.createEntity(null, formVal, _.bind(function(user){
                    messagesManager.messages.addMessage({"message": $.t("templates.admin.ManagedObjectEdit.addSuccess",{ objectTitle: this.data.objectTitle })});
                    this.backToList();
                }, this));
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
                
                managedObjectDelegate.patchEntityDifferences({id: this.oldObject._id, rev: this.oldObject._rev}, this.oldObject, formVal, _.bind(function(){
                    messagesManager.messages.addMessage({"message": $.t("templates.admin.ManagedObjectEdit.editSuccess",{ objectTitle: this.data.objectTitle })});
                    this.backToList();
                },this));
            }
        },
        backToList: function(e){
            if(e){
                e.preventDefault();
            }
            
            eventManager.sendEvent(constants.ROUTE_REQUEST, {routeName: "adminListManagedObjectView", args: [this.objectName]});
        },
        reset: function(e){
            e.preventDefault();
            
            this.render([this.objectName,this.objectId]);
        },
        deleteObject: function(e){
            e.preventDefault();
            
            uiUtils.jqConfirm($.t("templates.admin.ManagedObjectEdit.confirmDelete",{ objectTitle: this.data.objectTitle }), _.bind(function(){
                managedObjectDelegate.deleteEntity(this.objectId, _.bind(function(){
                    messagesManager.messages.addMessage({"message": $.t("templates.admin.ManagedObjectEdit.deleteSuccess",{ objectTitle: this.data.objectTitle })});
                    this.backToList();
                }, this));
            }, this));
        }
    }); 
    
    return new EditManagedObjectView();
});



/**
 * The contents of this file are subject to the terms of the Common Development and
 * Distribution License (the License). You may not use this file except in compliance with the
 * License.
 *
 * You can obtain a copy of the License at legal/CDDLv1.0.txt. See the License for the
 * specific language governing permission and limitations under the License.
 *
 * When distributing Covered Software, include this CDDL Header Notice in each file and include
 * the License file at legal/CDDLv1.0.txt. If applicable, add the following below the CDDL
 * Header, with the fields enclosed by brackets [] replaced by your own identifying
 * information: "Portions copyright [year] [name of copyright owner]".
 *
 * Copyright 2015 ForgeRock AS.
 */

/*global define */

define("org/forgerock/openidm/ui/common/linkedView/LinkedView", [
    "jquery",
    "underscore",
    "jsonEditor",
    "org/forgerock/commons/ui/common/main/AbstractView",
    "org/forgerock/openidm/ui/common/delegates/ResourceDelegate",
    "org/forgerock/openidm/ui/common/util/ResourceCollectionUtils"
], function($, _, JSONEditor, AbstractView, resourceDelegate, resourceCollectionUtils) {

    var LinkedView = AbstractView.extend({
        template: "templates/admin/linkedView/LinkedView.html",
        events: {
            "change #linkedViewSelect": "changeResource"
        },
        editors: {},

        render: function(args, callback) {
            resourceDelegate.linkedView(args.id, args.resourcePath).then(_.bind(function(linkedData){

                this.data.linkedData = linkedData;
                this.data.linkedResources = [];

                _.each(this.data.linkedData.linkedTo, function(resource, index){
                    this.data.linkedResources.push(this.cleanLinkName(resource.resourceName, resource.linkQualifier));

                    //This second loop is to ensure that null returned first level values actually display in JSON editor
                    //Without this it will not display the textfields
                    _.each(resource.content, function(attribute, key){
                        if(attribute === null) {
                            this.data.linkedData.linkedTo[index].content[key] = "";
                        }
                    }, this);

                }, this);

                this.parentRender(_.bind(function() {
                    this.loadEditor("all");

                    if(callback) {
                        callback();
                    }
                }, this));
            }, this));
        },

        cleanLinkName: function(name, linkQualifier){
            var cleanName = name.split("/");
            
            cleanName.pop();

            cleanName = cleanName.join("/");

            if(linkQualifier) {
                cleanName = cleanName +" - " +linkQualifier;
            }

            return cleanName;
        },

        changeResource: function(event) {
            event.preventDefault();

            this.loadEditor($(event.target).val());
        },

        loadEditor: function(selectedIndex) {
            var linkToResource = "#resource/",
                resourceId,
                displayEditor = _.bind(function (selection) {
                    if(this.editors[selection]) {
                        this.editors[selection].destroy();
                    }
    
                    if(this.data.linkedData.linkedTo.length > 0) {
    
                        this.$el.closest(".container").find("#linkedSystemsTabHeader").show();
    
                        if (this.data.linkedData.linkedTo[selection].content !== null) {
                            resourceId = _.last(this.data.linkedData.linkedTo[selection].resourceName.split("/"));
                            linkToResource += this.data.linkedData.linkedTo[selection].resourceName.replace(resourceId, "edit/" + resourceId);
    
                            this.$el.find("#linkToResource").attr("href",linkToResource);
    
                            resourceDelegate.getSchema(this.data.linkedData.linkedTo[selection].resourceName.split("/")).then(_.bind(function(schema) {
                                var propCount = 0;
                                if(schema.order) {
                                    _.each(schema.order, function(prop) {
                                        schema.properties[prop].propertyOrder = propCount++;
                                    });
                                }
                                
                                schema.properties = resourceCollectionUtils.convertRelationshipTypes(schema.properties);
                                
                                if (schema.allSchemas) {
                                    delete schema.allSchemas;
                                }
    
                                this.editors[selection] = new JSONEditor(
                                        this.$el.find("#linkedViewContent")[0],
                                        {
                                            theme: "bootstrap3",
                                            iconlib: "fontawesome4",
                                            disable_edit_json: true,
                                            disable_properties: true,
                                            disable_array_delete: true,
                                            disable_array_reorder: true,
                                            disable_array_add: true,
                                            schema: schema,
                                            horizontalForm: true
                                        }
                                    );
    
                                    if (this.data.linkedData.linkedTo[selection].content._id) {
                                        delete this.data.linkedData.linkedTo[selection].content._id;
                                    }
                                    
                                    _.each(this.data.linkedData.linkedTo[selection].content, function(value, key) {
                                        if(_.isArray(value) && value.length === 0) {
                                            this.data.linkedData.linkedTo[selection].content[key] = undefined;
                                        }
                                    }, this);
    
                                    this.editors[selection].setValue(this.data.linkedData.linkedTo[selection].content);
    
                                    this.$el.find(".row select").hide();
                                    this.$el.find(".row input").attr("disabled", true);
                            }, this));
                        } else {
                            this.$el.find("#linkedViewContent").text($.t("templates.admin.LinkedTemplate.recordMissing") + ': ' + this.data.linkedData.linkedTo[selection].resourceName);
                        }
                    }
                }, this);
            
            if (selectedIndex === "all") {
                this.$el.find("#linkToResource").hide();
                _.each(this.data.linkedResources, function (resource, index) {
                    displayEditor(index);
                });
            } else {
                this.$el.find("#linkToResource").show();
                displayEditor(selectedIndex);
            }
        }
    });

    return LinkedView;
});

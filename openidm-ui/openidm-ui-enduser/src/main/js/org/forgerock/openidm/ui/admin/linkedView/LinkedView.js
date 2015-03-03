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

/*global define, $, _, JSONEditor */

define("org/forgerock/openidm/ui/admin/linkedView/LinkedView", [
    "org/forgerock/commons/ui/common/main/AbstractView",
    "UserDelegate"
], function(AbstractView, userDelegate) {

    var LinkedView = AbstractView.extend({
        template: "templates/admin/linkedView/LinkedView.html",
        events: {
            "change #linkedViewSelect": "changeResource"
        },

        render: function(args, callback) {
            userDelegate.userLinkedView(args.id).then(_.bind(function(linkedData){

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
                    this.loadEditor(0);

                    if(callback) {
                        callback();
                    }
                }, this));
            }, this));
        },

        cleanLinkName: function(name, linkQualifier){
            var cleanName = name.split("/");

            if(cleanName[0] === "system" || cleanName[0] === "managed") {
                cleanName.splice(0 ,1);
            }

            cleanName.pop();

            cleanName = cleanName.join(" ");

            if(linkQualifier) {
                cleanName = cleanName +" - " +linkQualifier;
            }

            return cleanName;
        },

        changeResource: function(event) {
            event.preventDefault();

            this.loadEditor($(event.target).val());
        },

        loadEditor: function(selection) {
            if(this.editor) {
                this.editor.destroy();
            }

            if(this.data.linkedData.linkedTo.length > 0) {

                if (this.data.linkedData.linkedTo[selection].content !== null) {
                    this.editor = new JSONEditor(
                        this.$el.find("#linkedViewContent")[0],
                        {
                            theme: "bootstrap3",
                            iconlib: "fontawesome4",
                            disable_edit_json: true,
                            disable_properties: true,
                            disable_array_delete: true,
                            disable_array_reorder: true,
                            disable_array_add: true,
                            schema: {
                                type: "object",
                                title: this.cleanLinkName(this.data.linkedData.linkedTo[selection].resourceName)
                            }
                        }
                    );

                    if (this.data.linkedData.linkedTo[selection].content._id) {
                        delete this.data.linkedData.linkedTo[selection].content._id;
                    }

                    this.editor.setValue(this.data.linkedData.linkedTo[selection].content);

                    this.$el.find("#linkedViewContent h3:first").hide();
                    this.$el.find(".row select").hide();
                    this.$el.find(".row input").attr("disabled", true);
                } else {
                    this.$el.find("#linkedViewContent").text($.t("templates.admin.LinkedTemplate.recordMissing") + ': ' + this.data.linkedData.linkedTo[selection].resourceName);
                }
            }
        }
    });

    return LinkedView;
});
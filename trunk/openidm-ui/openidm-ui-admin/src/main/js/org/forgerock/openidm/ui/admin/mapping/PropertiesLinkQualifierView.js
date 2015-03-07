/**
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2015 ForgeRock AS. All rights reserved.
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

define("org/forgerock/openidm/ui/admin/mapping/PropertiesLinkQualifierView", [
    "org/forgerock/commons/ui/common/main/AbstractView",
    "org/forgerock/openidm/ui/common/delegates/ConfigDelegate",
    "org/forgerock/commons/ui/common/util/Constants",
    "org/forgerock/commons/ui/common/main/EventManager",
    "org/forgerock/openidm/ui/admin/delegates/BrowserStorageDelegate",
    "org/forgerock/openidm/ui/admin/mapping/MappingBaseView"
], function(AbstractView,
            ConfigDelegate,
            Constants,
            EventManager,
            BrowserStorageDelegate,
            MappingBaseView) {

    var PropertiesLinkQualifierView = AbstractView.extend({
        template: "templates/admin/mapping/PropertiesLinkQualifierTemplate.html",
        element: "#mappingLinkQualifiers",
        noBaseTemplate: true,
        events: {
            "click .removeLinkQualifier": "removeLinkQualifier",
            "click .addLinkQualifier": "addLinkQualifier",
            "submit form": "addLinkQualifier"
        },
        model: {},
        data: {},

        render: function (args) {
            this.model.mappingName = args;
            this.model.mapping = MappingBaseView.currentMapping();
            this.data.linkQualifiers = this.model.mapping.linkQualifiers || [];

            if (this.data.linkQualifiers.length === 0) {
                this.data.linkQualifiers = ["default"];
                this.data.doNotDelete = true;
            } else if (this.data.linkQualifiers.length === 1) {
                this.data.doNotDelete = true;
            } else {
                this.data.doNotDelete = false;
            }


            this.parentRender(function () {});
        },

        removeLinkQualifier: function(e) {
            e.preventDefault();
            if (!this.data.doNotDelete) {
                this.data.linkQualifiers = _.without(this.data.linkQualifiers, $(e.target).closest(".removeLinkQualifier").find(".linkQualifier").text());
                this.save();
            }
        },

        addLinkQualifier: function(e) {
            e.preventDefault();
            var toAdd = this.$el.find(".newLinkQualifier").val().trim();

            if (this.isValid(toAdd) && toAdd.length > 0 ) {
                this.data.linkQualifiers.push(toAdd);
                this.save();
            }
        },

        isValid: function(toAdd) {
            if (this.data.linkQualifiers.indexOf(toAdd) === -1 ) {
                this.$el.find(".notValid").hide();
                return true;
            } else {
                this.$el.find(".notValid").show();
                return false;
            }
        },

        save: function() {
            var sync = MappingBaseView.data.syncConfig,
                mapping;

            _.each(sync.mappings, function(map, key) {
                if (map.name === this.model.mappingName) {
                    sync.mappings[key].linkQualifiers = this.data.linkQualifiers;
                    mapping = sync.mappings[key];
                }
            }, this);

            ConfigDelegate.updateEntity("sync", sync).then(_.bind(function() {
                EventManager.sendEvent(Constants.EVENT_DISPLAY_MESSAGE_REQUEST, "linkQualifierSaveSuccess");
                BrowserStorageDelegate.set("currentMapping", mapping);

                this.render(this.model.mappingName);
            }, this));
        }
    });

    return new PropertiesLinkQualifierView();
});
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

/*global define, $, _, Handlebars */

define("org/forgerock/openidm/ui/admin/connector/ConnectorView", [
    "org/forgerock/openidm/ui/admin/util/AdminAbstractView",
    "org/forgerock/commons/ui/common/main/EventManager",
    "org/forgerock/commons/ui/common/util/Constants",
    "org/forgerock/commons/ui/common/main/Router",
    "org/forgerock/openidm/ui/admin/delegates/ConnectorDelegate",
    "org/forgerock/commons/ui/common/util/UIUtils",
    "org/forgerock/openidm/ui/admin/util/ConnectorUtils",
    "org/forgerock/openidm/ui/common/delegates/ConfigDelegate"
], function(AdminAbstractView, eventManager, constants, router, ConnectorDelegate, uiUtils, connectorUtils, ConfigDelegate) {
    var ConnectorView = AdminAbstractView.extend({
        template: "templates/admin/connector/ConnectorViewTemplate.html",
        events: {
            "click .connector-delete": "deleteConnections"
        },
        render: function(args, callback) {
            //Remove when commons updates
            Handlebars.registerHelper('select', function(value, options){
                var selected = $('<select />').html(options.fn(this));
                selected.find('[value=' + value + ']').attr({'selected':'selected'});

                return selected.html();
            });

            ConnectorDelegate.currentConnectors().then(_.bind(function(connectors){
                _.each(connectors,_.bind(function(connector){
                    if(_.isUndefined(connector.error)) {
                        connector.displayName = $.t("templates.connector." +connectorUtils.cleanConnectorName(connector.connectorRef.connectorName));
                        connector.cleanUrlName = connector.config.split("/")[2];
                        connector.editable = true;
                    } else {
                        //Temp code for handling a bad connector until a better method of testing valid connectors is developed 
                        connector.displayName = $.t("templates.connector.connectorNameUnknown");
                        connector.cleanUrlName = connector.name;
                        connector.editable = false;
                        connector.errorMessage = $.t("templates.connector.connectorError");
                    }
                }, this));

                this.data = {"currentConnectors": connectors};

                this.parentRender(_.bind(function(){
                    this.$el.find(".connector-body").tooltip({
                        position: { my: "left+15 center", at: "right center" },
                        track: true
                    });
                }, this));
            }, this));
        },

        deleteConnections: function(event) {
            var selectedItems = $(event.currentTarget).parents(".connector-body");

            uiUtils.jqConfirm($.t("templates.connector.connectorDelete"), function(){
                ConfigDelegate.deleteEntity("provisioner.openicf/" +selectedItems.attr("data-connector-title")).then(function(){
                        selectedItems.remove();
                        eventManager.sendEvent(constants.EVENT_DISPLAY_MESSAGE_REQUEST, "deleteConnectorSuccess");
                    },
                    function(){
                        eventManager.sendEvent(constants.EVENT_DISPLAY_MESSAGE_REQUEST, "deleteConnectorFail");
                    });
            }, "330px");
        }
    });

    return new ConnectorView();
});


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

/*global define, $, _, Handlebars */

define("org/forgerock/openidm/ui/admin/connector/ConnectorListView", [
    "org/forgerock/openidm/ui/admin/util/AdminAbstractView",
    "org/forgerock/commons/ui/common/main/EventManager",
    "org/forgerock/commons/ui/common/util/Constants",
    "org/forgerock/commons/ui/common/main/Router",
    "org/forgerock/openidm/ui/admin/delegates/ConnectorDelegate",
    "org/forgerock/commons/ui/common/util/UIUtils",
    "org/forgerock/openidm/ui/admin/util/ConnectorUtils",
    "org/forgerock/openidm/ui/common/delegates/ConfigDelegate"
], function(AdminAbstractView,
            eventManager,
            constants,
            router,
            ConnectorDelegate,
            uiUtils,
            connectorUtils,
            ConfigDelegate) {
    var ConnectorListView = AdminAbstractView.extend({
        template: "templates/admin/connector/ConnectorListViewTemplate.html",
        events: {
            "click .connector-delete": "deleteConnections"
        },
        render: function(args, callback) {
            var connectorPromise,
                iconPromise,
                splitConfig,
                tempIconClass;

            this.data.docHelpUrl = constants.DOC_URL;

            connectorPromise = ConnectorDelegate.currentConnectors();
            iconPromise = connectorUtils.getIconList();

            $.when(connectorPromise, iconPromise).then(_.bind(function(connectors, iconList){
                _.each(connectors, _.bind(function(connector){
                    tempIconClass = connectorUtils.getIcon(connector.connectorRef.connectorName, iconList);
                    connector.iconClass = tempIconClass.iconClass;
                    connector.iconSrc = tempIconClass.src;

                    splitConfig = connector.config.split("/");

                    connector.cleanUrlName = splitConfig[1] + "_" +splitConfig[2];
                    connector.cleanEditName = splitConfig[2];

                    //remove the __ALL__ objectType
                    connector.objectTypes = _.reject(connector.objectTypes, function(ot) { return ot === "__ALL__"; });
                }, this));

                this.data.currentConnectors = connectors;


                this.parentRender(_.bind(function(){
                    if(this.$el.find(".resource-unavailable").length !== 0) {
                        this.$el.find(".resource-unavailable").tooltip({
                            tooltipClass: "resource-error-tooltip"
                        });
                    }

                    if(this.$el.find(".resource-disabled").length !== 0) {
                        this.$el.find(".resource-disabled").tooltip({
                            tooltipClass: "resource-warning-tooltip"
                        });
                    }

                    if (callback) {
                        callback();
                    }

                }, this));
            }, this));
        },

        deleteConnections: function(event) {
            var selectedItems = $(event.currentTarget).parents(".card"),
                url,
                tempConnector = _.clone(this.data.currentConnectors);

            uiUtils.jqConfirm($.t("templates.connector.connectorDelete"), _.bind(function(){

                _.each(tempConnector, function(connectorObject, index){
                    if(connectorObject.cleanUrlName === selectedItems.attr("data-connector-title")) {
                        this.data.currentConnectors.splice(index, 1);
                    }
                }, this);

                url = selectedItems.attr("data-connector-title").split("_");

                ConfigDelegate.deleteEntity(url[0] +"/" +url[1]).then(function(){
                        ConnectorDelegate.deleteCurrentConnectorsCache();
                        selectedItems.parent().parent().remove();
                        eventManager.sendEvent(constants.EVENT_DISPLAY_MESSAGE_REQUEST, "deleteConnectorSuccess");
                    },
                    function(){
                        eventManager.sendEvent(constants.EVENT_DISPLAY_MESSAGE_REQUEST, "deleteConnectorFail");
                    });
            } , this));
        }
    });

    return new ConnectorListView();
});
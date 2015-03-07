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

/*global define, $, _, window */

define("org/forgerock/openidm/ui/admin/connector/oauth/AbstractOAuthView", [
    "org/forgerock/commons/ui/common/main/AbstractView",
    "org/forgerock/commons/ui/common/main/ValidatorsManager",
    "org/forgerock/openidm/ui/common/delegates/ConfigDelegate"
], function(AbstractView, validatorsManager, ConfigDelegate) {
    var AbstractOAuthView = AbstractView.extend({
        element: "#connectorDetails",
        noBaseTemplate: true,
        oAuthConnector: true,

        buildReturnUrl: function(id, name) {
            var urlBack = window.location.protocol+"//"+window.location.host + "/admin/oauth.html",
                builtUrl = this.getAuthUrl()
                    +"?scope="+this.getScopes()
                    +"&state=" +this.data.systemType +"_" +name
                    +"&redirect_uri=" +urlBack
                    +"&response_type=code"
                    +"&client_id=" +id
                    +"&approval_prompt=force"
                    +"&access_type=offline"
                    +"&token_uri=" + this.$el.find("#OAuthTokenUrl").val();

            return builtUrl;
        },
        submitOAuth: function(mergedResult, editConnector) {
            var name = mergedResult.name,
                id = mergedResult.configurationProperties.clientId,
                url = this.buildReturnUrl(id, name);

            mergedResult.configurationProperties.domain = window.location.protocol+"//"+window.location.host;

            if(this.cleanResult) {
                mergedResult = this.cleanResult(mergedResult);
            }

            ConfigDelegate[editConnector ? "updateEntity" : "createEntity" ](this.data.systemType + "/" + mergedResult.name, mergedResult).then(_.bind(function () {
                _.delay(function () {
                    window.location = url;
                }, 1500);
            }, this));
        },

        render: function(args, callback) {
            this.template = "templates/admin/connector/oauth/" +args.connectorType +".html";

            this.data.connectorDefaults = args.connectorDefaults;
            this.data.editState = args.editState;
            this.data.systemType = args.systemType;

            if(this.connectorSpecificChanges) {
                this.connectorSpecificChanges(this.data.connectorDefaults);
            }

            this.parentRender(_.bind(function() {

                validatorsManager.bindValidators(this.$el);

                if(callback){
                    callback();
                }
            }, this));
        }
    });

    return AbstractOAuthView;
});
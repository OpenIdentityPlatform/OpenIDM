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
 * Copyright 2014-2015 ForgeRock AS.
 */

/*global define, window */

define("org/forgerock/openidm/ui/admin/connector/oauth/AbstractOAuthView", [
    "underscore",
    "org/forgerock/commons/ui/common/main/AbstractView",
    "org/forgerock/commons/ui/common/main/ValidatorsManager",
    "org/forgerock/openidm/ui/common/delegates/ConfigDelegate"
], function(_, AbstractView, validatorsManager, ConfigDelegate) {
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
        },
        //This function returns false for all OAuth
        //For now all OAuth will not function with a generic JSON Editor
        //This may change in the future, but to prevent any issues with Google and Salesforce
        //This check is needed to match the existing functionality in other connectors
        getGenericState: function() {
            return false;
        }
    });

    return AbstractOAuthView;
});

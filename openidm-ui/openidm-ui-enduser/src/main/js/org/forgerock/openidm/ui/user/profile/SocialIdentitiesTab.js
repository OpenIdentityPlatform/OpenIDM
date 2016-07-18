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
 * Copyright 2015-2016 ForgeRock AS.
 */

define([
    "jquery",
    "underscore",
    "bootstrap",
    "org/forgerock/commons/ui/user/profile/AbstractUserProfileTab",
    "org/forgerock/openidm/ui/common/delegates/SocialDelegate"
], function($, _, bootstrap,
            AbstractUserProfileTab,
            SocialDelegate) {
    var SocialIdentitiesView = AbstractUserProfileTab.extend({
        template: "templates/profile/SocialIdentitiesTab.html",
        events: _.extend({
            "change .social-toggle" : "toggleSocialProvider"
        }, AbstractUserProfileTab.prototype.events),

        /**
         Expected by all dynamic user profile tabs - returns a map of details necessary to render the nav tab
         */
        getTabDetail : function () {
            return {
                "panelId": "socialIdentities",
                "label": $.t("templates.socialIdentities.socialIdentities")
            };
        },

        model: {

        },
        render: function(args, callback) {
            SocialDelegate.providerList().then((response) => {
                this.data.providers = response.providers;

                //TODO hardcoded for this demo
                if(this.data.providers.length > 0) {
                    this.data.providers[0].primary = true;
                    this.data.providers[0].enabled = true;
                    //TODO Scopes currently not being returned by available providers call
                    this.data.providers[0].scope = [
                        "Scope 1",
                        "Scope 2",
                        "Scope 3"
                    ];
                }

                this.parentRender(callback);
            });
        },

        toggleSocialProvider: function(event) {
            event.preventDefault();

            var check = $(event.target),
                card = check.parents(".card");

            if(check.is(":checked")) {
                card.toggleClass("disabled", false);
                card.find(".social-primary").show();
            } else {
                card.toggleClass("disabled", true);
                card.find(".social-primary").hide();
            }
        }
    });

    return new SocialIdentitiesView();
});

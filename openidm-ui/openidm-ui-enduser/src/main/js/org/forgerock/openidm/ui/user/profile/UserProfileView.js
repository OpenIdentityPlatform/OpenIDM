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
    "lodash",
    "handlebars",
    "org/forgerock/commons/ui/common/main/AbstractView",
    "org/forgerock/commons/ui/user/profile/UserProfileView",
    "org/forgerock/openidm/ui/user/profile/SocialIdentitiesView"
], function($, _,
            handlebars,
            AbstractView,
            UserProfileView,
            SocialIdentitiesView) {

    var UserProfileSocial = AbstractView.extend({
        partials: UserProfileView.partials.concat([
            "partials/_socialIdentities.html"
        ]),

        parentRender: function (callback) {
            UserProfileView.parentRender.call(this, function () {
                this.$el.find(".tab-content").append(handlebars.compile("{{> _socialIdentities}}")(this.data));

                this.$el.find(".nav-tabs").append(
                    $('<li role="presentation"><a href="#socialIdentitiesTab" role="tab" data-toggle="tab">' +$.t("templates.socialIdentities.socialIdentities") +'</a></li>')
                );

                SocialIdentitiesView.render();

                _.bind(callback, this)();
            });
        }
    });

    UserProfileSocial.prototype = _.extend(Object.create(UserProfileView), UserProfileSocial.prototype);

    return new UserProfileSocial();
});
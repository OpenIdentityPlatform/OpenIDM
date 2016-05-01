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
 * Copyright 2011-2016 ForgeRock AS.
 */

define([
    "org/forgerock/commons/ui/common/main/AbstractView",
    "org/forgerock/openidm/ui/common/resource/ResourceEditViewRegistry"
], function(AbstractView, ResourceEditViewRegistry) {
    var EditResourceView = AbstractView.extend({
        events: {},
        render: function(args, callback) {
            var view,
                resource = args[1];

            if (args[0] === "system") {
                resource += "/" + args[2];
            }

            ResourceEditViewRegistry.getEditViewModule(resource).then(function (view) {
                view.render(args, callback);
            });

        }
    });

    return new EditResourceView();
});

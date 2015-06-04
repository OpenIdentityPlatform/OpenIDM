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

/*global define, _ */

define("org/forgerock/openidm/ui/admin/components/Footer", [
    "org/forgerock/commons/ui/common/main/AbstractView",
    "org/forgerock/openidm/ui/admin/delegates/InfoDelegate",
    "org/forgerock/commons/ui/common/main/Configuration"
], function(AbstractView, InfoDelegate, conf) {

    var Footer = AbstractView.extend({

        element: "#footer",
        template: "templates/admin/components/FooterTemplate.html",
        noBaseTemplate: true,

        render: function() {
            if (_.has(conf.loggedUser, "roles") && _.indexOf(conf.loggedUser.roles, "openidm-admin") > -1) {
                InfoDelegate.getVersion().then(_.bind(function(data) {
                    this.data.version = data.productVersion;
                    this.data.revision = data.productRevision;
                    this.parentRender();
                }, this));

            } else {
                this.data.version = "";
                this.parentRender();
            }
        }
    });

    return new Footer();
});
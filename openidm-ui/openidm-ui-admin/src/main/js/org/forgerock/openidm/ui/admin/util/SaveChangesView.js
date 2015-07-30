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

/*global define */

define("org/forgerock/openidm/ui/admin/util/SaveChangesView", [
    "underscore",
    "org/forgerock/commons/ui/common/main/AbstractView"
], function(_, AbstractView) {

    var SaveChangesView = AbstractView.extend({
        template: "templates/admin/util/SaveChangesTemplate.html",
        element: "#jqConfirm",
        noBaseTemplate: true,

        render: function (args, callback) {
            this.element = "#"+args.id;
            this.data.msg = args.msg;
            this.data.changes = args.changes;
            this.data.emptyText = args.empty;

            if (this.data.changes === null) {
                this.data.noGrid = true;
            }

            _.each(this.data.changes, function(change, i) {
                if (_.isEmpty(change.values)){
                    this.data.changes[i].empty = true;
                }
            }, this);

            this.parentRender(function () {
                if (callback) {
                    callback();
                }
            });
        }

    });

    return new SaveChangesView();
});

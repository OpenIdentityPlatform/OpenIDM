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
 * Copyright 2015 ForgeRock AS.
 */

/*global define */

define("org/forgerock/openidm/ui/admin/assignment/AssignmentView", [
    "jquery",
    "underscore",
    "org/forgerock/openidm/ui/admin/util/AdminAbstractView",
    "org/forgerock/openidm/ui/admin/assignment/AddAssignmentView",
    "org/forgerock/openidm/ui/admin/assignment/EditAssignmentView"
], function($, _,
            AdminAbstractView,
            AddAssignmentView,
            EditAssignmentView) {
    var AssignmentView = AdminAbstractView.extend({
        template: "templates/common/EmptyTemplate.html",

        render: function(args, callback) {
            this.parentRender(_.bind(function(){
                this.$el.append('<div id="assignmentHolder"></div>');

                if(args.length === 2) {
                    AddAssignmentView.render(args);
                } else {
                    EditAssignmentView.render(args);
                }

                if(callback) {
                    callback();
                }
            },this));
        }
    });

    return new AssignmentView();
});

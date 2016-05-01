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
    "form2js",
    "org/forgerock/openidm/ui/admin/util/AdminAbstractView",
    "org/forgerock/commons/ui/common/main/ValidatorsManager",
    "org/forgerock/openidm/ui/common/delegates/ConfigDelegate",
    "org/forgerock/openidm/ui/common/delegates/ResourceDelegate",
    "org/forgerock/commons/ui/common/main/EventManager",
    "org/forgerock/commons/ui/common/util/Constants"
], function($, _,
            form2js,
            AdminAbstractView,
            ValidatorsManager,
            ConfigDelegate,
            ResourceDelegate,
            EventManager,
            Constants) {
    var AddAssignmentView = AdminAbstractView.extend({
        template: "templates/admin/assignment/AddAssignmentViewTemplate.html",
        element: "#assignmentHolder",
        events: {
            "click #addAssignment" : "addAssignment",
            "onValidate": "onValidate"
        },
        data: {

        },
        model: {

        },
        render: function(args, callback) {
            ConfigDelegate.readEntity("sync").then(_.bind(function(sync) {
                this.data.mappings = sync.mappings;
                this.model.serviceUrl = ResourceDelegate.getServiceUrl(args);
                this.model.args = args;

                this.parentRender(_.bind(function(){
                    ValidatorsManager.bindValidators(this.$el.find("#addAssignmentForm"));

                    if(callback) {
                        callback();
                    }
                },this));
            }, this));
        },

        addAssignment: function(event) {
            event.preventDefault();

            var formVal = form2js(this.$el.find('#addAssignmentForm')[0], '.', true);

            ResourceDelegate.createResource(this.model.serviceUrl, null, formVal, _.bind(function(result){
                EventManager.sendEvent(Constants.EVENT_DISPLAY_MESSAGE_REQUEST, "assignmentSaveSuccess");

                this.model.args.push(result._id);

                EventManager.sendEvent(Constants.ROUTE_REQUEST, {routeName: "adminEditManagedObjectView", args: this.model.args});
            }, this));
        }
    });

    return new AddAssignmentView();
});
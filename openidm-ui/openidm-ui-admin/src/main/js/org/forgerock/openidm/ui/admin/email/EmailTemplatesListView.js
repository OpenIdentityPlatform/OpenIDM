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
 * Copyright 2017 ForgeRock AS.
 */

define([
    "jquery",
    "lodash",
    "backgrid",
    "backbone",
    "handlebars",
    "org/forgerock/openidm/ui/admin/util/AdminAbstractView",
    "org/forgerock/commons/ui/common/main/EventManager",
    "org/forgerock/commons/ui/common/util/Constants",
    "org/forgerock/commons/ui/common/main/Router",
    "org/forgerock/openidm/ui/common/delegates/ConfigDelegate",
    "org/forgerock/openidm/ui/admin/util/BackgridUtils",
    "backgrid-paginator"
], function(
    $,
    _,
    Backgrid,
    Backbone,
    Handlebars,
    AdminAbstractView,
    eventManager,
    constants,
    router,
    ConfigDelegate,
    BackgridUtils
) {

    var EmailTemplatesListView = AdminAbstractView.extend({
        template: "templates/admin/email/EmailTemplatesListTemplate.html",
        element: "#templatesContainer",
        noBaseTemplate: true,
        events: {},
        partials: [
            "partials/email/_emailTemplateGridName.html",
            "partials/email/_emailTemplateGridStatus.html"
        ],
        render: function(args, callback) {
            ConfigDelegate.configQuery("_id sw 'emailTemplate'").then((response) => {
                let collection = new Backbone.Collection(response.result),
                    emailTemplatesGrid,
                    ClickableRow = Backgrid.Row.extend({
                        events: {
                            "click": "rowClick"
                        },
                        rowClick: function (event) {
                            event.preventDefault();
                            eventManager.sendEvent(constants.EVENT_CHANGE_VIEW, {
                                route: router.configuration.routes.emailTemplateView,
                                args: [this.model.get("_id").replace("emailTemplate/","")]
                            });
                        }
                    });
                this.parentRender(() => {
                    emailTemplatesGrid = new Backgrid.Grid({
                        className: "table backgrid table-hover",
                        row: ClickableRow,
                        columns: BackgridUtils.addSmallScreenCell([
                            {
                                name: "name",
                                sortable: false,
                                editable: false,
                                cell: Backgrid.Cell.extend({
                                    render: function () {
                                        var emailId = this.model.get("_id"),
                                            title =  emailId.slice(emailId.indexOf("/") + 1);

                                        this.$el.html(Handlebars.compile("{{> email/_emailTemplateGridName}}")({ emailId, title }));
                                        return this;
                                    }
                                })
                            },
                            {
                                name: "status",
                                sortable: false,
                                editable: false,
                                cell: Backgrid.Cell.extend({
                                    render: function () {
                                        var state = "templates.emailConfig.",
                                            className;
                                        if ( this.model.get('enabled') ) {
                                            state += "enabled";
                                            className = "text-success";
                                        } else {
                                            state += "disabled";
                                            className = "text-danger";
                                        }
                                        this.$el.html(
                                            Handlebars.compile("{{> email/_emailTemplateGridStatus}}")( { className, state } )
                                        );
                                        return this;
                                    }
                                })
                            }
                        ]),
                        collection
                    });

                    this.$el.find("#emailTemplatesGrid").append(emailTemplatesGrid.render().el);

                    if (callback) {
                        callback();
                    }
                });
            });

        }
    });

    return new EmailTemplatesListView();
});

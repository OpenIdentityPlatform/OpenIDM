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
    "backbone",
    "handlebars",
    "org/forgerock/commons/ui/common/main/AbstractModel",
    "org/forgerock/commons/ui/common/main/AbstractView",
    "org/forgerock/commons/ui/common/util/Constants",
    "org/forgerock/commons/ui/common/util/DateUtil",
    "org/forgerock/commons/ui/common/main/EventManager",
    "org/forgerock/commons/ui/common/main/ServiceInvoker"
],
function(
    $, _,
    Backbone,
    Handlebars,
    AbstractModel,
    AbstractView,
    Constants,
    DateUtil,
    EventManager,
    ServiceInvoker
) {
    var Collection,
        Notification,
        ItemView;

    Notification = AbstractModel.extend({
        url: "/openidm/endpoint/usernotifications"
    });

    Collection = Backbone.Collection.extend({
        model: Notification,
        url: Constants.host + "/openidm/endpoint/usernotifications/",
        parse: function(response) {
            return response.notifications;
        },
        initialize: function() {
            this.on("destroy", function(model) {
                EventManager.sendEvent("NOTIFICATION_DELETE", model.get("_id"));
            });

        },
        fetch: function(options) {
            var notificationPromise = ServiceInvoker.restCall(
                {
                    "url" : this.url,
                    "type" : "GET"
                }
            );

            notificationPromise.then(options.success, options.error);
        }
    });

    ItemView = AbstractView.extend({
        tagName: "li",
        attributes: {
            "class": "list-group-item"
        },
        events: {
            "click .list-item-close" : "deleteNotification"
        },

        noBaseTemplate: true,
        initialize: function(args, options) {
            this.template = args.template;
            return this.render();

        },
        render: function() {
            this.$el.empty();
            let data = _.cloneDeep(this.model.attributes);
            data = _.set(data, "createDate", DateUtil.formatDate(data.createDate, "MMMM dd, yyyy HH:mm"));
            this.$el.html(this.template(data));
            return this;
        },

        deleteNotification: function(event) {
            event.preventDefault();

            this.model.destroy({
                wait: true,
                error(model, response, options) {
                    EventManager.sendEvent(Constants.EVENT_NOTIFICATION_DELETE_FAILED);
                }
            });
        }
    });

    return { Collection, Notification, ItemView };
});

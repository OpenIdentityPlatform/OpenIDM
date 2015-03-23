/**
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2011-2012 ForgeRock AS. All rights reserved.
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

/*global define, $ */

/**
 * @author jdabrowski
 */
define("org/forgerock/openidm/ui/admin/notifications/NotificationsView", [
    "org/forgerock/commons/ui/common/components/LineTableView",
    "org/forgerock/openidm/ui/admin/notifications/NotificationViewHelper",
    "org/forgerock/openidm/ui/admin/notifications/NotificationDelegate",
    "org/forgerock/commons/ui/common/main/EventManager",
    "org/forgerock/commons/ui/common/util/Constants",
    "org/forgerock/commons/ui/common/main/Configuration",
    "org/forgerock/commons/ui/common/util/DateUtil"
], function(LineTableView, notificationViewHelper, notificationDelegate, eventManager, constants, conf,dateUtil) {
    var NotificationsView = LineTableView.extend({

        typeToIconMapping: [],

        events: {
            "click a[name=deleteLink]" : "deleteLink",
            "click a[name=moreItems]" : "moreItems",
            "click a[name=title]" : "title",
            "mouseleave #itemsView" : "closeOpenItems"
        },

        generateItemView: function(item) {
            var iconLink, message, requester, requestDate, requestDateString, deleteLink, id, notType;

            notType = notificationViewHelper.notificationTypes[item.notificationType];
            if (notType) {
                iconLink = notificationViewHelper.notificationTypes[item.notificationType].iconPath;
            }

            message = item.message;
            requester = item.requester;
            requestDate = dateUtil.formatDate(item.createDate);

            deleteLink = '<a class="delete-icon" name="deleteLink" href="#" style="float: right;"><i class="fa fa-times"></i></a>';
            id = item._id;


            return '<div class="notification-title">'
                +  "<span><i class='message-icon fa fa-comment-o'></i></span>"
                +  deleteLink + '<a name="title" href="#">' + message + '</a>'
                + '<div style="clear: both;"></div>'
                + '</div>'
                + '<div class="notification-details" style="clear: both;">'
                + '<div class="details"> '
                + $.t("common.application.requestedBy") +': '
                + (requester ? requester : $.t("common.user.system")) + '</br>'
                + requestDate + '</div>'
                + '<input type="hidden" name="id" value=' + id + ' />'
                + '</div>';
        },

        noItemsMessage: function(item) {
            return "<h5 class='text-center'>" + $.t("openidm.ui.apps.dashboard.NotificationsView.noNotifications") + "</h5>";
        },

        seeMoreItemsMessage: function(item) {
            return $.t("openidm.ui.apps.dashboard.NotificationsView.seeMoreNotifications");
        },

        maxToShow: 0,

        getHeightForItemsNumber: function(itemsNumber) {
            return this.itemHeight * ( itemsNumber - 1 ) + this.openItemHeight;
        },

        itemHeight: 55,

        openItemHeight: 110,

        render: function(params) {
            this.parentRender(params);
            this.installAccordion();
        },

        installAccordion: function(){
            $("#items").accordion({
                event: "click",
                active: false,
                collapsible:true
            });
        },

        deleteLink: function(event) {
            var notificationId,
                self = this;
            event.preventDefault();

            notificationId = $(event.target).parent().parent().next().find("input[name=id]").val();

            notificationDelegate.deleteEntity(notificationId, function() {
                self.removeItemAndRebuild(notificationId);
                self.installAccordion();
            }, function() {
                eventManager.sendEvent(constants.EVENT_NOTIFICATION_DELETE_FAILED);
                notificationDelegate.getNotificationsForUser(function(notifications) {
                    self.items = notifications;
                    self.rebuildView();
                    self.installAccordion();
                }, function() {
                    eventManager.sendEvent(constants.EVENT_GET_NOTIFICATION_FOR_USER_ERROR);
                    self.rebuildView();
                    self.installAccordion();
                });
            });

        },

        title: function(event){
            event.preventDefault();
            $("#items").accordion("option", "activate", $(event.target).parent().index() / 2);
        },

        closeOpenItems: function(){
            $("#items").accordion( "option", "activate", false );
        }

    });

    return NotificationsView;
});
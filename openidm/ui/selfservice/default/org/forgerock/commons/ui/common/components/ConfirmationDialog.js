"use strict";

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

define(["jquery", "org/forgerock/commons/ui/common/components/Dialog", "org/forgerock/commons/ui/common/components/BootstrapDialogView"], function ($, Dialog, BootstrapDialogView) {
    var ConfirmationDialog = BootstrapDialogView.extend({
        render: function render(title, msg, actionName, okCallback) {
            this.setElement($('<div id="CommonConfirmationDialog"></div>'));
            this.title = title;
            this.message = msg;
            this.actions = [{
                label: $.t("common.form.cancel"),
                action: function action(dialogRef) {
                    dialogRef.close();
                }
            }, {
                label: actionName,
                cssClass: "btn-primary",
                action: function action(dialogRef) {
                    if (okCallback) {
                        okCallback();
                    }
                    dialogRef.close();
                }
            }];

            this.show();
        }
    });

    return new ConfirmationDialog();
});

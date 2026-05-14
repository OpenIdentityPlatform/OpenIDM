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
 * Copyright 2015-2016 ForgeRock AS.
 */

define(["underscore", "jquery", "bootstrap-dialog"], function (_, $, BootstrapDialog) {

    function forceFocus(dialog) {
        dialog.$modalContent.find("[autofocus]").focus();
    }

    function setButtonStates(dialog) {
        _.each(dialog.options.buttons, function (button) {
            if (button.disabled === true) {
                dialog.getButton(button.id).disable();
            }
        });
    }

    function onShown(event) {
        forceFocus(event.data.dialog);
        setButtonStates(event.data.dialog);
    }

    /**
     * @exports org/forgerock/commons/ui/common/components/BootstrapDialog
     */
    var obj = {};

    obj.TYPE_DEFAULT = BootstrapDialog.TYPE_DEFAULT;
    obj.TYPE_INFO = BootstrapDialog.TYPE_INFO;
    obj.TYPE_PRIMARY = BootstrapDialog.TYPE_PRIMARY;
    obj.TYPE_SUCCESS = BootstrapDialog.TYPE_SUCCESS;
    obj.TYPE_WARNING = BootstrapDialog.TYPE_WARNING;
    obj.TYPE_DANGER = BootstrapDialog.TYPE_DANGER;
    obj.SIZE_NORMAL = BootstrapDialog.SIZE_NORMAL;
    obj.SIZE_SMALL = BootstrapDialog.SIZE_SMALL;
    obj.SIZE_WIDE = BootstrapDialog.SIZE_WIDE;
    obj.SIZE_LARGE = BootstrapDialog.SIZE_LARGE;

    _.each(["show", "confirm", "warning", "danger", "success"], function (method) {

        obj[method] = function (options) {

            var dialog = new BootstrapDialog[method](options),
                type = options.type || obj.TYPE_PRIMARY;

            // Gives the dialog header the native bootstrap text color classes.
            // The title then inherits from this.
            type = type.replace("type", "text");
            dialog.getModalHeader().addClass(type);

            /**
             * Workaround for autofocus having no effect in Bootstrap modals
             * @see http://getbootstrap.com/javascript/#modals
             */
            dialog.getModal().on("shown.bs.modal", { dialog: dialog }, onShown);

            return dialog;
        };
    });

    return obj;
});

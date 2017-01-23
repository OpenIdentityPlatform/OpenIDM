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
    "lodash",
    "bootstrap",
    "handlebars",
    "form2js",
    "org/forgerock/openidm/ui/admin/selfservice/AbstractSelfServiceView",
    "org/forgerock/openidm/ui/admin/selfservice/GenericSelfServiceStageView",
    "org/forgerock/commons/ui/common/util/UIUtils",
    "org/forgerock/openidm/ui/admin/util/AdminUtils",
    "bootstrap-dialog"
], function($, _,
            boostrap,
            handlebars,
            form2js,
            AbstractSelfServiceView,
            GenericSelfServiceStageView,
            UiUtils,
            AdminUtils,
            BootstrapDialog) {

    var SelfServiceStageDialogView = AbstractSelfServiceView.extend({
        element: "#dialogs",
        noBaseTemplate: true,

        render: function(args) {
            var view,
                viewPromise = $.Deferred();

            require(["org/forgerock/openidm/ui/admin/selfservice/" + args.type],
                (result) => {
                    viewPromise.resolve(result);
                },
                () => {
                    viewPromise.reject(GenericSelfServiceStageView);
                }
            );

            $.when(viewPromise).always((foundView) => {
                view = foundView;
                var currentDialog = $('<div id="SelfServiceStageDialog"></div>');
                this.setElement(currentDialog);
                $("#dialogs").append(currentDialog);

                this.parentRender(() => {
                    this.dialog = BootstrapDialog.show({
                        title: $.t("templates.selfservice.user." + args.type + "Title"),
                        type: BootstrapDialog.TYPE_DEFAULT,
                        size: BootstrapDialog.SIZE_WIDE,
                        message: '<div id="SelfServiceStageDialog"></div>',
                        onshown: (dialogRef) => {
                            view.render(args, dialogRef);
                        },
                        buttons: [
                            {
                                label: $.t("common.form.close"),
                                action: function (dialogRef) {
                                    dialogRef.close();
                                }
                            }, {
                                label: $.t("common.form.save"),
                                cssClass: "btn-primary",
                                id: "saveUserConfig",
                                action: function (dialogRef) {
                                    if (this.hasClass("disabled")) {
                                        return false;
                                    } else {
                                        args.saveCallback(view.getData());
                                        dialogRef.close();
                                    }
                                }
                            }
                        ]
                    });
                });
            });
        }
    });

    return new SelfServiceStageDialogView();
});

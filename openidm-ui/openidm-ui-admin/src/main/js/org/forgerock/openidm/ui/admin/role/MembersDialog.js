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
 * Copyright 2016 ForgeRock AS.
 */

/*global define */

define("org/forgerock/openidm/ui/admin/role/MembersDialog", [
    "jquery",
    "lodash",
    "handlebars",
    "org/forgerock/commons/ui/common/main/AbstractView",
    "org/forgerock/openidm/ui/common/resource/ResourceCollectionSearchDialog",
    "org/forgerock/openidm/ui/admin/role/util/TemporalConstraintsUtils",
    "org/forgerock/openidm/ui/admin/role/TemporalConstraintsFormView"
],
function ($, _, Handlebars,
    AbstractView,
    ResourceCollectionSearchDialog,
    TemporalConstraintsUtils,
    TemporalConstraintsFormView
  ) {
    var MembersDialog = ResourceCollectionSearchDialog.extend({
        template: "templates/admin/role/MembersSearchDialogTemplate.html",
        setRefProperties : function () {
            this.data._refProperties = {
                grantType: {
                    label: "Grant Type",
                    name: "_grantType",
                    value: (!_.isEmpty(this.data.propertyValue)) ? this.data.propertyValue._refProperties._grantType : ""
                },
                temporalConstraints: {
                    label: "Temporal Constraints",
                    name: "temporalConstraints",
                    value: (!_.isEmpty(this.data.propertyValue)) ? this.data.propertyValue._refProperties.temporalConstraints : []
                }
            };
        },
        renderDialog: function(opts, callback) {
            var renderCallback = _.bind(function () {
                this.addTemporalConstraintsForm();

                if (callback) {
                    callback();
                }
            }, this);

            this.render(opts, renderCallback);
        },
        getNewVal: function () {
            var propVal = this.data.propertyValue,
                typeValue = this.currentDialog.find("#select_" + this.data.property.propName + "_Type").val(),
                relationshipValue = this.currentDialog.find("#select_" + this.data.property.propName).val(),
                readPath = typeValue + "/" + relationshipValue,
                refProperties = this.currentDialog.find("._refProperties:input"),
                getRefProps = _.bind(function () {
                    var refProps = propVal._refProperties || {},
                        temporalConstraintsChecked = this.currentDialog.find(".enableTemporalConstraintsCheckbox").attr("checked");

                    refProps.temporalConstraints = [];
                    refProps._grantType = (!_.isEmpty(this.data.propertyValue)) ? this.data.propertyValue._refProperties._grantType : "";

                    if (temporalConstraintsChecked) {
                        refProps.temporalConstraints = TemporalConstraintsUtils.getTemporalConstraintsValue(this.currentDialog.find('.temporalConstraintsForm'));
                    }
                    return refProps;
                }, this),
                newVal = _.extend(propVal, { "_ref": readPath , "_refProperties" : getRefProps() });

            return {
                val: newVal,
                text: this.currentDialog.find(".selectize-input:eq(1)").find("[data-value]").text()
            };
        },
        addTemporalConstraintsForm : function () {
            var _this = this,
                resourceDetailsForm = this.currentDialog.find("#_refProperty-temporalConstraints"),
                formContainerId = "membersTemporalContstraintsFormContainer",
                formContainer = $("<div id='" + formContainerId + "'></div>"),
                temporalConstraints = [],
                temporalConstraintsView = new TemporalConstraintsFormView();

            if (this.data.propertyValue && this.data.propertyValue._refProperties && this.data.propertyValue._refProperties.temporalConstraints) {
                temporalConstraints = _.map(this.data.propertyValue._refProperties.temporalConstraints, function (constraint) {
                    return TemporalConstraintsUtils.convertFromIntervalString(constraint.duration);
                });
            }

            resourceDetailsForm.append(formContainer);

            temporalConstraintsView.render({
                element: "#" + formContainerId,
                temporalConstraints: temporalConstraints,
                dialogView: true
            });
        }
    });

    return MembersDialog;
});

/**
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2014 ForgeRock AS. All rights reserved.
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

/*global define, $, _ */

define("org/forgerock/openidm/ui/admin/connector/ConnectorTypeAbstractView", [
    "org/forgerock/commons/ui/common/main/AbstractView",
    "org/forgerock/commons/ui/common/main/ValidatorsManager"
], function(AbstractView, validatorsManager) {
    var ConnectorTypeAbstractView = AbstractView.extend({
        element: "#connectorDetails",
        noBaseTemplate: true,

        render: function(args, callback) {
            var base = "templates/admin/connector/";

            $("#connectorDetails").hide();

            this.data.connectorDefaults = args.connectorDefaults;

            this.template = base + args.connectorType +".html";

            this.parentRender(_.bind(function() {
                if(args.animate) {
                    $("#connectorDetails").slideDown("slow", function() {});
                } else {
                    $("#connectorDetails").show();
                }

                this.fieldButtonCheck();

                validatorsManager.bindValidators(this.$el);

                if(callback){
                    callback();
                }
            }, this));
        },

        fieldButtonCheck: function() {
            var arrayComponents = $(".connector-array-component");

            _.each(arrayComponents, function(component){
                if($(component).find(".remove-btn").length === 1) {
                    $(component).find(".remove-btn").hide();
                } else {
                    $(component).find(".remove-btn").show();
                }
            }, this);
        },

        addField: function (event){
            event.preventDefault();

            var clickedEle = event.target,
                field_type,
                field;

            if($(clickedEle).not("button")){
                clickedEle = $(clickedEle).closest("button");
            }

            field_type = $(clickedEle).attr('field_type');
            field = $(clickedEle).parent().next().clone();
            field.find('input[type=text]').val('');

            $('#' + field_type + 'Wrapper').append(field);
            $('#' + field_type + 'Wrapper').find('.remove-btn').show();

            validatorsManager.bindValidators(this.$el.find('#' + field_type + 'Wrapper'));
            validatorsManager.validateAllFields(this.$el.find('#' + field_type + 'Wrapper'));
        },

        removeField: function (event){
            event.preventDefault();

            var clickedEle = event.target,
                field_type;

            if($(clickedEle).not("button")){
                clickedEle = $(clickedEle).closest("button");
            }

            field_type = $(clickedEle).attr('field_type');

            $(clickedEle).parents(".group-field-block").remove();

            if($('#' + field_type + 'Wrapper').find('.field').size() === 1){
                $('#' + field_type + 'Wrapper').find('.remove-btn').hide();
            }

            validatorsManager.bindValidators(this.$el.find('#' + field_type + 'Wrapper'));
            validatorsManager.validateAllFields(this.$el.find('#' + field_type + 'Wrapper'));
        }
    });

    return ConnectorTypeAbstractView;
});


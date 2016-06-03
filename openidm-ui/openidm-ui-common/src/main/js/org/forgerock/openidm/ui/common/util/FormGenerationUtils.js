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
 * Copyright 2011-2015 ForgeRock AS.
 */

/*global define*/

define("org/forgerock/openidm/ui/common/util/FormGenerationUtils", [
    "jquery",
    "org/forgerock/commons/ui/common/util/DateUtil"
], function ($,
             dateUtil) {

    var obj = {};

    obj.standardErrorSpan = '<span class="form-control-feedback"><i class="fa validation-icon"></i></span>';

    obj.standardErrorMessageTag = '<div class="validation-message"></div>';

    obj.generateTemplateFromFormProperties = function(definition, formValues) {
        var property, formTemplate = "", formFieldType, formFieldDescription, i;
        for(i = 0; i < definition.formProperties.length; i++) {
            formFieldDescription = definition.formProperties[i];
            formFieldDescription.value = obj.getValueForKey(formFieldDescription._id, formValues);
            if (formFieldDescription._id !== '_formGenerationTemplate') {
                formFieldType = formFieldDescription.type;
                formTemplate = formTemplate + this.generateTemplateLine(formFieldDescription._id, formFieldDescription);
            }
        }
        return formTemplate;
    };

    obj.getValueForKey = function(key, formValues) {
        var i, formValueEntry;
        if (!formValues) {
            return null;
        }
        for(i = 0; i < formValues.length; i++) {
            formValueEntry = formValues[i];
            if (formValueEntry[key]) {
                return formValueEntry[key];
            }
        }
    };

    obj.generateTemplateLine = function(formFieldId, formFieldDescription) {

        var enumValues, handlebarsValueExpression, fieldValue, valueExpression, formFieldDisplayName,
            formFieldIsReadable, formFieldIsWritable, formFieldIsRequired, formFieldType, formFieldVariableExpression,
            formFieldVariableName, formFieldDefaultExpression, formFieldValue, formFieldDateFormat;

        formFieldIsReadable = formFieldDescription.readable;

        formFieldIsWritable = formFieldDescription.writable && formFieldDescription.readable;

        formFieldIsRequired = formFieldDescription.required && formFieldDescription.writable && formFieldDescription.readable;

        formFieldType = formFieldDescription.type;

        formFieldDisplayName = formFieldDescription.name;

        formFieldVariableName = formFieldDescription.variableName ? formFieldDescription.variableName : formFieldId;

        formFieldVariableExpression = formFieldDescription.variableExpression ? formFieldDescription.variableExpression.expressionText : null;
        formFieldDefaultExpression = formFieldDescription.defaultExpression ? formFieldDescription.defaultExpression.expressionText : null;
        formFieldValue = formFieldDescription.value ? formFieldDescription.value : null;

        if (formFieldValue) {
            valueExpression = formFieldValue;
        } else {
            if (formFieldVariableExpression) {
                valueExpression = formFieldVariableExpression;
            } else if (formFieldDefaultExpression) {
                valueExpression = formFieldDefaultExpression;
            }
        }

        if (valueExpression) {
            handlebarsValueExpression = valueExpression.replace(/\$\{/g,'{{variables.');
            handlebarsValueExpression = handlebarsValueExpression.replace(/\}/g,'}}');
        }

        if (!formFieldType || !formFieldType.name || formFieldType.name === 'string') {
            return this.generateStringTypeField(formFieldVariableName, formFieldDisplayName, handlebarsValueExpression, formFieldIsReadable, formFieldIsWritable, formFieldIsRequired);
        } else if (formFieldType.name === 'enum') {
            return this.generateEnumTypeField(formFieldVariableName, formFieldDisplayName, formFieldType.values, handlebarsValueExpression, formFieldIsReadable, formFieldIsWritable, formFieldIsRequired);
        } else if (formFieldType.name === 'long') {
            return this.generateLongTypeField(formFieldVariableName, formFieldDisplayName, handlebarsValueExpression, formFieldIsReadable, formFieldIsWritable, formFieldIsRequired);
        } else if (formFieldType.name === 'boolean') {
            return this.generateBooleanTypeField(formFieldVariableName, formFieldDisplayName, handlebarsValueExpression, formFieldIsReadable, formFieldIsWritable, formFieldIsRequired);
        } else if (formFieldType.name === 'date') {
            formFieldDateFormat = formFieldType.datePattern;
            return this.generateDateTypeField(formFieldVariableName, formFieldDisplayName, handlebarsValueExpression, formFieldIsReadable, formFieldIsWritable, formFieldIsRequired, formFieldDateFormat);
        }
    };

    obj.generateDateTypeField = function(elementName, elementDisplayName, value, isReadable, isWritable, isRequired, dateFormat) {
        var fieldTagStartPart = '<div class="form-group has-feedback">',
            fieldTagEndPart = '</div>',
            label = "",
            input,
            dateFormatInput,
            validatorMessageTag;

        if (isReadable) {
            label = this.generateLabel(elementDisplayName);
        }

        if (value && value.match(new RegExp("^\\{\\{variables\\."))) {
            value = "{{date " + value.substring(2).slice(-2) + " '" + dateFormat + "'}}";
        }

        dateFormatInput = this.generateInput("dateFormat", dateFormat, false, false, false);
        input = this.generateInput(elementName, value, isReadable, isWritable, isRequired, "formattedDate");
        validatorMessageTag = isReadable && isWritable ? obj.standardErrorSpan + obj.standardErrorMessageTag : '';
        return fieldTagStartPart + label +'<div class="col-sm-8">' + input +'</div>' + validatorMessageTag + dateFormatInput + fieldTagEndPart;
    };

    obj.generateBooleanTypeField = function(elementName, elementDisplayName, value, isReadable, isWritable, isRequired) {
        var map = {'true' : $.t('common.form.true'), 'false' : $.t('common.form.false'), '__null' : ' '};
        return obj.generateEnumTypeField(elementName, elementDisplayName, map, value, isReadable, isWritable, isRequired);
    };

    obj.generateEnumTypeField = function(elementName, elementDisplayName, variableMap, value, isReadable, isWritable, isRequired) {
        var fieldTagStartPart = '<div class="form-group has-feedback">',
            fieldTagEndPart = '</div>',
            label = '',
            select,
            additionalParams='',
            selectedKey,
            validatorMessageTag;

        additionalParams = isRequired ? additionalParams + ' data-validator="required" ' : '';
        additionalParams = !isWritable ? additionalParams + ' disabled="disabled" ' : additionalParams;
        additionalParams = !isReadable ? additionalParams + ' style="display: none" ' : additionalParams;

        selectedKey = value ? value : '__null';
        if (selectedKey.match(new RegExp("^\\{\\{variables\\."))) {
            selectedKey = selectedKey.substring(2).slice(-2);
        } else {
            selectedKey = "'" + selectedKey + "'";
        }
        variableMap.__null = ' ';
        if (isReadable) {
            label = this.generateLabel(elementDisplayName);
        }
        select = "{{select '" + JSON.stringify(variableMap) + "' '" + elementName + "' " + selectedKey + " '' '" + additionalParams + "' }}";
        validatorMessageTag = isRequired && isWritable ? obj.standardErrorSpan + obj.standardErrorMessageTag : '';
        return fieldTagStartPart + label +'<div class="col-sm-8">' + select + validatorMessageTag  +'</div>' + fieldTagEndPart;
    };

    obj.generateStringTypeField = function(elementName, elementDisplayName, handlebarsValueExpression, isReadable, isWritable, isRequired) {
        var fieldTagStartPart = '<div class="form-group has-feedback">',
            fieldTagEndPart = '</div>',
            label = "",
            input,
            validatorMessageTag;

        if (isReadable) {
            label = this.generateLabel(elementDisplayName);
        }
        input = this.generateInput(elementName, handlebarsValueExpression, isReadable, isWritable, isRequired);
        validatorMessageTag = isRequired && isWritable ? obj.standardErrorSpan + obj.standardErrorMessageTag : '';
        return fieldTagStartPart + label +'<div class="col-sm-8">' +input +validatorMessageTag +"</div>" + fieldTagEndPart;
    };

    obj.generateLongTypeField = function(elementName, elementDisplayName, handlebarsValueExpression, isReadable, isWritable, isRequired) {
        var fieldTagStartPart = '<div class="form-group has-feedback">',
            fieldTagEndPart = '</div>',
            label = "",
            input,
            validatorMessageTag;

        if (isReadable) {
            label = this.generateLabel(elementDisplayName);
        }
        input = this.generateInput(elementName, handlebarsValueExpression, isReadable, isWritable, isRequired, "long");
        validatorMessageTag = isReadable && isWritable ?  obj.standardErrorSpan + obj.standardErrorMessageTag : '';

        return fieldTagStartPart + label +'<div class="col-sm-8">' + input + validatorMessageTag +'</div>' + fieldTagEndPart;
    };

    obj.generateInput = function(elementName, value, isReadable, isWritable, isRequired, validatorType) {
        var isDisabledPart = isWritable ? '' : 'disabled="disabled"' , isHiddenPart = isReadable ? '' : 'style="display: none"', isRequiredPart, validatorName = 'required';

        if (validatorType) {
            if (isRequired) {
                validatorName = validatorName + "_" + validatorType;
            } else {
                validatorName = validatorType;
            }
        }
        isRequiredPart = isRequired || validatorType ? 'data-validator="' + validatorName + '"' : '';
        if (!value) {
            value = "";
        }
        return '<input class="form-control" type="text" name="' + elementName + '" value="' + value +'" ' + isDisabledPart + ' ' + isHiddenPart + ' ' + isRequiredPart + ' />';
    };

    obj.generateLabel = function(labelValue) {
        return '<label class="col-sm-3 control-label">' + labelValue +'</label>';
    };

    obj.buildPropertyTypeMap = function(formProperties) {
        var typeName, datePattern, property, formFieldType, formFieldDescription, result = {}, i, propName;
        for (i = 0; i < formProperties.length; i++) {
            formFieldDescription = formProperties[i];
            if (formFieldDescription._id !== '_formGenerationTemplate') {
                formFieldType = formFieldDescription.type;
                if (!formFieldType || !formFieldType.name || formFieldType.name === '') {
                    typeName = 'string';
                } else {
                    typeName = formFieldType.name;
                    if (typeName === 'date') {
                        datePattern = formFieldType.datePattern;
                    }
                }
                propName = formFieldDescription.variableName ? formFieldDescription.variableName : formFieldDescription._id;
                result[propName] = {type: typeName, datePattern: datePattern};
            }
        }
        return result;
    };

    obj.changeParamsToMeetTheirTypes = function(params, propertyTypeMapping) {
        var param, typeName, paramValue, dateFormat, date;
        for (param in params) {
            typeName = propertyTypeMapping[param].type;
            paramValue = params[param];
            if ("date" === typeName) {
                if (paramValue === '') {
                    params[param] = null;
                } else {
                    dateFormat = propertyTypeMapping[param].datePattern;
                    date = dateUtil.parseDateString(paramValue, dateFormat);
                    params[param] = date;
                }
            } else if ("long" === typeName) {
                if (paramValue === '') {
                    params[param] = null;
                } else {
                    params[param] = parseInt(paramValue, 10);
                }
            } else if ("boolean" === typeName) {
                if (paramValue === '') {
                    params[param] = null;
                } else {
                    params[param] = "true"===paramValue ? true : false ;
                }
            }
        }
    };

    obj.validateForm = function(options, validatorsManager, callback) {
        var baseEntity = this.$el.find(options.element).attr(options.attribute);
        validatorsManager.bindValidators(this.$el, baseEntity, function() {
            validatorsManager.validateAllFields(this.$el);

            this.$el.find("select").toggleClass("form-control", true);

            if (callback) {
                callback();
            }
        }.bind(this));


    };

    return obj;
});

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

/*jslint regexp:false, evil:true */

/*global require, define */

define("org/forgerock/openidm/ui/common/util/PolicyValidatorsManager", [
    "jquery",
    "underscore",
    "form2js",
    "org/forgerock/commons/ui/common/main/ValidatorsManager",
    "org/forgerock/openidm/ui/common/delegates/PolicyDelegate"
], function($, _, form2js, ValidatorsManager, PolicyDelegate) {
    var obj = {};

    // the various policies that we don't want to actually bind in the browser
    obj.excludedClientSidePolicies = [
        "valid-type",
        "re-auth-required"
    ];
    // related to above, the various requirements that we don't want to show in the browser
    obj.excludedClientSideRequirements = [
        "VALID_TYPE",
        "REAUTH_REQUIRED"
    ];

    /**
      A method which is passed to form2js for the purposes of determining
      the name and value for any given form node. Primarily necessary for working
      with JSONEditor form instances, which have complex input names such as:

        name="root[mail]"

     */
    obj.nodeCallbackHandler = function (isJsonEditor, node) {
        if (!node.getAttribute || !node.getAttribute("name")) {
            return false;
        }

        if (isJsonEditor) {
            return {
                "name": node.getAttribute('name')
                            .match(/(\[.*?\])/g)
                            .map(function (field) {
                                return field.replace(/\[|\]/g, '');
                            })
                            .join("."),
                "value": node.value
            };
        } else {
            return {
                "name": node.getAttribute("name"),
                "value": node.value
            };
        }
    };


    ValidatorsManager.afterBindValidators = [
        /**
            Registers a new function within the common ValidatorManager to execute
            after the core bindValidators, so that the extra policies returned from
            the policy service can be used to define new validation entries.
        */
        function (containerElement, baseEntity, callback) {
            // baseEntity is required for any of the policy-related features
            if (baseEntity) {
                containerElement.attr("data-validator-baseEntity", baseEntity);
                PolicyDelegate.readEntity(baseEntity).then(_.bind(function (allEntityPolicies) {
                    // look into the containerElement for every property with a policy declared,
                    // and bind validation handlers for each property found.
                    _.each(allEntityPolicies.properties, function (property) {
                        var input = containerElement.hasClass("jsonEditor") ?
                                    // could be improved to support more complex properties
                                    containerElement.find("[name$='\\[" + property.name + "\\]']") :
                                    containerElement.find("[name='" + property.name + "']"),
                            filteredPolicies = _.filter(property.policies, function (policy) {
                                return obj.excludedClientSidePolicies.indexOf(policy.policyId) === -1;
                            }),
                            policyNames = _.map(filteredPolicies, "policyId");

                        if (input.length) {
                            input.attr("data-validator", policyNames.join(" "));

                            _.each(filteredPolicies, function (policy) {
                                obj.registerRemotePolicyFunction(baseEntity, policy);
                                input.data("validatorParams-" + policy.policyId, policy.params);
                            });

                            ValidatorsManager.bindValidatorsForField(containerElement, input);
                        }
                    }, this);

                    if (callback) {
                        callback();
                    }
                }, this));
            } else {
                if (callback) {
                    callback();
                }
            }
        }
    ];

    /**
      Helper which translates the array of policy failures into an array of localized, readable messages
    */
    obj.processPolicyFailures = function (failures) {
        return _(failures)
                .filter(function (policyFailure) {
                    return obj.excludedClientSideRequirements.indexOf(policyFailure.policyRequirement) === -1;
                })
                .map(function (policyFailure) {
                    return $.t("common.form.validation." + policyFailure.policyRequirement, policyFailure.params);
                })
                .value();
    };

    /**
        Registers the remote policy as if it were a browser-based validation function
    */
    obj.registerRemotePolicyFunction = function (baseEntity, policyFunctionObject) {
        if (!_.has(ValidatorsManager.configuration.validators, policyFunctionObject.policyId)) {
            // client-side evaluation possible, based on the function definition provided from the backend
            if (policyFunctionObject.policyFunction && policyFunctionObject.policyFunction.length) {
                ValidatorsManager.configuration.validators[policyFunctionObject.policyId] = {
                    dependencies: [],
                    // maps the interface expected for front-end validators to the interface expected for
                    // backend policy function
                    validator: function (container, element, callback) {
                        var policyFunction,
                            failures,
                            nodeMap = obj.nodeCallbackHandler(container.hasClass("jsonEditor"), element[0]);

                        // the source of the jslint "evil" - this function is returned from the backend
                        // (as part of an administrative specification) so it should be safe to evaluate.
                        policyFunction = eval("(" + policyFunctionObject.policyFunction + ")");
                        failures = policyFunction.call({
                                failedPolicyRequirements: []
                            },
                            form2js(container[0], '.', true, _.curry(obj.nodeCallbackHandler)(container.hasClass("jsonEditor"))),
                            nodeMap.value,
                            element.data("validatorParams-" + policyFunctionObject.policyId) || {},
                            nodeMap.name
                        );

                        callback(obj.processPolicyFailures(failures));
                    }
                };
            } else { // server-side validation required
                ValidatorsManager.configuration.validators[policyFunctionObject.policyId] = {
                    dependencies: [],
                    // This evaluates "all" remote policies for the given data,
                    // not just the one for the current policy.
                    validator: obj.evaluateAllRemotePolicies
                };
            }
        }
    };

    /**
        Rate-limited version of the function from the policy delegate, so that repeated
        calls to this function within a very short window do not result in numerous repeated
        REST calls. Note the 5ms window - this is just big enough to capture calls which all
        originated from the same event.
    */
    obj.debouncedValidateProperty = _.debounce(PolicyDelegate.validateProperty,
        5, {'leading': true,'trailing': false}
    );

    /**
        For a given element, get all of the policies which apply.
        It is expected that this will be called multiple times in quick
        succession, when multiple remote policies are declared for the element.
        This is why it uses the above debounced delegate function.
     */
    obj.evaluateAllRemotePolicies = function (container, element, callback) {
        var nodeMap = obj.nodeCallbackHandler(container.hasClass("jsonEditor"), element[0]);
        obj.debouncedValidateProperty(container.attr("data-validator-baseEntity"),
            {
                "fullObject": form2js(container[0], '.', true, _.curry(obj.nodeCallbackHandler)(container.hasClass("jsonEditor"))),
                "value": nodeMap.value,
                "property": nodeMap.name
            }
        ).then(function (result) {
            callback(obj.processPolicyFailures(result.failedPolicyRequirements));
        });
    };

    return obj;

});

var policy = {
    "policyId" : "valid-utcdate",
    "policyExec" : "validUTCDate",
    "policyRequirements" : ["VALID_DATE"]
};

addPolicy(policy);

function validUTCDate(fullObject, value, params, property) {
    var isRequired = _.find(this.failedPolicyRequirements, function (fpr) {
            return fpr.policyRequirement === "REQUIRED";
        }),
        isNonEmptyString = (typeof(value) === "string" && value.length),
        datePattern = new RegExp("[0-9]{4}-[0-9]{2}-[0-9]{2}"),
        isValidFormat = !datePattern.test(value);

    if ((isRequired || isNonEmptyString) && isValidFormat) {
        return [ {"policyRequirement": "VALID_DATE"}];
    }

    return [];
};
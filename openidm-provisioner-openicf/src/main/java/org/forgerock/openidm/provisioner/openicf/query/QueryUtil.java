package org.forgerock.openidm.provisioner.openicf.query;

public abstract class QueryUtil {

    public static final String OPERATOR_AND = "AND";
    public static final String OPERATOR_OR = "OR";
    public static final String OPERATOR_NAND = "NAND";
    public static final String OPERATOR_NOR = "NOR";

    public static final String OPERATOR_EQUALS = "Equals";
    public static final String OPERATOR_STARTSWITH = "StartsWith";
    public static final String OPERATOR_LESSTHAN = "LessThan";
    public static final String OPERATOR_ENDSWITH = "EndsWith";
    public static final String OPERATOR_CONTAINSALLVALUES = "ContainsAllValues";
    public static final String OPERATOR_CONTAINS = "Contains";
    public static final String OPERATOR_GREATERTHAN = "GreaterThan";
    public static final String OPERATOR_GREATERTHANOREQUAL = "GreaterThanOrEqual";
    public static final String OPERATOR_LESSTHANOREQUAL = "LessThanOrEqual";
}
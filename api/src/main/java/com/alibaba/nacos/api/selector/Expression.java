package com.alibaba.nacos.api.selector;

/**
 * @author nkorange
 * @since 1.1.0
 */
public class Expression {

    private String expr;

    public static Expression startExpression() {
        return new Expression();
    }

    public Expression or() {
        expr += " or ";
        return this;
    }

    public Expression and() {
        expr += " and ";
        return this;
    }

    public Expression equal(String key, String value) {
        expr += key + " = '" + value + "'";
        return this;
    }

    public Expression notEqual(String key, String value) {
        expr += key + " != '" + value + "'";
        return this;
    }
}

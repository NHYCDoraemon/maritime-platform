package com.maritime.iam.sdk.dataperm;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Validates and renders {@link DataPermissionExpression} into
 * parameterized SQL fragments. All values use PreparedStatement
 * parameters; field names are whitelist-validated.
 */
public final class ExpressionParser {

    private static final Logger LOG =
            LoggerFactory.getLogger(ExpressionParser.class);

    private static final Pattern FIELD_PATTERN =
            Pattern.compile("^[a-zA-Z_][a-zA-Z0-9_]{0,63}$");

    private static final Set<String> VALID_OPS = Set.of(
            "=", "!=", ">", ">=", "<", "<=",
            "IN", "NOT_IN", "LIKE",
            "IS_NULL", "IS_NOT_NULL");

    private static final Map<String, String> SIMPLE_OPS =
            Map.ofEntries(
                    Map.entry("=", "%s = ?"),
                    Map.entry("!=", "%s != ?"),
                    Map.entry(">", "%s > ?"),
                    Map.entry(">=", "%s >= ?"),
                    Map.entry("<", "%s < ?"),
                    Map.entry("<=", "%s <= ?"),
                    Map.entry("LIKE", "%s LIKE ?"));

    private static final Set<String> NO_VALUE_OPS =
            Set.of("IS_NULL", "IS_NOT_NULL");

    private static final Set<String> LIST_OPS =
            Set.of("IN", "NOT_IN");

    private ExpressionParser() {
    }

    public static List<DataPermissionExpression> validate(
            List<DataPermissionExpression> raw) {
        if (raw == null || raw.isEmpty()) {
            return List.of();
        }
        List<DataPermissionExpression> result =
                new ArrayList<>();
        for (DataPermissionExpression expr : raw) {
            if (isValid(expr)) {
                result.add(expr);
            }
        }
        return result;
    }

    public static SqlFragment toSqlFragment(
            List<DataPermissionExpression> expressions) {
        StringBuilder sql = new StringBuilder();
        List<Object> values = new ArrayList<>();
        for (DataPermissionExpression expr : expressions) {
            if (!sql.isEmpty()) {
                sql.append(" AND ");
            }
            appendExpression(sql, values, expr);
        }
        return new SqlFragment(sql.toString(), values);
    }

    private static boolean isValid(
            DataPermissionExpression expr) {
        if (!isFieldValid(expr.field())) {
            return false;
        }
        if (expr.op() == null
                || !VALID_OPS.contains(expr.op())) {
            LOG.warn("Unsupported op: {}", expr.op());
            return false;
        }
        if (!NO_VALUE_OPS.contains(expr.op())
                && expr.value() == null) {
            LOG.warn("Null value for op: {}", expr.op());
            return false;
        }
        return true;
    }

    private static boolean isFieldValid(String field) {
        if (field == null
                || !FIELD_PATTERN.matcher(field).matches()) {
            LOG.warn("Invalid field name: {}", field);
            return false;
        }
        return true;
    }

    private static void appendExpression(
            StringBuilder sql,
            List<Object> values,
            DataPermissionExpression expr) {
        String field = expr.field();
        String op = expr.op();
        if (SIMPLE_OPS.containsKey(op)) {
            sql.append(String.format(SIMPLE_OPS.get(op), field));
            values.add(expr.value());
        } else if (LIST_OPS.contains(op)) {
            appendListOp(sql, values, field, op, expr.value());
        } else if ("IS_NULL".equals(op)) {
            sql.append(field).append(" IS NULL");
        } else if ("IS_NOT_NULL".equals(op)) {
            sql.append(field).append(" IS NOT NULL");
        }
    }

    private static void appendListOp(
            StringBuilder sql,
            List<Object> values,
            String field,
            String op,
            Object value) {
        List<?> list = (value instanceof List<?> l)
                ? l : List.of(value);
        String keyword = "IN".equals(op) ? "IN" : "NOT IN";
        sql.append(field).append(' ')
                .append(keyword).append(" (");
        for (int i = 0; i < list.size(); i++) {
            if (i > 0) {
                sql.append(", ");
            }
            sql.append('?');
            values.add(list.get(i));
        }
        sql.append(')');
    }

    public record SqlFragment(
            String condition,
            List<Object> values
    ) {
    }
}

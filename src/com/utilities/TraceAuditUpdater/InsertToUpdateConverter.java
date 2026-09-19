package com.utilities.TraceAuditUpdater;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;



import java.util.*;

public class InsertToUpdateConverter {

    // ==========================
    // CONFIGURATION
    // ==========================

    private static final String INPUT_FILE =
            "D:/tmp/inserts.sql";

    private static final String OUTPUT_FILE =
            "D:/tmp/updates.sql";

    /**
     * Table -> WHERE columns
     *
     * Empty / missing table entry:
     * UPDATE generated without WHERE clause.
     */
    private static final Map<String, List<String>> TABLE_WHERE_COLUMNS =
            new HashMap<>();

    static {

        TABLE_WHERE_COLUMNS.put(
                "cards",
                List.of("card_srno", "card_no"));

        TABLE_WHERE_COLUMNS.put(
                "ch_auth_batches1",
                List.of("batch_no", "record_id"));

        TABLE_WHERE_COLUMNS.put(
                "ch_auth_batches2",
                List.of("record_id" ));
        TABLE_WHERE_COLUMNS.put(
                "card_history_infos",
                List.of("sr_no" , "card_srno"));

        TABLE_WHERE_COLUMNS.put(
                "card_more_infos",
                List.of( "card_srno"));

        TABLE_WHERE_COLUMNS.put(
                "card_more_infos3",
                List.of( "card_srno"));

        TABLE_WHERE_COLUMNS.put(
                "customers",
                List.of( "ch_id"));

    }

    // ==========================

    public static void main(String[] args) {

        try {

            String sql = Files.readString(
                    Paths.get(INPUT_FILE),
                    StandardCharsets.UTF_8);

            List<String> statements =
                    splitStatements(sql);

            StringBuilder output =
                    new StringBuilder();

            int successCount = 0;
            int skippedCount = 0;

            for (String stmt : statements) {

                stmt = stmt.trim();

                if (stmt.isEmpty()) {
                    continue;
                }

                if (!stmt.toUpperCase()
                        .startsWith("INSERT INTO")) {
                    continue;
                }

                try {

                    String updateSql =
                            convertInsertToUpdate(stmt);

                    output.append(updateSql)
                            .append("\n\n");

                    successCount++;

                } catch (Exception ex) {

                    skippedCount++;

                    output.append("-- FAILED TO CONVERT\n");
                    output.append("-- ")
                            .append(ex.getMessage())
                            .append("\n\n");
                }
            }

            Files.writeString(
                    Paths.get(OUTPUT_FILE),
                    output.toString(),
                    StandardCharsets.UTF_8);

            System.out.println("Completed.");
            System.out.println("Converted : " + successCount);
            System.out.println("Skipped   : " + skippedCount);
            System.out.println("Output    : " + OUTPUT_FILE);

        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    private static String convertInsertToUpdate(
            String insertSql) {

        Pattern pattern = Pattern.compile(
                "INSERT\\s+INTO\\s+([^(\\s]+)\\s*\\((.*?)\\)\\s*VALUES\\s*\\((.*)\\)\\s*;?",
                Pattern.CASE_INSENSITIVE | Pattern.DOTALL);

        Matcher matcher =
                pattern.matcher(insertSql);

        if (!matcher.find()) {

            throw new RuntimeException(
                    "Invalid INSERT statement");
        }

        String tableName =
                matcher.group(1).trim();

        String columnsPart =
                matcher.group(2).trim();

        String valuesPart =
                matcher.group(3).trim();

        List<String> columns =
                splitCsv(columnsPart);

        List<String> values =
                splitCsv(valuesPart);

        if (columns.size() != values.size()) {

            throw new RuntimeException(
                    "Column count mismatch. Columns="
                            + columns.size()
                            + " Values="
                            + values.size());
        }

        List<String> whereColumns =
                TABLE_WHERE_COLUMNS.getOrDefault(
                        tableName.toLowerCase(),
                        Collections.emptyList());

        boolean whereProvided =
                !whereColumns.isEmpty();

        Map<String, String> whereValues =
                new LinkedHashMap<>();

        StringBuilder update =
                new StringBuilder();

        update.append("UPDATE ")
                .append(tableName)
                .append("\nSET\n");

        boolean first = true;

        for (int i = 0; i < columns.size(); i++) {

            String column =
                    columns.get(i).trim();

            String value =
                    values.get(i).trim();

            boolean isWhereColumn =
                    whereColumns.stream()
                            .anyMatch(
                                    c -> c.equalsIgnoreCase(column));

            if (whereProvided && isWhereColumn) {

                whereValues.put(column, value);

                continue;
            }

            if (!first) {
                update.append(",\n");
            }

            update.append("    ")
                    .append(column)
                    .append(" = ")
                    .append(value);

            first = false;
        }

        if (whereProvided) {

            for (String whereColumn : whereColumns) {

                boolean found =
                        whereValues.keySet()
                                .stream()
                                .anyMatch(
                                        c -> c.equalsIgnoreCase(whereColumn));

                if (!found) {

                    throw new RuntimeException(
                            "WHERE column not found: "
                                    + whereColumn
                                    + " for table "
                                    + tableName);
                }
            }

            update.append("\nWHERE ");

            boolean firstWhere = true;

            for (String whereColumn : whereColumns) {

                String whereValue = null;

                for (Map.Entry<String, String> entry
                        : whereValues.entrySet()) {

                    if (entry.getKey()
                            .equalsIgnoreCase(whereColumn)) {

                        whereValue =
                                entry.getValue();

                        break;
                    }
                }

                if (!firstWhere) {

                    update.append("\n  AND ");
                }

                update.append(whereColumn)
                        .append(" = ")
                        .append(whereValue);

                firstWhere = false;
            }
        }

        update.append(";");

        return update.toString();
    }

    private static List<String> splitStatements(
            String sql) {

        List<String> result =
                new ArrayList<>();

        StringBuilder current =
                new StringBuilder();

        boolean inQuotes = false;

        for (int i = 0; i < sql.length(); i++) {

            char c = sql.charAt(i);

            if (c == '\'') {

                inQuotes = !inQuotes;
            }

            current.append(c);

            if (c == ';' && !inQuotes) {

                result.add(current.toString());

                current.setLength(0);
            }
        }

        if (!current.toString()
                .trim()
                .isEmpty()) {

            result.add(current.toString());
        }

        return result;
    }

    private static List<String> splitCsv(
            String text) {

        List<String> result =
                new ArrayList<>();

        StringBuilder current =
                new StringBuilder();

        boolean inQuotes = false;
        int parenDepth = 0;

        for (int i = 0; i < text.length(); i++) {

            char c = text.charAt(i);

            if (c == '\'') {

                inQuotes = !inQuotes;
            }

            if (!inQuotes) {

                if (c == '(') {

                    parenDepth++;

                } else if (c == ')') {

                    parenDepth--;
                }

                if (c == ',' &&
                        parenDepth == 0) {

                    result.add(
                            current.toString().trim());

                    current.setLength(0);

                    continue;
                }
            }

            current.append(c);
        }

        result.add(
                current.toString().trim());

        return result;
    }
}


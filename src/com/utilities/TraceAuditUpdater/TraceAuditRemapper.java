package com.utilities.TraceAuditUpdater;
import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.regex.*;

/**
 * TraceAuditRemapper
 *
 * Reads three SQL INSERT files for:
 *   1. trans_requests
 *   2. cc_trns_colctn_stats
 *   3. cc_trns_colctn_stat1s
 *
 * Remaps all trace_audit_no values starting from a configured seed,
 * keeping cross-table references consistent.
 *
 * CONFIG:
 *   FOLDER_PATH        – directory containing the three SQL files
 *   START_TRACE_AUDIT  – first new trace_audit_no to assign
 *
 * File naming convention (configurable below):
 *   TRANS_REQUESTS_FILE, CC_STATS_FILE, CC_STAT1S_FILE
 */
public class TraceAuditRemapper {

    // ─── CONFIGURATION ────────────────────────────────────────────────────────
    private static final String FOLDER_PATH       = "D:\\tmp\\trs_new";
    private static final long   START_TRACE_AUDIT = 9219994176659169117L;   // ← change this

    private static final String TRANS_REQUESTS_FILE = "trans_requests.sql";
    private static final String CC_STATS_FILE        = "cc_trans.sql";
    private static final String CC_STAT1S_FILE       = "cc_tran1s.sql";

    private static final String OUT_TRANS_REQUESTS_FILE = "trans_requests_updated.sql";
    private static final String OUT_CC_STATS_FILE        = "cc_trns_colctn_stats_updated.sql";
    private static final String OUT_CC_STAT1S_FILE       = "cc_trns_colctn_stat1s_updated.sql";
    // ──────────────────────────────────────────────────────────────────────────

    public static void main(String[] args) throws Exception {

        Path folder = Paths.get(FOLDER_PATH);

        // 1. Parse trans_requests → build oldId → newId mapping
        List<String> trLines   = readLines(folder.resolve(TRANS_REQUESTS_FILE));
        List<String> statLines  = readLines(folder.resolve(CC_STATS_FILE));
        List<String> stat1Lines = readLines(folder.resolve(CC_STAT1S_FILE));

        // ── Step 1: collect all trace_audit_no values from trans_requests ─────
        //    Column position of trace_audit_no is the FIRST value in the INSERT.
        //    We also collect every id that appears in reference columns so we
        //    know the full universe of ids that need remapping.
        Map<Long, Long> idMap = new LinkedHashMap<>();   // old → new, insertion-ordered
        long nextId = START_TRACE_AUDIT;

        for (String line : trLines) {
            long traceId = extractTransRequestsTraceAuditNo(line);
            if (traceId != -1 && !idMap.containsKey(traceId)) {
                idMap.put(traceId, nextId++);
            }
        }

        System.out.println("=== Trace Audit ID Mapping ===");
        for (Map.Entry<Long, Long> e : idMap.entrySet()) {
            System.out.printf("  %d  →  %d%n", e.getKey(), e.getValue());
        }
        System.out.println("Total mappings: " + idMap.size());

        // ── Step 2: rewrite each file ─────────────────────────────────────────
        List<String> newTrLines    = remapTransRequests(trLines,   idMap);
        List<String> newStatLines  = remapCcStats(statLines,       idMap);
        List<String> newStat1Lines = remapCcStat1s(stat1Lines,     idMap);

        // ── Step 3: write output files ────────────────────────────────────────
        writeLines(folder.resolve(OUT_TRANS_REQUESTS_FILE), newTrLines);
        writeLines(folder.resolve(OUT_CC_STATS_FILE),        newStatLines);
        writeLines(folder.resolve(OUT_CC_STAT1S_FILE),       newStat1Lines);

        System.out.println("\nOutput files written to: " + FOLDER_PATH);
        System.out.println("  " + OUT_TRANS_REQUESTS_FILE);
        System.out.println("  " + OUT_CC_STATS_FILE);
        System.out.println("  " + OUT_CC_STAT1S_FILE);
    }

    // ═════════════════════════════════════════════════════════════════════════
    //  TRANS_REQUESTS processing
    // ═════════════════════════════════════════════════════════════════════════

    /**
     * Extracts the trace_audit_no (first column, first VALUE) from a
     * trans_requests INSERT line.  Returns -1 if the line is not an INSERT.
     */
    private static long extractTransRequestsTraceAuditNo(String line) {
        String trimmed = line.trim();
        if (!trimmed.toUpperCase().startsWith("INSERT INTO TRANS_REQUESTS")) return -1;

        // VALUES (...  first token after opening paren
        int valuesIdx = trimmed.toUpperCase().indexOf("VALUES");
        if (valuesIdx == -1) return -1;

        int parenOpen = trimmed.indexOf('(', valuesIdx);
        if (parenOpen == -1) return -1;

        String afterParen = trimmed.substring(parenOpen + 1).trim();
        // first value is the numeric trace_audit_no
        String firstToken = afterParen.split("[,\\s]")[0].trim();
        try {
            return Long.parseLong(firstToken);
        } catch (NumberFormatException e) {
            return -1;
        }
    }

    /**
     * For trans_requests we must remap:
     *   - trace_audit_no          (1st VALUES token)
     *   - sys_trace_auditno       (numeric column – same value as trace_audit_no normally)
     *   - org_trace_audit         (can be NULL or a numeric id)
     *   - rev_trace_audit         (can be NULL or a numeric id)
     *   - fee_trace_audit         (can be NULL or a numeric id)
     *
     * Strategy: after we know the column positions from the column-list header,
     * we remap every occurrence of any known old id that appears as a standalone
     * number token in the VALUES list.
     *
     * Because the INSERT may span multiple lines or be a single long line,
     * we work on the full VALUES string.
     */
    private static List<String> remapTransRequests(List<String> lines, Map<Long, Long> idMap) {
        // Column indices we care about (0-based)
        // We derive them dynamically from the column list.
        List<String> result = new ArrayList<>();

        for (String line : lines) {
            String trimmed = line.trim();
            if (!trimmed.toUpperCase().startsWith("INSERT INTO TRANS_REQUESTS")) {
                result.add(line);
                continue;
            }

            // Parse column names
            int colStart = trimmed.indexOf('(');
            int colEnd   = trimmed.toUpperCase().indexOf(") VALUES");
            if (colStart == -1 || colEnd == -1) { result.add(line); continue; }

            String colSection = trimmed.substring(colStart + 1, colEnd);
            List<String> columns = splitCsv(colSection);

            // column indices
            int idxTrace   = indexOfCol(columns, "trace_audit_no");
            int idxSys     = indexOfCol(columns, "sys_trace_auditno");
            int idxOrg     = indexOfCol(columns, "org_trace_audit");
            int idxRev     = indexOfCol(columns, "rev_trace_audit");
            int idxFee     = indexOfCol(columns, "fee_trace_audit");

            // Parse VALUES list
            int valuesKw   = trimmed.toUpperCase().indexOf(") VALUES");
            int valParen   = trimmed.indexOf('(', valuesKw);
            // find matching close paren (last one before semicolon)
            int valClose   = trimmed.lastIndexOf(')');
            String valStr  = trimmed.substring(valParen + 1, valClose);

            List<String> tokens = splitCsv(valStr);

            // Remap the specific columns
            remapToken(tokens, idxTrace, idMap);
            remapToken(tokens, idxSys,   idMap);
            remapToken(tokens, idxOrg,   idMap);
            remapToken(tokens, idxRev,   idMap);
            remapToken(tokens, idxFee,   idMap);

            // Reconstruct
            String newValues = String.join(", ", tokens);
            String prefix    = trimmed.substring(0, valParen + 1);
            result.add(prefix + newValues + ");");
        }
        return result;
    }

    // ═════════════════════════════════════════════════════════════════════════
    //  CC_TRNS_COLCTN_STATS processing
    // ═════════════════════════════════════════════════════════════════════════

    private static List<String> remapCcStats(List<String> lines, Map<Long, Long> idMap) {
        List<String> result = new ArrayList<>();
        for (String line : lines) {
            String trimmed = line.trim();
            if (!trimmed.toUpperCase().startsWith("INSERT INTO CC_TRNS_COLCTN_STATS")) {
                result.add(line);
                continue;
            }

            int colStart = trimmed.indexOf('(');
            int colEnd   = trimmed.toUpperCase().indexOf(") VALUES");
            if (colStart == -1 || colEnd == -1) { result.add(line); continue; }

            List<String> columns = splitCsv(trimmed.substring(colStart + 1, colEnd));
            int idxDr = indexOfCol(columns, "dr_trans_id");
            int idxCr = indexOfCol(columns, "cr_trans_id");

            int valParen = trimmed.indexOf('(', trimmed.toUpperCase().indexOf(") VALUES"));
            int valClose = trimmed.lastIndexOf(')');
            List<String> tokens = splitCsv(trimmed.substring(valParen + 1, valClose));

            remapToken(tokens, idxDr, idMap);
            remapToken(tokens, idxCr, idMap);

            result.add(trimmed.substring(0, valParen + 1) + String.join(", ", tokens) + ");");
        }
        return result;
    }

    // ═════════════════════════════════════════════════════════════════════════
    //  CC_TRNS_COLCTN_STAT1S processing
    // ═════════════════════════════════════════════════════════════════════════

    private static List<String> remapCcStat1s(List<String> lines, Map<Long, Long> idMap) {
        List<String> result = new ArrayList<>();
        for (String line : lines) {
            String trimmed = line.trim();
            if (!trimmed.toUpperCase().startsWith("INSERT INTO CC_TRNS_COLCTN_STAT1S")) {
                result.add(line);
                continue;
            }

            int colStart = trimmed.indexOf('(');
            int colEnd   = trimmed.toUpperCase().indexOf(") VALUES");
            if (colStart == -1 || colEnd == -1) { result.add(line); continue; }

            List<String> columns = splitCsv(trimmed.substring(colStart + 1, colEnd));
            int idxRef = indexOfCol(columns, "ref_trans_id");

            int valParen = trimmed.indexOf('(', trimmed.toUpperCase().indexOf(") VALUES"));
            int valClose = trimmed.lastIndexOf(')');
            List<String> tokens = splitCsv(trimmed.substring(valParen + 1, valClose));

            remapToken(tokens, idxRef, idMap);

            result.add(trimmed.substring(0, valParen + 1) + String.join(", ", tokens) + ");");
        }
        return result;
    }

    // ═════════════════════════════════════════════════════════════════════════
    //  Helpers
    // ═════════════════════════════════════════════════════════════════════════

    /** Replace token at index if it is a known old id. */
    private static void remapToken(List<String> tokens, int idx, Map<Long, Long> idMap) {
        if (idx < 0 || idx >= tokens.size()) return;
        String tok = tokens.get(idx).trim();
        try {
            long val = Long.parseLong(tok);
            if (idMap.containsKey(val)) {
                tokens.set(idx, String.valueOf(idMap.get(val)));
            }
        } catch (NumberFormatException ignored) {
            // null, string literal, TO_DATE(…) – skip
        }
    }

    /**
     * Splits a comma-separated SQL value list while respecting:
     *   - quoted strings  'hello, world'
     *   - TO_DATE('...', '...')  function calls with nested commas
     *   - nested parentheses
     */
    private static List<String> splitCsv(String s) {
        List<String> parts = new ArrayList<>();
        int depth = 0;
        boolean inQuote = false;
        StringBuilder cur = new StringBuilder();

        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c == '\'' && !inQuote) {
                inQuote = true;
                cur.append(c);
            } else if (c == '\'' && inQuote) {
                // handle escaped quote ''
                if (i + 1 < s.length() && s.charAt(i + 1) == '\'') {
                    cur.append(c);
                    cur.append(s.charAt(++i));
                } else {
                    inQuote = false;
                    cur.append(c);
                }
            } else if (!inQuote && c == '(') {
                depth++;
                cur.append(c);
            } else if (!inQuote && c == ')') {
                depth--;
                cur.append(c);
            } else if (!inQuote && depth == 0 && c == ',') {
                parts.add(cur.toString().trim());
                cur.setLength(0);
            } else {
                cur.append(c);
            }
        }
        if (cur.length() > 0) parts.add(cur.toString().trim());
        return parts;
    }

    /** Returns 0-based index of column name (case-insensitive), or -1. */
    private static int indexOfCol(List<String> columns, String name) {
        for (int i = 0; i < columns.size(); i++) {
            if (columns.get(i).trim().equalsIgnoreCase(name)) return i;
        }
        return -1;
    }

    private static List<String> readLines(Path path) throws IOException {
        List<String> lines = new ArrayList<>();
        try (BufferedReader br = new BufferedReader(new FileReader(path.toFile()))) {
            String line;
            while ((line = br.readLine()) != null) lines.add(line);
        }
        return lines;
    }

    private static void writeLines(Path path, List<String> lines) throws IOException {
        try (BufferedWriter bw = new BufferedWriter(new FileWriter(path.toFile()))) {
            for (String line : lines) {
                bw.write(line);
                bw.newLine();
            }
        }
    }
}

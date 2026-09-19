package com.utilities.JarLibComparator;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.XSSFColor;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Compares two jar library folders (old/prod vs new) and writes a single
 * Excel sheet: name/size/SHA-256 for each side plus a status column,
 * color-coded by status.
 */
public final class JarLibComparator {

    public static final String STATUS_SAME        = "Same";
    public static final String STATUS_RENAMED_ONLY = "Only name changed (SHA/size same)";
    public static final String STATUS_CHANGED      = "Different (name same, content changed)";
    public static final String STATUS_ADDED        = "New added in new-lib";
    public static final String STATUS_REMOVED      = "Removed in new-lib";

    private JarLibComparator() {}

    public interface ProgressListener {
        void log(String message);
    }

    public record JarInfo(String name, long size, String sha256) {
    }

    public record Result(
            int oldCount,
            int newCount,
            Map<String, Integer> statusCounts,
            String outputPath) {
    }

    public static Result run(String oldLibPath, String newLibPath, String outputXlsx,
                              String sheetName, ProgressListener listener) throws IOException {
        listener.log("// Scanning old-lib: " + oldLibPath);
        Map<String, JarInfo> oldJars = scanDir(oldLibPath, listener);
        listener.log("// Found " + oldJars.size() + " jar(s)");

        listener.log("// Scanning new-lib: " + newLibPath);
        Map<String, JarInfo> newJars = scanDir(newLibPath, listener);
        listener.log("// Found " + newJars.size() + " jar(s)");

        listener.log("// Comparing...");
        List<Object[]> rows = compare(oldJars, newJars);

        Map<String, Integer> statusCounts = new LinkedHashMap<>();
        for (Object[] r : rows) {
            String status = (String) r[6];
            statusCounts.merge(status, 1, Integer::sum);
        }

        listener.log("// Writing report: " + outputXlsx);
        writeExcel(rows, outputXlsx, sheetName);
        listener.log("// Done.");

        return new Result(oldJars.size(), newJars.size(), statusCounts, outputXlsx);
    }

    private static Map<String, JarInfo> scanDir(String dirPath, ProgressListener listener) throws IOException {
        Path dir = Paths.get(dirPath);
        if (!Files.isDirectory(dir)) {
            throw new IOException("Not a directory: " + dirPath);
        }
        Map<String, JarInfo> map = new LinkedHashMap<>();
        try (Stream<Path> paths = Files.walk(dir)) {
            List<Path> jars = paths
                    .filter(p -> Files.isRegularFile(p) && p.toString().toLowerCase().endsWith(".jar"))
                    .sorted()
                    .collect(Collectors.toList());
            for (Path p : jars) {
                String name = p.getFileName().toString();
                map.put(name, new JarInfo(name, Files.size(p), sha256(p)));
            }
        }
        return map;
    }

    private static String sha256(Path path) throws IOException {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            try (InputStream is = Files.newInputStream(path)) {
                byte[] buffer = new byte[8192];
                int read;
                while ((read = is.read(buffer)) != -1) {
                    digest.update(buffer, 0, read);
                }
            }
            StringBuilder sb = new StringBuilder();
            for (byte b : digest.digest()) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new IOException(e);
        }
    }

    // Each row: [oldName, oldSize, oldSha, newName, newSize, newSha, status]
    private static List<Object[]> compare(Map<String, JarInfo> oldJars, Map<String, JarInfo> newJars) {
        List<Object[]> result = new ArrayList<>();
        Set<String> matchedOld = new HashSet<>();
        Set<String> usedNew = new HashSet<>();

        for (String name : oldJars.keySet()) {
            JarInfo n = newJars.get(name);
            if (n != null) {
                JarInfo o = oldJars.get(name);
                String status = o.sha256().equals(n.sha256()) ? STATUS_SAME : STATUS_CHANGED;
                result.add(row(o, n, status));
                matchedOld.add(name);
                usedNew.add(name);
            }
        }

        // Same jar under a different name: match remaining old entries by SHA
        for (String name : oldJars.keySet()) {
            if (matchedOld.contains(name)) {
                continue;
            }
            JarInfo o = oldJars.get(name);
            Optional<JarInfo> renamed = newJars.values().stream()
                    .filter(n -> !usedNew.contains(n.name()) && n.sha256().equals(o.sha256()))
                    .findFirst();
            if (renamed.isPresent()) {
                JarInfo n = renamed.get();
                result.add(row(o, n, STATUS_RENAMED_ONLY));
                matchedOld.add(name);
                usedNew.add(n.name());
            }
        }

        for (String name : oldJars.keySet()) {
            if (!matchedOld.contains(name)) {
                result.add(row(oldJars.get(name), null, STATUS_REMOVED));
            }
        }

        for (String name : newJars.keySet()) {
            if (!usedNew.contains(name)) {
                result.add(row(null, newJars.get(name), STATUS_ADDED));
            }
        }

        Map<String, Integer> statusOrder = Map.of(
                STATUS_CHANGED, 0,
                STATUS_RENAMED_ONLY, 1,
                STATUS_ADDED, 2,
                STATUS_REMOVED, 3,
                STATUS_SAME, 4
        );
        result.sort(Comparator.comparingInt(r -> statusOrder.getOrDefault((String) r[6], 9)));
        return result;
    }

    private static Object[] row(JarInfo o, JarInfo n, String status) {
        return new Object[]{
                o != null ? o.name() : null, o != null ? o.size() : null, o != null ? o.sha256() : null,
                n != null ? n.name() : null, n != null ? n.size() : null, n != null ? n.sha256() : null,
                status
        };
    }

    private static void writeExcel(List<Object[]> rows, String outputPath, String sheetName) throws IOException {
        try (XSSFWorkbook wb = new XSSFWorkbook()) {
            Sheet sheet = wb.createSheet(sheetName);

            CellStyle headerStyle = wb.createCellStyle();
            Font headerFont = wb.createFont();
            headerFont.setBold(true);
            headerFont.setColor(IndexedColors.WHITE.getIndex());
            headerStyle.setFont(headerFont);
            headerStyle.setFillForegroundColor(IndexedColors.DARK_BLUE.getIndex());
            headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);

            Map<String, CellStyle> statusStyles = buildStatusStyles(wb);
            CellStyle plainStyle = wb.createCellStyle();

            String[] headers = {
                    "Prod Jar Name", "Prod Size (bytes)", "Prod SHA-256",
                    "New Jar Name", "New Size (bytes)", "New SHA-256", "Status"
            };
            Row headerRow = sheet.createRow(0);
            for (int i = 0; i < headers.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(headers[i]);
                cell.setCellStyle(headerStyle);
            }

            int rIdx = 1;
            for (Object[] r : rows) {
                Row row = sheet.createRow(rIdx++);
                CellStyle style = statusStyles.getOrDefault((String) r[6], plainStyle);
                for (int col = 0; col < r.length; col++) {
                    setCell(row, col, r[col], style);
                }
            }

            for (int i = 0; i < headers.length; i++) {
                sheet.autoSizeColumn(i);
            }
            sheet.createFreezePane(0, 1);
            if (rIdx > 1) {
                sheet.setAutoFilter(new CellRangeAddress(0, rIdx - 1, 0, headers.length - 1));
            }

            try (FileOutputStream fos = new FileOutputStream(outputPath)) {
                wb.write(fos);
            }
        }
    }

    private static void setCell(Row row, int col, Object value, CellStyle style) {
        Cell cell = row.createCell(col);
        if (value == null) {
            cell.setBlank();
        } else if (value instanceof Long l) {
            cell.setCellValue(l);
        } else {
            cell.setCellValue(value.toString());
        }
        cell.setCellStyle(style);
    }

    private static Map<String, CellStyle> buildStatusStyles(XSSFWorkbook wb) {
        Map<String, CellStyle> styles = new HashMap<>();
        styles.put(STATUS_SAME, wb.createCellStyle());
        styles.put(STATUS_RENAMED_ONLY, filledStyle(wb, 0xFD, 0xE5, 0x9A)); // yellow
        styles.put(STATUS_CHANGED, filledStyle(wb, 0xF4, 0xA6, 0xA6));      // red
        styles.put(STATUS_ADDED, filledStyle(wb, 0xB6, 0xE2, 0xB6));        // green
        styles.put(STATUS_REMOVED, filledStyle(wb, 0xD9, 0xD9, 0xD9));      // gray
        return styles;
    }

    private static CellStyle filledStyle(XSSFWorkbook wb, int r, int g, int b) {
        CellStyle style = wb.createCellStyle();
        style.setFillForegroundColor(new XSSFColor(new byte[]{(byte) r, (byte) g, (byte) b}, null));
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        return style;
    }
}

package com.utilities.FileCopier;

import java.io.*;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.zip.*;

public class FileCopier {

    private static final DateTimeFormatter TIMESTAMP_FMT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    public static void main(String[] args) throws Exception {

        // ── Locate the directory where the JAR lives ────────────────────────
        String jarDir = FileCopier.class.getProtectionDomain()
                .getCodeSource().getLocation().toURI().getPath();
        jarDir = new File(jarDir).getParent();
        Path jarDirPath = Paths.get(jarDir);

        File inputFile = new File(jarDir, "input.txt");

        // ── Read input.txt ──────────────────────────────────────────────────
        if (!inputFile.exists()) {
            System.err.println("ERROR: input.txt not found at " + inputFile.getAbsolutePath());
            Runtime.getRuntime().halt(1);
        }

        List<String> lines = new ArrayList<>();
        try (BufferedReader br = new BufferedReader(new FileReader(inputFile))) {
            String line;
            while ((line = br.readLine()) != null) {
                line = line.trim();
                if (!line.isEmpty()) lines.add(line);
            }
        }

        if (lines.isEmpty()) {
            System.err.println("ERROR: input.txt is empty.");
            Runtime.getRuntime().halt(1);
        }

        List<String> sourcePaths = lines;

        // ── Fixed output folder: always "output" next to the JAR ────────────
        Path outputDir = jarDirPath.resolve("output");

        // Delete old output folder completely, then recreate fresh
        if (Files.exists(outputDir)) {
            deleteDirectory(outputDir);
        }
        Files.createDirectories(outputDir);

        // output.txt lives inside the output folder
        File outputFile = outputDir.resolve("output.txt").toFile();

        // ── Process everything ───────────────────────────────────────────────
        try (BufferedWriter log = new BufferedWriter(new FileWriter(outputFile, false))) {

            writeLine(log, "=== FileCopier Run: " + now() + " ===");
            writeLine(log, "Output directory : " + outputDir.toAbsolutePath());
            writeLine(log, "Source entries   : " + sourcePaths.size());
            writeLine(log, "");

            int passed = 0, failed = 0;

            for (String rawPath : sourcePaths) {
                writeLine(log, "── Source: " + rawPath);

                boolean isGlob = rawPath.endsWith("/*");

                if (isGlob) {
                    String parentStr = rawPath.substring(0, rawPath.length() - 2);
                    Path parentPath  = Paths.get(parentStr);

                    if (!Files.exists(parentPath)) {
                        writeLine(log, "   [FAIL] Directory does not exist: " + parentPath);
                        failed++;
                    } else if (!Files.isDirectory(parentPath)) {
                        writeLine(log, "   [FAIL] Not a directory (glob path): " + parentPath);
                        failed++;
                    } else {
                        try (DirectoryStream<Path> stream = Files.newDirectoryStream(parentPath)) {
                            boolean anyFile = false;
                            for (Path child : stream) {
                                if (Files.isRegularFile(child)) {
                                    anyFile = true;
                                    Path target = resolveUnique(outputDir, child.getFileName().toString());
                                    try {
                                        Files.copy(child, target, StandardCopyOption.REPLACE_EXISTING);
                                        writeLine(log, "   [OK]  Copied: " + child + " → " + target.getFileName());
                                        passed++;
                                    } catch (IOException e) {
                                        writeLine(log, "   [FAIL] Cannot copy: " + child + " → " + e.getMessage());
                                        failed++;
                                    }
                                }
                            }
                            if (!anyFile) {
                                writeLine(log, "   [INFO] No files found inside: " + parentPath);
                            }
                        } catch (IOException e) {
                            writeLine(log, "   [FAIL] Cannot list directory: " + parentPath + " → " + e.getMessage());
                            failed++;
                        }
                    }

                } else {
                    Path sourcePath = Paths.get(rawPath);

                    if (!Files.exists(sourcePath)) {
                        writeLine(log, "   [FAIL] Path does not exist: " + sourcePath);
                        failed++;

                    } else if (Files.isRegularFile(sourcePath)) {
                        Path target = resolveUnique(outputDir, sourcePath.getFileName().toString());
                        try {
                            Files.copy(sourcePath, target, StandardCopyOption.REPLACE_EXISTING);
                            writeLine(log, "   [OK]  Copied file: " + sourcePath + " → " + target.getFileName());
                            passed++;
                        } catch (IOException e) {
                            writeLine(log, "   [FAIL] Cannot copy file: " + sourcePath + " → " + e.getMessage());
                            failed++;
                        }

                    } else if (Files.isDirectory(sourcePath)) {
                        Path targetDir = outputDir.resolve(sourcePath.getFileName());
                        try {
                            copyDirectory(sourcePath, targetDir, log);
                            writeLine(log, "   [OK]  Copied directory: " + sourcePath + " → " + targetDir.getFileName());
                            passed++;
                        } catch (IOException e) {
                            writeLine(log, "   [FAIL] Cannot copy directory: " + sourcePath + " → " + e.getMessage());
                            failed++;
                        }

                    } else {
                        writeLine(log, "   [FAIL] Not a file or directory: " + sourcePath);
                        failed++;
                    }
                }

                writeLine(log, "");
            }

            // ── Summary ──────────────────────────────────────────────────────
            writeLine(log, "── Summary ───────────────────────────────────────");
            writeLine(log, "   Passed : " + passed);
            writeLine(log, "   Failed : " + failed);
            writeLine(log, "");

            log.flush();

            // ── ZIP: always named "output.zip", overwrite previous ────────────
            Path zipPath = jarDirPath.resolve("output.zip");

            // Delete old zip if exists
            Files.deleteIfExists(zipPath);

            writeLine(log, "── Creating ZIP: " + zipPath.toAbsolutePath());
            log.flush();

            try {
                createZip(outputDir, zipPath);
                writeLine(log, "   [OK]  ZIP created: " + zipPath.toAbsolutePath());
            } catch (IOException e) {
                writeLine(log, "   [FAIL] ZIP failed: " + e.getMessage());
            }

            writeLine(log, "");
            writeLine(log, "=== Done: " + now() + " ===");

            System.out.println("Done.");
            System.out.println("Output folder : " + outputDir.toAbsolutePath());
            System.out.println("ZIP           : " + zipPath.toAbsolutePath());
            System.out.println("Passed: " + passed + "  |  Failed: " + failed);

        } // log writer closed — output.txt fully written

        // ── Kill the JVM ─────────────────────────────────────────────────────
        Runtime.getRuntime().halt(0);
    }

    // ── Delete a directory and all its contents ──────────────────────────────
    private static void deleteDirectory(Path dir) throws IOException {
        Files.walkFileTree(dir, new SimpleFileVisitor<Path>() {
            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                Files.delete(file);
                return FileVisitResult.CONTINUE;
            }
            @Override
            public FileVisitResult postVisitDirectory(Path d, IOException exc) throws IOException {
                Files.delete(d);
                return FileVisitResult.CONTINUE;
            }
        });
    }

    // ── Copy an entire directory tree recursively ───────────────────────────
    private static void copyDirectory(Path src, Path dest, BufferedWriter log) throws IOException {
        Files.walkFileTree(src, new SimpleFileVisitor<Path>() {

            @Override
            public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs)
                    throws IOException {
                Files.createDirectories(dest.resolve(src.relativize(dir)));
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs)
                    throws IOException {
                Path target = dest.resolve(src.relativize(file));
                try {
                    Files.copy(file, target, StandardCopyOption.REPLACE_EXISTING);
                    writeLine(log, "      [OK]  " + file);
                } catch (IOException e) {
                    writeLine(log, "      [FAIL] " + file + " → " + e.getMessage());
                }
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult visitFileFailed(Path file, IOException exc)
                    throws IOException {
                writeLine(log, "      [FAIL] Cannot access: " + file + " → " + exc.getMessage());
                return FileVisitResult.CONTINUE;
            }
        });
    }

    // ── ZIP the output directory ─────────────────────────────────────────────
    private static void createZip(Path sourceDir, Path zipPath) throws IOException {
        Path parent = sourceDir.getParent();
        try (ZipOutputStream zos = new ZipOutputStream(
                new BufferedOutputStream(new FileOutputStream(zipPath.toFile())))) {

            Files.walkFileTree(sourceDir, new SimpleFileVisitor<Path>() {

                @Override
                public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs)
                        throws IOException {
                    if (!dir.equals(sourceDir)) {
                        zos.putNextEntry(new ZipEntry(parent.relativize(dir) + "/"));
                        zos.closeEntry();
                    }
                    return FileVisitResult.CONTINUE;
                }

                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs)
                        throws IOException {
                    zos.putNextEntry(new ZipEntry(parent.relativize(file).toString()));
                    Files.copy(file, zos);
                    zos.closeEntry();
                    return FileVisitResult.CONTINUE;
                }
            });
        }
    }

    // ── Avoid name collisions when two sources have the same filename ─────────
    private static Path resolveUnique(Path dir, String fileName) {
        Path candidate = dir.resolve(fileName);
        if (!Files.exists(candidate)) return candidate;

        String base = fileName;
        String ext  = "";
        int dot = fileName.lastIndexOf('.');
        if (dot > 0) {
            base = fileName.substring(0, dot);
            ext  = fileName.substring(dot);
        }
        int counter = 1;
        do {
            candidate = dir.resolve(base + "_" + counter + ext);
            counter++;
        } while (Files.exists(candidate));
        return candidate;
    }

    private static void writeLine(BufferedWriter bw, String text) throws IOException {
        bw.write(text);
        bw.newLine();
    }

    private static String now() {
        return LocalDateTime.now().format(TIMESTAMP_FMT);
    }
}
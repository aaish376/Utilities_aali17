package com.utilities.FindInJars;

import java.util.*;
import java.io.*;
import java.util.jar.*;
import java.util.Enumeration;

public class FindInJars {

    public static void main(String[] args) {
        String libsToSearchPath = "D:\\projects\\WEB-REPO\\CAdmin\\CAdmin3\\WebContent\\WEB-INF\\lib";
        // Get the directory where this source file is located
        String basePath = System.getProperty("user.dir") + File.separator + "src" + File.separator + "com" + File.separator + "utilities" + File.separator + "FindInJars" + File.separator;

        String outputJsPath = basePath + "search_results.js";
        String htmlViewPath = basePath + "viewer.html";

        List<String> searchTextList = new ArrayList<>();
        searchTextList.add("package com.i2c.wservice.bean");

        // Root JSON array: one object per search text
        StringBuilder json = new StringBuilder();
        json.append("[\n");

        for (int i = 0; i < searchTextList.size(); i++) {
            String searchText = searchTextList.get(i);
            System.out.println("-----------------------------------------------------------");
            System.out.println("Searching : " + searchText);
            System.out.println("-----------------------------------------------------------");

            Map<String, List<Map<String, Object>>> jarResults = searchText(libsToSearchPath, searchText);

            json.append("  {\n");
            json.append("    \"searchText\": ").append(jsonString(searchText)).append(",\n");
            json.append("    \"jars\": [\n");

            List<String> jarNames = new ArrayList<>(jarResults.keySet());
            for (int j = 0; j < jarNames.size(); j++) {
                String jarName = jarNames.get(j);
                List<Map<String, Object>> matches = jarResults.get(jarName);

                json.append("      {\n");
                json.append("        \"jar\": ").append(jsonString(jarName)).append(",\n");
                json.append("        \"classes\": [\n");

                for (int k = 0; k < matches.size(); k++) {
                    Map<String, Object> match = matches.get(k);
                    json.append("          {\n");
                    json.append("            \"className\": ").append(jsonString((String) match.get("className"))).append(",\n");
                    json.append("            \"classPath\": ").append(jsonString((String) match.get("classPath"))).append(",\n");
                    json.append("            \"occurrences\": ").append(match.get("occurrences")).append("\n");
                    json.append("          }");
                    if (k < matches.size() - 1) json.append(",");
                    json.append("\n");
                }

                json.append("        ]\n");
                json.append("      }");
                if (j < jarNames.size() - 1) json.append(",");
                json.append("\n");
            }

            json.append("    ]\n");
            json.append("  }");
            if (i < searchTextList.size() - 1) json.append(",");
            json.append("\n");
        }

        json.append("]\n");

        // Write JS file: var resultData = [...];
        // This lets the HTML load it via <script src="search_results.js">
        // without needing any web server — works directly from file://
        try (FileWriter fw = new FileWriter(outputJsPath)) {
            fw.write("var resultData = ");
            fw.write(json.toString());
            fw.write(";\n");

            System.out.println("-----------------------------------------------------------");
            System.out.println("--- Writing results to JS file:");
            System.out.println("-----------------------------------------------------------");
            System.out.println("Results written to: " + outputJsPath);
        } catch (IOException e) {
            System.out.println("Error writing JS output file.");
            e.printStackTrace();
        }

        // Open the HTML viewer in Microsoft Edge
        try {
            String htmlFilePath = new File(htmlViewPath).getAbsolutePath();
            ProcessBuilder pb = new ProcessBuilder("cmd", "/c", "start", "msedge", htmlFilePath);
            pb.start();
            System.out.println("Opened viewer in Microsoft Edge: " + htmlFilePath);
        } catch (IOException e) {
            System.out.println("Error opening HTML viewer in Edge.");
            e.printStackTrace();
        }
    }

    /**
     * Searches for the given text across all JARs in the directory.
     *
     * @return Map of jarName -> list of match info maps (className, classPath, occurrences)
     */
    private static Map<String, List<Map<String, Object>>> searchText(String directoryPath, String searchText) {
        String textToSearch = replaceTokens(searchText);
        Map<String, List<Map<String, Object>>> jarResults = new LinkedHashMap<>();

        File rootDirectory = new File(directoryPath);
        if (!rootDirectory.exists() || !rootDirectory.isDirectory()) {
            System.out.println("Invalid directory: " + directoryPath);
            return jarResults;
        }

        File[] jarFiles = rootDirectory.listFiles((d, name) -> name.endsWith(".jar"));
        if (jarFiles == null || jarFiles.length == 0) {
            System.out.println("No JAR files found in the directory.");
            return jarResults;
        }

        for (File jarFile : jarFiles) {
            List<Map<String, Object>> matches = new ArrayList<>();

            System.out.println("-----------------------------------------------------------");
            System.out.println("--- In Jar: " + jarFile.getName()  );
            System.out.println("-----------------------------------------------------------");

            try (JarFile jar = new JarFile(jarFile)) {
                Enumeration<JarEntry> entries = jar.entries();

                Boolean anyFound=false;
                while (entries.hasMoreElements()) {
                    JarEntry entry = entries.nextElement();

                    if (!entry.getName().endsWith(".class")) continue;

                    int occurrences = 0;
                    try (InputStream is = jar.getInputStream(entry);
                         BufferedReader reader = new BufferedReader(new InputStreamReader(is))) {
                        String line;
                        while ((line = reader.readLine()) != null) {
                            if (line.contains(textToSearch)) {
                                occurrences++;
                            }
                        }
                    }

                    if (occurrences > 0) {
                        anyFound=true;
                        String fullPath = entry.getName();                          // e.g. com/example/Foo.class
                        String className = extractClassName(fullPath);             // e.g. Foo

                        Map<String, Object> match = new LinkedHashMap<>();
                        match.put("className", className);
                        match.put("classPath", fullPath);
                        match.put("occurrences", occurrences);
                        matches.add(match);

                        System.out.println("Found [ "+occurrences+ " ] occurrence(s)  in JAR [" + jarFile.getName() + "] -> " + fullPath );
                    }

                }
                if(!anyFound) {
                    System.out.println("\u001B[31mNo occurrences found in JAR: " + jarFile.getName() + "\u001B[0m");
                }

            } catch (IOException e) {
                System.out.println("Error reading JAR file: " + jarFile.getName());
                e.printStackTrace();
            }

            if (!matches.isEmpty()) {
                jarResults.put(jarFile.getName(), matches);
            }
        }

        return jarResults;
    }

    /**
     * Extracts the simple class name from a full path like com/example/MyClass.class
     */
    private static String extractClassName(String fullPath) {
        String name = fullPath;
        int lastSlash = name.lastIndexOf('/');
        if (lastSlash >= 0) {
            name = name.substring(lastSlash + 1);
        }
        if (name.endsWith(".class")) {
            name = name.substring(0, name.length() - 6);
        }
        return name;
    }

    /**
     * Replaces dots with slashes for matching binary class content.
     */
    private static String replaceTokens(String input) {
        if (input == null) return null;
        return input.replace('.', '/');
    }

    /**
     * Escapes a string for safe inclusion in JSON.
     */
    private static String jsonString(String value) {
        if (value == null) return "null";
        return "\"" + value
                .replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t")
                + "\"";
    }
}
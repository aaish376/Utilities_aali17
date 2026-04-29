
# Utilities Hub

A comprehensive desktop application suite featuring four powerful utilities for developers and database administrators. Built with Java Swing, Utilities Hub provides a unified, modern interface for common data and log processing tasks.

**Version:** 1.0  
**Created by:** ASAD ALI AAiSH

---

## Table of Contents

- [Overview](#overview)
- [Features](#features)
- [Installation & Setup](#installation--setup)
- [Getting Started](#getting-started)
- [Utilities Guide](#utilities-guide)
  - [1. Insert Formatter](#1-insert-formatter)
  - [2. Log Filter](#2-log-filter)
  - [3. Unload to Insert](#3-unload-to-insert)
  - [4. Jar Inspector](#4-jar-inspector)
- [System Requirements](#system-requirements)
- [Troubleshooting](#troubleshooting)

---

## Overview

Utilities Hub is a collection of four integrated tools designed to streamline common development workflows:

- **Quick data transformation** between different formats
- **Efficient log file searching and filtering**
- **SQL export/import conversion**
- **JAR file content searching**

All tools are accessible from a centralized hub with a modern, dark-themed interface.

---

## Features

✓ **Modern GUI** - Intuitive dark theme with neon accents  
✓ **Four Powerful Tools** - Each designed for specific development tasks  
✓ **Real-time Processing** - Instant feedback and results  
✓ **Multi-platform** - Runs on Windows, macOS, and Linux  
✓ **No External Dependencies** - Self-contained Java application  

---

## Installation & Setup

### Prerequisites

- **Java Runtime Environment (JRE)** 8 or higher
- Minimum 512 MB RAM recommended

### Running the Application

1. **Extract the Application**
   - Extract the provided JAR file to your desired location
   - Navigate to the application directory

2. **Launch the Application**
   - **Windows:** Double-click `Utilities_aali17.jar` or run:
     ```bash
     java -jar Utilities_aali17.jar
     ```
   - **macOS/Linux:** Open Terminal and run:
     ```bash
     java -jar Utilities_aali17.jar
     ```

3. **Application Window**
   - The Utilities Hub main window will open with a home screen
   - Select any utility tile to begin

---

## Getting Started

### Launching a Utility

1. **Open Utilities Hub** and view the home screen
2. **Click on a utility tile** to open that tool:
   - ⌗ INSERT FORMATTER
   - ≡ LOG FILTER
   - ⇄ UNLOAD→INSERT
   - ⬡ JAR INSPECTOR
3. **Use the toolbar** to perform operations
4. **Click "⌂ home"** in the navigation bar to return to the home screen

### Common Actions

- **Browse Files** - Click the folder icon to open file chooser dialogs
- **Copy Results** - Most results can be copied to clipboard
- **Save Output** - Results can typically be saved to output files
- **Clear Fields** - Use Reset or Clear buttons to start fresh

---

## Utilities Guide

### 1. INSERT Formatter

**Purpose:** Map INSERT statement columns to their corresponding values in a structured table format.

**Use Case:** Format database INSERT commands for easier reading and manipulation.

#### How to Use

1. Open the **INSERT FORMATTER** utility
2. **Input Format:**
   - Provide your INSERT statement or column-value pairs
   - Enter the table name (if required)
   - Specify column names
3. **Process:**
   - The formatter will map columns to values
   - Results display in an organized table format
4. **Output:**
   - Copy formatted output to clipboard
   - Save to file
   - Display in structured view

#### Tips

- Works best with standard SQL INSERT syntax
- Handles multiple rows of data
- Preserves data integrity and types

---

### 2. Log Filter

**Purpose:** Search and filter large log files by keywords, log levels, or custom criteria.

**Use Case:** Quickly isolate relevant log entries from massive log files for debugging.

#### How to Use

1. Open **LOG FILTER** utility
2. **Load Log File:**
   - Click the folder icon to browse and select a `.log` file
   - File info (size, line count) displays automatically
3. **Enter Search Criteria:**
   - Type keyword(s) in the search field
   - Use comma-separated values for multiple keywords
4. **Configure Options:**
   - **Case Sensitive** - Check to match exact case
   - **Log Level** - Filter by ERROR, WARN, INFO, DEBUG (if available)
5. **Filter:**
   - Click the **FILTER** button
   - Matching lines display with match counts
6. **Save Results:**
   - Export filtered results to a new file
   - View statistics (matched lines, total processed)

#### Example

| Scenario | Input | Result |
|----------|-------|--------|
| Find all errors | keyword: `ERROR` | All ERROR level entries |
| Find specific exception | keyword: `NullPointerException` | All lines containing that exception |
| Multi-keyword search | keywords: `ERROR, CRITICAL` | All ERROR or CRITICAL entries |

---

### 3. Unload to Insert

**Purpose:** Convert database unload/export files back into valid SQL INSERT statements.

**Use Case:** Transform exported data (from UNLOAD operations) into executable INSERT commands.

#### How to Use

1. Open **UNLOAD→INSERT** utility
2. **Input Panel:**
   - Paste exported/unload data (typically pipe or comma-delimited)
   - Shows preview of data structure
3. **Configuration:**
   - **Delimiter** - Specify the field separator (`,`, `|`, `\t`, etc.)
   - Use **Custom Delimiter** checkbox for non-standard separators
4. **SELECT Statement (Optional):**
   - Provide the original SELECT statement for reference
   - Helps map columns correctly
5. **Convert:**
   - Click **Convert** to generate INSERT statements
   - Specify target table name
6. **Output:**
   - Review generated SQL INSERT statements
   - Verify column mappings
   - Copy to clipboard or save to file

#### Example

Input (pipe-delimited):
```
101|John|Doe|2024-01-15
102|Jane|Smith|2024-01-16
```

Output (SQL INSERT):
```sql
INSERT INTO users (id, first_name, last_name, created_date) 
VALUES (101, 'John', 'Doe', '2024-01-15');
INSERT INTO users (id, first_name, last_name, created_date) 
VALUES (102, 'Jane', 'Smith', '2024-01-16');
```

---

### 4. Jar Inspector

**Purpose:** Search for text content within Java JAR and class files.

**Use Case:** Find specific class names, methods, or string literals across multiple JAR files without extracting them.

#### How to Use

1. Open **JAR INSPECTOR** utility
2. **Configure Paths:**
   - **Library Path** - Browse to folder containing JAR files (or individual JAR)
   - **Output Path** - Choose where to save search results
3. **Enter Keywords:**
   - Type words or phrases to search for (one per line or comma-separated)
   - Examples: class names, method signatures, string constants
4. **Search:**
   - Click **FIND** to begin searching
   - Progress displays in log area
   - Real-time status updates
5. **Results:**
   - Matching files and lines display in results panel
   - Click **Show Report** to view detailed findings
   - **Open in Explorer** to navigate to output folder

#### Search Examples

| Search For | Purpose |
|------------|---------|
| `className` | Find all usages of a specific class |
| `methodName` | Locate method definitions |
| `packageName` | Search for packages in JARs |
| `"String literal"` | Find hardcoded strings |

#### Output Format

Results save in structured format showing:
- JAR file name
- Location within JAR
- Matching lines/context
- File references

---

## System Requirements

| Requirement | Minimum | Recommended |
|-------------|---------|-------------|
| **Java Version** | JRE 8 | JRE 11 or higher |
| **Memory (RAM)** | 512 MB | 2 GB |
| **Disk Space** | 100 MB | 500 MB |
| **OS** | Windows 7+ | Windows 10+, macOS 10.12+, Ubuntu 18.04+ |
| **Display** | 1024x768 | 1920x1080 or higher |

---

## Troubleshooting

### Application Won't Start

**Problem:** JAR file won't open or Java error appears

**Solutions:**
- Verify Java is installed: `java -version`
- Install/update Java from [java.com](https://www.java.com)
- Try running from command line to see detailed error
- Ensure sufficient disk space

### File Access Issues

**Problem:** "File not found" or "Permission denied" errors

**Solutions:**
- Verify file paths are correct and accessible
- Ensure you have read/write permissions on directories
- Try using absolute paths instead of relative paths
- Close files if they're open in other applications

### Memory Issues

**Problem:** Application becomes slow or crashes with large files

**Solutions:**
- Close other applications to free RAM
- Use smaller input files or process in batches
- Run from command line with increased memory:
  ```bash
  java -Xmx2048m -jar Utilities_aali17.jar
  ```

### Log Filter Issues

**Problem:** Filter returns no results for keywords that exist

**Solutions:**
- Check "Case Sensitive" checkbox setting
- Verify exact spelling of search terms
- Try searching for partial keywords
- Ensure log file format is recognized

### JAR Inspector Issues

**Problem:** Can't find content in JAR files

**Solutions:**
- Verify JARs are valid and not corrupted
- Ensure search keywords match exactly
- Try simpler keywords to test
- Check output folder for detailed logs

### UI Display Issues

**Problem:** Buttons cut off or text not visible

**Solutions:**
- Resize the application window
- Adjust Java display settings:
  ```bash
  java -Dsun.java2d.uiScale=1.0 -jar Utilities_aali17.jar
  ```
- Try different screen resolution
- Update graphics drivers

---

## Tips & Best Practices

### For Best Performance

1. **Batch Processing** - Split large files into smaller chunks
2. **Keyword Specificity** - Use more specific keywords for faster filtering
3. **Output Organization** - Create separate output folders for different tasks
4. **Regular Cleanup** - Remove old output files periodically

### General Usage Tips

- **Copy Easily** - Most text areas support Ctrl+A and Ctrl+C
- **Keyboard Shortcuts** - Use Tab to navigate between fields
- **Undo/Redo** - Right-click text areas for context menu options
- **Drag & Drop** - Some panels support dragging files directly

### Security Notes

- Application runs locally; no data is sent online
- Process large files in steps to prevent memory issues
- Backup important files before processing
- Review generated SQL before executing in production

---

## Version History

**v1.0** (Current)
- Initial release
- Four core utilities: Insert Formatter, Log Filter, Unload to Insert, Jar Inspector
- Modern dark-themed GUI
- Cross-platform Java Swing implementation

---

## Support & Feedback

For issues or suggestions:
- Review the Troubleshooting section above
- Check the utility-specific guides
- Verify system requirements are met

---

**Made with ♥ by ASAD ALI AAiSH**

---

## License

This application is provided as-is. Ensure you have proper licenses for any data or files you process.

---
```

This README provides:
- ✅ Clear overview of all 4 utilities
- ✅ Step-by-step usage instructions for each tool
- ✅ System requirements and installation guide
- ✅ Examples and use cases
- ✅ Troubleshooting section
- ✅ Tips for optimal performance
- ✅ Professional formatting and structure

You can save this as `README.md` in your project root directory.

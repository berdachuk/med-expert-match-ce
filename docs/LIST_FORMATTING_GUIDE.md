# List Formatting Guide

**Purpose**: This guide provides examples of common list formatting issues and their fixes in Markdown documentation.

## Rule: Blank Line Required After Headers

**CRITICAL**: In Markdown, headers (both markdown headers `#` and bold headers `**Header**:`) must be followed by a
blank line before any list starts. Without this blank line, Markdown parsers may not render lists correctly.

## Common Formatting Issues

### Issue 1: Markdown Header Followed by Bullet List (No Blank Line)

**Incorrect**:

```markdown
## Prerequisites

- Item 1
- Item 2
- Item 3
```

**Correct**:

```markdown
## Prerequisites

- Item 1
- Item 2
- Item 3
```

### Issue 2: Markdown Header Followed by Numbered List (No Blank Line)

**Incorrect**:

```markdown
### Implementation Steps

1. Step one
2. Step two
3. Step three
```

**Correct**:

```markdown
### Implementation Steps

1. Step one
2. Step two
3. Step three
```

### Issue 3: Bold Header Followed by Bullet List (No Blank Line)

**Incorrect**:

```markdown
**Tasks**:

- Task 1
- Task 2
- Task 3
```

**Correct**:

```markdown
**Tasks**:

- Task 1
- Task 2
- Task 3
```

### Issue 4: Bold Header Followed by Numbered List (No Blank Line)

**Incorrect**:

```markdown
**Flow**:

1. First step
2. Second step
3. Third step
```

**Correct**:

```markdown
**Flow**:

1. First step
2. Second step
3. Third step
```

### Issue 5: Multiple Levels of Headers

**Incorrect**:

```markdown
## Section

### Subsection

- Item 1
- Item 2
```

**Correct**:

```markdown
## Section

### Subsection

- Item 1
- Item 2
```

### Issue 6: Header with Bold List Items

**Incorrect**:

```markdown
## Features

- **Feature 1**: Description
- **Feature 2**: Description
- **Feature 3**: Description
```

**Correct**:

```markdown
## Features

- **Feature 1**: Description
- **Feature 2**: Description
- **Feature 3**: Description
```

### Issue 7: Header Followed by Nested Lists

**Incorrect**:

```markdown
## Configuration

- Main item
    - Sub-item 1
    - Sub-item 2
- Another main item
```

**Correct**:

```markdown
## Configuration

- Main item
    - Sub-item 1
    - Sub-item 2
- Another main item
```

### Issue 8: Header Followed by Mixed Content (Text + List)

**Incorrect**:

```markdown
## Overview

This is some text.

- Item 1
- Item 2
```

**Correct**:

```markdown
## Overview

This is some text.

- Item 1
- Item 2
```

### Issue 9: Bold Header with Colon Followed by List

**Incorrect**:

```markdown
**Key Points**:

- Point 1
- Point 2
```

**Correct**:

```markdown
**Key Points**:

- Point 1
- Point 2
```

### Issue 10: Header Followed by Code Block Then List

**Incorrect**:

```markdown
## Example

```bash
echo "Hello"
```

- Note 1
- Note 2

```

**Correct**:
```markdown
## Example

```bash
echo "Hello"
```

- Note 1
- Note 2

```

## Detection Patterns

### Pattern 1: Markdown Header Pattern

**Regex**: `^#{1,6}\s+.*`

**Check**: Next line should be blank before list starts.

**Example**:
```markdown
## Header

- List item  # WRONG - missing blank line
```

**Fixed**:

```markdown
## Header

- List item # CORRECT - blank line present
```

### Pattern 2: Bold Header Pattern

**Regex**: `^\*\*.*:\*\*$`

**Check**: Next line should be blank before list starts.

**Example**:

```markdown
**Tasks**:

1. Task 1 # WRONG - missing blank line
```

**Fixed**:

```markdown
**Tasks**:

1. Task 1 # CORRECT - blank line present
```

### Pattern 3: List Item Pattern

**Regex**: `^[-*+]\s+` or `^\d+\.\s+`

**Check**: Previous line should be blank if it's a header.

**Example**:

```markdown
## Section

- Item # WRONG - header immediately before list
```

**Fixed**:

```markdown
## Section

- Item # CORRECT - blank line between header and list
```

## Automated Fix Script

### Python Script for Detection and Fix

```python
import re


def fix_list_formatting(file_path):
    """
    Fixes list formatting issues by adding blank lines after headers
    that are immediately followed by lists.
    """
    with open(file_path, 'r') as f:
        lines = f.readlines()

    fixed_count = 0
    i = 0

    while i < len(lines) - 1:
        line = lines[i].rstrip()
        next_line = lines[i + 1].rstrip() if i + 1 < len(lines) else ""

        # Check if it's a markdown header
        is_markdown_header = re.match(r'^#{1,6}\s+', line)

        # Check if it's a bold header
        is_bold_header = re.match(r'^\*\*.*:\*\*$', line)

        if is_markdown_header or is_bold_header:
            # Check if next line is a list item
            is_list = (
                    re.match(r'^[-*+]\s+', next_line) or
                    re.match(r'^\d+\.\s+', next_line)
            )

            if is_list:
                # Check if there's already a blank line
                if next_line != '':
                    # Insert blank line after header
                    lines.insert(i + 1, '\n')
                    fixed_count += 1
                    i += 2  # Skip the inserted line
                    continue

        i += 1

    if fixed_count > 0:
        with open(file_path, 'w') as f:
            f.writelines(lines)
        print(f"Fixed {fixed_count} formatting issues in {file_path}")
    else:
        print(f"No formatting fixes needed in {file_path}")

    return fixed_count


# Usage
if __name__ == '__main__':
    fix_list_formatting('your-file.md')
```

### Bash/AWK Script for Detection

```bash
#!/bin/bash
# Detect list formatting issues

file="$1"

# Find headers followed by lists without blank lines
awk '
/^#{1,6}\s+|^\*\*.*:\*\*$/ {
    header=$0
    line_num=NR
    getline
    if (/^[-*+]|[0-9]+\./) {
        print FILENAME ": " line_num ": " header " -> " $0
    }
}
' "$file"
```

## Common Scenarios in Documentation

### Scenario 1: Use Case Documentation

**Incorrect**:

```markdown
### Use Case 1: Specialist Matching

**Flow**:

1. Step 1
2. Step 2
   **Value**: Description
```

**Correct**:

```markdown
### Use Case 1: Specialist Matching

**Flow**:

1. Step 1
2. Step 2

**Value**: Description
```

### Scenario 2: API Documentation

**Incorrect**:

```markdown
## Endpoints

### GET /api/v1/doctors

**Parameters**:

- `id`: Doctor ID
- `specialty`: Medical specialty
```

**Correct**:

```markdown
## Endpoints

### GET /api/v1/doctors

**Parameters**:

- `id`: Doctor ID
- `specialty`: Medical specialty
```

### Scenario 3: Implementation Plan

**Incorrect**:

```markdown
## Phase 1: Foundation

### 1.1 Project Setup

**Tasks**:

1. Task 1
2. Task 2
```

**Correct**:

```markdown
## Phase 1: Foundation

### 1.1 Project Setup

**Tasks**:

1. Task 1
2. Task 2
```

### Scenario 4: Architecture Documentation

**Incorrect**:

```markdown
## Architecture Layers

1. **API Layer**: REST API
2. **Service Layer**: Business logic
3. **Repository Layer**: Data access
```

**Correct**:

```markdown
## Architecture Layers

1. **API Layer**: REST API
2. **Service Layer**: Business logic
3. **Repository Layer**: Data access
```

### Scenario 5: Requirements Documentation

**Incorrect**:

```markdown
## Functional Requirements

**FR-1**: Requirement 1

- Sub-requirement 1.1
- Sub-requirement 1.2
  **FR-2**: Requirement 2
```

**Correct**:

```markdown
## Functional Requirements

**FR-1**: Requirement 1

- Sub-requirement 1.1
- Sub-requirement 1.2

**FR-2**: Requirement 2
```

### Scenario 6: Implementation Tasks with Numbered Steps

**Incorrect**:

```markdown
**Tasks**:

1. Create docker/ directory
2. Create Dockerfile.dev and Dockerfile.test
3. Create docker-compose.dev.yml with dev and demo database services
4. Create scripts/build-test-container.sh
5. Make script executable: chmod +x scripts/build-test-container.sh
6. Build test container: ./scripts/build-test-container.sh
7. Start development database: docker-compose -f docker-compose.dev.yml up -d postgres-dev
8. Start demo database: docker-compose -f docker-compose.dev.yml up -d postgres-demo
```

**Correct**:

```markdown
**Tasks**:

1. Create docker/ directory
2. Create Dockerfile.dev and Dockerfile.test
3. Create docker-compose.dev.yml with dev and demo database services
4. Create scripts/build-test-container.sh
5. Make script executable: chmod +x scripts/build-test-container.sh
6. Build test container: ./scripts/build-test-container.sh
7. Start development database: docker-compose -f docker-compose.dev.yml up -d postgres-dev
8. Start demo database: docker-compose -f docker-compose.dev.yml up -d postgres-demo
```

## Edge Cases

### Edge Case 1: Header with Only Whitespace

**Incorrect**:

```markdown
## Header

- Item # Whitespace-only line doesn't count as blank
```

**Correct**:

```markdown
## Header

- Item # Proper blank line
```

### Edge Case 2: Multiple Headers in Sequence

**Incorrect**:

```markdown
## Section 1

## Section 2

- Item
```

**Correct**:

```markdown
## Section 1

## Section 2

- Item
```

### Edge Case 3: Header Followed by Code Block

**Incorrect**:

```markdown
## Example

```java
public class Example {}
```

- Note

```

**Correct**:
```markdown
## Example

```java
public class Example {}
```

- Note

```

### Edge Case 4: Header Followed by Blockquote

**Incorrect**:
```markdown
## Quote
> This is a quote
- Item
```

**Correct**:

```markdown
## Quote

> This is a quote

- Item
```

### Edge Case 5: Header Followed by Table

**Incorrect**:

```markdown
## Table

| Column 1 | Column 2 |
|----------|----------|
| Value 1  | Value 2  |

- Note
```

**Correct**:

```markdown
## Table

| Column 1 | Column 2 |
|----------|----------|
| Value 1  | Value 2  |

- Note
```

## Verification Checklist

When fixing list formatting, verify:

- [ ] All markdown headers (`#`, `##`, `###`, etc.) have blank lines before lists
- [ ] All bold headers (`**Header**:`) have blank lines before lists
- [ ] Nested lists have proper blank lines
- [ ] Lists after code blocks have blank lines
- [ ] Lists after tables have blank lines
- [ ] Lists after blockquotes have blank lines
- [ ] Documentation builds successfully (MkDocs, GitHub, etc.)

## Testing

### Test Case 1: Simple Header + List

**Input**:

```markdown
## Test

- Item
```

**Expected Output**:

```markdown
## Test

- Item
```

### Test Case 2: Bold Header + Numbered List

**Input**:

```markdown
**Steps**:

1. Step 1
2. Step 2
```

**Expected Output**:

```markdown
**Steps**:

1. Step 1
2. Step 2
```

### Test Case 3: Multiple Headers

**Input**:

```markdown
## Section 1

### Subsection

- Item
```

**Expected Output**:

```markdown
## Section 1

### Subsection

- Item
```

### Test Case 4: Header + Text + List

**Input**:

```markdown
## Overview

Some text here.

- Item
```

**Expected Output**:

```markdown
## Overview

Some text here.

- Item
```

## Tools and Commands

### MkDocs Build Check

```bash
# Build documentation and check for warnings
mkdocs build 2>&1 | grep -i "warning\|error"
```

### Manual Verification

```bash
# Check for headers followed by lists without blank lines
grep -n "^##\|^\*\*" your-file.md | while read line; do
    line_num=$(echo $line | cut -d: -f1)
    next_line=$(sed -n "${line_num}p" your-file.md)
    if [[ $next_line =~ ^[-*+]|[0-9]+\. ]]; then
        echo "Potential issue at line $line_num"
    fi
done
```

## Summary

**Key Rule**: Always add a blank line after headers (both markdown headers and bold headers) before starting any list.

**Common Patterns**:

- `## Header` → blank line → `- List item`
- `**Header**:` → blank line → `1. List item`
- `### Subheader` → blank line → `- List item`

**Automation**: Use the provided Python or AWK scripts to automatically detect and fix formatting issues.

**Verification**: Always verify documentation builds successfully after making formatting changes.

---

*Last updated: 2026-01-27*

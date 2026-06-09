#!/usr/bin/env bash
# Sanity tests for scripts/ralph/extract_patch.sh
# Each test should exit non-zero if extract_patch.sh behaves incorrectly.
# Run: bash scripts/ralph/test_extract_patch.sh

set -euo pipefail

REPO_ROOT="$(cd "$(dirname "$0")/../.." && pwd)"
EXTRACT="$REPO_ROOT/scripts/ralph/extract_patch.sh"

if [ ! -f "$EXTRACT" ]; then
    echo "SKIP: $EXTRACT does not exist yet (TDD: test before implementation)"
    exit 77
fi

fail=0
pass=0

TMP="$(mktemp -d)"
trap 'rm -rf "$TMP"' EXIT

# --- Case 1: one ```diff block → success, returned patch contains the diff ---
RESP1="$TMP/case1.txt"
cat > "$RESP1" <<'EOF'
Here's the patch you asked for:

```diff
--- a/foo.txt
+++ b/foo.txt
@@ -1,1 +1,1 @@
-old
+new
```

That should do it.
EOF
OUT1=$("$EXTRACT" < "$RESP1" 2>&1) || {
    echo "FAIL: extract on one diff block exited non-zero (rc=$?)"
    echo "Output: $OUT1"
    exit 1
}
PATCH1="$OUT1"
if [ -f "$PATCH1" ] && grep -q -- "^+new" "$PATCH1" && grep -q -- "^-old" "$PATCH1"; then
    echo "PASS: case 1: one diff block → returns patch with the changes"
    pass=$((pass + 1))
else
    echo "FAIL: case 1: expected patch file with +new/-old at $PATCH1"
    cat "$PATCH1" 2>/dev/null || echo "(patch path not a file)"
    fail=$((fail + 1))
fi

# --- Case 2: no ```diff block → exit 5 ---
RESP2="$TMP/case2.txt"
echo "I have no patch for you." > "$RESP2"
set +e
"$EXTRACT" < "$RESP2" >/dev/null 2>&1
rc2=$?
set -e
if [ "$rc2" -eq 5 ]; then
    echo "PASS: case 2: no diff block → exit 5"
    pass=$((pass + 1))
else
    echo "FAIL: case 2: expected exit 5, got $rc2"
    fail=$((fail + 1))
fi

# --- Case 3: two ```diff blocks → first is returned (not the second) ---
RESP3="$TMP/case3.txt"
cat > "$RESP3" <<'EOF'
First one:
```diff
--- a/first.txt
+++ b/first.txt
@@ -1,1 +1,1 @@
-FIRST_OLD
+FIRST_NEW
```

Then a second:
```diff
--- a/second.txt
+++ b/second.txt
@@ -1,1 +1,1 @@
-SECOND_OLD
+SECOND_NEW
```
EOF
OUT3=$("$EXTRACT" < "$RESP3" 2>&1) || {
    echo "FAIL: extract on two diff blocks exited non-zero (rc=$?)"
    exit 1
}
PATCH3="$OUT3"
if [ -f "$PATCH3" ] && grep -q "FIRST_NEW" "$PATCH3" && ! grep -q "SECOND_NEW" "$PATCH3"; then
    echo "PASS: case 3: two diff blocks → returns the first one"
    pass=$((pass + 1))
else
    echo "FAIL: case 3: expected first patch only at $PATCH3"
    cat "$PATCH3" 2>/dev/null || echo "(patch path not a file)"
    fail=$((fail + 1))
fi

echo
echo "Results: $pass passed, $fail failed"
exit $fail

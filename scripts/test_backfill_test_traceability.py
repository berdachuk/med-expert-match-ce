#!/usr/bin/env python3
"""Tests for backfill-test-traceability.py --check mode."""
from __future__ import annotations

import importlib.util
import json
import sys
import tempfile
import unittest
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
SCRIPT = ROOT / "scripts" / "backfill-test-traceability.py"


def load_module():
    spec = importlib.util.spec_from_file_location("backfill_test_traceability", SCRIPT)
    module = importlib.util.module_from_spec(spec)
    assert spec.loader is not None
    spec.loader.exec_module(module)
    return module


class BackfillTraceabilityCheckTest(unittest.TestCase):
    @classmethod
    def setUpClass(cls):
        cls.mod = load_module()

    def test_check_passes_on_valid_registry(self):
        code = self.mod.run_check(ROOT / ".agents/memory-bank/registry/test.jsonl")
        self.assertEqual(0, code)

    def test_check_fails_on_merged_json_line(self):
        with tempfile.TemporaryDirectory() as tmp:
            path = Path(tmp) / "test.jsonl"
            path.write_text(
                '{"id":"TEST-001","class":"a/FooIT","module":"core","reqRefs":["REQ-001"],"scnRefs":["SCN-001"]}'
                '{"id":"TEST-002","class":"a/BarIT","module":"core","reqRefs":["REQ-001"],"scnRefs":["SCN-001"]}\n'
            )
            code = self.mod.run_check(path)
            self.assertEqual(1, code)

    def test_check_fails_when_both_refs_empty(self):
        with tempfile.TemporaryDirectory() as tmp:
            path = Path(tmp) / "test.jsonl"
            path.write_text(
                json.dumps(
                    {
                        "id": "TEST-001",
                        "class": "a/FooIT",
                        "module": "core",
                        "reqRefs": [],
                        "scnRefs": [],
                    }
                )
                + "\n"
            )
            code = self.mod.run_check(path)
            self.assertEqual(1, code)

    def test_check_allows_provisional_without_refs(self):
        with tempfile.TemporaryDirectory() as tmp:
            path = Path(tmp) / "test.jsonl"
            path.write_text(
                json.dumps(
                    {
                        "id": "TEST-001",
                        "class": "a/FooIT",
                        "module": "core",
                        "status": "provisional",
                        "reqRefs": [],
                        "scnRefs": [],
                    }
                )
                + "\n"
            )
            code = self.mod.run_check(path)
            self.assertEqual(0, code)

    def test_sync_scn_test_refs_merges_from_test_jsonl(self):
        with tempfile.TemporaryDirectory() as tmp:
            test_path = Path(tmp) / "test.jsonl"
            scn_path = Path(tmp) / "scn.jsonl"
            test_path.write_text(
                "\n".join(
                    [
                        json.dumps(
                            {
                                "id": "TEST-001",
                                "class": "a/FooIT",
                                "module": "retrieval",
                                "reqRefs": ["REQ-001"],
                                "scnRefs": ["SCN-002"],
                            }
                        ),
                        json.dumps(
                            {
                                "id": "TEST-092",
                                "class": "b/BarIT",
                                "module": "retrieval",
                                "reqRefs": ["REQ-001"],
                                "scnRefs": ["SCN-002"],
                            }
                        ),
                    ]
                )
                + "\n"
            )
            scn_path.write_text(
                json.dumps(
                    {
                        "id": "SCN-002",
                        "title": "doctor-matcher",
                        "skill": "doctor-matcher",
                        "module": "retrieval",
                        "reqRefs": ["REQ-001"],
                        "testRefs": ["TEST-001"],
                        "status": "verified",
                        "featureFile": "features/doctor-matcher.feature",
                    }
                )
                + "\n"
            )
            updated = self.mod.sync_scn_test_refs(test_path, scn_path, dry_run=False)
            self.assertEqual(1, updated)
            scn_entries = self.mod.load_jsonl_strict(scn_path)
            self.assertEqual(["TEST-001", "TEST-092"], scn_entries[0]["testRefs"])


if __name__ == "__main__":
    raise SystemExit(unittest.main())

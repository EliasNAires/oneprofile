"""Tests of discover-skills, the run of skill discovery on a snapshot (#55, #64). The deciding agent is
a stub, so the tests spend nothing.

    python3 -m unittest discover -s scripts
"""

import json
import sys
import tempfile
import unittest
from pathlib import Path

sys.dont_write_bytecode = True
sys.path.insert(0, str(Path(__file__).resolve().parent))
import skillloop  # noqa: E402
from test_skillloop import load_script  # noqa: E402

run = load_script("discover-skills")

KEYS = {"fastapi": "FastAPI", "databricks": "Databricks", "go": "Go", "apache kafka": "Apache Kafka",
        "kafka": "Apache Kafka"}


class RunCandidatesTest(unittest.TestCase):

    def test_adds_the_names_given_to_the_candidates_once_by_name_form(self):
        candidates = [{"name": "Fastify", "df": 40}, {"name": "Telegraf", "df": 12}]
        self.assertEqual([c["name"] for c in run.run_candidates(candidates, ["telegraf", "Guice"], KEYS)],
                         ["Fastify", "Telegraf", "Guice"])

    def test_skips_a_name_given_that_is_a_key(self):
        self.assertEqual(run.run_candidates([], ["FastAPI", "Spacelift"], KEYS), [{"name": "Spacelift", "df": None}])


class SimilarTest(unittest.TestCase):

    def test_finds_a_skill_written_without_its_spaces_or_symbols(self):
        self.assertEqual(run.similar("Fast API", KEYS), ["FastAPI"])

    def test_finds_a_skill_whose_key_is_a_word_of_the_name(self):
        self.assertEqual(run.similar("Databricks Unity Catalog", KEYS), ["Databricks"])

    def test_finds_a_skill_by_the_plural_or_the_name_inside_a_key(self):
        self.assertEqual(run.similar("Kafka Streams", KEYS), ["Apache Kafka"])
        self.assertEqual(run.similar("Fastapis", KEYS), ["FastAPI"])

    def test_finds_a_skill_whose_key_holds_the_name_as_its_bare_form(self):
        self.assertEqual(run.similar("Kafka", {"apache kafka": "Apache Kafka"}), ["Apache Kafka"])
        self.assertEqual(run.similar("Glue", {"aws glue": "AWS Glue", "glueviz": "Glueviz"}), ["AWS Glue"])

    def test_ignores_keys_of_one_or_two_letters_inside_a_name(self):
        self.assertEqual(run.similar("Go-Live Planner", KEYS), [])


SEGMENTS = skillloop.Segments([
    "v1\tWe deploy with Spacelift and Terraform.",
    "v2\tExperience with Spacelift stacks.",
    "v3\tBuilt on Fast API and Postgres.",
])

ROWS = [("fastapi", "FastAPI", "framework", ["FastAPI"], set())]


class BatchEntriesTest(unittest.TestCase):

    def entries(self, candidates, record=None):
        return run.batch_entries(candidates, SEGMENTS, {"fastapi": "FastAPI"}, ROWS, record or {})

    def test_every_candidate_a_segment_mentions_is_an_entry_in_compact_order(self):
        entries, _ = self.entries([{"name": "Spacelift", "df": 2, "forms": {"Spacelift": 2}},
                                   {"name": "Fast API", "df": 1}])
        self.assertEqual([e["name"] for e in entries], ["Fast API", "Spacelift"])
        self.assertEqual(entries[1]["spellings"], {"Spacelift": 2})
        self.assertEqual(len(entries[1]["segments"]), 2)

    def test_a_candidate_no_segment_mentions_is_set_apart(self):
        entries, unmentioned = self.entries([{"name": "Key Vault Azure", "df": None}])
        self.assertEqual((entries, unmentioned), ([], ["Key Vault Azure"]))

    def test_a_name_given_takes_its_document_frequency_from_the_segments(self):
        entries, _ = self.entries([{"name": "Spacelift", "df": None}])
        self.assertEqual((entries[0]["df"], entries[0]["spellings"]), (2, {}))

    def test_similar_skills_carry_their_id_category_and_keys(self):
        entries, _ = self.entries([{"name": "Fast API", "df": 1}])
        self.assertEqual(entries[0]["similar"], [{"id": "fastapi", "canonical": "FastAPI", "category": "framework",
                                                  "keys": ["FastAPI"]}])

    def test_the_record_holds_every_decision_on_the_name_form(self):
        record = {"spacelift": [{"decision": "drop", "revision": "3", "source": "jev"},
                                {"decision": "keep", "revision": "5", "source": "session"}]}
        entries, _ = self.entries([{"name": "Spacelift", "df": 2}], record)
        self.assertEqual(entries[0]["record"], record["spacelift"])


class ReadRecordTest(unittest.TestCase):

    def test_keeps_every_row_of_a_name_form_in_order(self):
        with tempfile.NamedTemporaryFile("w", suffix=".tsv") as decisions:
            decisions.write("name\tdecision\trevision\tsnapshot\tsource\n"
                            "dataplex\tdrop\t3\traw-2026-09-20\tjev\n"
                            "kdb\tdrop\t3\traw-2026-09-20\tjev\n"
                            "dataplex\tkeep\t4\tclassified-2026-10-06\tsession\n")
            decisions.flush()
            record = run.read_record(decisions.name)
        self.assertEqual(record["dataplex"], [{"decision": "drop", "revision": "3", "source": "jev"},
                                              {"decision": "keep", "revision": "4", "source": "session"}])


def entry(name):
    return {"name": name, "df": 1, "spellings": {}, "segments": [], "similar": [], "record": []}


# Stands in for `claude -p`: decides every entry of the batch its prompt names, except one named
# "Left Out", and prints a result as the headless command does.
STUB = """#!/usr/bin/env python3
import json, re, sys
batch = re.search(r"Decide the batch (\\S+)\\.json\\.", sys.argv[-1]).group(1)
names = [e["name"] for e in json.load(open(batch + ".json")) if e["name"] != "Left Out"]
json.dump({n: {"kind": "drop", "reason": "a vendor's name"} for n in names}, open(batch + ".decisions.json", "w"))
print(json.dumps({"is_error": False, "total_cost_usd": 0.5, "duration_ms": 1000,
                  "usage": {"input_tokens": 10, "cache_creation_input_tokens": 20,
                            "cache_read_input_tokens": 30, "output_tokens": 40}}))
"""


class DecideTest(unittest.TestCase):

    def setUp(self):
        self.folder = tempfile.TemporaryDirectory()
        self.run_dir = Path(self.folder.name)
        stub = self.run_dir / "claude"
        stub.write_text(STUB)
        stub.chmod(0o755)
        self.claude = run.CLAUDE
        run.CLAUDE = str(stub)

    def tearDown(self):
        run.CLAUDE = self.claude
        self.folder.cleanup()

    def write(self, *batches):
        run.write_batches([entry(name) for batch in batches for name in batch], self.run_dir, size=2)

    def decided(self, number):
        return json.loads((self.run_dir / f"batch-{number:03}.decisions.json").read_text())

    def costs(self):
        return run.read_tsv(self.run_dir / "costs.tsv")

    def test_decides_every_batch_and_counts_its_cost(self):
        self.write(["Microsoft", "Spacelift"], ["Tempo"])
        self.assertEqual(run.decide(self.run_dir), [])
        self.assertEqual(set(self.decided(1)), {"Microsoft", "Spacelift"})
        self.assertEqual(set(self.decided(2)), {"Tempo"})
        self.assertEqual([(c["batch"], c["cost_usd"], c["output_tokens"]) for c in self.costs()],
                         [("batch-001", "0.5", "40"), ("batch-002", "0.5", "40")])

    def test_skips_a_batch_already_decided(self):
        self.write(["Microsoft", "Spacelift"])
        (self.run_dir / "batch-001.decisions.json").write_text(json.dumps(
            {"Microsoft": {"kind": "drop"}, "Spacelift": {"kind": "new"}}))
        self.assertEqual(run.decide(self.run_dir), [])
        self.assertEqual(self.decided(1)["Spacelift"], {"kind": "new"})
        self.assertFalse((self.run_dir / "costs.tsv").exists())

    def test_reports_a_batch_with_entries_left_out_and_decides_it_again_next_time(self):
        self.write(["Left Out", "Microsoft"])
        self.assertEqual(run.decide(self.run_dir), [("batch-001", ["Left Out"])])
        self.assertNotIn("Left Out", self.decided(1))
        run.decide(self.run_dir)
        self.assertEqual(len(self.costs()), 2)

    def test_writing_batches_again_starts_a_new_run(self):
        self.write(["Microsoft", "Spacelift"])
        run.decide(self.run_dir)
        self.write(["Tempo"])
        self.assertEqual(sorted(p.name for p in self.run_dir.glob("batch-*")), ["batch-001.json"])
        self.assertFalse((self.run_dir / "costs.tsv").exists())


class ReviewRowTest(unittest.TestCase):

    def test_a_kept_name_takes_its_draft(self):
        verdict = {"name": "Spacelift", "decision": "keep", "category": "devops"}
        row = run.review_row(verdict, {"Spacelift": {"id": "spacelift", "canonical": "Spacelift", "aliases": []}})
        self.assertEqual((row["source"], row["id"], row["category"]), ("jev", "spacelift", "devops"))

    def test_a_draft_written_as_its_id_alone_is_a_spelling(self):
        row = run.review_row({"name": "Fast API", "decision": "keep", "category": "framework"}, {"Fast API": "fastapi"})
        self.assertEqual((row["id"], row["canonical"], row["category"]), ("fastapi", "", ""))

    def test_a_name_no_segment_mentions_is_a_session_drop(self):
        row = run.review_row({"name": "Key Vault Azure", "decision": "unmentioned", "category": ""}, {})
        self.assertEqual((row["decision"], row["source"]), ("drop", "session"))

    def test_a_kept_name_without_a_draft_has_no_row(self):
        self.assertIsNone(run.review_row({"name": "Guice", "decision": "keep", "category": "framework"}, {}))


SKILLS = [
    ("fastapi", "FastAPI", "framework", ["FastAPI"], set()),
    ("go", "Go", "language", ["Go", "Golang"], {"Go"}),
]


def reviewed(name, decision, identifier="", canonical="", category="", aliases=""):
    return {"name": name, "decision": decision, "source": "jev", "id": identifier, "canonical": canonical,
            "category": category, "aliases": aliases, "note": ""}


class ApplyTest(unittest.TestCase):

    def test_a_kept_spelling_of_a_skill_becomes_its_alias(self):
        skills, _ = run.apply(SKILLS, [reviewed("Fast API", "keep", "fastapi")])
        self.assertEqual(skills[0], ("fastapi", "FastAPI", "framework", ["FastAPI", "Fast API"], set()))

    def test_a_kept_name_with_a_new_id_is_a_new_skill_in_id_order(self):
        skills, _ = run.apply(SKILLS, [reviewed("Guice", "keep", "guice", "Guice", "framework", "Google Guice")])
        self.assertEqual([row[0] for row in skills], ["fastapi", "go", "guice"])
        self.assertEqual(skills[2], ("guice", "Guice", "framework", ["Guice", "Google Guice"], set()))

    def test_two_kept_names_of_one_new_skill_make_one_row(self):
        skills, _ = run.apply(SKILLS, [reviewed("Server Sent Events", "keep", "server-sent-events",
                                                "Server-Sent Events", "networking"),
                                       reviewed("SSE", "keep", "server-sent-events")])
        self.assertEqual(skills[2], ("server-sent-events", "Server-Sent Events", "networking",
                                     ["Server-Sent Events", "SSE"], set()))

    def test_a_spelling_may_come_before_the_row_that_defines_its_new_skill(self):
        skills, _ = run.apply(SKILLS, [reviewed("SSE", "keep", "server-sent-events"),
                                       reviewed("Server Sent Events", "keep", "server-sent-events",
                                                "Server-Sent Events", "networking")])
        self.assertEqual(skills[2], ("server-sent-events", "Server-Sent Events", "networking",
                                     ["Server-Sent Events", "SSE"], set()))

    def test_a_key_already_held_is_not_added_again(self):
        skills, _ = run.apply(SKILLS, [reviewed("golang", "keep", "go", aliases="Go Lang")])
        self.assertEqual(skills[1][3], ["Go", "Golang", "Go Lang"])

    def test_refuses_a_key_another_skill_holds(self):
        with self.assertRaises(SystemExit):
            run.apply(SKILLS, [reviewed("Guice", "keep", "guice", "Guice", "framework", "Golang")])

    def test_a_drop_changes_no_skill(self):
        skills, _ = run.apply(SKILLS, [reviewed("Expert Python", "drop")])
        self.assertEqual(skills, SKILLS)

    def test_every_decision_is_recorded_by_name_form_drops_included(self):
        _, decisions = run.apply(SKILLS, [reviewed("Expert Python", "drop"),
                                          {**reviewed("Fast API", "keep", "fastapi"), "source": "session"}])
        self.assertEqual(decisions, [("expert python", "drop", "jev"), ("fast api", "keep", "session")])

    def test_refuses_a_keep_with_a_new_id_and_no_canonical_name(self):
        with self.assertRaises(SystemExit):
            run.apply(SKILLS, [reviewed("Guice", "keep", "guice")])


if __name__ == "__main__":
    unittest.main()

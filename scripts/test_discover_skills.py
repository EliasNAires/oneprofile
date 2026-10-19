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
        self.assertEqual(sorted((c["batch"], c["cost_usd"], c["output_tokens"]) for c in self.costs()),
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


class ReviewRowsTest(unittest.TestCase):

    def test_a_drop_carries_its_reason_and_doubt(self):
        rows = run.review_rows("Coupa", {"kind": "drop", "reason": "software no software person is hired for",
                                         "note": "doubt: procurement software"})
        self.assertEqual([(r["name"], r["kind"], r["source"], r["reason"], r["note"]) for r in rows],
                         [("Coupa", "drop", "skill-decider", "software no software person is hired for",
                           "doubt: procurement software")])

    def test_a_spelling_gives_the_id_and_marks_the_name_context_when_it_is(self):
        tempo, = run.review_rows("Tempo", {"kind": "spelling", "id": "grafana-tempo", "context": True})
        fast, = run.review_rows("Fast API", {"kind": "spelling", "id": "fastapi", "context": False})
        self.assertEqual([(r["kind"], r["id"], r["context"]) for r in (tempo, fast)],
                         [("spelling", "grafana-tempo", "Tempo"), ("spelling", "fastapi", "")])

    def test_a_new_skill_gives_its_fields_and_context_keys(self):
        row, = run.review_rows("Swift UI", {"kind": "new", "id": "swiftui", "canonical": "SwiftUI",
                                            "category": "framework", "aliases": ["Swift UI Kit", "SUI"],
                                            "context": ["SUI"]})
        self.assertEqual([row[c] for c in ("kind", "id", "canonical", "category", "aliases", "context")],
                         ["new", "swiftui", "SwiftUI", "framework", "Swift UI Kit|SUI", "SUI"])

    def test_a_split_is_a_row_for_the_glued_name_and_one_for_each_part(self):
        rows = run.review_rows("Spring Hibernate", {"kind": "split", "parts": [
            {"id": "spring-framework"},
            {"id": "hibernate", "canonical": "Hibernate", "category": "framework", "aliases": ["Hibernate ORM"],
             "context": []}]})
        self.assertEqual([(r["name"], r["kind"], r["id"], r["canonical"], r["aliases"]) for r in rows],
                         [("Spring Hibernate", "split", "", "", ""),
                          ("Spring Hibernate", "part", "spring-framework", "", ""),
                          ("Spring Hibernate", "part", "hibernate", "Hibernate", "Hibernate ORM")])

    def test_a_join_is_a_row_for_the_piece_naming_the_whole_and_one_for_the_whole(self):
        held = run.review_rows("Unity", {"kind": "join", "whole": "Databricks Unity Catalog",
                                         "id": "databricks-unity-catalog"})
        new = run.review_rows("Time-Sensitive", {"kind": "join", "whole": "Time-Sensitive Networking",
                                                 "id": "time-sensitive-networking",
                                                 "canonical": "Time-Sensitive Networking", "category": "networking",
                                                 "aliases": ["TSN"], "context": []})
        self.assertEqual([(r["kind"], r["id"], r["canonical"], r["aliases"], r["reason"]) for r in held + new],
                         [("join", "", "", "", "a fragment of Databricks Unity Catalog"),
                          ("whole", "databricks-unity-catalog", "", "", ""),
                          ("join", "", "", "", "a fragment of Time-Sensitive Networking"),
                          ("whole", "time-sensitive-networking", "Time-Sensitive Networking", "TSN", "")])


class CollectTest(unittest.TestCase):

    def setUp(self):
        self.folder = tempfile.TemporaryDirectory()
        self.run_dir = Path(self.folder.name)
        (self.run_dir / "unmentioned.txt").write_text("")

    def tearDown(self):
        self.folder.cleanup()

    def batch(self, number, decisions, left_out=()):
        (self.run_dir / f"batch-{number:03}.json").write_text(json.dumps([entry(n) for n in [*decisions, *left_out]]))
        (self.run_dir / f"batch-{number:03}.decisions.json").write_text(json.dumps(decisions))

    def test_refuses_while_a_batch_has_an_entry_left_out(self):
        self.batch(1, {"Microsoft": {"kind": "drop", "reason": "a vendor's name"}}, left_out=["Spacelift"])
        with self.assertRaisesRegex(SystemExit, "run decide again"):
            run.collect(self.run_dir)
        self.assertFalse((self.run_dir / "review.tsv").exists())

    def test_writes_the_rows_of_every_batch_then_each_unmentioned_name_as_a_session_drop(self):
        self.batch(1, {"Fast API": {"kind": "spelling", "id": "fastapi", "context": False},
                       "Microsoft": {"kind": "drop", "reason": "a vendor's name"}})
        self.batch(2, {"Spring Hibernate": {"kind": "split", "parts": [{"id": "spring-framework"}]}})
        (self.run_dir / "unmentioned.txt").write_text("Key Vault Azure\n")
        run.collect(self.run_dir)
        rows = run.read_tsv(self.run_dir / "review.tsv")
        self.assertEqual([(r["name"], r["kind"], r["source"]) for r in rows],
                         [("Fast API", "spelling", "skill-decider"), ("Microsoft", "drop", "skill-decider"),
                          ("Spring Hibernate", "split", "skill-decider"), ("Spring Hibernate", "part", "skill-decider"),
                          ("Key Vault Azure", "drop", "session")])
        self.assertEqual(rows[-1]["reason"], "mentioned in no segment of the snapshot")


SKILLS = [
    ("fastapi", "FastAPI", "framework", ["FastAPI"], set()),
    ("go", "Go", "language", ["Go", "Golang"], {"Go"}),
]


def reviewed(name, kind, identifier="", canonical="", category="", aliases="", context=""):
    return {"name": name, "kind": kind, "source": "skill-decider", "id": identifier, "canonical": canonical,
            "category": category, "aliases": aliases, "context": context, "reason": "", "note": ""}


class ApplyTest(unittest.TestCase):

    def test_a_spelling_of_a_skill_becomes_its_alias(self):
        skills, _ = run.apply(SKILLS, [reviewed("Fast API", "spelling", "fastapi")])
        self.assertEqual(skills[0], ("fastapi", "FastAPI", "framework", ["FastAPI", "Fast API"], set()))

    def test_a_new_skill_takes_its_place_in_id_order(self):
        skills, _ = run.apply(SKILLS, [reviewed("Guice", "new", "guice", "Guice", "framework", "Google Guice")])
        self.assertEqual([row[0] for row in skills], ["fastapi", "go", "guice"])
        self.assertEqual(skills[2], ("guice", "Guice", "framework", ["Guice", "Google Guice"], set()))

    def test_a_new_skill_and_a_spelling_of_it_make_one_row(self):
        skills, _ = run.apply(SKILLS, [reviewed("Server Sent Events", "new", "server-sent-events",
                                                "Server-Sent Events", "networking"),
                                       reviewed("SSE", "spelling", "server-sent-events")])
        self.assertEqual(skills[2], ("server-sent-events", "Server-Sent Events", "networking",
                                     ["Server-Sent Events", "SSE"], set()))

    def test_a_spelling_may_come_before_the_row_that_defines_its_new_skill(self):
        skills, _ = run.apply(SKILLS, [reviewed("SSE", "spelling", "server-sent-events"),
                                       reviewed("Server Sent Events", "new", "server-sent-events",
                                                "Server-Sent Events", "networking")])
        self.assertEqual(skills[2], ("server-sent-events", "Server-Sent Events", "networking",
                                     ["Server-Sent Events", "SSE"], set()))

    def test_a_key_already_held_is_not_added_again(self):
        skills, _ = run.apply(SKILLS, [reviewed("golang", "spelling", "go", aliases="Go Lang")])
        self.assertEqual(skills[1][3], ["Go", "Golang", "Go Lang"])

    def test_refuses_a_key_another_skill_holds(self):
        with self.assertRaises(SystemExit):
            run.apply(SKILLS, [reviewed("Guice", "new", "guice", "Guice", "framework", "Golang")])

    def test_a_drop_changes_no_skill(self):
        skills, _ = run.apply(SKILLS, [reviewed("Expert Python", "drop")])
        self.assertEqual(skills, SKILLS)

    def test_every_decision_is_recorded_by_name_form_drops_included(self):
        _, decisions = run.apply(SKILLS, [reviewed("Expert Python", "drop"),
                                          {**reviewed("Fast API", "spelling", "fastapi"), "source": "session"}])
        self.assertEqual(decisions, [("expert python", "drop", "skill-decider"), ("fast api", "keep", "session")])

    def test_refuses_a_spelling_of_an_id_no_row_defines(self):
        with self.assertRaises(SystemExit):
            run.apply(SKILLS, [reviewed("Guice", "spelling", "guice")])

    def test_a_split_records_the_glued_name_as_a_drop_and_adds_each_new_part_without_it(self):
        skills, decisions = run.apply(SKILLS, [reviewed("FastAPI Hibernate", "split"),
                                               reviewed("FastAPI Hibernate", "part", "fastapi"),
                                               reviewed("FastAPI Hibernate", "part", "hibernate", "Hibernate",
                                                        "framework", "Hibernate ORM")])
        self.assertEqual(skills, [SKILLS[0], SKILLS[1],
                                  ("hibernate", "Hibernate", "framework", ["Hibernate", "Hibernate ORM"], set())])
        self.assertEqual(decisions, [("fastapi hibernate", "drop", "skill-decider")])

    def test_a_join_records_the_piece_as_a_drop_and_adds_a_new_whole_without_it(self):
        skills, decisions = run.apply(SKILLS, [reviewed("Time-Sensitive", "join"),
                                               reviewed("Time-Sensitive", "whole", "time-sensitive-networking",
                                                        "Time-Sensitive Networking", "networking", "TSN")])
        self.assertEqual(skills[2], ("time-sensitive-networking", "Time-Sensitive Networking", "networking",
                                     ["Time-Sensitive Networking", "TSN"], set()))
        self.assertEqual(decisions, [("time sensitive", "drop", "skill-decider")])

    def test_a_join_to_an_entry_decided_new_gives_only_its_id(self):
        skills, _ = run.apply(SKILLS, [reviewed("Adobe Marketing", "join"),
                                       reviewed("Adobe Marketing", "whole", "adobe-marketing-cloud"),
                                       reviewed("Adobe Marketing Cloud", "new", "adobe-marketing-cloud",
                                                "Adobe Marketing Cloud", "tool")])
        self.assertEqual(skills[0], ("adobe-marketing-cloud", "Adobe Marketing Cloud", "tool",
                                     ["Adobe Marketing Cloud"], set()))

    def test_refuses_a_part_or_whole_whose_id_no_row_defines(self):
        with self.assertRaises(SystemExit):
            run.apply(SKILLS, [reviewed("Unity", "join"), reviewed("Unity", "whole", "unity-catalog")])

    def test_a_key_marked_context_joins_the_skill_s_context_keys(self):
        skills, _ = run.apply(SKILLS, [reviewed("Fast", "spelling", "fastapi", context="Fast"),
                                       reviewed("Swift UI", "new", "swiftui", "SwiftUI", "framework", "SUI|Swift UI Kit",
                                                context="SUI")])
        self.assertEqual(skills[0], ("fastapi", "FastAPI", "framework", ["FastAPI", "Fast"], {"Fast"}))
        self.assertEqual(skills[2], ("swiftui", "SwiftUI", "framework", ["SwiftUI", "SUI", "Swift UI Kit", "Swift UI"],
                                     {"SUI"}))

    def test_a_context_mark_holds_when_a_spelling_added_the_key_first(self):
        skills, _ = run.apply(SKILLS, [reviewed("Swift lang", "spelling", "swift"),
                                       reviewed("Swift", "new", "swift", "Swift", "language", context="Swift")])
        self.assertEqual(skills[2], ("swift", "Swift", "language", ["Swift", "Swift lang"], {"Swift"}))

    def test_refuses_one_new_skill_drafted_twice(self):
        with self.assertRaisesRegex(SystemExit, "guice"):
            run.apply(SKILLS, [reviewed("Guice", "new", "guice", "Guice", "framework"),
                               reviewed("Google Guice", "new", "guice", "Google Guice", "framework")])

    def test_refuses_a_new_skill_under_an_id_skills_tsv_holds(self):
        with self.assertRaisesRegex(SystemExit, "fastapi"):
            run.apply(SKILLS, [reviewed("Fast API", "new", "fastapi", "Fast API", "framework")])


if __name__ == "__main__":
    unittest.main()

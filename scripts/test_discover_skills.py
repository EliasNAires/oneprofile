"""Tests of the pure parts of discover-skills, the run of skill discovery on a snapshot (#55).

    python3 -m unittest discover -s scripts
"""

import sys
import unittest
from pathlib import Path

sys.dont_write_bytecode = True
sys.path.insert(0, str(Path(__file__).resolve().parent))
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

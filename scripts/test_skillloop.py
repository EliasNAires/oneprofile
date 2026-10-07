"""Tests of the pure parts of skillloop, the shared plumbing of skill discovery (ADR-0013).

    python3 -m unittest discover -s scripts
"""

import sys
import unittest
from pathlib import Path

sys.dont_write_bytecode = True
sys.path.insert(0, str(Path(__file__).resolve().parent))
import skillloop  # noqa: E402


class NameFormTest(unittest.TestCase):

    def test_lowercases_and_collapses_spaces_and_hyphens(self):
        self.assertEqual(skillloop.name_form("Spring-Boot"), "spring boot")
        self.assertEqual(skillloop.name_form("spring  boot"), "spring boot")
        self.assertEqual(skillloop.name_form("TOPS-10"), "tops 10")

    def test_keeps_every_other_character(self):
        self.assertEqual([skillloop.name_form(name) for name in ("C", "C++", "C#", "C--")], ["c", "c++", "c#", "c--"])
        self.assertEqual(skillloop.name_form("HAL/S"), "hal/s")

    def test_trims_the_ends(self):
        self.assertEqual(skillloop.name_form(" Node.js "), "node.js")


class SeedsTheDecisionRecordTest(unittest.TestCase):
    """name_form has to give the forms decisions.tsv was seeded with."""

    def test_every_seeded_form_is_its_own_form(self):
        decisions = skillloop.read_decisions()
        self.assertGreater(len(decisions), 9000)
        self.assertEqual([form for form in decisions if skillloop.name_form(form) != form], [])


class MentionsTest(unittest.TestCase):

    def test_finds_the_name_in_any_case_and_spacing(self):
        pattern = skillloop.mention("Spring Boot")
        self.assertTrue(pattern.search("experience with spring-boot and Kafka"))
        self.assertTrue(pattern.search("SPRING BOOT"))

    def test_needs_a_boundary_on_both_sides(self):
        self.assertFalse(skillloop.mention("Java").search("JavaScript"))
        self.assertFalse(skillloop.mention("R").search("our team"))
        self.assertTrue(skillloop.mention("R").search("R, Python or SAS"))

    def test_keeps_symbols_literal(self):
        self.assertTrue(skillloop.mention("C++").search("C++ and Rust"))
        self.assertFalse(skillloop.mention("C++").search("C and Rust"))
        self.assertTrue(skillloop.mention("C#").search("(C#)"))
        self.assertTrue(skillloop.mention("Node.js").search("Node.js."))


class SegmentsTest(unittest.TestCase):

    LINES = [
        "1\tWe use Python and Go.",
        "1\tPython 3 daily.",
        "2\tStrong PYTHON skills.",
        "3\tCPython internals.",
        "4\tİstanbul office, python preferred.",
        "5\tpython-based tooling.",
    ]

    def test_finds_mentions_in_distinct_vacancies(self):
        segments = skillloop.Segments(self.LINES)
        found = segments.examples("Python", 3, "seed")
        self.assertEqual(len(found), 3)
        self.assertEqual(len({vacancy_id for vacancy_id, _ in found}), 3)
        self.assertNotIn("CPython internals.", [text for _, text in found])
        self.assertEqual(segments.document_frequency("Python"), 4)

    def test_draws_each_text_once_though_ads_repeat_it(self):
        segments = skillloop.Segments(["1\tUse Rust.", "2\tUse Rust.", "3\tUse Rust.", "4\tRust daily."])
        self.assertEqual(sorted(text for _, text in segments.examples("Rust", 3, "seed")), ["Rust daily.", "Use Rust."])
        self.assertEqual(segments.document_frequency("Rust"), 4)

    def test_finds_nothing_for_an_absent_name(self):
        self.assertEqual(skillloop.Segments(self.LINES).examples("Rust", 3, "seed"), [])

    def test_draws_the_same_examples_for_the_same_seed(self):
        segments = skillloop.Segments(self.LINES)
        self.assertEqual(segments.examples("Python", 2, "a"), segments.examples("Python", 2, "a"))


class DropReasonTest(unittest.TestCase):

    def test_reads_the_criterions_reason_from_a_note(self):
        cases = {
            "generic concept": "generic concept",
            "generic ML model architecture (not a product)": "generic concept",
            "ordinary word": "ordinary word",
            "ordinary word/place": "ordinary word",
            "obscure": "obscure or historical",
            "historical OS": "obscure or historical",
            "ambiguous acronym (several products)": "ambiguous abbreviation",
            "ZK ambiguous": "ambiguous abbreviation",
            "office software; Teams ordinary word": "consumer, office or browser software",
            "web browser": "consumer, office or browser software",
            "single letter": "single letter",
            "multi-way homonym (Spark the Ada subset)": "homonym",
            "fragment of Microsoft Fabric": "not a technology",
            "company name": "not a technology",
            "noise: A": "key noise",
            "AS noise; honest forms match almost nothing": "key noise",
            "collides with ADA (disabilities act); no safe form": "homonym",
            "MQL means marketing qualified lead": "homonym",
            "matches are Open Policy Agent, not Opa language": "homonym",
            "common abbreviation": "ambiguous abbreviation",
            "esoteric language": "obscure or historical",
            "tiff is a file format": "not a technology",
            "iOS architecture pattern/methodology, not a technology (cf. MVC/MVVM)": "not a technology",
            "travel/expense SaaS, not an engineering skill": "not hired for",
            "marketing SEO tool": "not hired for",
            "": "other",
            "video game": "consumer, office or browser software",
            "too few ads": "other",
            "React built-in feature (Context API), not separate from React": "not a technology",
            "GitLab pricing tier, not a separate technology from GitLab": "not a technology",
        }
        self.assertEqual({note: skillloop.drop_reason(note) for note in cases}, cases)


class AllocateTest(unittest.TestCase):

    def test_splits_in_proportion_and_adds_up(self):
        self.assertEqual(skillloop.allocate({"a": 60, "b": 30, "c": 10}, 10), {"a": 6, "b": 3, "c": 1})
        self.assertEqual(sum(skillloop.allocate({"a": 7, "b": 7, "c": 7}, 10).values()), 10)

    def test_gives_every_stratum_at_least_one(self):
        counts = skillloop.allocate({"a": 990, "b": 5, "c": 5}, 10)
        self.assertEqual(counts, {"a": 8, "b": 1, "c": 1})

    def test_never_takes_more_than_a_stratum_holds(self):
        self.assertEqual(skillloop.allocate({"a": 2, "b": 3}, 10), {"a": 2, "b": 3})


class WilsonTest(unittest.TestCase):

    def test_brackets_the_share(self):
        low, high = skillloop.wilson(90, 100)
        self.assertAlmostEqual(low, 0.8256, places=3)
        self.assertAlmostEqual(high, 0.9448, places=3)


class CriterionRevisionTest(unittest.TestCase):

    def test_reads_the_revision_marker(self):
        self.assertEqual(skillloop.criterion_revision("# What counts\n\nRevision 3 (2026-11-01)\n"), "3")


if __name__ == "__main__":
    unittest.main()

"""Tests of the duplicate check's draw, precision and recall (#106).

    python3 -m unittest scripts/test_duplicate_check.py
"""

import importlib.util
import random
import sys
import tempfile
import unittest
from importlib.machinery import SourceFileLoader
from pathlib import Path

sys.dont_write_bytecode = True


def load_script(name):
    loader = SourceFileLoader(name, str(Path(__file__).resolve().parent / name))
    spec = importlib.util.spec_from_loader(name, loader)
    module = importlib.util.module_from_spec(spec)
    loader.exec_module(module)
    return module


score = load_script("score-duplicate-check")
draw = load_script("draw-duplicate-check")


class PrecisionTest(unittest.TestCase):

    def test_wilson_interval_for_38_of_40(self):
        low, high = score.wilson(38, 40)
        self.assertAlmostEqual(low, 0.8349583, places=6)
        self.assertAlmostEqual(high, 0.9861797, places=6)

    def test_precision_excludes_unclear(self):
        self.assertEqual(score.precision(["duplicate", "another vacancy", "unclear"]), (1, 2))

    def test_passes_at_the_bar_and_fails_below_it(self):
        self.assertEqual(score.verdict(38, 40), "PASS")
        self.assertEqual(score.verdict(37, 40), "FAIL")

    def test_no_clear_answers_has_no_estimate(self):
        self.assertEqual(score.precision(["unclear"]), (0, 0))
        self.assertIsNone(score.wilson(0, 0))
        self.assertEqual(score.verdict(0, 0), "FAIL")


class LabelsTest(unittest.TestCase):

    def test_missing_label_fails_with_pair_number(self):
        with tempfile.TemporaryDirectory() as temporary:
            directory = Path(temporary)
            (directory / "labels-1.tsv").write_text("pair\tanswer\treason\n1\tduplicate\tsame\n")
            with self.assertRaisesRegex(ValueError, "Missing label.*2"):
                score.labels_of(directory, [{"pair": "1"}, {"pair": "2"}])

    def test_invalid_answer_fails_clearly(self):
        with tempfile.TemporaryDirectory() as temporary:
            directory = Path(temporary)
            (directory / "labels-1.tsv").write_text("pair\tanswer\treason\n1\tmaybe\tunsure\n")
            with self.assertRaisesRegex(ValueError, "Invalid answer.*maybe"):
                score.labels_of(directory, [{"pair": "1"}])

    def test_recall_can_read_revision_one_nonduplicate_answers(self):
        with tempfile.TemporaryDirectory() as temporary:
            directory = Path(temporary)
            (directory / "labels-1.tsv").write_text("pair\tanswer\treason\n1\tsame group\trelated\n")
            labels = score.labels_of(directory, [{"pair": "1"}], answers=None)
            self.assertEqual(labels["1"]["answer"], "same group")


class RecallTest(unittest.TestCase):

    def test_merges_direct_marks_in_either_direction(self):
        self.assertTrue(score.merged("1", "2", {"1": "2", "2": None}))
        self.assertTrue(score.merged("1", "2", {"1": None, "2": "1"}))

    def test_merges_two_duplicates_of_one_kept_vacancy(self):
        self.assertTrue(score.merged("1", "2", {"1": "3", "2": "3"}))

    def test_does_not_merge_two_unmarked_or_differently_marked_vacancies(self):
        self.assertFalse(score.merged("1", "2", {"1": None, "2": None}))
        self.assertFalse(score.merged("1", "2", {"1": "3", "2": "4"}))

    def test_recall_counts_only_duplicates_still_in_the_subset(self):
        pairs = [{"pair": "1", "a": "1", "b": "2"},
                 {"pair": "2", "a": "3", "b": "4"},
                 {"pair": "3", "a": "5", "b": "6"},
                 {"pair": "4", "a": "7", "b": "8"}]
        labels = {str(n): {"answer": "duplicate"} for n in range(1, 4)}
        labels["4"] = {"answer": "different position"}
        self.assertEqual(score.recall(pairs, labels, {"1", "2", "3", "4", "5"},
                                      {"1": "2", "2": None, "3": None, "4": None}), (1, 2, 1))


class DrawTest(unittest.TestCase):

    def test_is_deterministic_and_draws_without_replacement(self):
        pairs = [(str(n), "1000") for n in range(100)]
        drawn = draw.draw(pairs, random.Random(106))
        self.assertEqual(drawn, draw.draw(pairs, random.Random(106)))
        self.assertEqual(len(drawn), 40)
        self.assertEqual(len(set(drawn)), 40)
        self.assertTrue(set(drawn) <= set(pairs))

    def test_draws_all_pairs_when_fewer_than_size(self):
        pairs = [("1", "2"), ("3", "4")]
        self.assertEqual(set(draw.draw(pairs, random.Random(106))), set(pairs))


if __name__ == "__main__":
    unittest.main()

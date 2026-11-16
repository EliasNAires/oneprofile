"""Tests of score-duplicate-sample's estimates, the measurement of the vacancy-group threshold (#27).

    python3 -m unittest discover -s scripts
"""

import sys
import unittest
from pathlib import Path

sys.dont_write_bytecode = True
sys.path.insert(0, str(Path(__file__).resolve().parent))
from test_skillloop import load_script  # noqa: E402

score = load_script("score-duplicate-sample")


def pair(level, right, inclusion=0.5, half="calibration", a="1", b="2", stratum="0.8-0.9"):
    return {"level": level, "right": right, "inclusion": inclusion, "half": half, "a": a, "b": b,
            "stratum": stratum}


class MergedTest(unittest.TestCase):

    def test_merges_a_pair_whose_level_reaches_the_threshold(self):
        self.assertTrue(score.merged(pair(0.8, True), 0.8))
        self.assertFalse(score.merged(pair(0.79, True), 0.8))

    def test_never_merges_a_pair_at_level_zero(self):
        self.assertFalse(score.merged(pair(0.0, True), 0.3))


class WeightTest(unittest.TestCase):

    def test_weighs_a_pair_by_its_inverse_inclusion(self):
        self.assertEqual(score.pair_weight(pair(0.9, True, inclusion=0.25)), 4)

    def test_weighs_a_merge_by_the_vacancies_it_joins(self):
        degrees = {"1": {0.8: 1}, "2": {0.8: 3}}
        self.assertAlmostEqual(score.vacancy_weight(pair(0.9, True, inclusion=0.5), 0.8, degrees),
                               2 * (1 + 1 / 3) / 2)


class PrecisionTest(unittest.TestCase):

    def test_is_the_weighted_share_right_among_merged_pairs(self):
        pairs = [pair(0.9, True, inclusion=0.5), pair(0.9, False, inclusion=0.25), pair(0.5, False)]
        share, n = score.precision(pairs, 0.8, score.pair_weight)
        self.assertAlmostEqual(share, 2 / 6)
        self.assertAlmostEqual(n, 36 / 20)

    def test_is_none_without_a_merged_pair(self):
        self.assertIsNone(score.precision([pair(0.5, True)], 0.8, score.pair_weight))


class RecallTest(unittest.TestCase):

    def test_is_the_weighted_share_merged_among_right_pairs(self):
        pairs = [pair(0.9, True, inclusion=0.5), pair(0.0, True, inclusion=0.25), pair(0.9, False)]
        self.assertAlmostEqual(score.recall(pairs, 0.8), 2 / 6)

    def test_is_none_without_a_right_pair(self):
        self.assertIsNone(score.recall([pair(0.9, False)], 0.8))


class AnswerTest(unittest.TestCase):

    def test_reads_an_answer_of_the_criterion_however_spaced_or_cased(self):
        self.assertEqual(score.answer(" Same_Group "), "same group")

    def test_is_none_for_an_answer_the_criterion_does_not_give(self):
        self.assertIsNone(score.answer("maybe"))


class KishTest(unittest.TestCase):

    def test_equal_weights_keep_the_count(self):
        self.assertAlmostEqual(score.kish([3, 3, 3]), 3)

    def test_unequal_weights_lower_it(self):
        self.assertAlmostEqual(score.kish([1, 3]), 16 / 10)


class ChooseTest(unittest.TestCase):

    def test_chooses_the_lowest_threshold_whose_calibration_precision_reaches_the_bar(self):
        shares = {0.6: 0.90, 0.7: 0.95, 0.8: 0.97, 0.9: 0.94}
        self.assertEqual(score.choose(shares), 0.7)

    def test_is_none_where_no_threshold_reaches_it(self):
        self.assertIsNone(score.choose({0.6: 0.90, 0.7: None}))


if __name__ == "__main__":
    unittest.main()

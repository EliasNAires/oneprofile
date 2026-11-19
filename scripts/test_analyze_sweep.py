"""Tests sweep timing, traffic, boards and description comparisons.

    python3 -m unittest scripts/test_analyze_sweep.py
"""

import contextlib
import csv
import importlib.util
import io
import sys
import tempfile
import unittest
from importlib.machinery import SourceFileLoader
from pathlib import Path
from unittest.mock import patch

sys.dont_write_bytecode = True


def load_script(name):
    loader = SourceFileLoader(name, str(Path(__file__).resolve().parent / name))
    spec = importlib.util.spec_from_loader(name, loader)
    module = importlib.util.module_from_spec(spec)
    loader.exec_module(module)
    return module


sweep = load_script("analyze-sweep")


def line(time, message):
    return f"2026-11-18T14:{time}-03:00 INFO 1 --- [worker] sweep : {message}\n"


def traffic(time, lists, descriptions, rate, challenge=0):
    return line(time, f"WORKDAY_TRAFFIC started_list={lists} started_description={descriptions} "
                f"answered_list={lists} list_ms={lists * 100} answered_description={descriptions} "
                f"description_ms={descriptions * 200} status_200={lists + descriptions - challenge} "
                f"status_404=0 status_other={challenge} tenant_429=0 challenge={challenge} gateway=0 "
                f"io_error=0 throttle_wait_ms=500 backoff_ms=1000 requests_at_once=4 "
                f"starts_a_second={rate} open=2 oldest_open_s=7")


LOG = "".join([
    line("00:00.000", "SWEEP_HEARTBEAT ats=WORKDAY finished=0 open=1 left=2 vacancies=0 resume_after=- longest_open=s longest_open_s=0"),
    line("00:05.000", "SWEEP_HEARTBEAT ats=GREENHOUSE finished=0 open=1 left=0 vacancies=0 resume_after=- longest_open=g longest_open_s=0"),
    line("00:10.000", "SWEEP_BOARD ats=GREENHOUSE slug=s outcome=published read_ms=1000 mirror_ms=100 vacancies=3"),
    line("00:20.000", "SWEEP_REPORT ats=GREENHOUSE after=- limit=1 boards=1 published=1 added=3 updated=0 deleted=0 not_found=0 unreadable=0 wall_s=15"),
    traffic("01:00.000", 10, 30, 4, challenge=2),
    line("02:00.000", "WORKDAY_BOARD slug=s list_pages=1 description_pages=4 parts=2 failed=false"),
    line("02:05.000", "SWEEP_BOARD ats=WORKDAY slug=s outcome=published read_ms=100000 mirror_ms=10000 vacancies=4"),
    line("02:06.000", "SWEEP_REPORT ats=WORKDAY after=- limit=1 boards=1 published=1 added=4 updated=0 deleted=0 not_found=0 unreadable=0 wall_s=126"),
    line("02:10.000", "Request was refused, so it slows down to 2 starts a second"),
    traffic("02:20.000", 5, 15, 2, challenge=1),
    line("04:05.000", "SWEEP_HEARTBEAT ats=WORKDAY finished=1 open=1 left=0 vacancies=4 resume_after=s longest_open=s longest_open_s=119"),
    line("04:10.000", "SWEEP_BOARD ats=WORKDAY slug=s outcome=unreadable read_ms=200000 mirror_ms=10000 vacancies=4"),
    line("04:11.000", "WORKDAY_BOARD slug=s list_pages=2 description_pages=8 parts=3 failed=true"),
    line("04:20.000", "SWEEP_REPORT ats=WORKDAY after=s limit=1 boards=1 published=0 added=0 updated=0 deleted=0 not_found=0 unreadable=1 wall_s=134"),
    line("05:00.000", "SWEEP_BOARD ats=WORKDAY slug=truncated outcome=published read_ms=20"),
    line("06:00.000", "Unrelated console output"),
])


class SweepTest(unittest.TestCase):
    def setUp(self):
        self.events = sweep.parse_log(LOG.splitlines())
        self.report = sweep.analyze(self.events)

    def test_wall_time_reports_restarts_and_global_heartbeat_gap(self):
        self.assertEqual(len(self.events), 14)
        self.assertEqual(self.report["wall_s"], 260)
        workday = self.report["ats"]["WORKDAY"]
        self.assertEqual(workday["wall_s"], 260)
        self.assertEqual(workday["reported_wall_s"], 260)
        self.assertEqual(workday["reports"], 2)
        self.assertEqual(self.report["ats"]["GREENHOUSE"]["wall_s"], 15)
        self.assertEqual([gap["seconds"] for gap in self.report["gaps"]], [240])

    def test_ats_without_report_ends_at_its_last_line(self):
        report = sweep.analyze([event for event in self.events
                                if not (event["ats"] == "GREENHOUSE" and event["kind"] == "SWEEP_REPORT")])
        greenhouse = report["ats"]["GREENHOUSE"]
        self.assertEqual(greenhouse["wall_s"], 5)
        self.assertEqual(greenhouse["reported_wall_s"], 0)
        self.assertEqual(greenhouse["reports"], 0)

    def test_ats_with_report_ends_at_its_last_report(self):
        events = sweep.parse_log((LOG + traffic("04:30.000", 0, 0, 2)).splitlines())
        report = sweep.analyze(events)
        self.assertEqual(report["wall_s"], 270)
        self.assertEqual(report["ats"]["WORKDAY"]["wall_s"], 260)

    def test_pace_spans_and_halving(self):
        self.assertEqual([(span["starts_a_second"], span["seconds"], span["started"])
                          for span in self.report["pace"]], [(4, 80, 40), (2, 120, 20)])
        self.assertEqual(self.report["pace"][0]["start"].minute, 1)
        self.assertEqual(len(self.report["halvings"]), 1)
        self.assertEqual(self.report["halvings"][0].second, 10)

    def test_requests_totals_outcomes_means_and_minutes(self):
        totals = self.report["totals"]
        self.assertEqual((totals["started_list"], totals["started_description"]), (15, 45))
        self.assertEqual((totals["answered_list"], totals["answered_description"]), (15, 45))
        self.assertEqual(totals["status_200"], 57)
        self.assertEqual(totals["status_other"], 3)
        self.assertEqual(totals["challenge"], 3)
        self.assertEqual(totals["status_404"], 0)
        self.assertEqual(totals["throttle_wait_ms"], 1000)
        self.assertEqual(totals["backoff_ms"], 2000)
        self.assertEqual(self.report["mean_ms"], {"list": 100, "description": 200})
        self.assertEqual([(row["started_list"], row["started_description"], row["challenge"],
                           row["starts_a_second"], row["open"], row["oldest_open_s"])
                          for row in self.report["minutes"]], [(10, 30, 2, 4, 2, 7), (5, 15, 1, 2, 2, 7)])

    def test_minute_sums_use_last_gauges_and_fill_silent_minutes(self):
        events = sweep.parse_log([traffic("01:00.000", 1, 2, 4),
                                  traffic("01:30.000", 2, 3, 2),
                                  traffic("03:00.000", 3, 4, 2)])
        rows = sweep.analyze(events)["minutes"]
        self.assertEqual(len(rows), 3)
        self.assertEqual((rows[0]["started_list"], rows[0]["started_description"], rows[0]["starts_a_second"]), (3, 5, 2))
        self.assertEqual((rows[1]["started_list"], rows[1]["started_description"], rows[1]["open"]), (0, 0, 2))

    def test_longest_boards_keep_last_completion_and_join_last_workday_detail(self):
        longest = self.report["longest"]
        self.assertEqual(len(longest), 2)
        board = longest[0]
        self.assertEqual((board["ats"], board["slug"], board["outcome"]), ("WORKDAY", "s", "unreadable"))
        self.assertEqual(board["duration_ms"], 210000)
        self.assertEqual(board["vacancies"], 4)
        self.assertAlmostEqual(board["share"], 210 / 260)
        self.assertEqual((board["workday"]["list_pages"], board["workday"]["description_pages"],
                          board["workday"]["parts"], board["workday"]["failed"]), (2, 8, 3, "true"))
        self.assertIsNone(longest[1]["workday"])

    def test_board_outcomes_and_report_reconciliation(self):
        workday = self.report["ats"]["WORKDAY"]
        self.assertEqual(workday["outcomes"], {"unreadable": 1})
        self.assertEqual((workday["finished_lines"], workday["report_boards"]), (2, 2))
        self.assertEqual(len(workday["boards"]), 1)
        self.assertEqual(self.report["ats"]["GREENHOUSE"]["outcomes"], {"published": 1})
        self.assertEqual([board["slug"] for board in self.report["unreadable"]], ["s"])
        self.assertEqual(self.report["unreadable"][0]["workday"]["parts"], 3)

    def test_ignores_malformed_lines(self):
        valid = line("00:00.000", "SWEEP_BOARD ats=WORKDAY slug=s outcome=published read_ms=1 mirror_ms=2 vacancies=3")
        for malformed in ("", "not a log", valid.replace("14:00", "99:00"),
                          valid.replace("-03:00", ""), valid.replace("read_ms=1", "read_ms=bad"),
                          valid.replace("published", "unknown"), valid.replace("vacancies=3", "vacancies=")):
            with self.subTest(line=malformed):
                self.assertIsNone(sweep.parse_line(malformed))

    def test_empty_log_has_no_estimated_rate(self):
        report = sweep.analyze([])
        self.assertEqual(report["wall_s"], 0)
        self.assertEqual(report["pace"], [])
        self.assertEqual(report["mean_ms"], {"list": None, "description": None})

    def test_printing_sections_is_thin_and_descriptions_are_optional(self):
        output = io.StringIO()
        with contextlib.redirect_stdout(output):
            sweep.print_report(self.report)
        text = output.getvalue()
        for heading in ("Wall time", "Workday pace", "Requests", "Longest boards", "Boards"):
            self.assertIn(heading, text)
        self.assertNotIn("\nDescriptions", text)
        self.assertIn("more than 100%", text)
        self.assertIn("match=true", text)


class DescriptionsTest(unittest.TestCase):
    def setUp(self):
        self.temporary = tempfile.TemporaryDirectory()
        self.addCleanup(self.temporary.cleanup)
        self.directory = Path(self.temporary.name)
        self.before = self.directory / "before.tsv"
        self.after = self.directory / "after.tsv"
        self.report = sweep.analyze(sweep.parse_log(LOG.splitlines()))
        self.write(self.before, [
            ("WORKDAY", "s", "empty", "a", ""),
            ("WORKDAY", "s", "same", "a", "d"),
            ("WORKDAY", "s", "retitled", "a", "d"),
            ("WORKDAY", "s", "changed", "a", "d"),
            ("WORKDAY", "other", "new", "a", "d"),
            ("GREENHOUSE", "s", "new", "a", "d"),
        ])
        self.write(self.after, [
            ("WORKDAY", "s", "new", "a", "d"),
            ("WORKDAY", "s", "empty", "b", "d"),
            ("WORKDAY", "s", "same", "a", "d"),
            ("WORKDAY", "s", "retitled", "b", "d"),
            ("WORKDAY", "s", "changed", "a", "e"),
            ("WORKDAY", "s", "still-empty", "a", ""),
            ("GREENHOUSE", "s", "ignored", "a", "d"),
        ])

    def write(self, path, rows):
        with path.open("w", encoding="utf-8", newline="") as output:
            writer = csv.writer(output, delimiter="\t")
            writer.writerow(sweep.COLUMNS)
            writer.writerows(rows)

    def test_description_categories_and_projection(self):
        changes = sweep.descriptions(self.before, self.after, self.report)
        self.assertEqual({key: changes[key] for key in ("new", "without_description_before", "same_description",
                                                       "same_description_and_title", "changed_description")},
                         dict(new=1, without_description_before=1, same_description=2,
                              same_description_and_title=1, changed_description=1))
        self.assertEqual(changes["requests_started"], 60)
        self.assertEqual(changes["projected_requests"], 59)
        self.assertAlmostEqual(changes["observed_rate"], 60 / 260)
        self.assertAlmostEqual(changes["saved_hours"], (260 / 60) / 3600)
        self.assertAlmostEqual(changes["measured_hours"], 260 / 3600)
        self.assertAlmostEqual(changes["projected_hours"], (260 * 59 / 60) / 3600)

    def test_no_traffic_has_no_projection_in_hours(self):
        changes = sweep.descriptions(self.before, self.after, sweep.analyze([]))
        self.assertIsNone(changes["observed_rate"])
        self.assertIsNone(changes["saved_hours"])
        self.assertIsNone(changes["projected_hours"])

    def test_missing_and_unreadable_tsv_fail_with_path(self):
        with self.assertRaisesRegex(ValueError, "Unreadable TSV.*missing.tsv"):
            list(sweep.read_tsv(self.directory / "missing.tsv"))
        for contents in ("", "wrong\theader\n", "\t".join(sweep.COLUMNS) + "\nWORKDAY\ts\n",
                         "\t".join(sweep.COLUMNS) + '\n"unterminated'):
            self.after.write_text(contents, encoding="utf-8")
            with self.subTest(contents=contents), self.assertRaisesRegex(ValueError, "Unreadable TSV.*after.tsv"):
                sweep.descriptions(self.before, self.after, self.report)
        self.after.write_bytes(b"\xff")
        with self.assertRaisesRegex(ValueError, "Unreadable TSV.*after.tsv"):
            sweep.descriptions(self.before, self.after, self.report)

    def test_report_main_reads_inputs_and_prints_projection(self):
        log = self.directory / "console.log"
        log.write_text(LOG, encoding="utf-8")
        output = io.StringIO()
        with patch.object(sys, "argv", ["analyze-sweep", "report", str(log), str(self.before), str(self.after)]), \
                contextlib.redirect_stdout(output):
            sweep.main()
        self.assertIn("\nDescriptions", output.getvalue())
        self.assertIn("projected=59", output.getvalue())
        self.assertIn("time ~ requests / observed rate", output.getvalue())

    def test_missing_log_and_invalid_arguments_fail(self):
        with patch.object(sys, "argv", ["analyze-sweep", "report", str(self.directory / "missing.log")]):
            with self.assertRaises(FileNotFoundError):
                sweep.main()
        with patch.object(sys, "argv", ["analyze-sweep", "report", "log", "before"]):
            with self.assertRaises(SystemExit):
                sweep.main()


if __name__ == "__main__":
    unittest.main()

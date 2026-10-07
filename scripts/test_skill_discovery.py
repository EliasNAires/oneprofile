"""Tests of the discovery script's rules (ADR-0013).

    python3 -m unittest discover -s scripts
"""

import re
import sys
import tempfile
import unittest
from pathlib import Path

sys.dont_write_bytecode = True
sys.path.insert(0, str(Path(__file__).resolve().parent))
import skill_discovery  # noqa: E402
from skill_discovery import mine, words  # noqa: E402

KNOWN = {"docker", "kubernetes", "python"}


def ads(*texts):
    """Each text as an ad of three companies, the least the mining takes a name from, one segment a
    sentence."""
    return [(company, re.split(r"(?<=\.)\s+", text)) for text in texts for company in ("Acme", "Globex", "Initech")]


def mined(docs, covered=frozenset(KNOWN), known=frozenset(KNOWN)):
    return {candidate["name"]: candidate for candidate in mine(docs, covered, known)}


class WordsTest(unittest.TestCase):

    def test_are_the_tokens_cleaning_stores(self):
        # Segment.tokens(): letters and digits holding together across ' . + # - inside, and the + or # at the end.
        text = "We ship Node.js, C++, C# and .NET... on k8s, Spring-Boot's way."
        self.assertEqual([word.text for word in words([text])],
                         ["We", "ship", "Node.js", "C++", "C#", "and", "NET", "on", "k8s", "Spring-Boot's", "way"])

    def test_mark_where_a_sentence_starts(self):
        starts = [word.text for word in words(["Ship it. Then: Grafana - Docker • Helm", "Kibana too"]) if word.starts]
        self.assertEqual(starts, ["Ship", "Then", "Grafana", "Docker", "Helm", "Kibana"])

    def test_do_not_take_a_leading_dot_for_a_sentence_end(self):
        self.assertEqual([word.text for word in words(["We use .NET and C#"]) if word.starts], ["We"])

    def test_mark_a_word_that_follows_the_last_across_spaces_only(self):
        joined = [word.text for word in words(["Grafana, Kibana and GitHub Actions"]) if word.joined]
        self.assertEqual(joined, ["and", "GitHub", "Actions"])

    def test_never_join_words_across_segments(self):
        self.assertEqual([word.text for word in words(["Use Grafana", "Kibana daily"]) if word.joined],
                         ["Grafana", "daily"])


class MineTest(unittest.TestCase):

    def test_finds_a_capitalized_name_near_a_known_skill(self):
        found = mined(ads("We run Grafana and Docker."))
        self.assertEqual(found["Grafana"]["df"], 3)

    def test_takes_nothing_already_covered(self):
        found = mined(ads("We run Grafana and Docker."), covered=KNOWN | {"grafana"})
        self.assertNotIn("Grafana", found)

    def test_compares_with_what_is_covered_by_name_form(self):
        found = mined(ads("We run Spring-Boot and Docker.", "We run Spring Boot and Docker."), covered=KNOWN | {"spring boot"})
        self.assertNotIn("Spring Boot", found)
        self.assertNotIn("Spring-Boot", found)

    def test_groups_the_spellings_of_one_name_form(self):
        found = mined(ads("We run Spring-Boot and Docker.", "We run Spring Boot and Docker.", "We run Spring Boot and Docker."))
        self.assertEqual(found["Spring Boot"]["forms"], {"Spring Boot": 6, "Spring-Boot": 3})

    def test_needs_three_companies(self):
        docs = [(company, ["We run Grafana and Docker."]) for company in ("Acme", "Acme", "Acme", "Globex")]
        self.assertNotIn("Grafana", mined(docs))

    def test_needs_the_name_capitalized_in_most_of_the_sentence(self):
        found = mined(ads("We use Docker with Team tools.", "Our team uses Docker.", "A team of Docker fans."))
        self.assertNotIn("Team", found)

    def test_does_not_take_a_capital_at_the_start_of_a_sentence_as_evidence(self):
        self.assertNotIn("Monitor", mined(ads("Docker is fine. Monitor it with Kubernetes.")))

    def test_keeps_a_name_written_in_lower_case_with_a_symbol(self):
        self.assertIn("next.js", mined(ads("We use next.js and Docker.")))

    def test_keeps_a_name_capitalized_less_than_half_the_time(self):
        found = mined(ads("Tests in Pytest with Python.", "Tests in pytest with Python.",
                          "Tests in PyTest with Python.", "Tests in pytest with Python.", "We use pytest with Python."))
        self.assertIn("pytest", found)

    def test_needs_the_name_near_a_known_skill(self):
        self.assertNotIn("Sullivan", mined(ads("Docker and Kubernetes are ours. We are an equal opportunity "
                                               "employer, as Mr Sullivan says, and we welcome everyone.")))

    def test_finds_a_known_skill_spelled_with_a_hyphen(self):
        self.assertIn("Grafana", mined(ads("We run Grafana and Spring-Boot."), known=KNOWN | {"spring boot"}))

    def test_joins_words_only_across_spaces(self):
        found = mined(ads("We run GitHub Actions and Docker.", "We run Grafana, Kibana with Docker."))
        self.assertIn("GitHub Actions", found)
        self.assertNotIn("Grafana Kibana", found)

    def test_needs_every_word_of_a_phrase_capitalized(self):
        found = mined(ads("We run Grafana and Docker."))
        self.assertNotIn("run Grafana", found)
        self.assertNotIn("Grafana and", found)

    def test_needs_a_phrase_to_hold_a_name(self):
        found = mined(ads("Our Key Responsibilities in Docker work.", "The key to our responsibilities is Docker.",
                          "A key part of the responsibilities is Kubernetes."))
        self.assertNotIn("Key Responsibilities", found)

    def test_names_a_skill_by_its_commonest_spelling_and_keeps_every_one(self):
        found = mined(ads("We run DataDog and Docker.", "We run Datadog and Docker.", "We run Datadog and Docker."))
        self.assertEqual(found["Datadog"]["forms"], {"Datadog": 6, "DataDog": 3})


class CandidatesFileTest(unittest.TestCase):

    def test_reads_back_what_it_writes(self):
        candidates = mine(ads("We run Datadog and Docker.", "Datadog and Docker."), KNOWN, KNOWN)
        with tempfile.TemporaryDirectory() as directory:
            path = Path(directory) / "candidates.tsv"
            skill_discovery.write_candidates(path, candidates, "a test corpus")
            self.assertEqual([(row["name"], row["df"], row["forms"]) for row in skill_discovery.read_candidates(path)],
                             [("Datadog", 6, {"Datadog": 3})])


if __name__ == "__main__":
    unittest.main()

"""Tests of the discovery script's rules (ADR-0013).

    python3 -m unittest discover -s scripts
"""

import re
import sys
import tempfile
import unittest
from pathlib import Path
from unittest import mock

sys.dont_write_bytecode = True
sys.path.insert(0, str(Path(__file__).resolve().parent))
import skill_discovery  # noqa: E402
from skill_discovery import mine, words  # noqa: E402

KNOWN = {"docker", "kubernetes", "python"}


def ads(*texts):
    """Each text as an ad of three companies, the least the mining takes a name from, one segment a
    sentence."""
    return [(company, re.split(r"(?<=\.)\s+", text)) for text in texts for company in ("Acme", "Globex", "Initech")]


def mined(docs, covered=frozenset(KNOWN), known=frozenset(KNOWN), anchors=None):
    return {candidate["name"]: candidate for candidate in mine(docs, covered, known, anchors)}


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

    def test_fold_a_possessive_and_a_hyphened_suffix_into_the_name(self):
        found = words(["Grafana's Linux-based, AI-assisted Spring-Boot's Self-Service"])
        self.assertEqual([word.form for word in found], ["grafana", "linux", "ai", "spring boot", "self service"])
        self.assertEqual([word.text for word in found][:2], ["Grafana's", "Linux-based"])

    def test_never_join_words_across_segments(self):
        self.assertEqual([word.text for word in words(["Use Grafana", "Kibana daily"]) if word.joined],
                         ["Grafana", "daily"])


class MineTest(unittest.TestCase):
    """Each ad of ads() makes a corpus of three vacancies, so the frequency a name needs is three here,
    except in test_needs_more_vacancies_for_a_name_than_for_a_respelled_key."""

    def setUp(self):
        patch = mock.patch.object(skill_discovery, "MIN_DF", 3)
        patch.start()
        self.addCleanup(patch.stop)

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

    def test_takes_a_seen_name_with_a_folded_suffix_for_the_name(self):
        found = mined(ads("We run Docker-based tools, Grafana's stack, Docker-native too."))
        self.assertNotIn("Docker-based", found)
        self.assertNotIn("Docker-native", found)
        self.assertEqual(found["Grafana"]["forms"], {"Grafana": 3})

    def test_takes_nothing_whose_singular_is_covered(self):
        found = mined(ads("We run Grafana Dashboards and GPUs with Docker."), covered=KNOWN | {"gpu", "grafana dashboard"})
        self.assertNotIn("GPUs", found)
        self.assertNotIn("Grafana Dashboards", found)

    def test_takes_no_job_title_or_certification(self):
        found = mined(ads("A Senior Grafana Engineer runs Docker.", "A Certified Grafana Administrator runs Docker.",
                          "We run Grafana and Docker.", "We run Grafana and Kubernetes."))
        self.assertIn("Grafana", found)
        for title in ("Senior Grafana", "Grafana Engineer", "Grafana Administrator", "Certified Grafana"):
            self.assertNotIn(title, found)

    def test_takes_a_developer_or_manager_for_a_title_after_a_skill_or_a_title_word(self):
        found = mined(ads("A Docker Developer and a Product Manager use Docker.", "We run Tag Manager and Docker."))
        self.assertNotIn("Docker Developer", found)
        self.assertNotIn("Product Manager", found)
        self.assertIn("Tag Manager", found)

    def test_takes_no_phrase_with_a_number(self):
        found = mined(ads("We run Grafana 11 and Docker since 2016."))
        self.assertIn("Grafana", found)
        self.assertNotIn("Grafana 11", found)
        self.assertNotIn("2016", found)

    def test_takes_no_count_with_a_unit(self):
        found = mined(ads("We serve 100M users and 5k shops with Docker, for 5년."))
        for count in ("100M", "5k", "5년"):
            self.assertNotIn(count, found)

    def test_takes_a_contraction_for_no_name(self):
        self.assertNotIn("I'm", mined(ads("Docker, I'm told, runs Kubernetes.")))

    def test_takes_an_abbreviation_for_no_name(self):
        self.assertNotIn("e.g Grafana", mined(ads("Use tools, e.g Grafana and Docker.")))

    def test_takes_no_known_names_run_together(self):
        found = mined(ads("We run Docker Kubernetes and Python."), covered=KNOWN, known=KNOWN)
        self.assertNotIn("Docker Kubernetes", found)

    def test_needs_each_word_beside_a_known_name_to_be_name_like_and_not_generic(self):
        found = mined(ads("We run Docker Atlas and Kubernetes.", "Grant Docker Access in Kubernetes.",
                          "Our platforms Docker and Kubernetes.", "Some Platforms Docker and Kubernetes."))
        self.assertIn("Docker Atlas", found)
        self.assertNotIn("Docker Access", found)
        self.assertNotIn("Platforms Docker", found)

    def test_takes_a_generic_noun_inside_a_name_for_part_of_it(self):
        found = mined(ads("We run Docker Data Factory and Kubernetes.", "Data Factory runs on Kubernetes."))
        self.assertIn("Docker Data Factory", found)

    def test_takes_a_known_key_respelled_from_two_keys_for_its_spelling(self):
        known = KNOWN | {"vue", "js", "vuejs"}
        found = mined(ads("We run Vue JS and Docker."), covered=known, known=known)
        self.assertIn("Vue JS", found)

    def test_takes_5g_for_a_name_not_a_count(self):
        self.assertIn("5G", mined(ads("We run 5G and Docker.")))

    def test_takes_a_product_word_after_a_known_name_when_the_phrase_is_capitalized_as_a_whole(self):
        found = mined(ads("We run Docker Cloud and Kubernetes.", "Our cloud runs Kubernetes.", "The cloud is Docker."))
        self.assertIn("Docker Cloud", found)
        self.assertNotIn("Cloud Docker", mined(ads("We run Cloud Docker and Kubernetes.", "Our cloud runs Kubernetes.",
                                                   "The cloud is Docker.")))

    def test_needs_an_acronym_mostly_near_a_known_skill(self):
        found = mined(ads("We run SIP and Docker.", "SIP trunks and Kubernetes.", "Docker runs here. Today, after a "
                          "long talk about it with the whole team, we run an LLM, and our LLM is the best LLM there is."))
        self.assertIn("SIP", found)
        self.assertNotIn("LLM", found)

    def test_takes_no_name_mostly_inside_a_longer_one(self):
        found = mined(ads("We run Google Workspace and Docker.", "We run Google Workspace and Kubernetes.",
                          "We run Google Workspace and Python.", "A Workspace and Docker."),
                      covered=KNOWN | {"google workspace"}, known=KNOWN | {"google workspace"})
        self.assertNotIn("Workspace", found)
        self.assertIn("Grafana", mined(ads("We run Grafana Loki and Docker.", "We run Grafana and Docker.",
                                           "Grafana dashboards with Docker.", "Use Grafana with Kubernetes.")))

    def test_takes_no_phrase_with_a_folded_suffix_as_a_word(self):
        self.assertNotIn("Grafana Based", mined(ads("Our Grafana Based stack with Docker.")))

    def test_takes_no_plural_of_another_candidate(self):
        found = mined(ads("We run GPUs and Docker.", "We run a GPU and Docker."))
        self.assertIn("GPU", found)
        self.assertNotIn("GPUs", found)

    def test_needs_more_vacancies_for_a_name_than_for_a_respelled_key(self):
        with mock.patch.object(skill_discovery, "MIN_DF", 4):
            found = mined(ads("We run Grafana and Docker.", "Our data sits in Mongo DB."), covered=KNOWN | {"mongodb"},
                          known=KNOWN | {"mongodb"})
        self.assertNotIn("Grafana", found)
        self.assertIn("Mongo DB", found)

    def test_takes_only_anchors_for_evidence(self):
        self.assertNotIn("Grafana", mined(ads("We run Grafana and Docker."), anchors=KNOWN - {"docker"}))

    def test_takes_a_known_key_written_with_a_space_for_its_spelling(self):
        found = mined(ads("Our data sits in Mongo DB.", "We keep mongo db too."), covered=KNOWN | {"mongodb"},
                      known=KNOWN | {"mongodb"})
        self.assertEqual(found["Mongo DB"]["forms"], {"Mongo DB": 3, "mongo db": 3})

    def test_needs_a_respelled_key_written_as_a_name(self):
        found = mined(ads("We work in design with Docker.", "We work in design with Kubernetes."),
                      covered=KNOWN | {"indesign"}, known=KNOWN | {"indesign"})
        self.assertNotIn("in design", found)


class CandidatesFileTest(unittest.TestCase):

    @mock.patch.object(skill_discovery, "MIN_DF", 3)
    def test_reads_back_what_it_writes(self):
        candidates = mine(ads("We run Datadog and Docker.", "Datadog and Docker."), KNOWN, KNOWN)
        with tempfile.TemporaryDirectory() as directory:
            path = Path(directory) / "candidates.tsv"
            skill_discovery.write_candidates(path, candidates, "a test corpus")
            self.assertEqual([(row["name"], row["df"], row["forms"]) for row in skill_discovery.read_candidates(path)],
                             [("Datadog", 6, {"Datadog": 3})])


if __name__ == "__main__":
    unittest.main()

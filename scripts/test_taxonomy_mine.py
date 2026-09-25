"""Tests for how skill names the seed missed are mined from the corpus. Run with

    python3 -m unittest discover -s scripts
"""
import unittest

from taxonomy_candidates import key, tokens
from taxonomy_mine import mine, words

KNOWN = {key('Docker'), key('Kubernetes'), key('Python')}


def ads(*texts):
    """Each text as an ad of three companies, the least the mining takes a name from."""
    return [(company, t) for t in texts for company in ('Acme', 'Globex', 'Initech')]


def mined(docs, covered=frozenset(KNOWN), known=frozenset(KNOWN)):
    return {m['name']: m for m in mine(docs, covered, known)}


class WordsTest(unittest.TestCase):

    def test_are_the_tokens_of_the_seed(self):
        text = 'We ship Node.js, C++ and .NET... on k8s.'
        self.assertEqual([w.text.lower() for w in words(text)], tokens(text))

    def test_mark_where_a_sentence_starts(self):
        starts = [w.text for w in words('Ship it. Then: Grafana - Docker • Helm') if w.starts]
        self.assertEqual(starts, ['Ship', 'Then', 'Grafana', 'Docker', 'Helm'])

    def test_mark_a_word_that_follows_the_last_across_punctuation(self):
        joined = [w.text for w in words('Grafana, Kibana and GitHub Actions') if w.joined]
        self.assertEqual(joined, ['and', 'GitHub', 'Actions'])


class MineTest(unittest.TestCase):

    def test_finds_a_capitalized_name_near_a_known_skill(self):
        found = mined(ads('We run Grafana and Docker.'))
        self.assertEqual(found['Grafana']['df'], 3)

    def test_takes_nothing_the_candidates_already_cover(self):
        found = mined(ads('We run Grafana and Docker.'), covered=KNOWN | {key('Grafana')})
        self.assertNotIn('Grafana', found)

    def test_needs_three_companies(self):
        docs = [(c, 'We run Grafana and Docker.') for c in ('Acme', 'Acme', 'Acme', 'Globex')]
        self.assertNotIn('Grafana', mined(docs))

    def test_needs_the_name_capitalized_in_most_of_the_sentence(self):
        found = mined(ads('We use Docker with Team tools.', 'Our team uses Docker.', 'A team of Docker fans.'))
        self.assertNotIn('Team', found)

    def test_does_not_take_a_capital_at_the_start_of_a_sentence_as_evidence(self):
        self.assertNotIn('Monitor', mined(ads('Docker is fine. Monitor it with Kubernetes.')))

    def test_keeps_a_name_written_in_lower_case_with_a_symbol(self):
        self.assertIn('next.js', mined(ads('We use next.js and Docker.')))

    def test_keeps_a_name_capitalized_less_than_half_the_time(self):
        found = mined(ads('Tests in Pytest with Python.', 'Tests in pytest with Python.',
                          'Tests in PyTest with Python.', 'Tests in pytest with Python.', 'We use pytest with Python.'))
        self.assertIn('pytest', found)

    def test_needs_the_name_near_a_known_skill(self):
        self.assertNotIn('Sullivan', mined(ads('Docker and Kubernetes are ours. ' + 'We are an equal opportunity '
                                                 'employer, as Mr Sullivan says, and we welcome everyone.')))

    def test_joins_words_only_across_spaces(self):
        found = mined(ads('We run GitHub Actions and Docker.', 'We run Grafana, Kibana with Docker.'))
        self.assertIn('GitHub Actions', found)
        self.assertNotIn('Grafana Kibana', found)

    def test_needs_every_word_of_a_phrase_capitalized(self):
        found = mined(ads('We run Grafana and Docker.'))
        self.assertNotIn('run Grafana', found)
        self.assertNotIn('Grafana and', found)

    def test_needs_a_phrase_to_hold_a_name(self):
        found = mined(ads('Our Key Responsibilities in Docker work.', 'The key to our responsibilities is Docker.',
                          'A key part of the responsibilities is Kubernetes.'))
        self.assertNotIn('Key Responsibilities', found)

    def test_names_a_skill_by_its_commonest_spelling_and_keeps_every_one(self):
        found = mined(ads('We run DataDog and Docker.', 'We run Datadog and Docker.', 'We run Datadog and Docker.'))
        self.assertEqual(found['Datadog']['forms'], {'Datadog': 6, 'DataDog': 3})


if __name__ == '__main__':
    unittest.main()

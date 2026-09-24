"""Tests for how the taxonomy is selected from the review verdicts. Run with

    python3 -m unittest discover -s scripts
"""
import unittest

from taxonomy_select import merge, select, skill_id
from taxonomy_candidates import tokens


def verdict(canonical, category, aliases='', outcome='keep'):
    return {'name': canonical, 'verdict': outcome, 'canonical': canonical, 'category': category,
            'aliases': aliases, 'note': ''}


class SkillIdTest(unittest.TestCase):

    def test_is_the_canonical_name_as_a_slug(self):
        self.assertEqual(skill_id('Apache Kafka'), 'apache-kafka')

    def test_spells_out_the_symbols_of_language_names(self):
        self.assertEqual([skill_id(n) for n in ('C#', 'C++', 'F#')], ['c-sharp', 'cpp', 'f-sharp'])

    def test_turns_dots_into_dashes(self):
        self.assertEqual([skill_id(n) for n in ('Express.js', '.NET')], ['express-js', 'net'])


class MergeTest(unittest.TestCase):

    def test_leaves_out_what_the_review_dropped(self):
        skills = merge([verdict('Clean', 'tool', outcome='drop'), verdict('Docker', 'devops')])
        self.assertEqual([s['canonical'] for s in skills], ['Docker'])

    def test_merges_the_verdicts_that_share_a_canonical_name(self):
        skills = merge([verdict('React', 'framework', 'react.js'), verdict('react', 'framework', 'ReactJS')])
        self.assertEqual(len(skills), 1)
        self.assertEqual(skills[0]['aliases'], ['react.js', 'ReactJS'])

    def test_drops_an_alias_that_is_the_canonical_name_or_repeats_another(self):
        skills = merge([verdict('Kubernetes', 'devops', 'kubernetes|k8s|K8S')])
        self.assertEqual(skills[0]['aliases'], ['k8s'])

    def test_refuses_one_skill_under_two_categories(self):
        with self.assertRaisesRegex(ValueError, 'Docker'):
            merge([verdict('Docker', 'devops'), verdict('Docker', 'tool')])

    def test_refuses_a_category_outside_the_fixed_set(self):
        with self.assertRaisesRegex(ValueError, 'orm'):
            merge([verdict('Hibernate', 'orm')])

    def test_refuses_a_key_that_names_two_skills(self):
        with self.assertRaisesRegex(ValueError, 'js'):
            merge([verdict('JavaScript', 'language', 'js'), verdict('Java', 'language', 'JS')])


class SelectTest(unittest.TestCase):

    def test_keeps_the_skills_named_in_enough_descriptions_by_the_keys_the_review_kept(self):
        docs = [tokens(d) for d in ('Go and Docker', 'Golang, Docker', 'golang', 'docker compose')]
        skills = merge([verdict('Golang', 'language'), verdict('Docker', 'devops')])
        self.assertEqual([s['canonical'] for s in select(skills, docs, 3)], ['Docker'])


if __name__ == '__main__':
    unittest.main()

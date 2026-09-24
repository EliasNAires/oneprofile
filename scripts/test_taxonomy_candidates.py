"""Tests for the rules the taxonomy seed applies. Run with

    python3 -m unittest discover -s scripts
"""
import unittest

from taxonomy_candidates import (build_candidates, clean_aliases, document_frequency, resolve,
                                 surface_forms, tokens)


def match(item, label, links, software=True, aliases=()):
    return {'item': item, 'label': label, 'links': links, 'software': software, 'aliases': list(aliases)}


def wikidata_row(item, label, cls, links, aliases=()):
    return {'item': item, 'label': label, 'class': cls, 'links': links, 'aliases': list(aliases)}


def by_name(candidates, name):
    return next(c for c in candidates if c.name == name)


class TokensTest(unittest.TestCase):

    def test_keeps_the_symbols_technology_names_are_spelled_with(self):
        self.assertEqual(tokens('Node.js, C++ and C#.'), ['node.js', 'c++', 'and', 'c#'])

    def test_keeps_a_leading_dot_only_before_a_letter(self):
        self.assertEqual(tokens('.NET and ...more'), ['.net', 'and', 'more'])


class SurfaceFormsTest(unittest.TestCase):

    def test_strips_the_software_suffix(self):
        self.assertEqual(surface_forms('Ansible software'), ['Ansible'])

    def test_splits_a_long_form_followed_by_its_acronym(self):
        self.assertEqual(sorted(surface_forms('Integrated development environment IDE software')),
                         ['IDE', 'Integrated development environment'])

    def test_never_keeps_a_vendor_prefixed_form(self):
        self.assertEqual(surface_forms('IBM Terraform'), ['Terraform'])
        self.assertEqual(surface_forms('Amazon Web Services AWS CloudFormation'), ['CloudFormation'])

    def test_keeps_a_vendor_that_is_the_whole_name(self):
        self.assertEqual(surface_forms('Amazon Web Services AWS software'), ['Amazon Web Services', 'AWS'])


class CleanAliasesTest(unittest.TestCase):

    def test_drops_parenthesised_dotted_and_programming_language_aliases(self):
        self.assertEqual(clean_aliases(['Python (lang)', '.yml', 'Python programming language', 'py']),
                         ['py'])


class ResolveTest(unittest.TestCase):

    def test_takes_the_most_linked_software_item_of_the_earliest_variant_that_names_one(self):
        lookup = {
            'Google Angular': [],
            'Angular': [match('Q1', 'Angular', 10), match('Q2', 'Angular', 90),
                        match('Q3', 'Angular (film)', 500, software=False)],
        }
        self.assertEqual(resolve('Google Angular', lookup)['item'], 'Q2')

    def test_finds_nothing_when_no_variant_names_a_software_item(self):
        lookup = {'Spring Boot': [match('Q4', 'Tupolev Tu-91', 30, software=False)]}
        self.assertIsNone(resolve('Spring Boot', lookup))


class BuildCandidatesTest(unittest.TestCase):

    def test_merges_by_wikidata_item_then_by_normalized_name(self):
        wikidata = [wikidata_row('Q7', 'PostgreSQL', 'DBMS', 80)]
        onet = {'PostgreSQL software': {'Data base management system software'},
                'Oracle Java': {'Object or component oriented development software'},
                'Java': {'Development environment software'}}
        lookup = {'PostgreSQL': [match('Q7', 'PostgreSQL', 80)]}
        candidates = build_candidates(wikidata, onet, lookup, {})
        self.assertEqual(sorted(c.name for c in candidates), ['Java', 'PostgreSQL'])
        self.assertEqual(by_name(candidates, 'PostgreSQL').qid, 'Q7')

    def test_merges_the_manual_leftovers(self):
        onet = {'Ansible software': {'Configuration management software'},
                'Red Hat Ansible Engine': {'Configuration management software'}}
        self.assertEqual([c.name for c in build_candidates([], onet, {}, {})], ['Ansible'])

    def test_takes_a_manually_merged_example_as_written_rather_than_from_wikidata(self):
        onet = {'Amazon Elastic Compute Cloud EC2': {'Cloud-based management software'}}
        lookup = {'Compute Cloud EC2': [match('Q11', 'Some extension', 3)]}
        [ec2] = build_candidates([], onet, lookup, {})
        self.assertEqual((ec2.name, ec2.qid, ec2.forms), ('EC2', None, ['EC2', 'Elastic Compute Cloud']))

    def test_merges_by_exact_name_before_other_candidates_written_the_same_way(self):
        wikidata = [wikidata_row('Q15777', 'C', 'programming language', 150),
                    wikidata_row('Q12', 'C shell', 'programming language', 20, aliases=['C']),
                    wikidata_row('Q81348', 'C--', 'programming language', 24)]
        linguist = {'C': {'type': 'programming', 'aliases': []}}
        self.assertEqual(sorted(c.name for c in build_candidates(wikidata, {}, {}, linguist)),
                         ['C', 'C shell', 'C--'])

    def test_drops_a_form_the_tokenizer_cannot_spell(self):
        wikidata = [wikidata_row('Q4659444', 'A♯', 'programming language', 10, aliases=['A sharp'])]
        [a_sharp] = build_candidates(wikidata, {}, {}, {})
        self.assertEqual((a_sharp.name, a_sharp.forms), ('A♯', ['A sharp']))

    def test_keeps_a_linguist_alias_only_when_it_names_no_other_candidate(self):
        wikidata = [wikidata_row('Q8', 'Terraform', 'software framework', 40),
                    wikidata_row('Q9', 'HCL', 'programming language', 10)]
        linguist = {'HCL': {'type': 'programming', 'aliases': ['terraform', 'hashicorp config']}}
        hcl = by_name(build_candidates(wikidata, {}, {}, linguist), 'HCL')
        self.assertEqual(hcl.forms, ['HCL', 'hashicorp config'])


class DocumentFrequencyTest(unittest.TestCase):

    def test_counts_a_document_once_per_candidate_over_all_its_forms(self):
        wikidata = [wikidata_row('Q7', 'PostgreSQL', 'DBMS', 80, aliases=['Postgres']),
                    wikidata_row('Q10', 'Spring Boot', 'software framework', 30)]
        candidates = build_candidates(wikidata, {}, {}, {})
        docs = [tokens('PostgreSQL, also called Postgres.'), tokens('Spring Boot on Postgres'),
                tokens('booting in spring')]
        document_frequency(candidates, docs)
        postgres = by_name(candidates, 'PostgreSQL')
        self.assertEqual(postgres.df, 2)
        self.assertEqual(postgres.form_df, {'PostgreSQL': 1, 'Postgres': 2})
        self.assertEqual(by_name(candidates, 'Spring Boot').df, 1)


if __name__ == '__main__':
    unittest.main()

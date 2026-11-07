# The contents of this file are subject to the terms of the Common Development and
# Distribution License (the License). You may not use this file except in compliance with the
# License.
#
# You can obtain a copy of the License at legal/CDDLv1.0.txt. See the License for the
# specific language governing permission and limitations under the License.
#
# When distributing Covered Software, include this CDDL Header Notice in each file and include
# the License file at legal/CDDLv1.0.txt. If applicable, add the following below the CDDL
# Header, with the fields enclosed by brackets [] replaced by your own identifying
# information: "Portions copyright [year] [name of copyright owner]".
#
# Copyright 2026 3A Systems, LLC.

"""Regression tests for How-To link validation."""

from pathlib import Path
import re
import subprocess
import sys
import tempfile
import unittest


CHECKER = Path(__file__).with_name("check-how-to-links.py")


class HowToLinksTest(unittest.TestCase):
    def run_checker(self, files):
        with tempfile.TemporaryDirectory() as directory:
            source = Path(directory)
            (source / "how-to").mkdir()
            for name, text in files.items():
                path = source / name
                path.parent.mkdir(parents=True, exist_ok=True)
                path.write_text(text, encoding="utf-8")
            return subprocess.run([sys.executable, str(CHECKER), str(source)],
                                  capture_output=True, text=True)

    def check_link(self, anchor, definition):
        return self.run_checker({
            "how-to/page.adoc": f"xref:../reference/module.adoc#{anchor}[Module]\n",
            "reference/module.adoc": definition,
        })

    def test_missing_page_is_reported(self):
        result = self.run_checker({"how-to/page.adoc": "xref:../reference/absent.adoc[M]\n"})
        self.assertEqual(1, result.returncode, result.stdout + result.stderr)
        self.assertIn("missing page", result.stdout)

    def test_same_page_fragment_resolves_on_the_article(self):
        result = self.run_checker({"how-to/page.adoc": "[[local]]\nxref:#local[L]\n"})
        self.assertEqual(0, result.returncode, result.stdout + result.stderr)

    def test_same_page_id_resolves_on_the_article(self):
        result = self.run_checker({"how-to/page.adoc": "[[local]]\nxref:local[L]\n"})
        self.assertEqual(0, result.returncode, result.stdout + result.stderr)

    def test_missing_same_page_id_is_reported(self):
        result = self.run_checker({"how-to/page.adoc": "xref:absent[A]\n"})
        self.assertEqual(1, result.returncode, result.stdout + result.stderr)
        self.assertIn("broken anchor", result.stdout)

    def test_dotted_target_remains_a_document_path(self):
        result = self.run_checker({"how-to/page.adoc": "xref:module.html[M]\n",
                                   "how-to/module.html": "<h1>Module</h1>\n"})
        self.assertEqual(0, result.returncode, result.stdout + result.stderr)
        result = self.run_checker({"how-to/page.adoc": "[[module.html]]\nxref:module.html[M]\n"})
        self.assertEqual(1, result.returncode, result.stdout + result.stderr)
        self.assertIn("missing page", result.stdout)

    def test_shorthand_xrefs_resolve_local_and_cross_page_ids(self):
        result = self.run_checker({
            "how-to/page.adoc": "[[local]]\n<<local>>\n<<local,Local>>\n"
                                "<<../reference/module.adoc#remote,Remote>>\n",
            "reference/module.adoc": "[[remote]]\n== Module\n",
        })
        self.assertEqual(0, result.returncode, result.stdout + result.stderr)
        self.assertIn("Checked 3 How-To xrefs", result.stdout)

    def test_shorthand_missing_destinations_are_reported(self):
        for link, diagnostic in (("<<absent>>", "broken anchor"),
                                 ("<<absent,Absent>>", "broken anchor"),
                                 ("<<../reference/absent.adoc#id,Absent>>", "missing page")):
            with self.subTest(link=link):
                result = self.run_checker({"how-to/page.adoc": link + "\n"})
                self.assertEqual(1, result.returncode, result.stdout + result.stderr)
                self.assertIn(diagnostic, result.stdout)

    def test_shorthand_dotted_id_resolves_on_the_article(self):
        result = self.run_checker({"how-to/page.adoc":
                                   "[[section.id]]\n<<section.id>>\n<<section.id,Section>>\n"})
        self.assertEqual(0, result.returncode, result.stdout + result.stderr)
        self.assertIn("Checked 2 How-To xrefs", result.stdout)

    def test_component_landing_page_is_skipped(self):
        result = self.run_checker({"how-to/page.adoc": "xref:ROOT:index.adoc[Home]\n"})
        self.assertEqual(0, result.returncode, result.stdout + result.stderr)

    def test_no_generated_pages_fails(self):
        result = self.run_checker({})
        self.assertEqual(1, result.returncode, result.stdout + result.stderr)
        self.assertIn("No generated How-To pages", result.stdout)

    def test_reftext_role_and_option_are_not_part_of_the_id(self):
        for definition in ("[[module-ref,Module]]", "[#module-ref.role]", "[#module-ref%opt]"):
            with self.subTest(definition=definition):
                result = self.check_link("module-ref", definition + "\n")
                self.assertEqual(0, result.returncode, result.stdout + result.stderr)

    def test_page_only_xref_is_counted(self):
        result = self.run_checker({"how-to/page.adoc": "xref:../reference/module.adoc[M]\n",
                                   "reference/module.adoc": "= Module\n"})
        self.assertEqual(0, result.returncode, result.stdout + result.stderr)
        self.assertIn("Checked 1 How-To xrefs", result.stdout)

    def test_automatic_section_id_requires_an_explicit_destination_id(self):
        result = self.check_link("_module", "== Module\n")
        self.assertEqual(1, result.returncode, result.stdout + result.stderr)
        self.assertIn("needs an explicit [[id]] or [#id]", result.stdout)

    def test_antora_page_id_requires_a_relative_source_path(self):
        result = self.run_checker({"how-to/page.adoc": "xref:reference:module.adoc[M]\n",
                                   "reference/module.adoc": "= Module\n"})
        self.assertEqual(1, result.returncode, result.stdout + result.stderr)
        self.assertIn("use a relative .adoc path", result.stdout)

    def test_cross_component_page_id_explains_the_url_alternative(self):
        result = self.run_checker({"how-to/page.adoc": "xref:opendj:install-guide:index.adoc[DJ]\n"})
        self.assertEqual(1, result.returncode, result.stdout + result.stderr)
        self.assertIn("or a link: URL for another component", result.stdout)

    def test_wiki_patch_destinations_exist(self):
        doc = Path(__file__).parent
        patch = (doc / "wiki-migration/wiki-how-to.patch").read_text(encoding="utf-8")
        links = set(re.findall(
            r"https://doc\.openidentityplatform\.org/openidm/how-to/([a-z0-9-]+)#([\w.-]+)", patch))
        self.assertEqual(37, len(links))
        files = {f"reference/{page}.adoc":
                 (doc / "src/main/asciidoc/how-to" / f"{page}.adoc").read_text(encoding="utf-8")
                 for page, _ in links}
        files["how-to/wiki-links.adoc"] = "\n".join(
            f"xref:../reference/{page}.adoc#++{anchor}++[Wiki]" for page, anchor in sorted(links))
        result = self.run_checker(files)
        self.assertEqual(0, result.returncode, result.stdout + result.stderr)
        self.assertIn("Checked 37 How-To xrefs", result.stdout)

    def test_wiki_patch_page_links_exist(self):
        doc = Path(__file__).parent
        patch = (doc / "wiki-migration/wiki-how-to.patch").read_text(encoding="utf-8")
        added = "\n".join(line for line in patch.splitlines() if line.startswith("+"))
        paths = re.findall(r"https://doc\.openidentityplatform\.org/openidm/([^)\s#\]]*)", added)
        self.assertEqual(48, len(paths))
        for path in sorted(set(paths)):
            with self.subTest(path=path):
                self.assertTrue(path.startswith("how-to/"), path)
                page = path[len("how-to/"):] or "index"
                self.assertTrue((doc / "src/main/asciidoc/how-to" / f"{page}.adoc").is_file(), path)

    def test_double_hyphen_between_word_characters_needs_passthrough(self):
        result = self.check_link("adaptive-risk--module-ref", "[[adaptive-risk--module-ref]]\n")
        self.assertEqual(1, result.returncode, result.stdout + result.stderr)
        self.assertIn("fragment needs ++...++", result.stdout)
        self.assertIn("Checked 1 How-To xrefs", result.stdout)

    def test_passthrough_preserves_double_hyphen(self):
        result = self.check_link("++adaptive-risk--module-ref++", "[[adaptive-risk--module-ref]]\n")
        self.assertEqual(0, result.returncode, result.stdout + result.stderr)

    def test_double_hyphen_outside_words_does_not_need_passthrough(self):
        result = self.check_link("--module-ref", "[[--module-ref]]\n")
        self.assertEqual(0, result.returncode, result.stdout + result.stderr)

    def test_anchors_inside_verbatim_blocks_are_not_destinations(self):
        for delimiter in ("////", "----", "...."):
            for definition in ("[[hidden]]", "[#hidden]"):
                with self.subTest(delimiter=delimiter, definition=definition):
                    result = self.check_link("hidden", f"{delimiter}\n{definition}\n{delimiter}\n")
                    self.assertEqual(1, result.returncode, result.stdout + result.stderr)
                    self.assertIn("broken anchor", result.stdout)

    def test_anchors_inside_unterminated_verbatim_blocks_are_not_destinations(self):
        for delimiter in ("////", "----", "...."):
            with self.subTest(delimiter=delimiter):
                result = self.check_link("hidden", f"{delimiter}\n[[hidden]]\ntext\n")
                self.assertEqual(1, result.returncode, result.stdout + result.stderr)
                self.assertIn("broken anchor", result.stdout)

    def test_anchor_after_verbatim_block_is_a_destination(self):
        result = self.check_link("visible", "----\n[[hidden]]\n----\n[#visible]\n")
        self.assertEqual(0, result.returncode, result.stdout + result.stderr)

    def test_block_closes_only_on_its_own_delimiter(self):
        for definition in ("-----\n----\n[[hidden]]\ntext\n-----\n",
                           "----\n....\n[[hidden]]\ntext\n----\n"):
            with self.subTest(definition=definition):
                result = self.check_link("hidden", definition)
                self.assertEqual(1, result.returncode, result.stdout + result.stderr)
                self.assertIn("broken anchor", result.stdout)

    def test_anchor_between_two_verbatim_blocks_is_a_destination(self):
        result = self.check_link("visible", "----\na\n----\n\n[[visible]]\ntext\n\n----\nb\n----\n")
        self.assertEqual(0, result.returncode, result.stdout + result.stderr)

    def test_longer_and_padded_delimiters_open_and_close_blocks(self):
        for definition in ("-----\n[[hidden]]\ntext\n-----\n",
                           "----\n[[hidden]]\ntext\n----  \n",
                           "---- \n[[hidden]]\ntext\n----\t\n"):
            with self.subTest(definition=repr(definition)):
                result = self.check_link("hidden", definition)
                self.assertEqual(1, result.returncode, result.stdout + result.stderr)
                self.assertIn("broken anchor", result.stdout)


if __name__ == "__main__":
    unittest.main()

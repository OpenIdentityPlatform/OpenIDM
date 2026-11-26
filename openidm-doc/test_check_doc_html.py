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

"""Verify the rendered documentation checker against small Antora-style pages."""

from contextlib import redirect_stdout
from io import StringIO
from pathlib import Path
from runpy import run_path
from tempfile import TemporaryDirectory
from unittest import TestCase


checker = run_path(str(Path(__file__).with_name("check-doc-html.py")))
check_site = checker["check_site"]
PUBLISHED_FRAGMENTS = checker["PUBLISHED_FRAGMENTS"]


class DocumentationHtmlTest(TestCase):
    def check(self, files, *, include_published_pages=True):
        site_files = {}
        if include_published_pages:
            for path, fragments in PUBLISHED_FRAGMENTS.items():
                site_files[path.relative_to("openidm")] = "".join(
                    f'<a id="{fragment}"></a>' for fragment in sorted(fragments)
                )
        site_files.update((Path(path), content) for path, content in files.items())
        with TemporaryDirectory() as directory:
            site = Path(directory)
            for filename, content in site_files.items():
                path = site / "openidm" / filename
                path.parent.mkdir(parents=True, exist_ok=True)
                path.write_text(content, encoding="utf-8")
            out = StringIO()
            with redirect_stdout(out):
                code = check_site(site)
            return code, out.getvalue()

    def test_extensionless_link_and_image(self):
        code, output = self.check({
            "intro.html": '<article class="doc"><a href="guide/chapter#chapter">Chapter</a>'
                          '<img src="_images/sample.png"></article>',
            "guide/chapter.html": '<article class="doc"><a id="chapter"></a></article>',
            "_images/sample.png": "",
        })
        self.assertEqual(0, code, output)

    def test_missing_chapter_anchor_and_image(self):
        code, output = self.check({
            "intro.html": '<article class="doc"><a href="guide/#lost">Chapter</a>'
                          '<img src="_images/missing.png"></article>',
            "guide/index.html": '<article class="doc"><h1>Chapter</h1></article>',
        })
        self.assertEqual(1, code, output)
        self.assertIn("missing fragment guide/#lost", output)
        self.assertIn("missing image destination _images/missing.png", output)

    def test_updating_openidm_preserves_bookmarks(self):
        code, output = self.check({
            "install-guide/chap-update.html": '<article class="doc"><a id="chap-update" '
                                               'href="#chap-update">Updating OpenIDM</a></article>',
        })
        self.assertEqual(1, code, output)
        self.assertIn("missing published fragment #install-update-process", output)
        self.assertIn("missing published fragment #update-logging-properties", output)

    def test_renamed_upgrade_page_breaks_published_bookmarks(self):
        code, output = self.check({
            "index.html": '<nav class="nav-menu">'
                          '<a href="install-guide/upgrade#install-update-process">Upgrade</a></nav>',
            "install-guide/upgrade.html": '<article class="doc">'
                                          '<a id="install-update-process"></a></article>',
        }, include_published_pages=False)
        self.assertEqual(1, code, output)
        self.assertIn("openidm/install-guide/chap-update.html: missing published page", output)

    def test_only_other_product_references_may_be_unresolved(self):
        code, output = self.check({
            "intro.html": '<article class="doc">'
                          '<a href="#opendj:install-guide:index.adoc" class="xref unresolved">OpenDJ</a>'
                          '<a href="#install-guide:missing.adoc" class="xref unresolved">Missing</a>'
                          '</article>',
        })
        self.assertEqual(1, code, output)
        self.assertIn("missing fragment #install-guide:missing.adoc", output)
        self.assertNotIn("opendj", output)

    def test_fail_if_no_document_references_were_checked(self):
        code, output = self.check({"intro.html": '<a href="guide/chapter.html">Outside docs</a>'})
        self.assertEqual(1, code, output)
        self.assertIn("No internal document references", output)

    def test_missing_navigation_target(self):
        code, output = self.check({
            "index.html": '<nav class="nav-menu"><a href="guide/missing">Guide</a></nav>'
                          '<article class="doc"><a id="top" href="#top">Top</a></article>',
        })
        self.assertEqual(1, code, output)
        self.assertIn("missing link destination guide/missing", output)

    def test_component_url_without_trailing_slash(self):
        code, output = self.check({
            "index.html": '<article class="doc"><a id="top" href="/openidm#top">Top</a></article>',
        })
        self.assertEqual(0, code, output)

    def test_percent_encoded_path_cannot_leave_the_component(self):
        code, output = self.check({
            "intro.html": '<article class="doc"><img src="%2e%2e/outside.png"></article>',
            "../outside.png": "",
        })
        self.assertEqual(1, code, output)
        self.assertIn("destination outside the OpenIDM component", output)

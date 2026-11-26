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

"""Check OpenIDM page, fragment, and image references in a rendered Antora site."""

from argparse import ArgumentParser
from html.parser import HTMLParser
from pathlib import Path
from urllib.parse import unquote, urljoin, urlsplit


# Published bookmarks can exist without a link in the rebuilt site.
PUBLISHED_FRAGMENTS = {
    Path("openidm/install-guide/chap-update.html"): {
        "d9505e3937",
        "install-update-process",
        "migrate-idm-40-windows",
        "openidm-update-repos",
        "update-4001-4002",
        "update-4002-45",
        "update-4045-repo",
        "update-4045-stage1",
        "update-afterwards",
        "update-afterwards-json",
        "update-afterwards-ui",
        "update-afterwards-ui-procedure",
        "update-file-after",
        "update-file-during",
        "update-file-preview",
        "update-file-states",
        "update-idm-cli",
        "update-logging-properties",
        "update-process-checksums",
        "update-to-4001-linux",
        "update-to-4002",
        "update-to-45-cli",
        "update-ui",
    },
}


class Page(HTMLParser):
    def __init__(self, path):
        super().__init__(convert_charrefs=True)
        self.ids = set()
        self.references = set()
        self.in_document = False
        self.in_navigation = False
        self.feed(path.read_text(encoding="utf-8"))

    def handle_starttag(self, tag, attributes):
        attrs = dict(attributes)
        classes = attrs.get("class", "").split()
        if tag == "article" and "doc" in classes:
            self.in_document = True
        if tag == "nav" and "nav-menu" in classes:
            self.in_navigation = True
        if attrs.get("id"):
            self.ids.add(attrs["id"])
        if (self.in_document or self.in_navigation) and tag == "a" and attrs.get("href"):
            href = attrs["href"]
            # The stand-alone build does not include the other product components.
            if "unresolved" not in classes or not href.startswith(("#openam:", "#opendj:")):
                self.references.add(("link", href))
        if self.in_document and tag == "img" and attrs.get("src"):
            self.references.add(("image", attrs["src"]))

    def handle_endtag(self, tag):
        if tag == "article":
            self.in_document = False
        if tag == "nav":
            self.in_navigation = False


def local_target(source, reference):
    dest = urlsplit(urljoin("/" + source.as_posix(), reference))
    if dest.scheme or dest.netloc:
        return None
    if dest.path != "/openidm" and not dest.path.startswith("/openidm/"):
        return None
    return Path(unquote(dest.path.lstrip("/"))), unquote(dest.fragment)


def check_site(site):
    site = site.resolve()
    component = site / "openidm"
    pages = {path.relative_to(site): Page(path) for path in sorted(component.rglob("*.html"))}
    if not pages:
        print(f"No OpenIDM HTML pages found in {component}")
        return 1

    checked = 0
    errors = 0
    for path, page in pages.items():
        for fragment in sorted(PUBLISHED_FRAGMENTS.get(path, ())):
            if fragment not in page.ids:
                print(f"{path}: missing published fragment #{fragment}")
                errors += 1
        for kind, reference in sorted(page.references):
            target = local_target(path, reference)
            if target is None:
                continue
            dest, fragment = target
            checked += 1
            candidate = site / dest
            if candidate.is_dir():
                candidate = candidate / "index.html"
            elif kind == "link" and not candidate.is_file():
                candidate = candidate.with_name(candidate.name + ".html")

            candidate = candidate.resolve()
            if not candidate.is_relative_to(component):
                print(f"{path}: destination outside the OpenIDM component {reference}")
                errors += 1
            elif not candidate.is_file():
                print(f"{path}: missing {kind} destination {reference}")
                errors += 1
            else:
                dest = candidate.relative_to(site)
                if fragment and dest in pages and fragment not in pages[dest].ids:
                    print(f"{path}: missing fragment {reference}")
                    errors += 1

    if not checked:
        print("No internal document references found in the rendered OpenIDM pages.")
        return 1
    print(f"Checked {checked} references across {len(pages)} OpenIDM pages, {errors} errors.")
    return int(errors != 0)


if __name__ == "__main__":
    parser = ArgumentParser(description=__doc__)
    parser.add_argument("site", type=Path, help="directory containing the rendered openidm component")
    raise SystemExit(check_site(parser.parse_args().site))

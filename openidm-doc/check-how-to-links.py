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

"""Check How-To xrefs against generated AsciiDoc, and explicit destination IDs."""

from pathlib import Path
import os
import re
import sys


VERBATIM = re.compile(r"^(/{4,}|-{4,}|\.{4,}|```)(?:[ \t]*|(?<=`)(?!`)[^\n]*)\n.*?"
                      r"(?:(?P<closed>^\1[ \t]*$)|\Z)",
                      re.MULTILINE | re.DOTALL)
VERBATIM_PARAGRAPH = re.compile(r"^\[(?:source|listing|literal)\b[^\]\n]*\]\n"
                                r"(?:[ \t]*\S.*(?:\n|\Z))*", re.MULTILINE)
LINE_COMMENT = re.compile(r"^//(?=[^/\n]|$)[^\n]*\n?", re.MULTILINE)
TABLE = re.compile(r"^\|===[ \t]*\n.*?^\|===[ \t]*$", re.MULTILINE | re.DOTALL)
BLOCK_BREAK = r"\n[ \t]*(?:\n|\||(?:={4,}|\*{4,}|_{4,}|\+{4,}|-{2,}|/{4,}|\.{4,})[ \t]*(?:\n|\Z))"
XREF = re.compile(r"(?<!\\)(?:xref:([^\s\[]+)\[|"
                  r"<<([\w#/.:][^,|\n\x00]*?(?:,(?:(?!" + BLOCK_BREAK + r")[^\x00])*?)?)>>)",
                  re.DOTALL)


def main():
    source = (Path(sys.argv[1]) if len(sys.argv) > 1 else
              Path(__file__).parent / "target/asciidoc/source")
    articles = sorted((source / "how-to").glob("*.adoc"))
    if not articles:
        print(f"No generated How-To pages found in {source}; build the documentation first.")
        return 1

    errors = []
    sources = {}

    def source_text(path):
        path = path.resolve()
        if path not in sources:
            original = path.read_text(encoding="utf-8")
            unclosed = False

            def strip_block(match):
                nonlocal unclosed
                if match.group("closed") is None:
                    errors.append(f"{os.path.relpath(path, source.resolve())}: "
                                  f"unclosed {match.group(1)} block")
                    unclosed = True
                return "\n\n"

            text = VERBATIM.sub(strip_block, original)
            text = VERBATIM_PARAGRAPH.sub("\n\n", text)
            sources[path] = LINE_COMMENT.sub("", text), unclosed
        return sources[path]

    checked = 0
    for article in articles:
        article_text, _ = source_text(article)
        # A table cell boundary must also stop shorthand link text on the same line.
        article_text = TABLE.sub(lambda table: re.sub(r"(?<!\\)\|", "\x00", table.group()),
                                 article_text)
        for match in XREF.finditer(article_text):
            target = match.group(1) or match.group(2).split(",", 1)[0]
            # Antora creates the component landing page; it has no source file here.
            if target == "ROOT:index.adoc":
                continue
            page, separator, anchor = target.partition("#")
            checked += 1
            if ":" in page:
                errors.append(f"{article.name}: unsupported Antora page ID "
                              f"(use a relative .adoc path, or a link: URL for another component): {target}")
                continue
            if not separator and (match.group(2) is not None or "." not in page):
                # Shorthand IDs may contain dots; dotted xref: targets are document paths.
                page, anchor = "", page
            if anchor.startswith("++") and anchor.endswith("++"):
                anchor = anchor[2:-2]
            elif re.search(r"\w--\w", anchor):
                # Asciidoctor turns an intraword double hyphen into an em dash.
                errors.append(f"{article.name}: fragment needs ++...++: {target}")
                continue
            destination = article.parent / page if page else article
            if not destination.is_file():
                errors.append(f"{article.name}: missing page: {target}")
                continue
            if anchor:
                # Comment, listing and literal blocks neither link nor define anchors.
                text, unclosed = source_text(destination)
                ids = re.findall(r"^\[\[([^\],]+)", text, re.MULTILINE)
                # In shorthand attributes, a dot starts a role and % starts an option.
                ids += re.findall(r"^\[#([^\].,%]+)", text, re.MULTILINE)
                if anchor not in ids and not unclosed:
                    errors.append(f"{article.name}: broken anchor "
                                  f"(needs an explicit [[id]] or [#id]): {target}")

    for error in errors:
        print(error)
    print(f"Checked {checked} How-To xrefs in {len(articles)} pages; {len(errors)} errors.")
    return 1 if errors else 0


if __name__ == "__main__":
    sys.exit(main())

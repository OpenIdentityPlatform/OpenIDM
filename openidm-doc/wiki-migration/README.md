# Deferred How-To publication

This migration follows [OpenAM #1218](https://github.com/OpenIdentityPlatform/OpenAM/pull/1218).
Five standalone tutorials and 28 referenced screenshots are now part of the Maven-built How-To guide.
The formal guide landing pages, Getting Started (already covered by the existing guide), sidebar/footer and external build/Docker links are not duplicated.

## Source and inventory

Wiki repository: https://github.com/OpenIdentityPlatform/OpenIDM.wiki.git

Source revision: `c2b7bfa16d9e024771014c4f01b8ab00f376edfb`

| Wiki page | Source in `../src/main/asciidoc/how-to/` | Retained section anchors |
| --- | --- | --- |
| `Active-Directory-Identity-Management.md` | `active-directory-identity-management.adoc` | 10 |
| `Can-your-IDM-play-chess?.md` | `can-your-idm-play-chess.adoc` | 7 |
| `Configuring-OpenIDM-for-Synchronization-Between-Active-Directory-and-OpenDJ.md` | `configuring-openidm-for-synchronization-between-active-directory-and-opendj.adoc` | 10 |
| `Configuring-OpenIDM-for-Synchronization-Between-OpenLDAP-and-OpenDJ.md` | `configuring-openidm-for-synchronization-between-openldap-and-opendj.adoc` | 10 |
| `TIP:-How-to-Persist-OpenIDM-Configuration-Data-Between-Restarts.md` | `how-to-persist-openidm-configuration-data-between-restarts.adoc` | 0 |

The two synchronization tutorials were absent from Home; the new guide and deferred Home update include both. Home keeps its `How-To` heading and links directly to the published guide below it; the patch does not create a separate wiki landing page.
Article-prefixed IDs keep the combined PDF's section identifiers unique. The wiki patch retains the original heading levels, spelling and duplicate-heading suffixes so existing incoming section links continue to land on links to the matching documentation section.

## Content adjustments

- Removed the Active Directory `cn` transformation because the sample connector ignores writes to that attribute; `cn` comes from the RDN in `dn`. Protected JavaScript `||` operators in Markdown tables during conversion.
- Corrected the AD bind-name example to `DOMAIN\Administrator`, retained the sample's filters excluding disabled accounts and computers, and clarified the comma needed before adding the password property. The REST create request and all four example responses use `givenName: John` and `sn: Doe`.
- Corrected `sAMAccountName` in the AD source mapping. For the reverse OpenLDAP mapping, replaced the copied AD DN and `sAMAccountName` with `cn=<userName>,ou=users,dc=example,dc=com` and `uid` to match the supplied OpenLDAP data and connector schema.
- Set the AD mapping's Source Missing policy to Delete so deleting a managed user also deletes its linked AD account. The reverse OpenLDAP mapping sets `cn` to `userName`, retaining its RDN value during updates; its manual checks cover existing `user01`/`user02` entries and a repeated synchronization as well as the new `bjensen` entry.
- Corrected obvious spelling, the chess article's OpenIDM documentation label and sample project paths, and the AD hostname typo. Kept historical OpenIDM 6.2.3 command output labeled as such.
- Created the host logs directory before copying and changing ownership of persisted Docker data, retaining the wiki's UID/GID 1001:1001.
- Converted links to this product's guides to version-aware xrefs. OpenLDAP JSON/LDIF downloads keep their original wiki URLs (the raw wiki endpoint does not support commit-pinned URLs); the wiki patch retains these files. Images are local copies. Existing external product links and sample versions are preserved. External integrations have not been retested.

The link checker accepts `xref:` and `<<...>>` references to relative `.adoc` source paths with optional fragments, or to same-page explicit IDs. A bare `xref:id[]` addresses the current page; a dotted `xref:` target is a document path and needs `#` to address a local ID. Shorthand references without `#`, including dotted IDs, address the current page. Fragment destinations require explicit `[[id]]` or `[#id]` anchors outside comment, listing and literal blocks, including blocks left unclosed at end of file. Automatic section IDs and Antora page IDs such as `integrators-guide:chap-workflow.adoc` are outside its scope and produce diagnostics explaining the required form; use a `link:` URL for another component. `ROOT:index.adoc` is skipped because Antora generates that landing page.

The Linux/JDK 17 build runs the checker against generated AsciiDoc and runs 26 regression tests. These cover missing pages, empty generated output, same-page fragments and bare IDs, shorthand references, dotted targets, the landing-page exception, page-only xref counts, both explicit anchor syntaxes and their modifiers, closed and unclosed verbatim blocks, double-hyphen fragments, and the documented syntax restrictions. Wiki tests check all 48 added documentation URLs and run all 37 section destinations through the checker so anchors inside verbatim blocks cannot pass. Linux build, deployment and release jobs use `-Xmx6g` for illustrated PDF generation; other build platforms use `-Xmx4g`.

## Validation at this review

- All 26 Python link-checker regression tests pass; generated source has 5 xrefs across 6 How-To pages, with 0 errors.
- New tests first reproduced the bare-ID, shorthand, unclosed-block and cross-component diagnostic gaps. The wiki tests reject a page-only URL typo and a section ID wrapped in a listing block.
- doc-maven-plugin 3.2.2 preprocessing and Antora generation pass under JDK 17. Source whitespace checks pass. The wiki and site patches are unchanged in this round.
- The How-To PDF builds under JDK 17 with `-Xmx6g`: 38 pages, 2,864,405 bytes. HTML assertions and extracted PDF text confirm the AD deletion policy, the reverse OpenLDAP `userName` to `cn` mapping and the repeated-synchronization checks; the changed PDF pages were inspected visually.

## Earlier migration validation

- The previous review round passed 17 tests, seven targeted checker mutations and a renamed wiki destination. Its How-To PDF had 38 pages and 2,853,886 bytes; extracted text and visual inspection covered the AD table. The REST examples and password insertion parsed as JSON, and the regenerated wiki patch passed forward/reverse applicability checks at its pinned revision with the correct Home post-image hash.
- The current-version site integration build produced all 6 pages with no How-To diagnostics; local article links, fragments, 28 images, all 37 wiki section destinations and the repaired JavaScript expressions resolved.
- The How-To PDF built under JDK 17 with `-Xmx6g`: 38 pages, 2,853,264 bytes. Text checks and sample page inspection covered the repaired mapping table, screenshots and Docker commands.
- Both deferred patches passed forward/reverse applicability checks at their recorded source revisions. All 28 image copies were byte-identical to the wiki originals, and workflow YAML parsing passed.

The full site build, full product tests, CI and external directory/Docker integrations were not rerun for this review.

## Publication order

Neither patch is applied by the build. Do not replace the live wiki articles until the documentation and site navigation have deployed and their links/images have been verified.

1. Publish OpenIDM's generated `how-to` module and `HowTo_Guides.pdf` through the existing documentation deployment.
2. Apply and review `site-how-to.patch` in `OpenIdentityPlatform/doc.openidentityplatform.org`, then deploy the site. The patch adds the guide to OpenIDM navigation, the site landing page and PDF mapping. Its source revision is `c365eaaf207bab56f4a1065de19deaa3fcd0f765`.
3. Check all five published articles, their section links and images, and the PDF.
4. Apply and review `wiki-how-to.patch` in a separate wiki checkout. Existing image files and original downloads stay in the wiki for historical links.

```sh
# In the documentation-site checkout:
git apply --check /path/to/site-how-to.patch
git apply /path/to/site-how-to.patch
git diff --check
git diff

# In a clean wiki checkout at the pinned revision:
git apply --check /path/to/wiki-how-to.patch
git apply /path/to/wiki-how-to.patch
git diff --check
git diff
```

If either repository has changed, review intervening edits and refresh the corresponding sources and patch before applying. Commit and publish each change separately after review.

Cross-guide PDF links use the shared converter's site URL policy; preserving historical release version paths is a separate shared-converter concern, as recorded in OpenAM #1220.

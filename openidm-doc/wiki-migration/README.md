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

The two synchronization tutorials were absent from Home; the new guide and deferred Home update include both.
Article-prefixed IDs keep the combined PDF's section identifiers unique. The wiki patch retains the original heading levels, spelling and duplicate-heading suffixes so existing incoming section links continue to land on links to the matching documentation section.

## Content adjustments

- Repaired the Active Directory `cn` transformation and protected JavaScript `||` operators in Markdown tables during conversion.
- Corrected `sAMAccountName` in the AD source mapping. For the reverse OpenLDAP mapping, replaced the copied AD DN and `sAMAccountName` with `cn=<userName>,ou=users,dc=example,dc=com` and `uid` to match the supplied OpenLDAP data and connector schema.
- Corrected obvious spelling, the chess article's OpenIDM documentation label and sample project paths, and the AD hostname typo. Kept historical OpenIDM 6.2.3 command output labeled as such.
- Created the host logs directory before copying and changing ownership of persisted Docker data, retaining the wiki's UID/GID 1001:1001.
- Converted links to this product's guides to version-aware xrefs. OpenLDAP JSON/LDIF downloads keep their original wiki URLs (the raw wiki endpoint does not support commit-pinned URLs); the wiki patch retains these files. Images are local copies. Existing external product links and sample versions are preserved. External integrations have not been retested.

The reviewed OpenAM link checker and its eight regression tests are ported without changing their validation rules. The Linux/JDK 17 build runs them against generated AsciiDoc. Linux build, deployment and release jobs use `-Xmx6g` for illustrated PDF generation; other build platforms use `-Xmx4g`.

## Validation

- Eight Python link-checker regression tests pass; generated source has 5 xrefs across 6 How-To pages, with 0 errors.
- doc-maven-plugin 3.2.2 preprocessing and Antora generation pass under JDK 17. The current-version site integration build produces all 6 pages with no How-To diagnostics; local article links, fragments, 28 images, all 37 wiki section destinations and the repaired JavaScript expressions resolve.
- The How-To PDF builds under JDK 17 with `-Xmx6g`: 38 pages, 2,853,264 bytes. Text checks and sample page inspection cover the repaired mapping table, screenshots and Docker commands.
- Both deferred patches pass forward/reverse applicability checks at their recorded source revisions. All 28 image copies are byte-identical to the wiki originals.
- Workflow YAML parsing and source whitespace checks pass. Patch files preserve historical whitespace in context/deleted lines. Full product tests, CI and external directory/Docker integrations were not run.

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

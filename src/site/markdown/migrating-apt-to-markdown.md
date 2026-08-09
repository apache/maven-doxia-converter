<!--
Licensed to the Apache Software Foundation (ASF) under one
or more contributor license agreements.  See the NOTICE file
distributed with this work for additional information
regarding copyright ownership.  The ASF licenses this file
to you under the Apache License, Version 2.0 (the
"License"); you may not use this file except in compliance
with the License.  You may obtain a copy of the License at

http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing,
software distributed under the License is distributed on an
"AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
KIND, either express or implied.  See the License for the
specific language governing permissions and limitations
under the License.
-->

# Migrating a project's site from APT to Markdown

`doxia-converter` does most of the mechanical work of turning `src/site/apt` into
`src/site/markdown`, but a handful of differences between the two formats are not mechanical
and will silently damage a page if they go unnoticed. This page collects them, together with
the procedure that catches them.

Nothing here needs a POM change: `maven-site-plugin` already ships `doxia-module-markdown`.

## The one rule that matters

**Build the site before the migration, build it again afterwards, and compare the generated
HTML.** Every problem described below was found that way and none of them is visible by
reading the converted Markdown. A page that looks right can be missing a whole heading.

## Converting

```
doxia-converter -in src/site/apt/index.apt.vm -from apt \
                -out src/site/markdown -to markdown -outEncoding UTF-8
```

The converter renames `x.apt` to `x.md` and `x.apt.vm` to `x.md.vm` by itself.

Pass `-outEncoding UTF-8` explicitly. Without it the output follows the autodetected input
encoding, and an ASCII source is detected as ISO-8859-1; an APT escaped space (`\ `, which
becomes U+00A0) is then written as a lone `0xa0` byte.

**Keep the YAML front matter the converter writes.** It carries the title, authors and date
from the APT header, and the skin renders them into `<title>` and `<meta>`. Delete it and the
parser falls back to the first heading for the title, so a page headed *Release Notes* under
project *Modello* silently retitles itself `Modello – Modello`. The front matter must be the
**first bytes of the file** — the parser only looks for it when the source starts with `---`,
so the licence header goes below it. RAT accepts either order.

## Velocity and Markdown disagree about `#`

A Markdown ATX heading below level one starts with `##`, which is also how a Velocity line
comment starts. In a `*.md.vm` file Velocity removes the heading before Doxia ever sees the
document: the line simply disappears from the rendered page, with no error and no warning.
Level one is safe, because a single `#` followed by a space is literal to Velocity.

There are three ways out, in order of preference.

**Drop the `.vm`.** Most `index.apt.vm` pages only use Velocity to interpolate
`${project.name}` into the title. Spell the title out, name the file `index.md`, and the
problem disappears along with the template.

**Use a setext underline for level two.** A title line followed by a line of `-` is a level
two heading and starts with no `#` at all:

```
Configuration
-------------
```

The title line needs a **blank line before it**. Without one it is read as a lazy
continuation of the preceding paragraph or list item and the heading is lost — a different
failure from the one being fixed, and just as quiet.

**Shield deeper headings.** There is no setext form below level two, so wrap the heading in
Velocity's unparsed block:

```
#[[### Advanced options]]#
```

Doxia 2.1.1 and later shield these automatically in the site renderer, but a page that must
build with an older Doxia has to do it itself.

## References the source meant to show, not resolve

APT writes `$\{project.version\}` when a page wants to display `${project.version}` rather
than interpolate it. The parser unescapes that, so the converted `*.vm` holds a live
reference and Velocity resolves it: a page documenting a default value ends up showing
something like `[org.apache.maven.model.ReportPlugin@c754401]`.

Write these as `${esc.d}{project.version}`. `${esc.d}` comes from Velocity's `EscapeTool`,
which the site renderer always provides, and always yields a literal `$`.

Do **not** use a backslash. `\${project.version}` only renders as `${project.version}` when
`project.version` actually resolves in the document's Velocity context; when it does not,
Velocity emits the backslash too and the page shows `\${project.version}`.

The converter warns about every reference that was literal in the source and is live in the
output, so watch its output rather than hunting for these by hand.

**On a source that was already `*.apt.vm`, read those warnings with suspicion.** The converter
compares what the APT parser saw against what Velocity will see, and it cannot tell a reference
the page wanted to *show* from one it wanted to *resolve* — in a `.vm` source the latter was
already live before the conversion and must stay live. Converting this project's own
`usage.apt.vm` produces

```
WARN "${project.version}" was written literally in the source but is a live Velocity
     reference in "usage.md.vm", so escape it there
```

for a reference that is deliberately live: it renders the current version into the sample
command lines, it did so in the APT source too, and escaping it as the warning suggests would
replace the version with the literal text `${project.version}` on the published page. Escape a
reference only when the *rendered* page is supposed to display it.

The same conversion warns

```
WARN Velocity directive "# java -jar target/doxia-converter-${project.version}-shaded.jar -h"
     was kept but the parser treated it as ordinary content, so check its placement
```

for a shell prompt inside a code block. A `#` followed by a space is literal to Velocity, so
these are safe; the warning asks you to check, and the check is that the line is prose or a
prompt rather than a directive.

## Do not tidy away a code block's info string

The converter writes an info string of `unknown` for a boxed APT block (`+-----+`):

````
```unknown
String in = "...";
```
````

It reads like a placeholder, and deleting it is the obvious tidy-up. **Do not.** The Markdown
parser treats *any non-empty* info string as the boxed flag, so removing it silently downgrades
the block:

```
with an info string:     <pre class="prettyprint linenums"><code class="language-java">
with none:               <pre><code class="nohighlight nocode">
```

The block still renders and the text is unchanged, so a body-only comparison passes and the
build stays green — only the box and the line numbers are gone.

Replace `unknown` with the language the block actually is, rather than deleting it. That keeps
the box and gets the syntax highlighting the placeholder never had:

````
```java
String in = "...";
```
````

## Things Markdown cannot express

Some APT constructs have no Markdown equivalent. None of them is a converter defect, but each
changes the rendered page, so decide deliberately:

* **Table captions.** APT puts a caption line after a table and Doxia renders `<caption>`.
  Markdown has no caption syntax; the text becomes a paragraph after the table.
* **Intraword emphasis.** The sink writes `_x_`, which CommonMark does not treat as emphasis
  when it follows a word character, so `Set<i>String</i>` renders as literal `Set_String_`.
* **Typographic substitution.** The Markdown module turns straight quotes into curly quotes
  and `...` into an ellipsis in prose. Code spans and code blocks are left alone.

## Differences that are improvements

Several APT constructs render incorrectly today and come out *better* after conversion. Do
not "fix" these back:

* `{{{#My Anchor}My Anchor}}` renders as `href="#My Anchor"` pointing at `id="My_Anchor"`, so
  the link never resolves. The converted Markdown links to `#My_Anchor`.
* `<<<${basedir}/src>>>` renders as `$<a id="basedir">basedir</a>/src` — APT reads the braces
  as an anchor definition and invents an anchor.
* `<<<\<overlay\>>>>` can end up with an anchor spliced into the middle of the code span.

## Checking the result

Comparing raw HTML is too noisy to be useful: `<b>` becomes `<strong>`, `<i>` becomes `<em>`,
attribute order changes, and `<code>` and `<a>` nest the other way round. Reduce each page to
the parts that carry meaning — its title, author and date metadata, its visible text and its
link targets — and compare that instead. A script that does so ships with this project as
`tools/normalize-site-page.py`:

```
diff <(tools/normalize-site-page.py before/index.html) \
     <(tools/normalize-site-page.py after/index.html)
```

**Compare the `<head>`, not just the body.** An APT header carries the document's title,
authors and date, and the skin turns them into `<title>` and `<meta name="author">` /
`<meta name="date">`. A normaliser that reduces a page to its rendered body will pass a page
that has lost all of it. This is the single easiest way to ship a migration that looks
perfect and is not: the body matches on every page while the metadata quietly disappears.

Two further mistakes produce a reassuring but meaningless result:

* **Check the exit status of `mvn site`.** A failed build leaves the previous output in
  `target/site`, so a comparison against it reports no difference at all.
* **Derive the page list from the built site, not from an inventory of the APT files.** Once
  the sources are converted that inventory is empty, and an empty list compares nothing.

## What not to convert

* Anything under `src/test` or `src/it`. Those APT files are fixtures for Doxia's own tests.
* Anything under `src/main/resources/archetype-resources`. That is template content generated
  into a user's new project, not the project's own documentation.

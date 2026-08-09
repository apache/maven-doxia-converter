#!/usr/bin/env python3
# Licensed to the Apache Software Foundation (ASF) under one
# or more contributor license agreements.  See the NOTICE file
# distributed with this work for additional information
# regarding copyright ownership.  The ASF licenses this file
# to you under the Apache License, Version 2.0 (the
# "License"); you may not use this file except in compliance
# with the License.  You may obtain a copy of the License at
#
#   http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing,
# software distributed under the License is distributed on an
# "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
# KIND, either express or implied.  See the License for the
# specific language governing permissions and limitations
# under the License.
"""Reduce a generated site page to the parts a migration must not change.

An APT page and the Markdown page it was converted from render the same document with
differences that carry no meaning: <b> against <strong>, attribute order, whether <code>
sits inside <a> or the other way round. Comparing raw HTML drowns a real regression in
those. This prints the visible text plus the link targets, one token per line, so that

    diff <(normalize-site-page.py before/index.html) <(normalize-site-page.py after/index.html)

shows only what a reader would notice.

usage: normalize-site-page.py <generated-html-file>
"""
import io
import re
import sys

html = io.open(sys.argv[1], encoding="utf-8", errors="replace").read()

# The document's metadata is part of what the migration must not change: the APT header
# carries the title, authors and date, and the skin turns them into <title> and <meta>.
# Comparing only the rendered body hides a page that has lost them, which is exactly how
# a migration once dropped the metadata of 387 pages without any comparison noticing.
meta = ["[TITLE:%s]" % t for t in re.findall(r"<title>([^<]*)</title>", html)]
meta += ["[META:%s=%s]" % (k, v) for k, v in
         re.findall(r'<meta name="(author|date)" content="([^"]*)"', html)]

# the skin wraps the document; only the rendered document itself is of interest
match = re.search(r"<main.*?</main>", html, re.DOTALL)
body = match.group(0) if match else html

body = re.sub(r"<!--.*?-->", "", body, flags=re.DOTALL)

# keep the structural facts a reader depends on, as visible tokens
body = re.sub(r'<a[^>]*href="([^"]*)"[^>]*>', r" [LINK:\1] ", body)
body = re.sub(r"<(h[1-6])[^>]*>", r" [\1] ", body)
body = re.sub(r"<pre[^>]*>", " [PRE] ", body)
body = re.sub(r"<t[hd](?=[ />])[^>]*>", " [CELL] ", body)  # not <thead>
body = re.sub(r"<li[^>]*>", " [ITEM] ", body)
body = re.sub(r"<[^>]+>", " ", body)

body = body.replace("&quot;", '"').replace("&apos;", "'").replace("&#x27;", "'")
# the Markdown module applies typographic substitution in prose; it is not a content change
body = body.replace("&#x2018;", "'").replace("&#x2019;", "'")
body = body.replace("&#x201c;", '"').replace("&#x201d;", '"')
body = body.replace("&#x2026;", "...").replace("…", "...")
body = body.replace("&lt;", "<").replace("&gt;", ">").replace("&amp;", "&")
body = re.sub(r"\s+", " ", body).strip()

sys.stdout.write("\n".join(meta + body.split(" ")))

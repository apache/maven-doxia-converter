/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.maven.doxia;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Hides Velocity constructs from a Doxia parser and restores them afterwards.
 *
 * <p>A {@code *.vm} source file is only valid Doxia markup <em>after</em> Velocity has processed it.
 * When such a file is converted, the parser sees the unprocessed source and mangles anything it does
 * not understand: {@code #set( $prefix = '.' )} ends up as escaped body text and a reference used as
 * a section title, such as {@code ${project.name}}, is read as an anchor definition because the APT
 * syntax gives {@code {...}} a meaning of its own.</p>
 *
 * <p>Replacing every Velocity construct with an opaque placeholder before parsing, and substituting
 * the original text back into the generated document afterwards, keeps those constructs intact and
 * in place.</p>
 */
class VelocityMasker {

    /**
     * Wraps a placeholder. Deliberately built from ASCII only: characters outside US-ASCII (such as
     * the Unicode private use area) do not survive the output encoding, and letters and digits are
     * never escaped by a sink.
     *
     * <p>It carries {@code ://} because a reference frequently opens a link destination, as in
     * <code>{{{$&#123;project.scm.url&#125;/src/main/README.md}README}}</code>. A parser decides
     * from the destination whether it has a link or an in-page anchor, and a placeholder of bare
     * letters looks like neither, so the destination would be mangled into an anchor.</p>
     */
    private static final String PLACEHOLDER_START = "velocitymask://";

    private static final String PLACEHOLDER_END = "/endmask";

    /** A block comment {@code #* ... *#}, which may span several lines. */
    private static final Pattern BLOCK_COMMENT = Pattern.compile("#\\*.*?\\*#", Pattern.DOTALL);

    /**
     * A whole line holding nothing but a directive or a line comment, such as {@code #if( $a )},
     * {@code #end} or {@code ## remark}. No APT, XDoc or FML construct starts a line with {@code #},
     * so the whole line can be taken verbatim.
     */
    private static final Pattern DIRECTIVE_LINE = Pattern.compile("^[ \\t]*#[^\\n]*", Pattern.MULTILINE);

    /** A formal reference, {@code ${...}} or the quiet form {@code $!{...}}. */
    private static final Pattern REFERENCE = Pattern.compile("\\$!?\\{[^{}\\n]*\\}");

    private final List<String> maskedValues = new ArrayList<>();

    private final List<String> maskedDirectives = new ArrayList<>();

    /**
     * @param content the raw source of a Velocity template
     * @return the same content with every Velocity construct replaced by a placeholder
     */
    String mask(String content) {
        String masked = mask(content, BLOCK_COMMENT);
        int directivesStart = maskedValues.size();
        masked = mask(masked, DIRECTIVE_LINE);
        maskedDirectives.addAll(maskedValues.subList(directivesStart, maskedValues.size()));
        return mask(masked, REFERENCE);
    }

    /**
     * A reference is masked in place and comes back where it was, but a directive occupying a whole
     * line is a block of its own to the parser, so the converted document may well put it somewhere
     * the template no longer works. Those need to be checked by hand.
     *
     * @return the directives and line comments which took up a whole line, in order of appearance
     */
    List<String> getMaskedDirectives() {
        return maskedDirectives;
    }

    private String mask(String content, Pattern pattern) {
        Matcher matcher = pattern.matcher(content);
        StringBuffer buffer = new StringBuffer(content.length());
        while (matcher.find()) {
            maskedValues.add(matcher.group());
            matcher.appendReplacement(buffer, Matcher.quoteReplacement(placeholder(maskedValues.size() - 1)));
        }
        matcher.appendTail(buffer);
        return buffer.toString();
    }

    /**
     * A parser may unescape markup which the source used to render a reference literally: APT
     * writes {@code $\{foo\}} to show {@code ${foo}} as text, and the APT parser hands the sink the
     * unescaped {@code ${foo}}. Written back into a {@code *.vm} file that is a live reference, and
     * Velocity resolves it. Comparing the references found in the converted document against the
     * ones taken out of the source spots exactly those.
     *
     * @param converted the converted document, after {@link #unmask(String)}
     * @return references that appear in the converted document but were not in the source
     */
    List<String> findNewReferences(String converted) {
        List<String> extra = new ArrayList<>();
        List<String> known = new ArrayList<>(maskedValues);
        Matcher matcher = REFERENCE.matcher(converted);
        while (matcher.find()) {
            if (!known.remove(matcher.group())) {
                extra.add(matcher.group());
            }
        }
        return extra;
    }

    /**
     * @param content a converted document still holding the placeholders produced by {@link #mask(String)}
     * @return the same content with the original Velocity constructs substituted back in
     */
    String unmask(String content) {
        String unmasked = content;
        // replace in reverse order so that a placeholder is never found inside restored text
        for (int i = maskedValues.size() - 1; i >= 0; i--) {
            unmasked = unmasked.replace(placeholder(i), maskedValues.get(i));
        }
        return unmasked;
    }

    private static String placeholder(int index) {
        // the trailing marker keeps a lower index from being a prefix of a higher one
        return PLACEHOLDER_START + index + PLACEHOLDER_END;
    }
}

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

import java.util.Arrays;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class VelocityMaskerTest {

    @Test
    void referencesAreHiddenAndRestored() {
        VelocityMasker masker = new VelocityMasker();
        String source = "Version ${project.version} of ${project.artifactId}.";

        String masked = masker.mask(source);

        assertFalse(masked.contains("${"), "references still visible to the parser: " + masked);
        assertEquals(source, masker.unmask(masked));
    }

    @Test
    void quietReferencesAreHiddenAndRestored() {
        VelocityMasker masker = new VelocityMasker();
        String source = "See $!{project.url} for details.";

        assertEquals(source, masker.unmask(masker.mask(source)));
    }

    @Test
    void indexesAboveNineAreRestoredCorrectly() {
        VelocityMasker masker = new VelocityMasker();
        StringBuilder source = new StringBuilder();
        for (int i = 0; i < 15; i++) {
            source.append("value ${property").append(i).append("} ");
        }

        assertEquals(source.toString(), masker.unmask(masker.mask(source.toString())));
    }

    @Test
    void directiveLinesAreHiddenRestoredAndReported() {
        VelocityMasker masker = new VelocityMasker();
        String source = "#set( $prefix = '.' )\n" + "Some text.\n" + "#parse( \"index.txt\" )\n";

        String masked = masker.mask(source);

        assertFalse(masked.contains("#set"), "directive still visible to the parser: " + masked);
        assertTrue(masked.contains("Some text."));
        assertEquals(source, masker.unmask(masked));
        assertEquals(Arrays.asList("#set( $prefix = '.' )", "#parse( \"index.txt\" )"), masker.getMaskedDirectives());
    }

    @Test
    void blockCommentsSpanningLinesAreHiddenAndRestored() {
        VelocityMasker masker = new VelocityMasker();
        String source = "before\n#*\n  a comment\n*#\nafter\n";

        String masked = masker.mask(source);

        assertFalse(masked.contains("a comment"), "block comment still visible to the parser: " + masked);
        assertEquals(source, masker.unmask(masked));
    }

    @Test
    void referencesTheSourceEscapedAreReportedAsNew() {
        VelocityMasker masker = new VelocityMasker();
        // the source shows ${project.version} literally and uses a real ${esc.d}
        String source = "literal $\\{project.version\\} and real ${esc.d}";

        masker.mask(source);

        // the parser unescaped the literal one, so the converted document holds two references
        String converted = "literal ${project.version} and real ${esc.d}";
        assertEquals(Arrays.asList("${project.version}"), masker.findNewReferences(converted));
    }

    @Test
    void referencesCarriedOverUnchangedAreNotReported() {
        VelocityMasker masker = new VelocityMasker();
        String source = "Version ${project.version} of ${project.artifactId}.";

        String converted = masker.unmask(masker.mask(source));

        assertTrue(masker.findNewReferences(converted).isEmpty());
    }

    @Test
    void plainTextIsLeftAlone() {
        VelocityMasker masker = new VelocityMasker();
        String source = "A price of $5, a shell variable $HOME and a C directive-looking #hashtag inline.";

        assertEquals(source, masker.mask(source));
        assertTrue(masker.getMaskedDirectives().isEmpty());
    }
}

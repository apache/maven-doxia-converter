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
package org.apache.maven.doxia.wrapper;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

class PreserveVelocityStatementsTest {

    @Test
    void testReferencePattern() {
        // variables
        assertTrue(PreserveVelocityStatements.REFERENCE_PATTERN.matcher("$foo").matches());
        assertTrue(PreserveVelocityStatements.REFERENCE_PATTERN
                .matcher("$mudSlinger")
                .matches());
        assertTrue(PreserveVelocityStatements.REFERENCE_PATTERN
                .matcher("$mud_slinger")
                .matches());
        assertTrue(PreserveVelocityStatements.REFERENCE_PATTERN
                .matcher("$mudSlinger1")
                .matches());
        // properties
        assertTrue(PreserveVelocityStatements.REFERENCE_PATTERN
                .matcher("$customer.Address")
                .matches());
        assertTrue(PreserveVelocityStatements.REFERENCE_PATTERN
                .matcher("$purchase.Total")
                .matches());
        // methods
        assertTrue(PreserveVelocityStatements.REFERENCE_PATTERN
                .matcher("$customer.getAddress()")
                .matches());
        assertTrue(PreserveVelocityStatements.REFERENCE_PATTERN
                .matcher("$purchase.getTotal()")
                .matches());
        assertTrue(PreserveVelocityStatements.REFERENCE_PATTERN
                .matcher("$page.setTitle( \"My Home Page\" )")
                .matches());
        assertTrue(PreserveVelocityStatements.REFERENCE_PATTERN
                .matcher("$person.setAttributes( [\"Strange\", \"Weird\", \"Excited\"] )")
                .matches());
        // https://velocity.apache.org/engine/devel/user-guide.html#index-notation

        // https://velocity.apache.org/engine/devel/user-guide.html#formal-reference-notation

        // https://velocity.apache.org/engine/devel/user-guide.html#alternate-values

        // https://velocity.apache.org/engine/devel/user-guide.html#quiet-reference-notation
    }
}

/*-
 * ========================LICENSE_START=================================
 * flyway-core
 * ========================================================================
 * Copyright (C) 2010 - 2025 Red Gate Software Ltd
 * ========================================================================
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * =========================LICENSE_END==================================
 */
package org.flywaydb.core.internal.parser;

public class StatementType {
    public static final StatementType GENERIC = new StatementType();
    public static final StatementType UNKNOWN = new StatementType();

    /**
     * Whether the character should be treated as if it is a letter; this allows statement types to handle
     * characters that appear in specific contexts
     *
     * @param c
     * @return
     */
    public boolean treatAsIfLetter(char c) {
        return false;
    }
}

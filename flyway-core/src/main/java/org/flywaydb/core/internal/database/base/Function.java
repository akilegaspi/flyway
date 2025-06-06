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
package org.flywaydb.core.internal.database.base;

import org.flywaydb.core.internal.jdbc.JdbcTemplate;
import org.flywaydb.core.internal.util.StringUtils;

public abstract class Function<D extends Database, S extends Schema> extends SchemaObject<D, S> {
    protected String[] args;

    public Function(JdbcTemplate jdbcTemplate, D database, S schema, String name, String... args) {
        super(jdbcTemplate, database, schema, name);
        this.args = args == null ? new String[0] : args;
    }

    @Override
    public String toString() {
        return super.toString() + "(" + StringUtils.arrayToCommaDelimitedString(args) + ")";
    }
}

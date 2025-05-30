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
package org.flywaydb.core.internal.database;

import org.flywaydb.core.internal.util.SqlCallable;

import java.sql.SQLException;

/**
 * Defines a strategy for executing a {@code SqlCallable} against a particular database.
 */
public interface DatabaseExecutionStrategy {

    /**
     * Execute the given callable using the defined strategy.
     *
     * @param callable The SQL callable to execute.
     * @param <T> The return type of the SQL callable.
     * @return The object returned by the SQL callable.
     */
    <T> T execute(final SqlCallable<T> callable) throws SQLException;
}

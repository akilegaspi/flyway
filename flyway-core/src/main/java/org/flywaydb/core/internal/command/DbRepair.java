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
package org.flywaydb.core.internal.command;

import lombok.CustomLog;
import org.flywaydb.core.api.FlywayException;
import org.flywaydb.core.api.MigrationInfo;
import org.flywaydb.core.api.MigrationState;
import org.flywaydb.core.api.MigrationVersion;
import org.flywaydb.core.api.callback.Event;
import org.flywaydb.core.api.configuration.Configuration;
import org.flywaydb.core.api.output.CommandResultFactory;
import org.flywaydb.core.api.output.RepairResult;
import org.flywaydb.core.api.resolver.ResolvedMigration;
import org.flywaydb.core.extensibility.AppliedMigration;
import org.flywaydb.core.internal.callback.CallbackExecutor;
import org.flywaydb.core.internal.database.base.Connection;
import org.flywaydb.core.internal.database.base.Database;
import org.flywaydb.core.internal.info.MigrationInfoImpl;
import org.flywaydb.core.internal.info.MigrationInfoServiceImpl;
import org.flywaydb.core.internal.jdbc.ExecutionTemplateFactory;
import org.flywaydb.core.internal.resolver.CompositeMigrationResolver;
import org.flywaydb.core.internal.schemahistory.SchemaHistory;
import org.flywaydb.core.internal.util.StopWatch;
import org.flywaydb.core.internal.util.TimeFormat;
import org.flywaydb.core.internal.util.ValidatePatternUtils;

import java.util.Arrays;
import java.util.Objects;
import java.util.concurrent.Callable;

/**
 * Handles Flyway's repair command.
 */
@CustomLog
public class DbRepair {
    /**
     * The database connection to use for accessing the schema history table.
     */
    private final Connection connection;

    /**
     * The migration infos.
     */
    private final MigrationInfoServiceImpl migrationInfoService;

    /**
     * The schema history table.
     */
    private final SchemaHistory schemaHistory;

    /**
     * The callback executor.
     */
    private final CallbackExecutor callbackExecutor;

    /**
     * The database-specific support.
     */
    private final Database database;

    /**
     * The POJO containing the repair result.
     */
    private RepairResult repairResult;

    /**
     * The Flyway configuration.
     */
    private final Configuration configuration;

    /**
     * Creates a new DbRepair.
     *
     * @param database The database-specific support.
     * @param migrationResolver The migration resolver.
     * @param schemaHistory The schema history table.
     * @param callbackExecutor The callback executor.
     */
    public DbRepair(Database database, CompositeMigrationResolver migrationResolver, SchemaHistory schemaHistory,
                    CallbackExecutor callbackExecutor, Configuration configuration) {
        this.database = database;
        this.connection = database.getMainConnection();
        this.schemaHistory = schemaHistory;
        this.callbackExecutor = callbackExecutor;
        this.configuration = configuration;

        this.migrationInfoService = new MigrationInfoServiceImpl(migrationResolver, schemaHistory, database, configuration,
                                                                 MigrationVersion.LATEST, true, ValidatePatternUtils.getIgnoreAllPattern(), configuration.getCherryPick());

        this.repairResult = CommandResultFactory.createRepairResult(database.getCatalog());
    }

    /**
     * Repairs the schema history table.
     */
    public RepairResult repair() {
        callbackExecutor.onEvent(Event.BEFORE_REPAIR);

        CompletedRepairActions repairActions;
        try {
            StopWatch stopWatch = new StopWatch();
            stopWatch.start();

            repairActions = ExecutionTemplateFactory.createExecutionTemplate(connection.getJdbcConnection(), database).execute(new Callable<CompletedRepairActions>() {
                public CompletedRepairActions call() {
                    CompletedRepairActions completedActions = new CompletedRepairActions();

                    completedActions.removedFailedMigrations = schemaHistory.removeFailedMigrations(repairResult, configuration.getCherryPick());
                    migrationInfoService.refresh();

                    completedActions.deletedMissingMigrations = deleteMissingMigrations();

                    completedActions.alignedAppliedMigrationChecksums = alignAppliedMigrationsWithResolvedMigrations();
                    return completedActions;
                }
            });

            stopWatch.stop();

            LOG.info("Successfully repaired schema history table " + schemaHistory + " (execution time "
                             + TimeFormat.format(stopWatch.getTotalTimeMillis()) + ").");
            if (repairActions.deletedMissingMigrations) {
                LOG.info("Please ensure the previous contents of the deleted migrations are removed from the database, or moved into an existing migration.");
            }
            if (repairActions.removedFailedMigrations && !database.supportsDdlTransactions()) {
                LOG.info("Manual cleanup of the remaining effects of the failed migration may still be required.");
            }
        } catch (FlywayException e) {
            callbackExecutor.onEvent(Event.AFTER_REPAIR_ERROR);
            throw e;
        }

        callbackExecutor.onEvent(Event.AFTER_REPAIR);

        repairResult.setRepairActions(repairActions);
        return repairResult;
    }

    private boolean deleteMissingMigrations() {
        boolean removed = false;
        for (MigrationInfo migrationInfo : migrationInfoService.all()) {
            MigrationInfoImpl migrationInfoImpl = (MigrationInfoImpl) migrationInfo;

            if (migrationInfo.getType().isSynthetic()
                    || migrationInfo.getType().isUndo()
            ) {
                continue;
            }

            AppliedMigration applied = migrationInfoImpl.getAppliedMigration();
            MigrationState state = migrationInfoImpl.getState();
            boolean isMigrationMissing = state == MigrationState.MISSING_SUCCESS || state == MigrationState.MISSING_FAILED || state == MigrationState.FUTURE_SUCCESS || state == MigrationState.FUTURE_FAILED;
            boolean isMigrationIgnored = Arrays.stream(configuration.getIgnoreMigrationPatterns()).anyMatch(p -> p.matchesMigration(migrationInfoImpl.getVersion() != null, state));
            if (isMigrationMissing && !isMigrationIgnored) {
                schemaHistory.delete(applied);
                removed = true;
                repairResult.migrationsDeleted.add(CommandResultFactory.createRepairOutput(migrationInfo));
            }
        }

        return removed;
    }

    private boolean alignAppliedMigrationsWithResolvedMigrations() {
        boolean repaired = false;
        for (MigrationInfo migrationInfo : migrationInfoService.all()) {
            MigrationInfoImpl migrationInfoImpl = (MigrationInfoImpl) migrationInfo;

            ResolvedMigration resolved = migrationInfoImpl.getResolvedMigration();
            AppliedMigration applied = migrationInfoImpl.getAppliedMigration();

            // Repair versioned
            if (resolved != null
                    && resolved.getVersion() != null
                    && applied != null
                    && !applied.getType().isSynthetic()
                    && migrationInfoImpl.getState() != MigrationState.UNDONE
                    && migrationInfoImpl.getState() != MigrationState.IGNORED
                    && updateNeeded(resolved, applied)) {
                schemaHistory.update(applied, resolved);
                repaired = true;
                repairResult.migrationsAligned.add(CommandResultFactory.createRepairOutput(migrationInfo));
            }

            // Repair repeatable
            if (resolved != null
                    && resolved.getVersion() == null
                    && applied != null
                    && !applied.getType().isSynthetic()
                    && migrationInfoImpl.getState() != MigrationState.UNDONE
                    && migrationInfoImpl.getState() != MigrationState.IGNORED
                    && resolved.checksumMatchesWithoutBeingIdentical(applied.getChecksum())) {
                schemaHistory.update(applied, resolved);
                repaired = true;
                repairResult.migrationsAligned.add(CommandResultFactory.createRepairOutput(migrationInfo));
            }
        }

        return repaired;
    }

    private boolean updateNeeded(ResolvedMigration resolved, AppliedMigration applied) {
        return checksumUpdateNeeded(resolved, applied)
                || descriptionUpdateNeeded(resolved, applied)
                || typeUpdateNeeded(resolved, applied);
    }

    private boolean checksumUpdateNeeded(ResolvedMigration resolved, AppliedMigration applied) {
        return !resolved.checksumMatches(applied.getChecksum());
    }

    private boolean descriptionUpdateNeeded(ResolvedMigration resolved, AppliedMigration applied) {
        if (!database.supportsEmptyMigrationDescription() && "".equals(resolved.getDescription())) {
            return !Objects.equals(SchemaHistory.NO_DESCRIPTION_MARKER, applied.getDescription());
        }
        return !Objects.equals(resolved.getDescription(), applied.getDescription());
    }

    private boolean typeUpdateNeeded(ResolvedMigration resolved, AppliedMigration applied) {
        return !Objects.equals(resolved.getType(), applied.getType());
    }

    public static class CompletedRepairActions {
        public boolean removedFailedMigrations = false;
        public boolean deletedMissingMigrations = false;
        public boolean alignedAppliedMigrationChecksums = false;

        public String removedMessage() {
            return "Removed failed migrations";
        }

        public String deletedMessage() {
            return "Marked missing migrations as deleted";
        }

        public String alignedMessage() {
            return "Aligned applied migration checksums";
        }
    }
}

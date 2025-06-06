---
subtitle: flyway.undoSqlMigrationPrefix
redirect_from: Configuration/undoSqlMigrationPrefix/
---

{% include teams.html %}

## Description

The file name prefix for undo SQL migrations.

Undo SQL migrations are responsible for undoing the effects of the versioned migration with the same version.

They have the following file name structure: prefixVERSIONseparatorDESCRIPTIONsuffix, which using the defaults translates to U1.1__My_description.sql

## Type

String

## Default

`"U"`

## Usage

### Flyway Desktop

This can't be configured via Flyway Desktop, although it will be honoured.

### Command-line

```powershell
./flyway -undoSqlMigrationPrefix="B" info
```

### TOML Configuration File

```toml
[flyway]
undoSqlMigrationPrefix = "B"
```

### Configuration File

```properties
flyway.undoSqlMigrationPrefix=B
```

### Environment Variable

```properties
FLYWAY_UNDO_SQL_MIGRATION_PREFIX=B
```

### API

```java
UndoConfigurationExtension undoConfigurationExtension = flyway.getConfiguration().getPluginRegister().getPlugin(UndoConfigurationExtension.class);
undoConfigurationExtension.setUndoSqlMigrationPrefix("B");
```

### Gradle

```groovy
flyway {
    pluginConfiguration = [
        undoSqlMigrationPrefix = 'B'
    ]
}
```

### Maven

```xml
<configuration>
    <pluginConfiguration>
        <undoSqlMigrationPrefix>B</undoSqlMigrationPrefix>
    </pluginConfiguration>
</configuration>
```

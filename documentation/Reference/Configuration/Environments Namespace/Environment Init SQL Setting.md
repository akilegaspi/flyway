---
subtitle: flyway.environments.*.initSql
redirect_from: Configuration/initSql/
---

## Description

SQL statements to be run immediately after a database connection has been established.

This is mainly used for setting some state on the connection that needs to be shared across all scripts, or should not be included into any one script.

This could effectively be considered an environment specific afterConnect [callback](<Callback Events>).

Please note that this parameter defines an "Initial SQL command," not an "Initialization SQL command." It may be executed multiple times, as it runs immediately after each database connection is established.

## Type

String

## Default

<i>none</i>

## Usage

### Flyway Desktop

This can't be set in a config file via Flyway Desktop, although it will be honoured, and it can be configured as an advanced parameter in operations on the Migrations page.

### Command-line

```powershell
./flyway -initSql="ALTER SESSION SET NLS_LANGUAGE='ENGLISH';" info
```

To configure a named environment via command line when using a TOML configuration, prefix `initSql` with
`environments.{environment name}.` for example:

```powershell
./flyway "-environments.sample.initSql=ALTER SESSION SET NLS_LANGUAGE='ENGLISH';" info
```

### TOML Configuration File

```toml
[environments.default]
initSql = "ALTER SESSION SET NLS_LANGUAGE='ENGLISH';"
```

### Configuration File

```properties
flyway.initSql=ALTER SESSION SET NLS_LANGUAGE='ENGLISH';
```

### Environment Variable

```properties
FLYWAY_INIT_SQL=ALTER SESSION SET NLS_LANGUAGE='ENGLISH';
```

### API

```java
Flyway.configure()
    .initSql("ALTER SESSION SET NLS_LANGUAGE='ENGLISH';")
    .load()
```

### Gradle

```groovy
flyway {
    initSql = "ALTER SESSION SET NLS_LANGUAGE='ENGLISH';"
}
```

### Maven

```xml
<configuration>
    <initSql>ALTER SESSION SET NLS_LANGUAGE='ENGLISH';</initSql>
</configuration>
```

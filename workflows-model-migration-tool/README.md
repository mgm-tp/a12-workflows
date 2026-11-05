# Workflows Model Migration Tool

## Using the Workflows Model Migration tool

**Running**

Start the tool by executing following command on the `workflows-model-migration-tool--<VERSION>-jar-with-dependencies.jar`:
```
java -jar workflows-model-migration-tool--<VERSION>-jar-with-dependencies.jar
```
It is necessary to specify .bpmn file to migrate or a folder which includes .bpmn files, and also provide at least the
current model version you are using. Read the instructions below for further explanations.

**Options**

There are a couple of options to use with the migration tool:

- `-b` or `--backup`: Creates a backup (.bak file) before migrating a model
- `-d` or `--debug` : Enables debug mode, which prints migration infos of steps
- `-h` or `--help`: Prints the command line options 
- `-n` or `--no-validation`: Skips validation of the model against schema definitions after migration (not recommended)
- `-o` or `--optional`: Enables optional migration steps, enable by listing the names of the steps
- `-t` or `--test`: Prints migrated model to stdout instead of to a file
- `-smv` or `--source-model-version`: Version to start migration from [mandatory]
- `-tmv` or `--target-model-version`: Version until migration shall be run including the specified version. [optional]

**Example**

The following will migrate `ParallelGatewayProcess.bpmn` which is located in the same directory as the .jar file, creates a backup
and performs migration from model version 8.1.0 until the latest model version available.

```
java -jar workflows-model-migration-tool-9.0.0-jar-with-dependencies.jar ParallelGatewayProcess.bpmn -b -smv 8.1.0
```

You can also migrate multiple bpmn files, in this case it's only necessary to provide the path to a folder containing .bpmn files.

```
java -jar workflows-model-migration-tool-9.0.0-jar-with-dependencies.jar your/bpmn/file/path -b -smv 8.1.0
```

## Building 

**Build the project**
```
gradle build
```

Includes the `generateBeans` task.

**Generating config beans**

Creates the configuration beans in package `com.mgmtp.a12.model.workflows.migration.config.types` based on xml-schema file
`resources/xml/schema/MigratorConfig.xsd` and `resources/xml/schema/MigratorBindings.xjb`. They are generated into
`build/generated/sources/xjc/java/main`, which is a main source directory, so every compilation generates them first and they
are not checked in. In a fresh clone, run it once so that the IDE resolves the bean classes.
```
gradle generateBeans
```

## Maintenance

**Order of general migration process**

1. `WorkflowsMigratorCLI#main` is called with model path and options.
2. CLI options are checked for validity and completeness.
3. Check if path is a directory or concrete file, either `WorkflowsMigratorCLI#migrateDirectoryRecursively` or `WorkflowsMigratorCLI#migrateSingleModel` is called.
4. Options are parsed e.g. backups created or optional steps are built.
5. New `BPMNMigratorBuilder` is instantiated.
6. Start the actual migration in `WorkflowsMigrator#migrate` and instantiate a `MigrationStepExecutor` with all necessary configurations.
7. Current model is validated against all validation checks in `BPMNModelValidator`.
8. Migration steps are executed which are registered in `resources/xml/config/BPMNMigratorConfig.xml` for the specified versions.
9. After migration another (skippable) validation against schema definitions in `resources/bpmn/schema` is performed.
10. `MigrationResult` is evaluated, and optional migration infos or migration errors in case of unsuccessful migration are printed.

The `resources/bpmn/data` directory contains models for testing during development.

**Adding a new MigrationStep**

In order to extend the migration by new steps, following actions have to be performed:

1. Create a new `MigrationStep`-class in `src/main/java/com/mgmtp/a12/model/workflows/migration/bpmn/steps` which extends `BaseMigrationStep`.
2. Include the changes to the bpmn models which should be performed, as a reference check out the existing migration steps.
3. Edit `src/main/resources/xml/config/BPMNMigratorConfig.xml` and add the new migration step with `sourceModelVersion` and `targetModelVersion` and specify the `MigrationStep` bean to be used.
It's necessary that the `sourceModelVersion` matches the `targetModelVersion` of previous `MigrationStep`.
4. Validation can be extended by modifying `src/main/java/com/mgmtp/a12/model/workflows/migration/bpmn/validation/BPMNModelValidator.java`, by adding a new check, just like the `BPMNModelValidator#checkFormKeyExists`.

**Whitelisting SAX errors**

As JDOM implementation, and in order to parse documents, `SAXBuilder` is used.
The validator implementation class `src/main/java/com/mgmtp/a12/model/workflows/migration/validation/JDomSchemaValidator.java` offers the usage of
quirks, which can be defined in `resources/validation/quirks.regex`. This allows for overriding the default SAX ErrorHandler by whitelisting specific
errors (quirks), which should not throw an exception during validation.

Per default no quirks are registered, and the default SAX ErrorHandler is used.

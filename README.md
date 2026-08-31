<picture>
  <source media="(prefers-color-scheme: dark)" srcset="https://www.mgm-tp.com/global-content/cd/logos/a12/app-icons/dark/A12-Dark.svg" />
  <img src="https://www.mgm-tp.com/global-content/cd/logos/a12/app-icons/light/A12-Light.svg" height="200" alt="A12 logo" />
</picture>


# A12 Workflows

A12 Workflows is a component to facilitate process modeling and execution within the A12 platform. It provides a Dataservices
extension, a BPMN modeler with custom templates, and a process engine integration, currently using the CIB 7 open source process engine.

Refer to https://geta12.com/#/docs to get started with A12 development

---

## License
Parts of the A12 platform are made available under a **dual license**.  
Please check the [LICENSE](./LICENSE) file for details.

---

## Getting Started

### How to Use It
#### Import & Install

##### Libraries
For the A12 Workflows backend libraries, you can include the artifacts via the dependency inclusion mechanism of your build tool. The coordinates for
each artifact are of the form `com.mgmtp.a12.workflows:<artifact>:<version>`.

##### Workflows Core

- To use the workflows-core frontend library, import the artifact `@com.mgmtp.a12.workflows/workflows-core` into your
 A12 frontend via the dependency inclusion mechanism of your build tool.
- Follow further instructions in the [Workflows Core documentation](https://geta12.com/#/docs/2026.06/ext2/workflows/dev-docs%23_workflows_core) to set up the frontend integration.

##### Workflows Dataservices Extension
- To use the Dataservices extension, import the artifact `com.mgmtp.a12.workflows:workflows-extension:<version>` into your
 A12 Dataservices instance via the dependency inclusion mechanism of your build tool.

##### Workflows Codemod Recipes
- To apply the provided codemod recipes, follow the instructions in the [Hypermod documentation](https://www.hypermod.io/docs/local-development/running-codemods)
and run the recipes as necessary from the `@com.mgmtp.a12.workflows/workflows-codemod` artifact.

##### Workflows OpenRewrite Recipes
- To apply the provided OpenRewrite recipes, follow the instructions in the [OpenRewrite documentation](https://docs.openrewrite.org/running-recipes)
and run the recipes as necessary from the `com.mgmtp.a12.workflows:workflows-rewrite:<version>` artifact.


---

### How to Build and Run

#### Prerequisites (tools and their versions)

| Tool           | Version    | Note                                                       |
|----------------|------------|------------------------------------------------------------|
| Adoptium JDK   | `^25`      | For compiling and running you should use JAVA 21 or higher |
| Gradle         | `^9`       | Gradle version might be latest available                   |
| Node           | `^22.10.0` |                                                            |
| npm            | `^10.9.0`  |                                                            |

#### How to Build

Build the project using Gradle:

```shell
./gradlew assemble
```

#### How to Test

```shell
./gradlew check
```

#### How to Run

Start the Workflows process engine:
```shell
./gradlew :workflows-engine:bootRun
```

To run the Workflows Dataservices extension, follow the instructions to include the extension in your A12 Dataservices instance and start your Dataservices instance.



#### How to Access It

Once the Workflows process engine is running, you can access it at `http://localhost:8088`.

---

### Documentation
- Full technical documentation is available at [GetA12.com](https://GetA12.com).
- The website also provides access to the **A12 Discourse Community Forum**.

---

**The mgm A12 Team**

[mgm technology partners GmbH](https://www.mgm-tp.com) • [Imprint](https://www.mgm-tp.com/imprint.html)
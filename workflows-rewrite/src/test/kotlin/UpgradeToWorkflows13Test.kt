/**
 * (c) copyright 2012-2026 mgm technology partners GmbH.
 * This software, the underlying source code and other artifacts are protected by copyright.
 * All rights, in particular the right to use, reproduce, publish and edit are reserved.
 * A simple right of use (license) can be acquired for use, duplication, publication, editing etc..
 * Requests for this can be made at A12-license@mgm-tp.com or other official channels of the copyright holder.
 */
package com.mgmtp.a12.workflows.rewrite

import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.openrewrite.java.Assertions.java
import org.openrewrite.properties.Assertions.properties
import org.openrewrite.test.RecipeSpec
import org.openrewrite.test.RewriteTest
import org.openrewrite.test.SourceSpecs
import org.openrewrite.test.TypeValidation
import org.openrewrite.yaml.Assertions.yaml

private class UpgradeToWorkflows13Test : RewriteTest {
    @Override
    override fun defaults(spec: RecipeSpec) {
        spec.typeValidationOptions(TypeValidation.none())
        spec.recipeFromResources(RECIPE_NAME)
    }

    companion object {
        const val RECIPE_NAME = "com.mgmtp.a12.workflows.UpgradeWorkflows_13_0_0"
        const val KOTLIN_FILE_NAME = "src/main/kotlin/Some.kt"
        const val TEXT_FILE_NAME = "src/main/resources/some.txt"
    }

    @Nested
    @DisplayName("Renaming ProcessVariableBeforeDeserializationEvent...")
    inner class RenameDeserializationEvent {
        @Test
        fun `should rename event references in java file`() {
            // given
            val before = """
                        package com.example;
                        import com.mgmtp.a12.workflows.engine.internal.serialization.ProcessVariableBeforeDeserializationEvent;

                        public class MyClass {
                            public void method(ProcessVariableBeforeDeserializationEvent event) {
                                // handle event
                            }
                        }
                     """
            val after = """
                        package com.example;
                        import com.mgmtp.a12.workflows.engine.internal.serialization.ProcessVariableAfterDeserializationEvent;

                        public class MyClass {
                            public void method(ProcessVariableAfterDeserializationEvent event) {
                                // handle event
                            }
                        }
                     """
            // when/then
            rewriteRun(java(before, after))
        }

        @Test
        fun `should rename event references in kotlin file`() {
            // given
            val before = """
                        package com.example
                        import com.mgmtp.a12.workflows.engine.internal.serialization.ProcessVariableBeforeDeserializationEvent

                        class MyClass {
                            fun method(event: ProcessVariableBeforeDeserializationEvent) {
                                // handle event
                            }
                        }
                     """
            val after = """
                        package com.example
                        import com.mgmtp.a12.workflows.engine.internal.serialization.ProcessVariableAfterDeserializationEvent

                        class MyClass {
                            fun method(event: ProcessVariableAfterDeserializationEvent) {
                                // handle event
                            }
                        }
                     """
            // when/then
            rewriteRun(SourceSpecs.text(before, after) { s -> s.path(KOTLIN_FILE_NAME) })
        }

        @Test
        fun `should not rename event references in non java or kotlin files`() {
            // given
            val before = """
                        ProcessVariableBeforeDeserializationEvent
                     """
            // when/then
            rewriteRun(SourceSpecs.text(before) { s -> s.path(TEXT_FILE_NAME) })
        }
    }

    @Nested
    @DisplayName("Migrating automatic synchronization property...")
    inner class AutomaticSynchronizationProperty {
        @Test
        fun `should move automaticSynchronization property in properties file`() {
            // given
            val before = """
                        mgmtp.a12.workflows.automaticSynchronization.enabled=true
                     """
            val after = """
                        mgmtp.a12.workflows.extension.automaticSynchronization.enabled=true
                    """
            // when/then
            rewriteRun(properties(before, after))
        }

        @Test
        fun `should move automaticSynchronization property in yaml file`() {
            // given
            val before = """
                        mgmtp.a12.workflows.automaticSynchronization.enabled: true
                     """
            val after = """
                        mgmtp.a12.workflows.extension.automaticSynchronization.enabled: true
                    """
            // when/then
            rewriteRun(yaml(before, after))
        }

        @Test
        fun `should move automaticSynchronization property in nested yaml structure`() {
            // given
            val before = """
                        mgmtp:
                          a12:
                            workflows:
                              automaticSynchronization:
                                enabled: true
                     """
            val after = """
                        mgmtp:
                          a12:
                            workflows:
                                extension.automaticSynchronization.enabled: true
                    """
            // when/then
            rewriteRun(yaml(before, after))
        }

        @Test
        fun `should move automaticSynchronization property in nested yaml structure with sibling branches`() {
            // given
            val before = """
                        mgmtp:
                          a12:
                            workflows:
                              extension:
                                something: false
                              automaticSynchronization:
                                enabled: true
                            dataservices:
                              query:
                                mode: EXCLUSIVE
                     """
            val after = """
                        mgmtp:
                          a12:
                            workflows:
                                extension:
                                  something: false
                                extension.automaticSynchronization.enabled: true
                            dataservices:
                              query:
                                mode: EXCLUSIVE
                    """
            // when/then
            rewriteRun(yaml(before, after))
        }
    }
}

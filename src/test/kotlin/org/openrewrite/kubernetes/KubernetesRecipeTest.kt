/*
 * Copyright 2021 the original author or authors.
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * https://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.openrewrite.kubernetes

import org.intellij.lang.annotations.Language
import org.openrewrite.*
import org.openrewrite.marker.SearchResult
import java.io.File

interface KubernetesRecipeTest : RecipeTest<Kubernetes> {
    override val parser: KubernetesParser
        get() = KubernetesParser.builder()
            .build()

    override val treePrinter: TreePrinter<*>?
        get() = SearchResult.printer("~~>", "~~(%s)~~>")

    fun assertChanged(
        parser: KubernetesParser = this.parser,
        recipe: Recipe = this.recipe!!,
        @Language("yml") before: String,
        @Language("yml") dependsOn: Array<String> = emptyArray(),
        @Language("yml") after: String,
        cycles: Int = 2,
        expectedCyclesThatMakeChanges: Int = cycles - 1,
        afterConditions: (Kubernetes) -> Unit = { }
    ) {
        super.assertChangedBase(parser, recipe, before, dependsOn, after, cycles, expectedCyclesThatMakeChanges, afterConditions)
    }

    fun assertChanged(
        parser: KubernetesParser = this.parser,
        recipe: Recipe = this.recipe!!,
        @Language("yml") before: File,
        @Language("yml") dependsOn: Array<File> = emptyArray(),
        @Language("yml") after: String,
        cycles: Int = 2,
        expectedCyclesThatMakeChanges: Int = cycles - 1,
        afterConditions: (Kubernetes) -> Unit = { }
    ) {
        super.assertChangedBase(parser, recipe, before, dependsOn, after, cycles, expectedCyclesThatMakeChanges, afterConditions)
    }

    fun assertUnchanged(
        parser: KubernetesParser = this.parser,
        recipe: Recipe = this.recipe!!,
        @Language("yml") before: String,
        @Language("yml") dependsOn: Array<String> = emptyArray()
    ) {
        super.assertUnchangedBase(parser, recipe, before, dependsOn)
    }

    fun assertUnchanged(
        parser: KubernetesParser = this.parser,
        recipe: Recipe = this.recipe!!,
        @Language("yml") before: File,
        @Language("yml") dependsOn: Array<File> = emptyArray()
    ) {
        super.assertUnchangedBase(parser, recipe, before, dependsOn)
    }
}

package org.openrewrite.kubernetes

import org.intellij.lang.annotations.Language
import org.openrewrite.*
import org.openrewrite.marker.SearchResult

interface KubernetesRecipeTest : RecipeTest {
    override val parser: Parser<*>?
        get() = KubernetesParser.builder()
            .build()

    override val treePrinter: TreePrinter<*>?
        get() = SearchResult.printer("~~>", "~~(%s)~~>")

    fun assertChanged(
        @Language("yaml") before: String,
        @Language("yaml") dependsOn: Array<String>,
        @Language("yaml") after: String,
    ) {
        super.assertChanged(parser, recipe, before, dependsOn, after, 1) {}
    }

    fun assertChanged(
        @Language("yaml") before: String,
        @Language("yaml") after: String,
    ) {
        super.assertChanged(parser, recipe, before, emptyArray(), after, 1) {}
    }

    fun assertChanged(
        parser: Parser<*>?,
        @Language("yaml") before: String,
        @Language("yaml") dependsOn: Array<String>,
        @Language("yaml") after: String,
        cycles: Int
    ) {
        super.assertChanged(parser, recipe, before, dependsOn, after, cycles) {}
    }

    fun assertChanged(
        recipe: Recipe?,
        @Language("yaml") before: String,
        @Language("yaml") dependsOn: Array<String>,
        @Language("yaml") after: String,
        cycles: Int
    ) {
        super.assertChanged(parser, recipe, before, dependsOn, after, cycles) {}
    }

    fun assertChanged(
        @Language("yaml") before: String,
        @Language("yaml") dependsOn: Array<String>,
        @Language("yaml") after: String,
        cycles: Int
    ) {
        super.assertChanged(parser, recipe, before, dependsOn, after, cycles) {}
    }

    fun assertChanged(
        recipe: Recipe?,
        @Language("yaml") before: String,
        @Language("yaml") after: String,
        cycles: Int,
    ) {
        super.assertChanged(parser, recipe, before, emptyArray(), after, cycles) {}
    }

    fun <T : SourceFile> assertChanged(
        parser: Parser<T>?,
        recipe: Recipe?,
        @Language("yaml") before: String,
        @Language("yaml") after: String,
        cycles: Int,
    ) {
        super.assertChanged(parser, recipe, before, emptyArray(), after, cycles) {}
    }
    
    override fun assertChanged(
        parser: Parser<*>?,
        recipe: Recipe?,
        @Language("yaml") before: String,
        @Language("yaml") dependsOn: Array<String>,
        @Language("yaml") after: String,
        cycles: Int
    ) {
        super.assertChanged(parser, recipe, before, dependsOn, after, cycles) {}
    }

    override fun <T : SourceFile> assertChanged(
        parser: Parser<T>?,
        recipe: Recipe?,
        @Language("yaml") before: String,
        @Language("yaml") dependsOn: Array<String>,
        @Language("yaml") after: String,
        cycles: Int,
        afterConditions: (T) -> Unit
    ) {
        super.assertChanged(parser, recipe, before, dependsOn, after, cycles, afterConditions)
    }

    fun assertUnchanged(
        parser: Parser<*>?,
        recipe: Recipe?,
        @Language("yaml") before: String,
    ) {
        super.assertUnchanged(parser, recipe, before, emptyArray())
    }

    fun assertUnchanged(
        recipe: Recipe?,
        @Language("yaml") before: String,
    ) {
        super.assertUnchanged(parser, recipe, before, emptyArray())
    }

    fun assertUnchanged(
        @Language("yaml") before: String
    ) {
        super.assertUnchanged(parser, recipe, before, emptyArray())
    }

    fun assertUnchanged(
        recipe: Recipe?,
        @Language("yaml") before: String,
        @Language("yaml") dependsOn: Array<String>
    ) {
        super.assertUnchanged(parser, recipe, before, dependsOn)
    }

    fun assertUnchanged(
        parser: Parser<*>?,
        @Language("yaml") before: String,
        @Language("yaml") dependsOn: Array<String>
    ) {
        super.assertUnchanged(parser, recipe, before, dependsOn)
    }

    override fun assertUnchanged(
        parser: Parser<*>?,
        recipe: Recipe?,
        @Language("yaml") before: String,
        @Language("yaml") dependsOn: Array<String>
    ) {
        super.assertUnchanged(parser, recipe, before, dependsOn)
    }
}

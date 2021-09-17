/*
 *  Copyright 2021 the original author or authors.
 *  <p>
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *  <p>
 *  https://www.apache.org/licenses/LICENSE-2.0
 *  <p>
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package org.openrewrite.kubernetes.tree

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.openrewrite.kubernetes.tree.K8S.Pod.inSpec
import org.openrewrite.yaml.YamlParser
import org.openrewrite.yaml.YamlVisitor
import org.openrewrite.yaml.search.YamlSearchResult
import org.openrewrite.yaml.tree.Yaml

class K8STest {

    private val source = """
        apiVersion: v1
        kind: Pod
        spec:
          containers:             
            - image: image:latest
        ---
        apiVersion: apps/v1
        kind: Deployment
        spec:
          template:
            spec:
              containers:             
                - image: image:latest
    """.trimIndent()

    @Test
    fun `must understand resource`() {
        val doc = YamlParser().parse(source)[0].documents[0]
        val result = object : YamlVisitor<Any>() {
            val m = YamlSearchResult(null, "found pod")

            override fun visitSequenceEntry(entry: Yaml.Sequence.Entry, p: Any): Yaml {
                if (K8S.inPod(cursor)) {
                    return entry.withMarkers(entry.markers.addIfAbsent(m))
                }
                return super.visitSequenceEntry(entry, p)
            }
        }.visit(doc, "")

        assertThat(result?.markers?.findFirst(YamlSearchResult::class.java)?.isPresent)
    }

    @Test
    fun `must understand image container name`() {
        val doc = YamlParser().parse(source)[0].documents[0]
        val result = object : YamlVisitor<Any>() {
            val m = YamlSearchResult(null, "found image")

            override fun visitMappingEntry(entry: Yaml.Mapping.Entry, p: Any): Yaml {
                if (K8S.Containers.isImageName(cursor)) {
                    return entry.withMarkers(entry.markers.addIfAbsent(m))
                }
                return super.visitMappingEntry(entry, p)
            }
        }.visit(doc, "")

        assertThat(result?.markers?.findFirst(YamlSearchResult::class.java)?.isPresent)
    }

    @Test
    fun `must understand deployment pod spec`() {
        val doc = YamlParser().parse(source)[0].documents[1]
        val result = object : YamlVisitor<Any>() {
            val m = YamlSearchResult(null, "found spec")

            override fun visitMappingEntry(entry: Yaml.Mapping.Entry, p: Any): Yaml {
                if (inSpec(cursor)) {
                    return entry.withMarkers(entry.markers.addIfAbsent(m))
                }
                return super.visitMappingEntry(entry, p)
            }
        }.visit(doc, "")

        assertThat(result?.markers?.findFirst(YamlSearchResult::class.java)?.isPresent)
    }
}
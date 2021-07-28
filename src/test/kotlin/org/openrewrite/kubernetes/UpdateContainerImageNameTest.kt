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
package org.openrewrite.kubernetes;

import org.junit.jupiter.api.Test

class UpdateContainerImageNameTest : KubernetesRecipeTest {

    @Test
    fun `must update container image with all values`() = assertChanged(
        recipe = UpdateContainerImageName(
            null,
            "nginx",
            null,
            "gcr.io/myaccount/myrepo",
            "nginx-custom",
            "latest",
            false,
            null
        ),
        before = """
            apiVersion: v1
            kind: Pod
            spec:
                containers:             
                - image: nginx
            ---
            apiVersion: v1
            kind: Pod
            spec:
                containers:             
                - image: gcr.io/myaccount/myrepo/nginx
                initContainers:             
                - image: gcr.io/myaccount/myrepo/myinit:latest
        """.trimIndent(),
        after = """
            apiVersion: v1
            kind: Pod
            spec:
                containers:             
                - image: gcr.io/myaccount/myrepo/nginx-custom:latest
            ---
            apiVersion: v1
            kind: Pod
            spec:
                containers:             
                - image: gcr.io/myaccount/myrepo/nginx
                initContainers:             
                - image: gcr.io/myaccount/myrepo/myinit:latest
        """.trimIndent()
    )

    @Test
    fun `must update container image with partial values`() = assertChanged(
        recipe = UpdateContainerImageName(
            null,
            "nginx*",
            null,
            "gcr.io/myaccount/myrepo",
            null,
            null,
            true,
            null
        ),
        before = """
            apiVersion: v1
            kind: Pod
            spec:
                containers:             
                - image: nginx
            ---
            apiVersion: v1
            kind: Pod
            spec:
                containers:             
                - image: nginx-custom
            ---
            apiVersion: v1
            kind: Pod
            spec:
                containers:             
                - image: gcr.io/myaccount/mydevrepo/nginx-custom:v2
                initContainers:             
                - image: gcr.io/myaccount/myrepo/myinit:latest
        """.trimIndent(),
        after = """
            apiVersion: v1
            kind: Pod
            spec:
                containers:             
                - image: gcr.io/myaccount/myrepo/nginx
            ---
            apiVersion: v1
            kind: Pod
            spec:
                containers:             
                - image: gcr.io/myaccount/myrepo/nginx-custom
            ---
            apiVersion: v1
            kind: Pod
            spec:
                containers:             
                - image: gcr.io/myaccount/mydevrepo/nginx-custom:v2
                initContainers:             
                - image: gcr.io/myaccount/myrepo/myinit:latest
        """.trimIndent()
    )

}

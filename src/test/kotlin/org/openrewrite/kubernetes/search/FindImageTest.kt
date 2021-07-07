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

package org.openrewrite.kubernetes.search

import org.junit.jupiter.api.Test
import org.openrewrite.kubernetes.KubernetesRecipeTest

class FindImageTest : KubernetesRecipeTest {

    @Test
    fun `must find fully specified image`() = assertChanged(
        recipe = FindImage(
            "repo.id/account/bucket",
            "image",
            "v1.2.3",
            false
        ),
        before = """
            apiVersion: v1
            kind: Pod
            spec:
                containers:             
                - image: image
            ---
            apiVersion: v1
            kind: Pod
            spec:
                containers:             
                - image: app:v1.2.3
                initContainers:             
                - image: account/image:latest
            ---
            apiVersion: v1
            kind: Pod
            spec:
                containers:             
                - image: repo.id/account/bucket/image:v1.2.3@digest
        """,
        after = """
            apiVersion: v1
            kind: Pod
            spec:
                containers:             
                - image: image
            ---
            apiVersion: v1
            kind: Pod
            spec:
                containers:             
                - image: app:v1.2.3
                initContainers:             
                - image: account/image:latest
            ---
            apiVersion: v1
            kind: Pod
            spec:
                containers:             
                - image: ~~(repo.id/account/bucket/image:v1.2.3)~~>repo.id/account/bucket/image:v1.2.3@digest
        """
    )

    @Test
    fun `must support globbing in image name`() = assertChanged(
        recipe = FindImage(
            "repo.id/*",
            "image",
            "v1.*",
            false
        ),
        before = """
            apiVersion: v1
            kind: Pod
            spec:
                containers:             
                - image: image
            ---
            apiVersion: v1
            kind: Pod
            spec:
                containers:             
                - image: app:v1.2.3
                initContainers:             
                - image: account/image:latest
            ---
            apiVersion: v1
            kind: Pod
            spec:
                containers:             
                - image: repo.id/account/bucket/image:v1.2.3@digest
        """,
        after = """
            apiVersion: v1
            kind: Pod
            spec:
                containers:             
                - image: image
            ---
            apiVersion: v1
            kind: Pod
            spec:
                containers:             
                - image: app:v1.2.3
                initContainers:             
                - image: account/image:latest
            ---
            apiVersion: v1
            kind: Pod
            spec:
                containers:             
                - image: ~~(repo.id/*/image:v1.*)~~>repo.id/account/bucket/image:v1.2.3@digest
        """
    )

    @Test
    fun `must find partly specified image`() = assertChanged(
        recipe = FindImage(
            "*",
            "nginx",
            "latest",
            false
        ),
        before = """
            apiVersion: v1
            kind: Pod
            spec:
                containers:             
                - image: nginx:latest
            ---
            apiVersion: v1
            kind: Pod
            spec:
                containers:             
                - image: app:v1.2.3
                initContainers:             
                - image: account/nginx:latest
            ---
            apiVersion: v1
            kind: Pod
            spec:
                containers:             
                - image: repo.id/account/bucket/nginx:v1.2.3@digest
        """,
        after = """
            apiVersion: v1
            kind: Pod
            spec:
                containers:             
                - image: ~~(*/nginx:latest)~~>nginx:latest
            ---
            apiVersion: v1
            kind: Pod
            spec:
                containers:             
                - image: app:v1.2.3
                initContainers:             
                - image: account/nginx:latest
            ---
            apiVersion: v1
            kind: Pod
            spec:
                containers:             
                - image: repo.id/account/bucket/nginx:v1.2.3@digest
        """
    )

    @Test
    fun `image name must preserve digest value`() = assertChanged(
        recipe = FindImage(
            "*",
            "*",
            "*",
            true
        ),
        before = """
            apiVersion: v1
            kind: Pod
            spec:
                containers:             
                - image: image:latest
            ---
            apiVersion: v1
            kind: Pod
            spec:
                containers:             
                - image: app:v1.2.3
                initContainers:             
                - image: account/image:latest
            ---
            apiVersion: v1
            kind: Pod
            spec:
                containers:             
                - image: repo.id/account/bucket/image:v1.2.3@digest
        """,
        after = """
            apiVersion: v1
            kind: Pod
            spec:
                containers:             
                - image: ~~(*/*:*)~~>image:latest
            ---
            apiVersion: v1
            kind: Pod
            spec:
                containers:             
                - image: ~~(*/*:*)~~>app:v1.2.3
                initContainers:             
                - image: ~~(*/*:*)~~>account/image:latest
            ---
            apiVersion: v1
            kind: Pod
            spec:
                containers:             
                - image: ~~(*/*:*)~~>repo.id/account/bucket/image:v1.2.3@digest
        """
    )

}
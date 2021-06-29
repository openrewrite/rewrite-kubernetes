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
package org.openrewrite.kubernetes.opa

import org.junit.jupiter.api.Test
import org.openrewrite.kubernetes.KubernetesRecipeTest

class UpgradeContainerImageTest : KubernetesRecipeTest {

    @Test
    fun mustChangeImageNameIfNotApprovedRepo() = assertChanged(
        recipe = UpgradeContainerImage(
            setOf("repo.dev.lan", "repo.prod.wan"),
            "repo.dev.lan",
            "nginx",
            "1.2.3"
        ),
        before = """
            apiVersion: v1
            kind: Pod
            metadata:
              labels:
                app: application
            spec:
              containers:            
              - name: mycontainer
                image: nginx:latest
        """,
        after = """
            apiVersion: v1
            kind: Pod
            metadata:
              labels:
                app: application
            spec:
              containers:            
              - name: mycontainer
                image: repo.dev.lan/nginx:1.2.3
        """
    )

    @Test
    fun mustNotChangeImageNameIfApprovedRepo() = assertUnchanged(
        recipe = UpgradeContainerImage(
            setOf("repo.dev.lan", "repo.prod.wan"),
            "repo.dev.lan",
            "nginx",
            "latest"
        ),
        before = """
            apiVersion: v1
            kind: Pod
            metadata:
              labels:
                app: application
            spec:
              containers:            
              - name: mycontainer
                image: repo.prod.wan/nginx:latest
        """
    )

    @Test
    fun mustChangeImageNameWithRepo() = assertChanged(
        recipe = UpgradeContainerImage(
            setOf("repo.dev.lan", "repo.prod.wan"),
            "repo.dev.lan",
            "nginx",
            "1.2.3"
        ),
        before = """
            apiVersion: v1
            kind: Pod
            metadata:
              labels:
                app: application
            spec:
              containers:            
              - name: mycontainer
                image: repo.prod.wan/nginx:latest
        """,
        after = """
            apiVersion: v1
            kind: Pod
            metadata:
              labels:
                app: application
            spec:
              containers:            
              - name: mycontainer
                image: repo.prod.wan/nginx:1.2.3
        """
    )

    @Test
    fun mustChangePreferredRepoForAllImages() = assertChanged(
        recipe = UpgradeContainerImage(
            setOf("repo.dev.lan", "repo.prod.wan"),
            "repo.dev.lan",
            null,
            null
        ),
        before = """
            apiVersion: v1
            kind: Pod
            metadata:
              labels:
                app: application
            spec:
              containers:            
              - name: mycontainer
                image: nginx:latest
        """,
        after = """
            apiVersion: v1
            kind: Pod
            metadata:
              labels:
                app: application
            spec:
              containers:            
              - name: mycontainer
                image: repo.dev.lan/nginx:latest
        """
    )

    @Test
    fun mustChangePreferredRepoAndVersionForAllImages() = assertChanged(
        recipe = UpgradeContainerImage(
            setOf("repo.dev.lan", "repo.prod.wan"),
            "repo.dev.lan",
            null,
            "latest"
        ),
        before = """
            apiVersion: v1
            kind: Pod
            metadata:
              labels:
                app: application
            spec:
              containers:            
              - name: mycontainer
                image: nginx:1.2.3
        """,
        after = """
            apiVersion: v1
            kind: Pod
            metadata:
              labels:
                app: application
            spec:
              containers:            
              - name: mycontainer
                image: repo.dev.lan/nginx:latest
        """
    )

}
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
package org.openrewrite.kubernetes.search

import org.junit.jupiter.api.Test
import org.openrewrite.kubernetes.KubernetesRecipeTest

class FindExceedsResourceValueTest : KubernetesRecipeTest {

    @Test
    fun `must find limits that exceed a given maximum`() = assertChanged(
        recipe = FindExceedsResourceValue(
            "limits",
            "memory",
            "64m"
        ),
        before = """
            apiVersion: v1
            kind: Pod
            metadata:
              labels:
                app: application
            spec:
              containers:            
              - image: nginx:latest
                resources:
                    limits:
                        cpu: "500Mi"
                        memory: "256m"
        """,
        after = """
            apiVersion: v1
            kind: Pod
            metadata:
              labels:
                app: application
            spec:
              containers:            
              - image: nginx:latest
                resources:
                    limits:
                        cpu: "500Mi"
                        memory: ~~(exceeds maximum of 64M)~~>"256m"
        """
    )

    @Test
    fun `must convert limits in different units`() = assertChanged(
        recipe = FindExceedsResourceValue(
            "limits",
            "memory",
            "1Gi"
        ),
        before = """
            apiVersion: v1
            kind: Pod
            metadata:
              labels:
                app: application
            spec:
              containers:            
              - image: nginx:latest
                resources:
                    limits:
                        cpu: "500Mi"
                        memory: "2000M"
        """,
        after = """
            apiVersion: v1
            kind: Pod
            metadata:
              labels:
                app: application
            spec:
              containers:            
              - image: nginx:latest
                resources:
                    limits:
                        cpu: "500Mi"
                        memory: ~~(exceeds maximum of 1Gi)~~>"2000M"
        """
    )

    @Test
    fun `must find requests that exceed a given maximum`() = assertChanged(
        recipe = FindExceedsResourceValue(
            "requests",
            "cpu",
            "100m"
        ),
        before = """
            apiVersion: v1
            kind: Pod
            metadata:
              labels:
                app: application
            spec:
              containers:            
              - image: nginx:latest
                resources:
                    requests:
                        cpu: "500Mi"
                        memory: "256m"
        """,
        after = """
            apiVersion: v1
            kind: Pod
            metadata:
              labels:
                app: application
            spec:
              containers:            
              - image: nginx:latest
                resources:
                    requests:
                        cpu: ~~(exceeds maximum of 100M)~~>"500Mi"
                        memory: "256m"
        """
    )

    @Test
    fun `must convert requests in different units`() = assertChanged(
        recipe = FindExceedsResourceValue(
            "requests",
            "memory",
            "1Gi"
        ),
        before = """
            apiVersion: v1
            kind: Pod
            metadata:
              labels:
                app: application
            spec:
              containers:            
              - image: nginx:latest
                resources:
                    requests:
                        cpu: "500Mi"
                        memory: "2000M"
        """,
        after = """
            apiVersion: v1
            kind: Pod
            metadata:
              labels:
                app: application
            spec:
              containers:            
              - image: nginx:latest
                resources:
                    requests:
                        cpu: "500Mi"
                        memory: ~~(exceeds maximum of 1Gi)~~>"2000M"
        """
    )

}
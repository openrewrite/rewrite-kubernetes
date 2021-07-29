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

package org.openrewrite.kubernetes.services

import org.junit.jupiter.api.Test
import org.openrewrite.kubernetes.KubernetesRecipeTest

class FindServicesByTypeTest : KubernetesRecipeTest {

    @Test
    fun `must find NodePort services`() = assertChanged(
        recipe = FindServicesByType("NodePort", null),
        before = """
            apiVersion: v1
            kind: Service
            metadata:
              name: my-service
            spec:
              selector:
                app: MyApp
              ports:
                - name: http
                  protocol: TCP
                  port: 80
                  targetPort: 9376
                - name: https
                  protocol: TCP
                  port: 443
                  targetPort: 9377
            ---
            apiVersion: v1
            kind: Service
            metadata:
              name: my-service
            spec:
              type: NodePort
              selector:
                app: MyApp
              ports:
                - port: 80
                  targetPort: 80
                  nodePort: 30007
            ---
            apiVersion: v1
            kind: Service
            metadata:
              name: my-service
            spec:
              selector:
                app: MyApp
              ports:
                - protocol: TCP
                  port: 80
                  targetPort: 9376
              clusterIP: 10.0.171.239
              type: LoadBalancer
            status:
              loadBalancer:
                ingress:
                - ip: 192.0.2.127
        """.trimIndent(),
        after = """
            apiVersion: v1
            kind: Service
            metadata:
              name: my-service
            spec:
              selector:
                app: MyApp
              ports:
                - name: http
                  protocol: TCP
                  port: 80
                  targetPort: 9376
                - name: https
                  protocol: TCP
                  port: 443
                  targetPort: 9377
            ---
            apiVersion: v1
            kind: Service
            metadata:
              name: my-service
            ~~(type:NodePort)~~>spec:
              type: NodePort
              selector:
                app: MyApp
              ports:
                - port: 80
                  targetPort: 80
                  nodePort: 30007
            ---
            apiVersion: v1
            kind: Service
            metadata:
              name: my-service
            spec:
              selector:
                app: MyApp
              ports:
                - protocol: TCP
                  port: 80
                  targetPort: 9376
              clusterIP: 10.0.171.239
              type: LoadBalancer
            status:
              loadBalancer:
                ingress:
                - ip: 192.0.2.127
        """.trimIndent()
    )

    @Test
    fun `must find ClusterIP services`() = assertChanged(
        recipe = FindServicesByType("ClusterIP", null),
        before = """
            apiVersion: v1
            kind: Service
            metadata:
              name: my-service
            spec:
              selector:
                app: MyApp
              ports:
                - name: http
                  protocol: TCP
                  port: 80
                  targetPort: 9376
                - name: https
                  protocol: TCP
                  port: 443
                  targetPort: 9377
            ---
            apiVersion: v1
            kind: Service
            metadata:
              name: my-service
            spec:
              type: NodePort
              selector:
                app: MyApp
              ports:
                - port: 80
                  targetPort: 80
                  nodePort: 30007
            ---
            apiVersion: v1
            kind: Service
            metadata:
              name: my-service
            spec:
              selector:
                app: MyApp
              ports:
                - protocol: TCP
                  port: 80
                  targetPort: 9376
              clusterIP: 10.0.171.239
              type: LoadBalancer
            status:
              loadBalancer:
                ingress:
                - ip: 192.0.2.127
        """.trimIndent(),
        after = """
            apiVersion: v1
            kind: Service
            metadata:
              name: my-service
            ~~(type:ClusterIP)~~>spec:
              selector:
                app: MyApp
              ports:
                - name: http
                  protocol: TCP
                  port: 80
                  targetPort: 9376
                - name: https
                  protocol: TCP
                  port: 443
                  targetPort: 9377
            ---
            apiVersion: v1
            kind: Service
            metadata:
              name: my-service
            spec:
              type: NodePort
              selector:
                app: MyApp
              ports:
                - port: 80
                  targetPort: 80
                  nodePort: 30007
            ---
            apiVersion: v1
            kind: Service
            metadata:
              name: my-service
            spec:
              selector:
                app: MyApp
              ports:
                - protocol: TCP
                  port: 80
                  targetPort: 9376
              clusterIP: 10.0.171.239
              type: LoadBalancer
            status:
              loadBalancer:
                ingress:
                - ip: 192.0.2.127
        """.trimIndent()
    )

}
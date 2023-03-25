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
package org.openrewrite.kubernetes.services;

import org.junit.jupiter.api.Test;
import org.openrewrite.kubernetes.KubernetesRecipeTest;

import java.util.Set;

import static org.openrewrite.yaml.Assertions.yaml;

class FindServiceExternalIPTest extends KubernetesRecipeTest {

    @Test
    void findServicesByExternalIPs() {
        rewriteRun(
          spec -> spec.recipe(new FindServiceExternalIPs(
            Set.of("192.168.0.1", "10.10.10.10"),
            false,
            null
          )),
          yaml(
            """
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
                externalIPs:
                  - 192.168.0.1
              ---
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
                externalIPs:
                  - 10.10.10.1
              """,
            """
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
                ~~(found ip)~~>externalIPs:
                  - 192.168.0.1
              ---
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
                externalIPs:
                  - 10.10.10.1
              """
          )
        );
    }


    @Test
    void findServicesByExternalIPsExclusion() {
        rewriteRun(
          spec -> spec.recipe(new FindServiceExternalIPs(
            Set.of("192.168.0.1", "10.10.10.1"),
            true,
            null
          )),
          yaml(
            """
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
                externalIPs:
                  - 192.168.0.1
              ---
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
                externalIPs:
                  - 10.10.0.1
              """,
            """
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
                externalIPs:
                  - 192.168.0.1
              ---
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
                ~~(missing ip)~~>externalIPs:
                  - 10.10.0.1
              """
          )
        );
    }
}

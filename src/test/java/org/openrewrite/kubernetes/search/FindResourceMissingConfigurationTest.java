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
package org.openrewrite.kubernetes.search;

import org.junit.jupiter.api.Test;
import org.openrewrite.DocumentExample;
import org.openrewrite.config.Environment;
import org.openrewrite.kubernetes.KubernetesRecipeTest;

import static org.openrewrite.yaml.Assertions.yaml;

class FindResourceMissingConfigurationTest extends KubernetesRecipeTest {

    @DocumentExample
    @Test
    void podLivenessProbe() {
        rewriteRun(
          spec -> spec.recipe(new FindResourceMissingConfiguration(
            "Pod",
            "$.spec.containers[*].livenessProbe",
            null
          )),
          yaml(
            """
              apiVersion: v1
              kind: Pod
              spec:
                containers:
                - name: <container name>
                  image: <image>
              """,
            """
              ~~>apiVersion: v1
              kind: Pod
              spec:
                containers:
                - name: <container name>
                  image: <image>
              """
          )
        );
    }

    @Test
    void correctlyConfiguredPodLivenessProbe() {
        rewriteRun(
          spec -> spec.recipe(new FindResourceMissingConfiguration(
            "Pod",
            "$.spec.containers[*].livenessProbe",
            null
          )),
          yaml(
            """
              apiVersion: v1
              kind: Pod
              metadata:
                labels:
                  app: application
              spec:
                containers:
                - image: nginx:latest
                  livenessProbe:
                    httpGet:
                      path: /healthz
                      port: 8080
                    initialDelaySeconds: 3
                    periodSeconds: 3
              """
          )
        );
    }

    @Test
    void onlyMatchOnConfiguredResources() {
        rewriteRun(
          spec -> spec.recipe(new FindResourceMissingConfiguration(
            "Pod",
            "..spec.containers[*].livenessProbe",
            null
          )),
          yaml(
            """
              apiVersion: apps/v1
              kind: Deployment
              metadata:
                labels:
                  app: application
              spec:
                template:
                  spec:
                    containers:
                    - image: nginx:latest
                      livenessProbe:
                        httpGet:
                          path: /healthz
                          port: 8080
                        initialDelaySeconds: 3
                        periodSeconds: 3
              """
          )
        );
    }

    @Test
    void missingPodLivenessProbe() {
        rewriteRun(
          spec -> spec.recipe(Environment.builder()
            .scanRuntimeClasspath()
            .build()
            .activateRecipes("org.openrewrite.kubernetes.MissingPodLivenessProbe")),
          yaml(
            """
              apiVersion: apps/v1
              kind: Pod
              metadata:
                labels:
                  app: application
              spec:
                containers:
                - image: nginx:latest
              """,
            """
              ~~>apiVersion: apps/v1
              kind: Pod
              metadata:
                labels:
                  app: application
              spec:
                containers:
                - image: nginx:latest
              """
          )
        );
    }

    @Test
    void missingCpuLimits() {
        rewriteRun(
          spec -> spec.recipe(Environment.builder()
            .scanRuntimeClasspath()
            .build()
            .activateRecipes("org.openrewrite.kubernetes.MissingCpuLimits")),
          yaml(
            """
              apiVersion: apps/v1
              kind: Deployment
              metadata:
                labels:
                  app: application
              spec:
                template:
                  spec:
                    containers:
                    - image: nginx:latest
              """,
            """
              ~~>apiVersion: apps/v1
              kind: Deployment
              metadata:
                labels:
                  app: application
              spec:
                template:
                  spec:
                    containers:
                    - image: nginx:latest
              """
          )
        );
    }
}

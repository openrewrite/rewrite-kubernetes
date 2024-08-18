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
import org.openrewrite.Issue;
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
          //language=yaml
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
              ~~(missing: $.spec.containers[*].livenessProbe)~~>apiVersion: v1
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
    void noChangeIfPresent() {
        rewriteRun(
          spec -> spec.recipe(new FindResourceMissingConfiguration(
            "Pod",
            "$.spec.containers[*].livenessProbe",
            null
          )),
          //language=yaml
          yaml(
            """
              apiVersion: v1
              kind: Pod
              spec:
                containers:
                - name: <container name>
                  image: <image>
                  livenessProbe:
                    httpGet:
                      path: /healthz
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
          //language=yaml
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
          //language=yaml
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
          spec -> spec.recipeFromResources("org.openrewrite.kubernetes.MissingPodLivenessProbe"),
          //language=yaml
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
              ~~(missing: $.spec.containers[:1].livenessProbe)~~>apiVersion: apps/v1
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
    void cpuLimitsMissing() {
        rewriteRun(
          spec -> spec.recipeFromResources("org.openrewrite.kubernetes.MissingCpuLimits"),
          //language=yaml
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
              ~~(missing: ..spec.containers[:1].resources.limits.cpu)~~>apiVersion: apps/v1
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

    @Test
    void cpuLimitsPresent() {
        rewriteRun(
          spec -> spec.recipeFromResources("org.openrewrite.kubernetes.MissingCpuLimits"),
          //language=yaml
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
                      resources:
                        limits:
                          cpu: "64Mi"
              """
          )
        );
    }

    @Test
    @Issue("https://github.com/openrewrite/rewrite-kubernetes/issues/51")
    void springApplicationProperties() {
        rewriteRun(
          spec -> spec.recipeFromResources("org.openrewrite.kubernetes.MissingCpuLimits"),
          //language=yaml
          yaml(
            """
              spring:
               application:
                 foo: hello
              """
          )
        );
    }
}

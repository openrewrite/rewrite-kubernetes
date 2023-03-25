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
package org.openrewrite.kubernetes;

import org.junit.jupiter.api.Test;

import static org.openrewrite.yaml.Assertions.yaml;

class AddConfigurationTest extends KubernetesRecipeTest {

    @Test
    void addConfigurationToMappingEntryIfThePathDoesNotExist() {
        rewriteRun(
          spec -> spec.recipe(new AddConfiguration(null,
            "Pod",
            "$.spec",
            "privileged: false")),
          yaml(
            """
              apiVersion: v1
              kind: Pod
              metadata:
                name: nginx
              spec:
                containers:
                  - name: nginx
                    image: nginx
              """,
            """
              apiVersion: v1
              kind: Pod
              metadata:
                name: nginx
              spec:
                containers:
                  - name: nginx
                    image: nginx
                privileged: false
              """
          )
        );
    }

    @Test
    void addConfigurationToNestedMappingEntriesIfThePathDoesNotExist() {
        rewriteRun(
          spec -> spec.recipe(new AddConfiguration(null,
            "PodSecurityPolicy",
            "$",
            """
              spec:
                privileged: false
              """)),
          yaml("""
              apiVersion: policy/v1beta1
              kind: PodSecurityPolicy
              metadata:
                name: psp
              """,
            """
              apiVersion: policy/v1beta1
              kind: PodSecurityPolicy
              metadata:
                name: psp
              spec:
                privileged: false
              """
          )
        );
    }

    @Test
    void addConfigurationToSequenceEntriesIfThePathDoesNotExist() {
        rewriteRun(
          spec -> spec.recipe(new AddConfiguration(
            null,
            "Pod",
            "$.spec.containers",
            "imagePullPolicy: Always")),
          yaml("""
              apiVersion: v1
              kind: Pod
              metadata:
                name: example
              spec:
                containers:
                  - name: pod-0
                    image: busybox
                  - name: pod-1
                    image: busybox
              """,
            """
              apiVersion: v1
              kind: Pod
              metadata:
                name: example
              spec:
                containers:
                  - name: pod-0
                    image: busybox
                    imagePullPolicy: Always
                  - name: pod-1
                    image: busybox
                    imagePullPolicy: Always
              """
          )
        );
    }

    @Test
    void doNotAddSequencesIfThePathHasNoExistingEntries() {
        rewriteRun(
          spec -> spec.recipe(new AddConfiguration(
            null,
            "Pod",
            "$.spec.containers",
            "imagePullPolicy: Always"
          )),
          yaml(
            """
              apiVersion: v1
              kind: Pod
              metadata:
                name: example
              spec:
                containers: []
              """
          )
        );
    }

    @Test
    void addConfigurationToMappingEntriesWithinSequenceEntriesIfThePathDoesNotExist() {
        rewriteRun(
          spec -> spec.recipe(new AddConfiguration(
            null,
            "Pod",
            "$.spec.containers",
            """
              securityContext:
                allowPrivilegeEscalation: false
              """)),
          yaml(
            """
              apiVersion: v1
              kind: Pod
              metadata:
                name: nginx
              spec:
                containers:
                - name: nginx
                  image: nginx
              """,
            """
              apiVersion: v1
              kind: Pod
              metadata:
                name: nginx
              spec:
                containers:
                - name: nginx
                  image: nginx
                  securityContext:
                    allowPrivilegeEscalation: false
              """
          )
        );
    }

    @Test
    void doNotModifyExistingConfiguration() {
        rewriteRun(
          spec -> spec.recipe(new AddConfiguration(
            null,
            "PodSecurityPolicy",
            "$.spec",
            "privileged: false"
          )),
          yaml(
            """
                apiVersion: policy/v1beta1
                kind: PodSecurityPolicy
                metadata:
                  name: psp
                spec:
                  privileged: true
              """
          )
        );
    }
}

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
import org.openrewrite.kubernetes.KubernetesParserTest;

import static org.openrewrite.yaml.Assertions.yaml;

class FindMissingOrInvalidLabelTest extends KubernetesParserTest {

    @Test
    void findMissingLabel() {
        rewriteRun(
          spec -> spec.recipe(new FindMissingOrInvalidLabel(
            "mylabel",
            null,
            null
          )),
          yaml(
            """
              apiVersion: v1
              kind: Pod
              metadata:
                name: mypod1
                labels:
                  something: "hasvalue"
              ---
              apiVersion: v1
              kind: Pod
              metadata:
                name: mypod2
                labels:
                  mylabel: "novalue"
              ---
              apiVersion: apps/v1
              kind: Deployment
              spec:
                  template:
                      spec:
                          metadata:
                              labels:
                                  something: "hasvalue"
                          containers:            
                          - name: app
                            image: repo/app:latest
                          - name: sidecar
                            image: repo/sidecar:dev
              """,
            """
              apiVersion: v1
              kind: Pod
              metadata:
                name: mypod1
                ~~(missing:mylabel)~~>labels:
                  something: "hasvalue"
              ---
              apiVersion: v1
              kind: Pod
              metadata:
                name: mypod2
                labels:
                  mylabel: "novalue"
              ---
              apiVersion: apps/v1
              kind: Deployment
              spec:
                  template:
                      spec:
                          metadata:
                              ~~(missing:mylabel)~~>labels:
                                  something: "hasvalue"
                          containers:            
                          - name: app
                            image: repo/app:latest
                          - name: sidecar
                            image: repo/sidecar:dev
              """
          )
        );
    }

    @Test
    void findInvalidLabel() {
        rewriteRun(
          spec -> spec.recipe(new FindMissingOrInvalidLabel(
            "mylabel",
            "has(.*)",
            null
          )),
          yaml(
            """
              apiVersion: v1
              kind: Pod
              metadata:
                name: mypod1
                labels:
                  mylabel: "hasvalue"
              ---
              apiVersion: v1
              kind: Pod
              metadata:
                name: mypod2
                labels:
                  mylabel: "novalue"
              ---
              apiVersion: apps/v1
              kind: Deployment
              spec:
                  template:
                      spec:
                          metadata:
                              labels:
                                  mylabel: "hasvalue"
                          containers:
                          - name: app
                            image: repo/app:latest
                          - name: sidecar
                            image: repo/sidecar:dev
              """,
            """
              apiVersion: v1
              kind: Pod
              metadata:
                name: mypod1
                labels:
                  mylabel: "hasvalue"
              ---
              apiVersion: v1
              kind: Pod
              metadata:
                name: mypod2
                labels:
                  ~~(invalid:has(.*))~~>mylabel: "novalue"
              ---
              apiVersion: apps/v1
              kind: Deployment
              spec:
                  template:
                      spec:
                          metadata:
                              labels:
                                  mylabel: "hasvalue"
                          containers:
                          - name: app
                            image: repo/app:latest
                          - name: sidecar
                            image: repo/sidecar:dev
              """
          )
        );
    }
}

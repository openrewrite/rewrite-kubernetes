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

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.openrewrite.DocumentExample;
import org.openrewrite.kubernetes.KubernetesRecipeTest;

import static org.openrewrite.yaml.Assertions.yaml;

class FindDisallowedImageTagsTest extends KubernetesRecipeTest {

    @Disabled("JsonPathMatcher has changed, need to figure out how to fix this.")
    @Test
    void findDisallowedImageTags() {
        rewriteRun(
          spec -> spec.recipe(new FindDisallowedImageTags(
            "latest, dev",
            true,
            null
          )),
          yaml(
            """
              apiVersion: v1
              kind: Pod
              spec:
                  containers:
                  - image: repo/app:latest
              ---
              apiVersion: apps/v1
              kind: Deployment
              spec:
                  template:
                      spec:
                          containers:
                          - name: app
                            image: repo/app:latest
                          - name: sidecar
                            image: repo/sidecar:dev
                          initContainers:
                          - name: migration
                            image: repo/migration:latest
                          - name: backup
                            image: repo/backup:dev
              """,
            """
              apiVersion: v1
              kind: Pod
              spec:
                  containers:
                  - image: ~~(disallowed tag: [latest, dev])~~>repo/app:latest
              ---
              apiVersion: apps/v1
              kind: Deployment
              spec:
                  template:
                      spec:
                          containers:
                          - name: app
                            image: ~~(disallowed tag: [latest, dev])~~>repo/app:latest
                          - name: sidecar
                            image: ~~(disallowed tag: [latest, dev])~~>repo/sidecar:dev
                          initContainers:
                          - name: migration
                            image: ~~(disallowed tag: [latest, dev])~~>repo/migration:latest
                          - name: backup
                            image: ~~(disallowed tag: [latest, dev])~~>repo/backup:dev
              """
          )
        );
    }

    @DocumentExample
    @Test
    void findDisallowedImageTagsInWorkloads() {
        rewriteRun(
          spec -> spec.recipe(new FindDisallowedImageTags(
            "latest, dev",
            false,
            null
          )),
          yaml(
            """
              apiVersion: apps/v1
              kind: Deployment
              spec:
                template:
                  spec:
                    containers:
                    - image: nginx:latest
              ---
              apiVersion: apps/v1
              kind: StatefulSet
              spec:
                template:
                  spec:
                    containers:
                    - image: app:dev
              """,
            """
              apiVersion: apps/v1
              kind: Deployment
              spec:
                template:
                  spec:
                    containers:
                    - image: ~~(disallowed tag: [latest])~~>nginx:latest
              ---
              apiVersion: apps/v1
              kind: StatefulSet
              spec:
                template:
                  spec:
                    containers:
                    - image: ~~(disallowed tag: [dev])~~>app:dev
              """
          )
        );
    }

    @Test
    void notExecuteOnPathsThatDoNotMatch() {
        rewriteRun(
          spec -> spec.recipe(new FindDisallowedImageTags(
            "latest, dev",
            false,
            "/some/path/to/*.yaml"
          )),
          yaml(
            """
              apiVersion: apps/v1
              kind: Deployment
              spec:
                template:
                  spec:
                    containers:
                    - image: nginx:latest
              ---
              apiVersion: apps/v1
              kind: StatefulSet
              spec:
                template:
                  spec:
                    containers:
                    - image: app:dev
              """
          )
        );
    }
}

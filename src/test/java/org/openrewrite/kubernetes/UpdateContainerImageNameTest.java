/*
 * Copyright 2024 the original author or authors.
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

import org.intellij.lang.annotations.Language;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.openrewrite.DocumentExample;

import java.util.List;

import static org.openrewrite.yaml.Assertions.yaml;

class UpdateContainerImageNameTest extends KubernetesRecipeTest {
    @Language("yaml")
    private static String getYamlWithImage(String image) {
        //language=yaml
        return """
          apiVersion: v1
          kind: Pod
          spec:
              containers:
              - image: %s
          """.formatted(image);
    }

    @Language("yaml")
    private static String getYamlWithInitContainerImage(String icImage) {
        //language=yaml
        return """
          apiVersion: v1
          kind: Pod
          spec:
              containers:
              - image: notGoingToMatch
              initContainers:
              - image: %s
           """.formatted(icImage);
    }

    @DocumentExample
    @Test
    void updateContainerImageWithAllValues() {
        rewriteRun(
          spec -> spec.recipe(new UpdateContainerImageName(
            null,
            "nginx",
            null,
            null,
            "gcr.io/myaccount/myrepo",
            "nginx-custom",
            "latest",
            null,
            false,
            null
          )),
          //language=yaml
          yaml(
            """
              apiVersion: v1
              kind: Pod
              spec:
                  containers:
                  - image: nginx
              ---
              apiVersion: v1
              kind: Pod
              spec:
                  containers:
                  - image: gcr.io/myaccount/myrepo/nginx
                  initContainers:
                  - image: gcr.io/myaccount/myrepo/myinit:latest
              """,
            """
              apiVersion: v1
              kind: Pod
              spec:
                  containers:
                  - image: gcr.io/myaccount/myrepo/nginx-custom:latest
              ---
              apiVersion: v1
              kind: Pod
              spec:
                  containers:
                  - image: gcr.io/myaccount/myrepo/nginx
                  initContainers:
                  - image: gcr.io/myaccount/myrepo/myinit:latest
              """
          )

        );
    }

    @Test
    void updateContainerImageWithPartialValues() {
        rewriteRun(
          spec -> spec.recipe(new UpdateContainerImageName(
            null,
            "nginx*",
            null,
            null,
            "gcr.io/myaccount/myrepo",
            null,
            null,
            null,
            true,
            null
          )),
          //language=yaml
          yaml(
            """
              apiVersion: v1
              kind: Pod
              spec:
                  containers:
                  - image: nginx
              ---
              apiVersion: v1
              kind: Pod
              spec:
                  containers:
                  - image: nginx-custom
              ---
              apiVersion: v1
              kind: Pod
              spec:
                  containers:
                  - image: gcr.io/myaccount/mydevrepo/nginx-custom:v2
                  initContainers:
                  - image: gcr.io/myaccount/myrepo/myinit:latest
              """,
            """
              apiVersion: v1
              kind: Pod
              spec:
                  containers:
                  - image: gcr.io/myaccount/myrepo/nginx
              ---
              apiVersion: v1
              kind: Pod
              spec:
                  containers:
                  - image: gcr.io/myaccount/myrepo/nginx-custom
              ---
              apiVersion: v1
              kind: Pod
              spec:
                  containers:
                  - image: gcr.io/myaccount/mydevrepo/nginx-custom:v2
                  initContainers:
                  - image: gcr.io/myaccount/myrepo/myinit:latest
              """
          )
        );
    }

    @Test
    void digestToUpdate_as_emptyString_should_remove_digest() {
        rewriteRun(
          spec -> spec.recipe(new UpdateContainerImageName(
            null,
            "nginx*",
            null,
            "*",
            "gcr.io/myaccount/myrepo",
            null,
            null,
            "",
            true,
            null
          )),
          //language=yaml
          yaml(
            """
              apiVersion: v1
              kind: Pod
              spec:
                  containers:
                  - image: nginx
              ---
              apiVersion: v1
              kind: Pod
              spec:
                  containers:
                  - image: nginx-custom@sha256:cb5c1bddd1b5665e1867a7fa1b5fa843a47ee433bbb75d4293888b71def53229
              ---
              apiVersion: v1
              kind: Pod
              spec:
                  containers:
                  - image: gcr.io/myaccount/mydevrepo/nginx-custom:v2@sha256:cb5c1bddd1b5665e1867a7fa1b5fa843a47ee433bbb75d4293888b71def53229
                  initContainers:
                  - image: gcr.io/myaccount/myrepo/myinit:latest@sha256:cb5c1bddd1b5665e1867a7fa1b5fa843a47ee433bbb75d4293888b71def53229
              """,
            """
              apiVersion: v1
              kind: Pod
              spec:
                  containers:
                  - image: gcr.io/myaccount/myrepo/nginx
              ---
              apiVersion: v1
              kind: Pod
              spec:
                  containers:
                  - image: gcr.io/myaccount/myrepo/nginx-custom
              ---
              apiVersion: v1
              kind: Pod
              spec:
                  containers:
                  - image: gcr.io/myaccount/mydevrepo/nginx-custom:v2@sha256:cb5c1bddd1b5665e1867a7fa1b5fa843a47ee433bbb75d4293888b71def53229
                  initContainers:
                  - image: gcr.io/myaccount/myrepo/myinit:latest@sha256:cb5c1bddd1b5665e1867a7fa1b5fa843a47ee433bbb75d4293888b71def53229
              """
          )
        );
    }

    @Test
    void respect_all_find_flags() {
        rewriteRun(
          spec -> spec.recipe(new UpdateContainerImageName(
            "repo123",
            "image456",
            "v7.8.9",
            "SHA256:9876543",
            "123repo",
            "456image",
            "v9.9.9",
            "SHA256:3456789",
            false,
            null
          )),
          yaml(
            getYamlWithImage("repo123/image456:v7.8.9@SHA256:9876543"),
            getYamlWithImage("123repo/456image:v9.9.9@SHA256:3456789")
          )
        );
        rewriteRun(
          spec -> spec.recipe(new UpdateContainerImageName(
            "repo123",
            "image456",
            "v7.8.9",
            "SHA256:9876543",
            "123repo",
            "456image",
            null,
            null,
            false,
            null
          )),
          yaml(
            getYamlWithImage("repo123/image456:v7.8.9@SHA256:9876543"),
            getYamlWithImage("123repo/456image:v7.8.9@SHA256:9876543")
          )
        );
        rewriteRun(
          spec -> spec.recipe(new UpdateContainerImageName(
            "repo123",
            "image456",
            "v7.8.9",
            "SHA256:9876543",
            null,
            "456image",
            null,
            null,
            false,
            null
          )),
          yaml(
            getYamlWithImage("repo123/image456:v7.8.9@SHA256:9876543"),
            getYamlWithImage("repo123/456image:v7.8.9@SHA256:9876543")
          )
        );
    }

    @ParameterizedTest
    @ValueSource(strings = {
      "repoXXX/image456:v7.8.9@SHA256:9876543",
      "repo123/image457:v7.8.9@SHA256:9876543",
      "repo123/image456:v1.8.9@SHA256:9876543",
      "repo123/image456:v7.8.9@SHA256:987654X"
    })
    void shouldNotUpdateMismatches(String imageThatDontMatch) {
        rewriteRun(
          spec -> spec.recipe(new UpdateContainerImageName(
            "repo123",
            "image456",
            "v7.8.9",
            "SHA256:9876543",
            "",
            "456image",
            "",
            "",
            false,
            null
          )),
          yaml(getYamlWithImage(imageThatDontMatch))
        );
        rewriteRun(
          spec -> spec.recipe(new UpdateContainerImageName(
            "repo123",
            "image456",
            "v7.8.9",
            "SHA256:9876543",
            "",
            "456image",
            "",
            "",
            true,
            null
          )),
          yaml(getYamlWithInitContainerImage(imageThatDontMatch))
        );
    }

    @Test
    void unset_when_flags_are_empty_string() {
        UpdateContainerImageName recipe1 = new UpdateContainerImageName(
          "repo123",
          "image456",
          "v7.8.9",
          "SHA256:9876543",
          "",
          "456image",
          "",
          "",
          true,
          null
        );
        rewriteRun(
          spec -> spec.recipe(recipe1),
          yaml(
            getYamlWithImage("repo123/image456:v7.8.9@SHA256:9876543"),
            getYamlWithImage("456image")
          )
        );
        rewriteRun(
          spec -> spec.recipe(recipe1),
          yaml(
            getYamlWithInitContainerImage("repo123/image456:v7.8.9@SHA256:9876543"),
            getYamlWithInitContainerImage("456image")
          )
        );
        UpdateContainerImageName recipe2 = new UpdateContainerImageName(
          "repo123",
          "image456",
          "v7.8.9",
          "SHA256:9876543",
          null,
          "456image",
          "",
          "",
          true,
          null
        );
        rewriteRun(
          spec -> spec.recipe(recipe2),
          yaml(
            getYamlWithImage("repo123/image456:v7.8.9@SHA256:9876543"),
            getYamlWithImage("repo123/456image")
          )
        );
        rewriteRun(
          spec -> spec.recipe(recipe2),
          yaml(
            getYamlWithInitContainerImage("repo123/image456:v7.8.9@SHA256:9876543"),
            getYamlWithInitContainerImage("repo123/456image")
          )
        );
        UpdateContainerImageName recipe3 = new UpdateContainerImageName(
          "repo123",
          "image456",
          "v7.8.9",
          "SHA256:9876543",
          null,
          "456image",
          "",
          "",
          false,
          null
        );
        rewriteRun(
          spec -> spec.recipe(recipe3),
          yaml(
            getYamlWithImage("repo123/image456:v7.8.9@SHA256:9876543"),
            getYamlWithImage("repo123/456image")
          )
        );
        for (String imageThatDontMatch : List.of(
          "image456:v7.8.9@SHA256:9876543",
          "repo123/image456@SHA256:9876543",
          "repo123/image456:v7.8.9",
          "repo123/image456"
        )) {
            rewriteRun(
              spec -> spec.recipe(recipe3),
              yaml(
                getYamlWithImage(imageThatDontMatch)
              )
            );
        }
        rewriteRun(
          spec -> spec.recipe(recipe3),
          yaml(
            getYamlWithInitContainerImage("repo123/image456:v7.8.9@SHA256:9876543")
          )
        );
    }

    @ParameterizedTest
    @ValueSource(strings = {
      "image456:v7.8.9@SHA256:9876543",
      "image456@SHA256:9876543",
      "image456:v7.8.9",
      "repo123/image456@SHA256:9876543",
      "repo123/image456:v7.8.9",
      "repo123/image456",
      "repo/image456:v7.8.9@SHA256:9876543",
      "image456"
    })
    void update_for_image_match_query(String imageThatMatch) {
        UpdateContainerImageName recipe = new UpdateContainerImageName(
          "*",
          "image456",
          "*",
          "*",
          "changedRepo/changedRepo",
          "changedImageName",
          "v913",
          "SHA256:999",
          true,
          null
        );
        rewriteRun(
          spec -> spec.recipe(recipe),
          yaml(
            getYamlWithImage(imageThatMatch),
            getYamlWithImage("changedRepo/changedRepo/changedImageName:v913@SHA256:999")
          )
        );
        rewriteRun(
          spec -> spec.recipe(recipe),
          yaml(
            getYamlWithInitContainerImage(imageThatMatch),
            getYamlWithInitContainerImage("changedRepo/changedRepo/changedImageName:v913@SHA256:999")
          )
        );
    }
}

package org.openrewrite.kubernetes.search;

import org.junit.jupiter.api.Test;
import org.openrewrite.kubernetes.KubernetesParserTest;

import static org.openrewrite.yaml.Assertions.yaml;

class FindImageTest extends KubernetesParserTest {

    @Test
    void findFullySpecifiedImage() {
        rewriteRun(
          spec -> spec.recipe(new FindImage(
            "repo.id/account/bucket",
            "image",
            "v1.2.3",
            false,
            null
          )),
          yaml(
            """
              apiVersion: v1
              kind: Pod
              spec:
                  containers:
                  - image: image
              ---
              apiVersion: v1
              kind: Pod
              spec:
                  containers:
                  - image: app:v1.2.3
                  initContainers:
                  - image: account/image:latest
              ---
              apiVersion: v1
              kind: Pod
              spec:
                  containers:
                  - image: repo.id/account/bucket/image:v1.2.3@digest
              """,
            """
              apiVersion: v1
              kind: Pod
              spec:
                  containers:
                  - image: image
              ---
              apiVersion: v1
              kind: Pod
              spec:
                  containers:
                  - image: app:v1.2.3
                  initContainers:
                  - image: account/image:latest
              ---
              apiVersion: v1
              kind: Pod
              spec:
                  containers:
                  - image: ~~(repo.id/account/bucket/image:v1.2.3)~~>repo.id/account/bucket/image:v1.2.3@digest
              """
          )
        );
    }

    @Test
    void supportGlobbingInImageName() {
        rewriteRun(
          spec -> spec.recipe(new FindImage(
            "repo.id/*",
            "image",
            "v1.*",
            false,
            null
          )),
          yaml(
            """
              apiVersion: v1
              kind: Pod
              spec:
                  containers:
                  - image: image
              ---
              apiVersion: v1
              kind: Pod
              spec:
                  containers:
                  - image: app:v1.2.3
                  initContainers:
                  - image: account/image:latest
              ---
              apiVersion: v1
              kind: Pod
              spec:
                  containers:
                  - image: repo.id/account/bucket/image:v1.2.3@digest
              """,
            """
              apiVersion: v1
              kind: Pod
              spec:
                  containers:
                  - image: image
              ---
              apiVersion: v1
              kind: Pod
              spec:
                  containers:
                  - image: app:v1.2.3
                  initContainers:
                  - image: account/image:latest
              ---
              apiVersion: v1
              kind: Pod
              spec:
                  containers:
                  - image: ~~(repo.id/*/image:v1.*)~~>repo.id/account/bucket/image:v1.2.3@digest
              """
          )
        );
    }

    @Test
    void findPartlySpecifiedImage() {
        rewriteRun(
          spec -> spec.recipe(new FindImage(
            "*",
            "nginx",
            "latest",
            false,
            null
          )),
          yaml(
            """
              apiVersion: v1
              kind: Pod
              spec:
                  containers:
                  - image: nginx:latest
              ---
              apiVersion: v1
              kind: Pod
              spec:
                  containers:
                  - image: app:v1.2.3
                  initContainers:
                  - image: account/nginx:latest
              ---
              apiVersion: v1
              kind: Pod
              spec:
                  containers:
                  - image: repo.id/account/bucket/nginx:v1.2.3@digest
              """,
            """
              apiVersion: v1
              kind: Pod
              spec:
                  containers:
                  - image: ~~(*/nginx:latest)~~>nginx:latest
              ---
              apiVersion: v1
              kind: Pod
              spec:
                  containers:
                  - image: app:v1.2.3
                  initContainers:
                  - image: account/nginx:latest
              ---
              apiVersion: v1
              kind: Pod
              spec:
                  containers:
                  - image: repo.id/account/bucket/nginx:v1.2.3@digest
              """
          )
        );
    }

    @Test
    void imageNameMustPreserveDigestValue() {
        rewriteRun(
          spec -> spec.recipe(new FindImage(
            "*",
            "*",
            "*",
            true,
            null
          )),
          yaml(
            """
              apiVersion: v1
              kind: Pod
              spec:
                  containers:
                  - image: image:latest
              ---
              apiVersion: v1
              kind: Pod
              spec:
                  containers:
                  - image: app:v1.2.3
                  initContainers:
                  - image: account/image:latest
              ---
              apiVersion: v1
              kind: Pod
              spec:
                  containers:
                  - image: repo.id/account/bucket/image:v1.2.3@digest
              """,
            """
              apiVersion: v1
              kind: Pod
              spec:
                  containers:
                  - image: ~~(*/*:*)~~>image:latest
              ---
              apiVersion: v1
              kind: Pod
              spec:
                  containers:
                  - image: ~~(*/*:*)~~>app:v1.2.3
                  initContainers:
                  - image: ~~(*/*:*)~~>account/image:latest
              ---
              apiVersion: v1
              kind: Pod
              spec:
                  containers:
                  - image: ~~(*/*:*)~~>repo.id/account/bucket/image:v1.2.3@digest
              """
          )
        );
    }
}

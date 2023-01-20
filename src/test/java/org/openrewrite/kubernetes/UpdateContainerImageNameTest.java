package org.openrewrite.kubernetes;

import org.junit.jupiter.api.Test;

import static org.openrewrite.yaml.Assertions.yaml;

class UpdateContainerImageNameTest extends KubernetesParserTest {

    @Test
    void updateContainerImageWithAllValues() {
        rewriteRun(
          spec -> spec.recipe(new UpdateContainerImageName(
            null,
            "nginx",
            null,
            "gcr.io/myaccount/myrepo",
            "nginx-custom",
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
            "gcr.io/myaccount/myrepo",
            null,
            null,
            true,
            null
          )),
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
}

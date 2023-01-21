package org.openrewrite.kubernetes.search;

import org.junit.jupiter.api.Test;
import org.openrewrite.kubernetes.KubernetesParserTest;

import static org.openrewrite.yaml.Assertions.yaml;

class FindMissingDigestTest extends KubernetesParserTest {

    @Test
    void detectWhenDigestIsMissing() {
        rewriteRun(
          spec -> spec.recipe(new FindMissingDigest(true, null)),
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
                  - image: account/image:latest@digest
              """,
            """
              apiVersion: v1
              kind: Pod
              spec:
                  containers:
                  - image: ~~(missing digest)~~>image
              ---
              apiVersion: v1
              kind: Pod
              spec:
                  containers:
                  - image: ~~(missing digest)~~>app:v1.2.3
                  initContainers:
                  - image: account/image:latest@digest
              """
          )
        );
    }
}

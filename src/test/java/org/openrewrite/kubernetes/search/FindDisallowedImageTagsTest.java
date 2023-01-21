package org.openrewrite.kubernetes.search;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.openrewrite.kubernetes.KubernetesParserTest;

import java.util.Set;

import static org.openrewrite.yaml.Assertions.yaml;

class FindDisallowedImageTagsTest extends KubernetesParserTest {

    @Disabled("JsonPathMatcher has changed, need to figure out how to fix this.")
    @Test
    void findDisallowedImageTags() {
        rewriteRun(
          spec -> spec.recipe(new FindDisallowedImageTags(
            Set.of("latest", "dev"),
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


    @Test
    void findDisallowedImageTagsInWorkloads() {
        rewriteRun(
          spec -> spec.recipe(new FindDisallowedImageTags(
            Set.of("latest", "dev"),
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
                    - image: ~~(disallowed tag: [latest, dev])~~>nginx:latest
              ---
              apiVersion: apps/v1
              kind: StatefulSet
              spec:
                template:
                  spec:
                    containers:
                    - image: ~~(disallowed tag: [latest, dev])~~>app:dev
              """
          )
        );
    }

    @Test
    void notExecuteOnPathsThatDoNotMatch() {
        rewriteRun(
          spec -> spec.recipe(new FindDisallowedImageTags(
            Set.of("latest", "dev"),
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

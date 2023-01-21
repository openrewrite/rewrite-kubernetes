package org.openrewrite.kubernetes.search;

import org.junit.jupiter.api.Test;
import org.openrewrite.kubernetes.KubernetesParserTest;

import static org.openrewrite.yaml.Assertions.yaml;

class FindMissingOrInvalidAnnotationTest extends KubernetesParserTest {

    @Test
    void findMissingAnnotation() {
        rewriteRun(
          spec -> spec.recipe(new FindMissingOrInvalidAnnotation(
            "mycompany.io/annotation",
            null,
            null
          )),
          yaml(
            """
              apiVersion: v1
              kind: Pod
              metadata:
                name: mypod1
                annotations:
                  mycompany.io/something: "hasvalue"
              ---
              apiVersion: v1
              kind: Pod
              metadata:
                name: mypod2
                annotations:
                  mycompany.io/annotation: "novalue"
              ---
              apiVersion: apps/v1
              kind: Deployment
              spec:
                  template:
                      spec:
                          metadata:
                              annotations:
                                  mycompany.io/something: "hasvalue"
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
                ~~(missing:mycompany.io/annotation)~~>annotations:
                  mycompany.io/something: "hasvalue"
              ---
              apiVersion: v1
              kind: Pod
              metadata:
                name: mypod2
                annotations:
                  mycompany.io/annotation: "novalue"
              ---
              apiVersion: apps/v1
              kind: Deployment
              spec:
                  template:
                      spec:
                          metadata:
                              ~~(missing:mycompany.io/annotation)~~>annotations:
                                  mycompany.io/something: "hasvalue"
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
    void findInvalidAnnotation() {
        rewriteRun(
          spec -> spec.recipe(new FindMissingOrInvalidAnnotation(
            "mycompany.io/annotation",
            "has(.*)",
            null
          )),
          yaml(
            """
              apiVersion: v1
              kind: Pod
              metadata:
                name: mypod1
                annotations:
                  mycompany.io/annotation: "hasvalue"
              ---
              apiVersion: v1
              kind: Pod
              metadata:
                name: mypod2
                annotations:
                  mycompany.io/annotation: "novalue"
              ---
              apiVersion: apps/v1
              kind: Deployment
              spec:
                  template:
                      spec:
                          metadata:
                              annotations:
                                  mycompany.io/annotation: "hasvalue"
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
                annotations:
                  mycompany.io/annotation: "hasvalue"
              ---
              apiVersion: v1
              kind: Pod
              metadata:
                name: mypod2
                annotations:
                  ~~(invalid:has(.*))~~>mycompany.io/annotation: "novalue"
              ---
              apiVersion: apps/v1
              kind: Deployment
              spec:
                  template:
                      spec:
                          metadata:
                              annotations:
                                  mycompany.io/annotation: "hasvalue"
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

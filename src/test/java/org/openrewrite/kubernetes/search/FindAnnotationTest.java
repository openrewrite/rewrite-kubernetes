package org.openrewrite.kubernetes.search;

import org.junit.jupiter.api.Test;
import org.openrewrite.kubernetes.KubernetesParserTest;

import static org.openrewrite.yaml.Assertions.yaml;

class FindAnnotationTest extends KubernetesParserTest {

    @Test
    void findIfAnnotationExists() {
        rewriteRun(
          spec -> spec.recipe(new FindAnnotation(
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
                  mycompany.io/annotation: "hasvalue"
              ---
              apiVersion: v1
              kind: Pod
              metadata:
                name: mypod2
                annotations:
                  mycompany.io/annotation: "novalue"
              """,
            """
              apiVersion: v1
              kind: Pod
              metadata:
                name: mypod1
                annotations:
                  ~~(found:mycompany.io/annotation)~~>mycompany.io/annotation: "hasvalue"
              ---
              apiVersion: v1
              kind: Pod
              metadata:
                name: mypod2
                annotations:
                  ~~(found:mycompany.io/annotation)~~>mycompany.io/annotation: "novalue"
              """
          )
        );
    }

    @Test
    void findByAnnotationValue() {
        rewriteRun(
          spec -> spec.recipe(new FindAnnotation(
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
                  ~~(found:has(.*))~~>mycompany.io/annotation: "hasvalue"
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
                                  ~~(found:has(.*))~~>mycompany.io/annotation: "hasvalue"
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

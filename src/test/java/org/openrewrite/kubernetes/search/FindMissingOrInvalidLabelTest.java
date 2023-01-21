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

package org.openrewrite.kubernetes.resource;

import org.junit.jupiter.api.Test;
import org.openrewrite.kubernetes.KubernetesParserTest;

import static org.openrewrite.yaml.Assertions.yaml;

class CapResourceLimitToMaximumTest extends KubernetesParserTest {

    @Test
    void capResourceLimitToGivenMaximumInDifferentUnits() {
        rewriteRun(
          spec -> spec.recipe(new CapResourceValueToMaximum(
            "limits",
            "memory",
            "64Mi",
            null
          )),
          yaml(
            """
              apiVersion: v1
              kind: Pod
              metadata:
                labels:
                  app: application
              spec:
                containers:
                - image: nginx:latest
                  resources:
                      limits:
                          cpu: "500Mi"
                          memory: "256M"
              """,
            """
              apiVersion: v1
              kind: Pod
              metadata:
                labels:
                  app: application
              spec:
                containers:
                - image: nginx:latest
                  resources:
                      limits:
                          cpu: "500Mi"
                          memory: "67M"
              """
          )
        );
    }

    @Test
    void capResourceRequestsToGivenMaximumInDifferentUnits() {
        rewriteRun(
          spec -> spec.recipe(new CapResourceValueToMaximum(
            "requests",
            "cpu",
            "100Mi",
            null
          )),
          yaml(
            """
              apiVersion: v1
              kind: Pod
              metadata:
                labels:
                  app: application
              spec:
                containers:
                - image: nginx:latest
                  resources:
                      requests:
                          cpu: "500Mi"
                          memory: "256M"
              """,
            """
              apiVersion: v1
              kind: Pod
              metadata:
                labels:
                  app: application
              spec:
                containers:
                - image: nginx:latest
                  resources:
                      requests:
                          cpu: "100Mi"
                          memory: "256M"
              """
          )
        );
    }
}

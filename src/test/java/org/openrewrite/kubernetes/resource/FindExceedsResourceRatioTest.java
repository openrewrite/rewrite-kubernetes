package org.openrewrite.kubernetes.resource;

import org.junit.jupiter.api.Test;
import org.openrewrite.kubernetes.KubernetesParserTest;

import static org.openrewrite.yaml.Assertions.yaml;

class FindExceedsResourceRatioTest extends KubernetesParserTest {

    @Test
    void findLimitsThatExceedAGivenMaximumRatio() {
        rewriteRun(
          spec -> spec.recipe(new FindExceedsResourceRatio(
            "memory",
            "2",
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
                          cpu: "2Gi"
                          memory: "1Gi"
                      requests:
                          cpu: "100Mi"
                          memory: "64m"
                - image: k8s.gcr.io/test-webserver
                  resources:
                      limits:
                          cpu: "2Gi"
                          memory: "1Gi"
                      requests:
                          cpu: "100Mi"
                          memory: "64m"
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
                  ~~(exceeds max memory limits/requests ratio of 2)~~>resources:
                      limits:
                          cpu: "2Gi"
                          memory: "1Gi"
                      requests:
                          cpu: "100Mi"
                          memory: "64m"
                - image: k8s.gcr.io/test-webserver
                  ~~(exceeds max memory limits/requests ratio of 2)~~>resources:
                      limits:
                          cpu: "2Gi"
                          memory: "1Gi"
                      requests:
                          cpu: "100Mi"
                          memory: "64m"
              """
          )
        );
    }
}

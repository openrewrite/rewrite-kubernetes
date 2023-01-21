package org.openrewrite.kubernetes.search;

import org.junit.jupiter.api.Test;
import org.openrewrite.config.Environment;
import org.openrewrite.kubernetes.KubernetesParserTest;

import static org.openrewrite.yaml.Assertions.yaml;

class FindResourceMissingConfigurationTest extends KubernetesParserTest {

    @Test
    void podLivenessProbe() {
        rewriteRun(
          spec -> spec.recipe(new FindResourceMissingConfiguration(
            "Pod",
            "$.spec.containers[*].livenessProbe",
            null
          )),
          yaml(
            """
              apiVersion: v1
              kind: Pod
              spec:
                containers:
                - name: <container name>
                  image: <image>
              """,
            """
              ~~>apiVersion: v1
              kind: Pod
              spec:
                containers:
                - name: <container name>
                  image: <image>
              """
          )
        );
    }

    @Test
    void correctlyConfiguredPodLivenessProbe() {
        rewriteRun(
          spec -> spec.recipe(new FindResourceMissingConfiguration(
            "Pod",
            "$.spec.containers[*].livenessProbe",
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
                  livenessProbe:
                    httpGet:
                      path: /healthz
                      port: 8080
                    initialDelaySeconds: 3
                    periodSeconds: 3
              """
          )
        );
    }

    @Test
    void onlyMatchOnConfiguredResources() {
        rewriteRun(
          spec -> spec.recipe(new FindResourceMissingConfiguration(
            "Pod",
            "..spec.containers[*].livenessProbe",
            null
          )),
          yaml(
            """
              apiVersion: apps/v1
              kind: Deployment
              metadata:
                labels:
                  app: application
              spec:
                template:
                  spec:
                    containers:
                    - image: nginx:latest
                      livenessProbe:
                        httpGet:
                          path: /healthz
                          port: 8080
                        initialDelaySeconds: 3
                        periodSeconds: 3
              """
          )
        );
    }

    @Test
    void missingPodLivenessProbe() {
        rewriteRun(
          spec -> spec.recipe(Environment.builder()
            .scanRuntimeClasspath()
            .build()
            .activateRecipes("org.openrewrite.kubernetes.MissingPodLivenessProbe")),
          yaml(
            """
              apiVersion: apps/v1
              kind: Pod
              metadata:
                labels:
                  app: application
              spec:
                containers:
                - image: nginx:latest
              """,
            """
              ~~>apiVersion: apps/v1
              kind: Pod
              metadata:
                labels:
                  app: application
              spec:
                containers:
                - image: nginx:latest
              """
          )
        );
    }

    @Test
    void missingCpuLimits() {
        rewriteRun(
          spec -> spec.recipe(Environment.builder()
            .scanRuntimeClasspath()
            .build()
            .activateRecipes("org.openrewrite.kubernetes.MissingCpuLimits")),
          yaml(
            """
              apiVersion: apps/v1
              kind: Deployment
              metadata:
                labels:
                  app: application
              spec:
                template:
                  spec:
                    containers:
                    - image: nginx:latest
              """,
            """
              ~~>apiVersion: apps/v1
              kind: Deployment
              metadata:
                labels:
                  app: application
              spec:
                template:
                  spec:
                    containers:
                    - image: nginx:latest
              """
          )
        );
    }
}

package org.openrewrite.kubernetes;

import org.junit.jupiter.api.Test;

import static org.openrewrite.yaml.Assertions.yaml;

class AddConfigurationTest extends KubernetesParserTest {

    @Test
    void addConfigurationToMappingEntryIfThePathDoesNotExist() {
        rewriteRun(
          spec -> spec.recipe(new AddConfiguration(null,
            "Pod",
            "$.spec",
            "privileged: false")),
          yaml(
            """
              apiVersion: v1
              kind: Pod
              metadata:
                name: nginx
              spec:
                containers:
                  - name: nginx
                    image: nginx
              """,
            """
              apiVersion: v1
              kind: Pod
              metadata:
                name: nginx
              spec:
                containers:
                  - name: nginx
                    image: nginx
                privileged: false
              """
          )
        );
    }

    @Test
    void addConfigurationToNestedMappingEntriesIfThePathDoesNotExist() {
        rewriteRun(
          spec -> spec.recipe(new AddConfiguration(null,
            "PodSecurityPolicy",
            "$",
            """
              spec:
                privileged: false
              """)),
          yaml("""
              apiVersion: policy/v1beta1
              kind: PodSecurityPolicy
              metadata:
                name: psp
              """,
            """
              apiVersion: policy/v1beta1
              kind: PodSecurityPolicy
              metadata:
                name: psp
              spec:
                privileged: false
              """
          )
        );
    }

    @Test
    void addConfigurationToSequenceEntriesIfThePathDoesNotExist() {
        rewriteRun(
          spec -> spec.recipe(new AddConfiguration(
            null,
            "Pod",
            "$.spec.containers",
            "imagePullPolicy: Always")),
          yaml("""
              apiVersion: v1
              kind: Pod
              metadata:
                name: example
              spec:
                containers:
                  - name: pod-0
                    image: busybox
                  - name: pod-1
                    image: busybox
              """,
            """
              apiVersion: v1
              kind: Pod
              metadata:
                name: example
              spec:
                containers:
                  - name: pod-0
                    image: busybox
                    imagePullPolicy: Always
                  - name: pod-1
                    image: busybox
                    imagePullPolicy: Always
              """
          )
        );
    }

    @Test
    void doNotAddSequencesIfThePathHasNoExistingEntries() {
        rewriteRun(
          spec -> spec.recipe(new AddConfiguration(
            null,
            "Pod",
            "$.spec.containers",
            "imagePullPolicy: Always"
          )),
          yaml(
            """
              apiVersion: v1
              kind: Pod
              metadata:
                name: example
              spec:
                containers: []
              """
          )
        );
    }

    @Test
    void addConfigurationToMappingEntriesWithinSequenceEntriesIfThePathDoesNotExist() {
        rewriteRun(
          spec -> spec.recipe(new AddConfiguration(
            null,
            "Pod",
            "$.spec.containers",
            """
              securityContext:
                allowPrivilegeEscalation: false
              """)),
          yaml(
            """
              apiVersion: v1
              kind: Pod
              metadata:
                name: nginx
              spec:
                containers:
                - name: nginx
                  image: nginx
              """,
            """
              apiVersion: v1
              kind: Pod
              metadata:
                name: nginx
              spec:
                containers:
                - name: nginx
                  image: nginx
                  securityContext:
                    allowPrivilegeEscalation: false
              """
          )
        );
    }

    @Test
    void doNotModifyExistingConfiguration() {
        rewriteRun(
          spec -> spec.recipe(new AddConfiguration(
            null,
            "PodSecurityPolicy",
            "$.spec",
            "privileged: false"
          )),
          yaml(
            """
                apiVersion: policy/v1beta1
                kind: PodSecurityPolicy
                metadata:
                  name: psp
                spec:
                  privileged: true
              """
          )
        );
    }
}

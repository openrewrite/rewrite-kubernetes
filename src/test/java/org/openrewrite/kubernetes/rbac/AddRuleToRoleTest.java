package org.openrewrite.kubernetes.rbac;

import org.junit.jupiter.api.Test;
import org.openrewrite.kubernetes.KubernetesParserTest;

import java.util.Set;

import static org.openrewrite.yaml.Assertions.yaml;

class AddRuleToRoleTest extends KubernetesParserTest {

    @Test
    void unsupportedApiVersion() {
        rewriteRun(
          spec -> spec.recipe(new AddRuleToRole(
            "ClusterRole",
            "cluster-role",
            Set.of(""),
            Set.of("pods"),
            null,
            Set.of("update"),
            null
          )),
          yaml(
            """
              apiVersion: rbac.authorization.k8s.io/v2
              kind: ClusterRole
              metadata:
                name: cluster-role
              rules:
              - apiGroups: [""]
                resources: ["pods"]
                verbs: ["update"]
              """
          )
        );
    }

    @Test
    void clusterRoleAlreadyContainsRule() {
        rewriteRun(
          spec -> spec.recipe(new AddRuleToRole(
            "ClusterRole",
            "cluster-role",
            Set.of(""),
            Set.of("pods"),
            null,
            Set.of("update"),
            null
          )),
          yaml(
            """
              apiVersion: rbac.authorization.k8s.io/v1
              kind: ClusterRole
              metadata:
                name: cluster-role
              rules:
              - apiGroups: [""]
                resources: ["pods"]
                verbs: ["update"]
              """
          )
        );
    }

    @Test
    void addRuleToClusterRole() {
        rewriteRun(
          spec -> spec.recipe(new AddRuleToRole(
            "ClusterRole",
            "cluster-role",
            Set.of(""),
            Set.of("pods"),
            null,
            Set.of("update"),
            null
          )),
          yaml(
            """
              apiVersion: rbac.authorization.k8s.io/v1
              kind: Role
              metadata:
                namespace: default
                name: namespaced-role
              rules:
              - apiGroups: [""]
                resources: ["pods"]
                verbs: ["get"]
              ---
              apiVersion: rbac.authorization.k8s.io/v1
              kind: ClusterRole
              metadata:
                name: cluster-role
              rules:
              - apiGroups: [""]
                resources: ["pods"]
                verbs: ["list"]
              """,
            """
              apiVersion: rbac.authorization.k8s.io/v1
              kind: Role
              metadata:
                namespace: default
                name: namespaced-role
              rules:
              - apiGroups: [""]
                resources: ["pods"]
                verbs: ["get"]
              ---
              apiVersion: rbac.authorization.k8s.io/v1
              kind: ClusterRole
              metadata:
                name: cluster-role
              rules:
              - apiGroups: [""]
                resources: ["pods"]
                verbs: ["list"]
              - apiGroups: [""]
                resources: ["pods"]
                verbs: ["update"]
              """
          )
        );
    }

    @Test
    void supportGlobbingGorNames() {
        rewriteRun(
          spec -> spec.recipe(new AddRuleToRole(
            "Role",
            "*-role",
            Set.of(""),
            Set.of("pods"),
            null,
            Set.of("update"),
            null
          )),
          yaml(
            """
              apiVersion: rbac.authorization.k8s.io/v1
              kind: Role
              metadata:
                namespace: default
                name: namespaced-role
              rules:
              - apiGroups: [""]
                resources: ["pods"]
                verbs: ["get"]
              ---
              apiVersion: rbac.authorization.k8s.io/v1
              kind: ClusterRole
              metadata:
                name: cluster-role
              rules:
              - apiGroups: [""]
                resources: ["pods"]
                verbs: ["list"]
              """,
            """
              apiVersion: rbac.authorization.k8s.io/v1
              kind: Role
              metadata:
                namespace: default
                name: namespaced-role
              rules:
              - apiGroups: [""]
                resources: ["pods"]
                verbs: ["get"]
              - apiGroups: [""]
                resources: ["pods"]
                verbs: ["update"]
              ---
              apiVersion: rbac.authorization.k8s.io/v1
              kind: ClusterRole
              metadata:
                name: cluster-role
              rules:
              - apiGroups: [""]
                resources: ["pods"]
                verbs: ["list"]
              """
          )
        );
    }
}

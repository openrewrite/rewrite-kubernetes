/*
 *  Copyright 2021 the original author or authors.
 *  <p>
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *  <p>
 *  https://www.apache.org/licenses/LICENSE-2.0
 *  <p>
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package org.openrewrite.kubernetes.rbac

import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import org.openrewrite.kubernetes.KubernetesRecipeTest

class AddRuleToRoleTest : KubernetesRecipeTest {

    @Disabled
    @Test
    fun `must add rule to ClusterRole`() = assertChanged(
        recipe = AddRuleToRole(
            "ClusterRole",
            "cluster-role",
            setOf(""),
            setOf("pods"),
            null,
            setOf("update"),
            null
        ),
        before = """
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
        after = """
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

    @Disabled
    @Test
    fun `must support globbing for names`() = assertChanged(
        recipe = AddRuleToRole(
            "Role",
            "*-role",
            setOf(""),
            setOf("pods"),
            null,
            setOf("update"),
            null
        ),
        before = """
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
        after = """
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

}
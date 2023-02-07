/*
 * Copyright 2021 the original author or authors.
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * https://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.openrewrite.kubernetes;

import org.junit.jupiter.api.Test;
import org.openrewrite.config.Environment;

import static org.openrewrite.yaml.Assertions.yaml;

class KubernetesBestPracticesTest extends KubernetesRecipeTest {

    private static final Environment ENV = Environment.builder()
            .scanRuntimeClasspath()
            .build();

    @Test
    void noPrivilegedContainers() {
        rewriteRun(
          spec -> spec.recipe(ENV.activateRecipes("org.openrewrite.kubernetes.NoPrivilegedContainers")),
          yaml(
            """
              apiVersion: policy/v1beta1
              kind: PodSecurityPolicy
              metadata:
                name: example
              spec:
                seLinux:
                  rule: RunAsAny
              """,
            """
              apiVersion: policy/v1beta1
              kind: PodSecurityPolicy
              metadata:
                name: example
              spec:
                seLinux:
                  rule: RunAsAny
                privileged: false
              """
          )
        );
    }

    @Test
    void lifecycleRuleOnStorageBucket() {
        rewriteRun(
          spec -> spec.recipe(ENV.activateRecipes("org.openrewrite.kubernetes.LifecycleRuleOnStorageBucket")),
          yaml(
            """
              apiVersion: storage.cnrm.cloud.google.com/v1beta1
              kind: StorageBucket
              metadata:
                name: example
              spec:
                bucketPolicyOnly: true
              """,
            """
              apiVersion: storage.cnrm.cloud.google.com/v1beta1
              kind: StorageBucket
              metadata:
                name: example
              spec:
                bucketPolicyOnly: true
                lifecycleRule:
                  - action:
                      type: Delete
                    condition:
                      age: 7
              """
          )
        );
    }

    @Test
    void noHostProcessIdSharing() {
        rewriteRun(
          spec -> spec.recipe(ENV.activateRecipes("org.openrewrite.kubernetes.NoHostProcessIdSharing")),
          yaml(
            """
              apiVersion: v1
              kind: Pod
              metadata:
                name: example
              spec:
                containers:
                  - name: pod
                    image: busybox
              """,
            """
              apiVersion: v1
              kind: Pod
              metadata:
                name: example
              spec:
                containers:
                  - name: pod
                    image: busybox
                hostPID: false
              """
          )
        );
    }

    @Test
    void noHostIPCSharing() {
        rewriteRun(
          spec -> spec.recipe(ENV.activateRecipes("org.openrewrite.kubernetes.NoHostIPCSharing")),
          yaml(
            """
              apiVersion: v1
              kind: Pod
              metadata:
                name: example
              spec:
                containers:
                  - name: pod
                    image: busybox
              """,
            """
              apiVersion: v1
              kind: Pod
              metadata:
                name: example
              spec:
                containers:
                  - name: pod
                    image: busybox
                hostIPC: false
              """
          )
        );
    }

    @Test
    void noRootContainers() {
        rewriteRun(
          spec -> spec.recipe(ENV.activateRecipes("org.openrewrite.kubernetes.NoRootContainers")),
          yaml(
            """
              apiVersion: policy/v1beta1
              kind: PodSecurityPolicy
              metadata:
                name: example
              spec:
                privileged: false
                seLinux:
                  rule: RunAsAny
              """,
            """
              apiVersion: policy/v1beta1
              kind: PodSecurityPolicy
              metadata:
                name: example
              spec:
                privileged: false
                seLinux:
                  rule: RunAsAny
                runAsUser:
                  rule: MustRunAsNonRoot
              """
          )
        );
    }

    @Test
    void imagePullPolicyAlways() {
        rewriteRun(
          spec -> spec.recipe(ENV.activateRecipes("org.openrewrite.kubernetes.ImagePullPolicyAlways")),
          yaml(
            """
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
    void noPrivilegeEscalation() {
        rewriteRun(
          spec -> spec.recipe(ENV.activateRecipes("org.openrewrite.kubernetes.NoPrivilegeEscalation")),
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
    void noHostNetworkSharing() {
        rewriteRun(
          spec -> spec.recipe(ENV.activateRecipes("org.openrewrite.kubernetes.NoHostNetworkSharing")),
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
                hostNetwork: false
              """
          )
        );
    }

    @Test
    void readOnlyRootFilesystem() {
        rewriteRun(
          spec -> spec.recipe(ENV.activateRecipes("org.openrewrite.kubernetes.ReadOnlyRootFilesystem")),
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
                    readOnlyRootFilesystem: false
              """
          )
        );
    }

    @Test
    void limitContainerCapabilities() {
        rewriteRun(
          spec -> spec.recipe(ENV.activateRecipes("org.openrewrite.kubernetes.LimitContainerCapabilities")),
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
                    capabilities:
                      drop:
                        - ALL
              """
          )
        );
    }
}

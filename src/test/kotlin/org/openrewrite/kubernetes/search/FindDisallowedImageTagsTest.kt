package org.openrewrite.kubernetes.search

import org.junit.jupiter.api.Test
import org.openrewrite.kubernetes.KubernetesRecipeTest

class FindDisallowedImageTagsTest : KubernetesRecipeTest {

    @Test
    fun `must find disallowed image tags`() = assertChanged(
        recipe = FindDisallowedImageTags(
            setOf("latest", "dev")
        ),
        before = """
            apiVersion: v1
            kind: Pod
            spec:
                containers:             
                - image: repo/app:latest
            ---
            apiVersion: apps/v1
            kind: Deployment
            spec:
                template:
                    spec:
                        containers:            
                        - name: app
                          image: repo/app:latest
                        - name: sidecar
                          image: repo/sidecar:dev
                        initContainers:            
                        - name: migration
                          image: repo/migration:latest
                        - name: backup
                          image: repo/backup:dev
        """,
        after = """
            apiVersion: v1
            kind: Pod
            spec:
                containers:             
                - image: ~~(disallowed tag: [latest, dev])~~>repo/app:latest
            ---
            apiVersion: apps/v1
            kind: Deployment
            spec:
                template:
                    spec:
                        containers:            
                        - name: app
                          image: ~~(disallowed tag: [latest, dev])~~>repo/app:latest
                        - name: sidecar
                          image: ~~(disallowed tag: [latest, dev])~~>repo/sidecar:dev
                        initContainers:            
                        - name: migration
                          image: ~~(disallowed tag: [latest, dev])~~>repo/migration:latest
                        - name: backup
                          image: ~~(disallowed tag: [latest, dev])~~>repo/backup:dev
        """
    )

    @Test
    fun `must find disallowed image tags in workloads`() = assertChanged(
        recipe = FindDisallowedImageTags(
            setOf("latest", "dev")
        ),
        before = """
            apiVersion: apps/v1
            kind: Deployment
            spec:
              template:
                spec:
                  containers:            
                  - image: nginx:latest
            ---
            apiVersion: apps/v1
            kind: StatefulSet
            spec:
              template:
                spec:
                  containers:            
                  - image: app:dev
        """.trimIndent(),
        after = """
            apiVersion: apps/v1
            kind: Deployment
            spec:
              template:
                spec:
                  containers:            
                  - image: ~~(disallowed tag: [latest, dev])~~>nginx:latest
            ---
            apiVersion: apps/v1
            kind: StatefulSet
            spec:
              template:
                spec:
                  containers:            
                  - image: ~~(disallowed tag: [latest, dev])~~>app:dev
        """.trimIndent()
    )

}
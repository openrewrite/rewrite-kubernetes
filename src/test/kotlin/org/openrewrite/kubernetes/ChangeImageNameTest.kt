package org.openrewrite.kubernetes

import org.junit.jupiter.api.Test

class ChangeImageNameTest : KubernetesRecipeTest {

    @Test
    fun mustChangeImageName() = assertChanged(
        recipe = ChangeImageName(
            "repo.containers",
            "ngninx",
            "latest"
        ),
        before = """
            apiVersion: v1
            kind: Pod
            metadata:
              labels:
                app: application
            spec:
              containers:            
              - name: mycontainer
                image: notnginx
        """,
        after = """
            apiVersion: v1
            kind: Pod
            metadata:
              labels:
                app: application
            spec:
              containers:            
              - name: mycontainer
                image: repo.containers/ngninx:latest
        """
    )

    @Test
    fun mustChangeImageNameWithRepo() = assertChanged(
        recipe = ChangeImageName(
            "repo.containers",
            "ngninx",
            "latest"
        ),
        before = """
            apiVersion: v1
            kind: Pod
            metadata:
              labels:
                app: application
            spec:
              containers:            
              - name: mycontainer
                image: other.repo.containers/nginx
        """,
        after = """
            apiVersion: v1
            kind: Pod
            metadata:
              labels:
                app: application
            spec:
              containers:            
              - name: mycontainer
                image: repo.containers/ngninx:latest
        """
    )

}
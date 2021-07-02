package org.openrewrite.kubernetes

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class ContainerImageTest {

    @Test
    fun `must parse full container image name`() {
        val s = "repo.io/account/bucket/image:v1.2.3@digest"

        val image = ContainerImage(s)
        assertThat(image.imageName.repository).isEqualTo("repo.io/account/bucket")
        assertThat(image.imageName.image).isEqualTo("image")
        assertThat(image.imageName.tag).isEqualTo("v1.2.3")
        assertThat(image.imageName.digest).isEqualTo("digest")
    }

    @Test
    fun `must parse partial container image name`() {
        val s = "bucket/image:latest"

        val image = ContainerImage(s)
        assertThat(image.imageName.repository).isEqualTo("bucket")
        assertThat(image.imageName.image).isEqualTo("image")
        assertThat(image.imageName.tag).isEqualTo("latest")
        assertThat(image.imageName.digest).isNullOrEmpty()
    }

    @Test
    fun `must parse only container image name`() {
        val s = "image"

        val image = ContainerImage(s)
        assertThat(image.imageName.repository).isNullOrEmpty()
        assertThat(image.imageName.image).isEqualTo("image")
        assertThat(image.imageName.tag).isNullOrEmpty()
        assertThat(image.imageName.digest).isNullOrEmpty()
    }

    @Test
    fun `must output correct image name`() {
        val image = ContainerImage.ImageName(
            "repo.io/account/bucket",
            "image",
            "v1.2.3",
            "digest"
        )
        assertThat(image.toString()).isEqualTo("repo.io/account/bucket/image:v1.2.3@digest")
    }

}
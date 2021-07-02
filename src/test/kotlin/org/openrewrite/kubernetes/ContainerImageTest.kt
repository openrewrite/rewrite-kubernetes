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
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

import static org.assertj.core.api.Assertions.assertThat;

class ContainerImageTest {

    @Test
    void parseFullContainerImageName() {
        ContainerImage image = new ContainerImage("repo.io/account/bucket/image:v1.2.3@digest");
        assertThat(image.getImageName().getRepository()).isEqualTo("repo.io/account/bucket");
        assertThat(image.getImageName().getImage()).isEqualTo("image");
        assertThat(image.getImageName().getTag()).isEqualTo("v1.2.3");
        assertThat(image.getImageName().getDigest()).isEqualTo("digest");
    }

    @Test
    void parsePartialContainerImageName() {
        ContainerImage image = new ContainerImage("bucket/image:latest");
        assertThat(image.getImageName().getRepository()).isEqualTo("bucket");
        assertThat(image.getImageName().getImage()).isEqualTo("image");
        assertThat(image.getImageName().getTag()).isEqualTo("latest");
        assertThat(image.getImageName().getDigest()).isNullOrEmpty();
    }

    @Test
    void parseContainerImageNameWithDigestButNoTag() {
        ContainerImage image = new ContainerImage("bucket/image@sha256:cb5c1bddd1b5665e1867a7fa1b5fa843a47ee433bbb75d4293888b71def53229");
        assertThat(image.getImageName().getRepository()).isEqualTo("bucket");
        assertThat(image.getImageName().getImage()).isEqualTo("image");
        assertThat(image.getImageName().getTag()).isNullOrEmpty();
        assertThat(image.getImageName().getDigest()).isEqualTo("sha256:cb5c1bddd1b5665e1867a7fa1b5fa843a47ee433bbb75d4293888b71def53229");
    }

    @Test
    void parseOnlyContainerImageName() {
        ContainerImage image = new ContainerImage("image");
        assertThat(image.getImageName().getRepository()).isNullOrEmpty();
        assertThat(image.getImageName().getImage()).isEqualTo("image");
        assertThat(image.getImageName().getTag()).isNullOrEmpty();
        assertThat(image.getImageName().getDigest()).isNullOrEmpty();
    }

    @Test
    void parseOnlyContainerImageWithDigestNoRepo() {
        ContainerImage image = new ContainerImage("image@sha:1234567890");
        assertThat(image.getImageName().getRepository()).isNullOrEmpty();
        assertThat(image.getImageName().getImage()).isEqualTo("image");
        assertThat(image.getImageName().getTag()).isNullOrEmpty();
        assertThat(image.getImageName().getDigest()).isEqualTo("sha:1234567890");
    }

    @Test
    void parseOnlyContainerImageWithTagDigestNoRepo() {
        ContainerImage image = new ContainerImage("image:v999@sha:1234567890");
        assertThat(image.getImageName().getRepository()).isNullOrEmpty();
        assertThat(image.getImageName().getImage()).isEqualTo("image");
        assertThat(image.getImageName().getTag()).isEqualTo("v999");
        assertThat(image.getImageName().getDigest()).isEqualTo("sha:1234567890");
    }

    @Test
    void outputCorrectImageName() {
        ContainerImage.ImageName image = new ContainerImage.ImageName(
          "repo.io/account/bucket",
          "image",
          "v1.2.3",
          "digest"
        );
        assertThat(image.toString()).isEqualTo("repo.io/account/bucket/image:v1.2.3@digest");
    }
}

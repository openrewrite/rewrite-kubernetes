package org.openrewrite.kubernetes;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class ContainerImageTest {

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
    void parseOnlyContainerImageName() {
        ContainerImage image = new ContainerImage("image");
        assertThat(image.getImageName().getRepository()).isNullOrEmpty();
        assertThat(image.getImageName().getImage()).isEqualTo("image");
        assertThat(image.getImageName().getTag()).isNullOrEmpty();
        assertThat(image.getImageName().getDigest()).isNullOrEmpty();
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

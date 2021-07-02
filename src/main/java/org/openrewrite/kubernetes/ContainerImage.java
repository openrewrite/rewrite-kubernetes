package org.openrewrite.kubernetes;

import lombok.EqualsAndHashCode;
import lombok.Value;
import org.openrewrite.yaml.tree.Yaml;

import static org.openrewrite.internal.StringUtils.isNullOrEmpty;

@Value
@EqualsAndHashCode
public class ContainerImage {

    ImageName imageName;

    public ContainerImage(Yaml.Scalar scalar) {
        this(scalar.getValue());
    }

    public ContainerImage(String imageName) {
        String repository = null;
        String image = imageName;
        String tag = null;
        String digest = null;

        int idx = imageName.lastIndexOf('@');
        if (idx > -1) {
            digest = imageName.substring(idx + 1);
            imageName = imageName.substring(0, idx);
        }
        idx = imageName.lastIndexOf(':');
        if (idx > -1) {
            tag = imageName.substring(idx + 1);
            imageName = imageName.substring(0, idx);
        }
        idx = imageName.lastIndexOf('/');
        if (idx > -1) {
            image = imageName.substring(idx + 1);
            String s = imageName.substring(0, idx);
            if (!isNullOrEmpty(s)) {
                repository = s;
            }
        }
        this.imageName = new ImageName(repository, image, tag, digest);
    }

    @Value
    public static class ImageName {

        String repository;
        String image;
        String tag;
        String digest;

        @Override
        public String toString() {
            String s = "";
            if (!isNullOrEmpty(repository)) {
                s += repository + "/";
            }
            s += image;
            if (!isNullOrEmpty(tag)) {
                s += ":" + tag;
            }
            if (!isNullOrEmpty(digest)) {
                s += "@" + digest;
            }
            return s;
        }
    }

}

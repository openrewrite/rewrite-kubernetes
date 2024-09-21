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

import lombok.EqualsAndHashCode;
import lombok.Value;
import lombok.With;
import org.jspecify.annotations.Nullable;
import org.openrewrite.yaml.tree.Yaml;

import java.nio.file.*;
import java.util.Arrays;
import java.util.Objects;

import static org.openrewrite.internal.StringUtils.isNullOrEmpty;

@Value
@EqualsAndHashCode
public class ContainerImage {

    private static final FileSystem FS = FileSystems.getDefault();

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
            image = imageName;
        }
        idx = imageName.lastIndexOf(':');
        if (idx > -1) {
            image = imageName.substring(0, idx);
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

        @Nullable
        @With
        String repository;

        @With
        @Nullable
        String image;

        @With
        @Nullable
        String tag;

        @With
        @Nullable
        String digest;

        public boolean matches(ImageName otherName) {
            boolean matchesRepo =
                    bothNull(this.getRepository(), otherName.getRepository()) ||
                            Objects.equals(this.getRepository(), otherName.getRepository()) ||
                            isGlobMatch(this.getRepository(), otherName.getRepository());
            boolean matchesImage =
                    bothNull(this.getImage(), otherName.getImage()) ||
                            Objects.equals(this.getImage(), otherName.getImage()) ||
                            isGlobMatch(this.getImage(), otherName.getImage());
            boolean matchesTag =
                    bothNull(this.getTag(), otherName.getTag()) ||
                            Objects.equals(this.getTag(), otherName.getTag()) ||
                            isGlobMatch(this.getTag(), otherName.getTag());
            boolean matchesDigest =
                    bothNull(this.getDigest(), otherName.getDigest()) ||
                            Objects.equals(this.getDigest(), otherName.getDigest()) ||
                            isGlobMatch(this.getDigest(), otherName.getDigest());

            return matchesRepo && matchesImage && matchesTag && matchesDigest;
        }

        public boolean hasDigest() {
            return !isNullOrEmpty(digest);
        }

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
            if (!isNullOrEmpty(digest) && !"*".equals(digest)) {
                s += "@" + digest;
            }
            return s;
        }


        private static boolean bothNull(@Nullable String s1, @Nullable String s2) {
            return s1 == null && s2 == null;
        }

        private static boolean isGlobMatch(@Nullable String s1, @Nullable String s2) {
            if ("*".equals(s2)) {
                return true;
            }
            PathMatcher pm = FS.getPathMatcher("glob:" + s2);
            Path path;
            if (s1 != null && s1.contains("/")) {
                String[] parts = s1.split("/");
                if (parts.length > 1) {
                    path = Paths.get(parts[0], Arrays.copyOfRange(parts, 1, parts.length - 1));
                } else {
                    path = Paths.get(parts[0]);
                }
            } else if (s1 == null) {
                path = Paths.get("");
            } else {
                path = Paths.get(s1);
            }
            return pm.matches(path);
        }
    }

}

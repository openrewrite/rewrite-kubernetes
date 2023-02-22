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
package org.openrewrite.kubernetes.tree;

import org.junit.jupiter.api.Test;
import org.openrewrite.yaml.YamlParser;
import org.openrewrite.yaml.YamlVisitor;
import org.openrewrite.yaml.tree.Yaml;

import java.util.concurrent.atomic.AtomicBoolean;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.openrewrite.kubernetes.tree.K8S.Pod.inSpec;

@SuppressWarnings("YAMLDuplicatedKeys")
class K8STest {

    Yaml.Documents source = YamlParser.builder()
      .build()
      //language=yaml
      .parse("""
            ---
            apiVersion: v1
            kind: Pod
            spec:
              containers:
                - image: image:latest
            ---
            apiVersion: apps/v1
            kind: Deployment
            spec:
              template:
                spec:
                  containers:
                    - image: image:latest
            """)
      .get(0);

    @Test
    void resource() {
        boolean found = new YamlVisitor<AtomicBoolean>() {
            @Override
            public Yaml visitSequenceEntry(Yaml.Sequence.Entry entry, AtomicBoolean found) {
                Yaml.Sequence.Entry e = (Yaml.Sequence.Entry) super.visitSequenceEntry(entry, found);
                if(K8S.inPod(getCursor())) {
                    found.set(true);
                }
                return e;
            }
        }.reduce(source, new AtomicBoolean()).get();

        assertThat(found).isTrue();
    }

    @Test
    void imageName() {
        boolean found = new YamlVisitor<AtomicBoolean>() {
            @Override
            public Yaml visitMappingEntry(Yaml.Mapping.Entry entry, AtomicBoolean found) {
                Yaml.Mapping.Entry e = (Yaml.Mapping.Entry) super.visitMappingEntry(entry, found);
                if(K8S.Containers.isImageName(getCursor())) {
                    found.set(true);
                }
                return e;
            }
        }.reduce(source, new AtomicBoolean()).get();

        assertThat(found).isTrue();
    }

    @Test
    void podSpec() {
        boolean found = new YamlVisitor<AtomicBoolean>() {
            @Override
            public Yaml visitSequenceEntry(Yaml.Sequence.Entry entry, AtomicBoolean found) {
                Yaml.Sequence.Entry e = (Yaml.Sequence.Entry) super.visitSequenceEntry(entry, found);
                if(inSpec(getCursor())) {
                    found.set(true);
                }
                return e;
            }
        }.reduce(source, new AtomicBoolean()).get();

        assertThat(found).isTrue();
    }
}

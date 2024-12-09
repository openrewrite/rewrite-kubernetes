/*
 * Copyright 2024 the original author or authors.
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
package org.openrewrite.kubernetes.search;

import org.junit.jupiter.api.Test;
import org.openrewrite.DocumentExample;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.test.SourceSpecs.text;
import static org.openrewrite.yaml.Assertions.yaml;

class FindHarcodedIPAddressesTest implements RewriteTest {

    @Override
    public void defaults(RecipeSpec spec) {
        spec.recipeFromResource(
          "/META-INF/rewrite/find-hardcoded-ipaddress.yml",
          "org.openrewrite.kubernetes.search.FindHarcodedIPAddresses"
        );
    }

    @DocumentExample
    @Test
    void findInYaml() {
        rewriteRun(
          //language=YAML
          yaml(
            """
              foo:
                bar: 192.168.0.1
              """,
            """
              foo:
                bar: ~~>192.168.0.1
              """
          )
        );
    }

    @Test
    void findInTextOnLine() {
        rewriteRun(
          text(
            """
              192.168.0.1
              """,
            """
              ~~>192.168.0.1
              """
          )
        );
    }

    @Test
    void findInTextInLine() {
        rewriteRun(
          text(
            """
              foo, 192.168.0.1, bar
              """,
            """
              foo, ~~>192.168.0.1, bar
              """
          )
        );
    }

    @Test
    void doNotFind256() {
        rewriteRun(
          text(
            """
              256.168.0.1
              """
          )
        );
    }

    @Test
    void skipSvg() {
        rewriteRun(
          text(
            """
              192.168.0.1
              """,
            spec -> spec.path("foo.svg")
          )
        );
    }
}

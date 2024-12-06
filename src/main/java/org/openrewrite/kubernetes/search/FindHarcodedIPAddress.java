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

import org.openrewrite.Recipe;
import org.openrewrite.text.Find;

import java.util.Collections;
import java.util.List;

public class FindHarcodedIPAddress extends Recipe {

    @Override
    public String getDisplayName() {
        return "Find hardcoded IP address";
    }

    @Override
    public String getDescription() {
        return "Find hardcoded IP address anywhere in text-based files.";
    }

    private static final Find FIND_TEXT = new Find(
            "\\b" + // word boundary
                    "((25[0-5]|2[0-4]\\d|1\\d\\d|[1-9]?\\d)\\.){3}" + // three octets
                    "(25[0-5]|2[0-4]\\d|1\\d\\d|[1-9]?\\d)" + // final octet
                    "\\b", // word boundary
            true,
            null,
            null,
            null,
            null);

    @Override
    public List<Recipe> getRecipeList() {
        return Collections.singletonList(FIND_TEXT);
    }
}

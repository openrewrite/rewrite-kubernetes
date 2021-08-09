/*
 *  Copyright 2021 the original author or authors.
 *  <p>
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *  <p>
 *  https://www.apache.org/licenses/LICENSE-2.0
 *  <p>
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package org.openrewrite.kubernetes.examples;

import io.fabric8.kubernetes.api.model.ListOptionsBuilder;
import io.fabric8.kubernetes.api.model.apiextensions.v1beta1.CustomResourceDefinition;
import io.fabric8.kubernetes.api.model.apiextensions.v1beta1.CustomResourceDefinitionList;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.dsl.NonNamespaceOperation;
import io.fabric8.kubernetes.client.dsl.Resource;
import io.javaoperatorsdk.operator.springboot.starter.OperatorAutoConfiguration;
import org.openrewrite.kubernetes.examples.cr.Application;
import org.openrewrite.kubernetes.examples.cr.ApplicationController;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import static io.fabric8.kubernetes.api.model.HasMetadata.getKind;
import static io.fabric8.kubernetes.client.dsl.base.CustomResourceDefinitionContext.v1beta1CRDFromCustomResourceType;

@Configuration
@ImportAutoConfiguration(OperatorAutoConfiguration.class)
public class OperatorConfiguration {

    @Bean
    public ApplicationController applicationResourceController(KubernetesClient kubernetesClient) {
        NonNamespaceOperation<CustomResourceDefinition, CustomResourceDefinitionList, Resource<CustomResourceDefinition>> crds
                = kubernetesClient.apiextensions().v1beta1().customResourceDefinitions();
        if (crds.list(new ListOptionsBuilder().withKind(getKind(Application.class)).build()).getItems().isEmpty()) {
            CustomResourceDefinition appCrd = v1beta1CRDFromCustomResourceType(Application.class).build();
            crds.createOrReplace(appCrd);
        }
        return new ApplicationController(kubernetesClient);
    }

}

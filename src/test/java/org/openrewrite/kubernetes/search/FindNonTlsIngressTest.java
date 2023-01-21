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
package org.openrewrite.kubernetes.search;

import org.junit.jupiter.api.Test;
import org.openrewrite.kubernetes.KubernetesParserTest;

import static org.openrewrite.yaml.Assertions.yaml;

public class FindNonTlsIngressTest extends KubernetesParserTest {

    @Test
    void findIngressWithNoTLSConfigured() {
        rewriteRun(
          spec -> spec.recipe(new FindNonTlsIngress()),
          yaml(
            """
              apiVersion: extensions/v1beta1
              kind: Ingress
              metadata:
                name: ingress-demo-disallowed
              spec:
                rules:
                  - host: example-host.example.com
                    http:
                      paths:
                        - backend:
                            serviceName: nginx
                            servicePort: 80
              """,
            """
              ~~(missing TLS)~~>~~(missing disallow http)~~>apiVersion: extensions/v1beta1
              kind: Ingress
              metadata:
                name: ingress-demo-disallowed
              spec:
                rules:
                  - host: example-host.example.com
                    http:
                      paths:
                        - backend:
                            serviceName: nginx
                            servicePort: 80
              """
          )
        );
    }

    @Test
    void notFindIfIngressTLSIsConfigured() {
        rewriteRun(
          spec -> spec.recipe(new FindNonTlsIngress()),
          yaml(
            """
              apiVersion: extensions/v1beta1
              kind: Ingress
              metadata:
                name: ingress-demo-disallowed
                annotations:
                  kubernetes.io/ingress.allow-http: false
              spec:
                tls:
                - hosts:
                  - https-example.foo.com
                  secretName: testsecret-tls
                rules:
                  - host: example-host.example.com
                    http:
                      paths:
                        - backend:
                            serviceName: nginx
                            servicePort: 80
              """
          )
        );
    }
}

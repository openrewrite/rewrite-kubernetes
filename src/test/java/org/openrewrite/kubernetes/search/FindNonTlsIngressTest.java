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

package org.openrewrite.kubernetes.services;

import org.junit.jupiter.api.Test;
import org.openrewrite.kubernetes.KubernetesParserTest;

import java.util.Set;

import static org.openrewrite.yaml.Assertions.yaml;

class FindServiceExternalIPTest extends KubernetesParserTest {

    @Test
    void findServicesByExternalIPs() {
        rewriteRun(
          spec -> spec.recipe(new FindServiceExternalIPs(
            Set.of("192.168.0.1", "10.10.10.10"),
            false,
            null
          )),
          yaml(
            """
              apiVersion: v1
              kind: Service
              metadata:
                name: my-service
              spec:
                selector:
                  app: MyApp
                ports:
                  - name: http
                    protocol: TCP
                    port: 80
                    targetPort: 9376
                externalIPs:
                  - 192.168.0.1
              ---
              apiVersion: v1
              kind: Service
              metadata:
                name: my-service
              spec:
                selector:
                  app: MyApp
                ports:
                  - name: http
                    protocol: TCP
                    port: 80
                    targetPort: 9376
                externalIPs:
                  - 10.10.10.1
              """,
            """
              apiVersion: v1
              kind: Service
              metadata:
                name: my-service
              spec:
                selector:
                  app: MyApp
                ports:
                  - name: http
                    protocol: TCP
                    port: 80
                    targetPort: 9376
                ~~(found ip)~~>externalIPs:
                  - 192.168.0.1
              ---
              apiVersion: v1
              kind: Service
              metadata:
                name: my-service
              spec:
                selector:
                  app: MyApp
                ports:
                  - name: http
                    protocol: TCP
                    port: 80
                    targetPort: 9376
                externalIPs:
                  - 10.10.10.1
              """
          )
        );
    }


    @Test
    void findServicesByExternalIPsExclusion() {
        rewriteRun(
          spec -> spec.recipe(new FindServiceExternalIPs(
            Set.of("192.168.0.1", "10.10.10.1"),
            true,
            null
          )),
          yaml(
            """
              apiVersion: v1
              kind: Service
              metadata:
                name: my-service
              spec:
                selector:
                  app: MyApp
                ports:
                  - name: http
                    protocol: TCP
                    port: 80
                    targetPort: 9376
                externalIPs:
                  - 192.168.0.1
              ---
              apiVersion: v1
              kind: Service
              metadata:
                name: my-service
              spec:
                selector:
                  app: MyApp
                ports:
                  - name: http
                    protocol: TCP
                    port: 80
                    targetPort: 9376
                externalIPs:
                  - 10.10.0.1
              """,
            """
              apiVersion: v1
              kind: Service
              metadata:
                name: my-service
              spec:
                selector:
                  app: MyApp
                ports:
                  - name: http
                    protocol: TCP
                    port: 80
                    targetPort: 9376
                externalIPs:
                  - 192.168.0.1
              ---
              apiVersion: v1
              kind: Service
              metadata:
                name: my-service
              spec:
                selector:
                  app: MyApp
                ports:
                  - name: http
                    protocol: TCP
                    port: 80
                    targetPort: 9376
                ~~(missing ip)~~>externalIPs:
                  - 10.10.0.1
              """
          )
        );
    }
}

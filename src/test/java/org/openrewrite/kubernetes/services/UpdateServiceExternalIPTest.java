package org.openrewrite.kubernetes.services;

import org.junit.jupiter.api.Test;
import org.openrewrite.kubernetes.KubernetesParserTest;

import static org.openrewrite.yaml.Assertions.yaml;

class UpdateServiceExternalIPTest extends KubernetesParserTest {


    @Test
    void updateFoundExternalIP() {
        rewriteRun(
          spec -> spec.recipe(new UpdateServiceExternalIP(
            "192.168.0.1",
            "10.10.0.1",
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
                  - 10.10.0.1
              """
          )
        );
    }
}

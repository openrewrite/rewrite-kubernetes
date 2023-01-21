package org.openrewrite.kubernetes.services;

import org.junit.jupiter.api.Test;
import org.openrewrite.kubernetes.KubernetesParserTest;

import static org.openrewrite.yaml.Assertions.yaml;

class FindServicesByTypeTest extends KubernetesParserTest {

    @Test
    void findNodePortServices() {
        rewriteRun(
          spec -> spec.recipe(new FindServicesByType("NodePort", null)),
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
                  - name: https
                    protocol: TCP
                    port: 443
                    targetPort: 9377
              ---
              apiVersion: v1
              kind: Service
              metadata:
                name: my-service
              spec:
                type: NodePort
                selector:
                  app: MyApp
                ports:
                  - port: 80
                    targetPort: 80
                    nodePort: 30007
              ---
              apiVersion: v1
              kind: Service
              metadata:
                name: my-service
              spec:
                selector:
                  app: MyApp
                ports:
                  - protocol: TCP
                    port: 80
                    targetPort: 9376
                clusterIP: 10.0.171.239
                type: LoadBalancer
              status:
                loadBalancer:
                  ingress:
                  - ip: 192.0.2.127
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
                  - name: https
                    protocol: TCP
                    port: 443
                    targetPort: 9377
              ---
              apiVersion: v1
              kind: Service
              metadata:
                name: my-service
              ~~(type:NodePort)~~>spec:
                type: NodePort
                selector:
                  app: MyApp
                ports:
                  - port: 80
                    targetPort: 80
                    nodePort: 30007
              ---
              apiVersion: v1
              kind: Service
              metadata:
                name: my-service
              spec:
                selector:
                  app: MyApp
                ports:
                  - protocol: TCP
                    port: 80
                    targetPort: 9376
                clusterIP: 10.0.171.239
                type: LoadBalancer
              status:
                loadBalancer:
                  ingress:
                  - ip: 192.0.2.127
              """
          )
        );
    }

    @Test
    void findClusterIPServices() {
        rewriteRun(
          spec -> spec.recipe(new FindServicesByType("ClusterIP", null)),
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
                  - name: https
                    protocol: TCP
                    port: 443
                    targetPort: 9377
              ---
              apiVersion: v1
              kind: Service
              metadata:
                name: my-service
              spec:
                type: NodePort
                selector:
                  app: MyApp
                ports:
                  - port: 80
                    targetPort: 80
                    nodePort: 30007
              ---
              apiVersion: v1
              kind: Service
              metadata:
                name: my-service
              spec:
                selector:
                  app: MyApp
                ports:
                  - protocol: TCP
                    port: 80
                    targetPort: 9376
                clusterIP: 10.0.171.239
                type: LoadBalancer
              status:
                loadBalancer:
                  ingress:
                  - ip: 192.0.2.127
              """,
            """
              apiVersion: v1
              kind: Service
              metadata:
                name: my-service
              ~~(type:ClusterIP)~~>spec:
                selector:
                  app: MyApp
                ports:
                  - name: http
                    protocol: TCP
                    port: 80
                    targetPort: 9376
                  - name: https
                    protocol: TCP
                    port: 443
                    targetPort: 9377
              ---
              apiVersion: v1
              kind: Service
              metadata:
                name: my-service
              spec:
                type: NodePort
                selector:
                  app: MyApp
                ports:
                  - port: 80
                    targetPort: 80
                    nodePort: 30007
              ---
              apiVersion: v1
              kind: Service
              metadata:
                name: my-service
              spec:
                selector:
                  app: MyApp
                ports:
                  - protocol: TCP
                    port: 80
                    targetPort: 9376
                clusterIP: 10.0.171.239
                type: LoadBalancer
              status:
                loadBalancer:
                  ingress:
                  - ip: 192.0.2.127
              """
          )
        );
    }
}

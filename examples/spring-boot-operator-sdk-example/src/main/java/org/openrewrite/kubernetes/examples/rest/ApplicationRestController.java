package org.openrewrite.kubernetes.examples.rest;

import io.fabric8.kubernetes.api.model.KubernetesResourceList;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.dsl.MixedOperation;
import io.fabric8.kubernetes.client.dsl.Resource;
import org.openrewrite.kubernetes.examples.cr.Application;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
public class ApplicationRestController {

    private final KubernetesClient kubernetesClient;

    public ApplicationRestController(KubernetesClient kubernetesClient) {
        this.kubernetesClient = kubernetesClient;
    }

    @GetMapping(path = "/hello", produces = MediaType.APPLICATION_JSON_VALUE)
    public Map<String, String> sayHello() {
        Map<String, String> m = new HashMap<>();
        m.put("hello", "world!");
        return m;
    }

    @GetMapping(path = "/apps", produces = MediaType.APPLICATION_JSON_VALUE)
    public Map<String, Object> getApplicationInfo() {
        Map<String, Object> m = new HashMap<>();
        MixedOperation<Application, KubernetesResourceList<Application>, Resource<Application>> apps = kubernetesClient.customResources(Application.class);
        m.put("apps", apps.list().getItems().stream()
                .map(a -> a.getMetadata().getName())
                .collect(Collectors.toList()));
        return m;
    }

}

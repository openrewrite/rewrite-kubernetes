package org.openrewrite.kubernetes.examples.cr;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLMapper;
import io.fabric8.kubernetes.api.model.*;
import io.fabric8.kubernetes.api.model.apps.Deployment;
import io.fabric8.kubernetes.api.model.apps.DeploymentBuilder;
import io.fabric8.kubernetes.api.model.apps.DeploymentList;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.Watcher;
import io.fabric8.kubernetes.client.WatcherException;
import io.fabric8.kubernetes.client.dsl.MixedOperation;
import io.fabric8.kubernetes.client.dsl.NonNamespaceOperation;
import io.fabric8.kubernetes.client.dsl.RollableScalableResource;
import io.fabric8.kubernetes.client.dsl.ServiceResource;
import io.javaoperatorsdk.operator.api.Context;
import io.javaoperatorsdk.operator.api.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static io.javaoperatorsdk.operator.api.UpdateControl.noUpdate;
import static io.javaoperatorsdk.operator.api.UpdateControl.updateCustomResourceAndStatus;

@Controller
public class ApplicationController implements ResourceController<Application> {

    private static final Logger LOGGER = LoggerFactory.getLogger(ApplicationController.class);

    private final KubernetesClient kubernetesClient;
    private final ObjectMapper mapper;

    public ApplicationController(KubernetesClient kubernetesClient) {
        this.kubernetesClient = kubernetesClient;
        this.mapper = new YAMLMapper();
    }

    @Override
    public UpdateControl<Application> createOrUpdateResource(Application resource, Context<Application> context) {
        LOGGER.info("createOrUpdateResource: {}/{}", resource.getMetadata().getNamespace(), resource.getMetadata().getName());

        Map<String, String> matchLabels = new HashMap<>();
        matchLabels.put("app", resource.getSpec().getName());

        OwnerReference ownerRef = new OwnerReferenceBuilder()
                .withApiVersion(resource.getApiVersion())
                .withKind(resource.getKind())
                .withName(resource.getMetadata().getName())
                .withUid(resource.getMetadata().getUid())
                .build();
        List<ObjectReference> resourceList = new ArrayList<>();

        MixedOperation<Deployment, DeploymentList, RollableScalableResource<Deployment>> deployments
                = kubernetesClient.apps().deployments();
        boolean deploymentExists = deployments.inNamespace(resource.getMetadata().getNamespace()).list().getItems().stream()
                .anyMatch(d -> d.getMetadata().getName().equals(resource.getSpec().getName()));
        Deployment d = readDeployment();
        if (!deploymentExists) {
            d = deployments.create(new DeploymentBuilder(d)
                    .editMetadata()
                    .withNamespace(resource.getMetadata().getNamespace())
                    .withName(resource.getSpec().getName())
                    .withLabels(matchLabels)
                    .withOwnerReferences(ownerRef)
                    .endMetadata()
                    .editSpec()
                    .editOrNewSelector()
                    .withMatchLabels(matchLabels)
                    .endSelector()
                    .editTemplate()
                    .editMetadata()
                    .withLabels(matchLabels)
                    .endMetadata()
                    .endTemplate()
                    .endSpec()
                    .build());
            resourceList.add(new ObjectReferenceBuilder()
                    .withApiVersion(d.getApiVersion())
                    .withKind(d.getKind())
                    .withNamespace(d.getMetadata().getNamespace())
                    .withName(d.getMetadata().getName())
                    .withUid(d.getMetadata().getUid())
                    .build());

            kubernetesClient.pods().watch(new Watcher<>() {
                @Override
                public void eventReceived(Action action, Pod resource) {
                    switch (action) {
                        case ADDED:
                            LOGGER.info("ADDED: {}", resource.getMetadata().getName());
                            break;
                        case MODIFIED:
                            LOGGER.info("MODIFIED: {}", resource.getMetadata().getName());
                            break;
                        case DELETED:
                            LOGGER.info("DELETED: {}", resource.getMetadata().getName());
                            break;
                        case ERROR:
                            LOGGER.info("ERROR: {}", resource.getMetadata().getName());
                            break;
                    }
                }

                @Override
                public void onClose(WatcherException cause) {
                    LOGGER.error(cause.getMessage(), cause);
                }
            });
        }

        NonNamespaceOperation<Service, ServiceList, ServiceResource<Service>> svcs
                = kubernetesClient.services().inNamespace(resource.getMetadata().getNamespace());
        boolean serviceExists = svcs.list().getItems().stream()
                .anyMatch(s -> s.getMetadata().getName().equals(resource.getSpec().getName()));
        Service s = readService();
        if (!serviceExists) {
            s = svcs.create(new ServiceBuilder(s)
                    .editMetadata()
                    .withNamespace(resource.getMetadata().getNamespace())
                    .withName(resource.getSpec().getName())
                    .withLabels(matchLabels)
                    .withOwnerReferences(ownerRef)
                    .endMetadata()
                    .editSpec()
                    .editFirstPort()
                    .withPort(resource.getSpec().getPort())
                    .withTargetPort(new IntOrString(8080))
                    .endPort()
                    .withSelector(matchLabels)
                    .endSpec()
                    .build());
            resourceList.add(new ObjectReferenceBuilder()
                    .withApiVersion(s.getApiVersion())
                    .withKind(s.getKind())
                    .withNamespace(s.getMetadata().getNamespace())
                    .withName(s.getMetadata().getName())
                    .withUid(s.getMetadata().getUid())
                    .build());
        }

        if (!resourceList.isEmpty()) {
            resource.setStatus(new ApplicationStatus(resourceList));
            return updateCustomResourceAndStatus(resource);
        } else {
            return noUpdate();
        }
    }

    @Override
    public DeleteControl deleteResource(Application resource, Context<Application> context) {
        LOGGER.info("deleteResource: {} ({})", resource, context);

        kubernetesClient.services()
                .inNamespace(resource.getMetadata().getNamespace())
                .withName(resource.getSpec().getName())
                .delete();
        kubernetesClient.apps().deployments()
                .inNamespace(resource.getMetadata().getNamespace())
                .withName(resource.getSpec().getName())
                .delete();

        return DeleteControl.DEFAULT_DELETE;
    }

    private Deployment readDeployment() {
        return readYaml("/nginx-deployment.yaml", Deployment.class);
    }

    private Service readService() {
        return readYaml("/nginx-service.yaml", Service.class);
    }

    private <H extends HasMetadata> H readYaml(String resourceUrl, Class<H> type) {
        try {
            return mapper.readValue(ApplicationController.class.getResourceAsStream(resourceUrl), type);
        } catch (IOException e) {
            throw new IllegalStateException(e.getMessage(), e);
        }
    }

}

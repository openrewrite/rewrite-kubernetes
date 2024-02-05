package org.openrewrite.kubernetes.examples.cr;

import io.fabric8.kubernetes.api.model.Namespaced;
import io.fabric8.kubernetes.client.CustomResource;
import io.fabric8.kubernetes.model.annotation.*;

@Group("kubernetes.openrewrite.org")
@Kind("Application")
@Version("v1alpha1")
@Plural("applications")
@Singular("application")
@ShortNames("app")
public class Application extends CustomResource<ApplicationSpec, ApplicationStatus> implements Namespaced {
}

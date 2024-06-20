package org.openrewrite.kubernetes.examples.cr;


import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.fabric8.kubernetes.api.model.ObjectReference;
import org.openrewrite.internal.lang.Nullable;

import java.util.List;

public class ApplicationStatus {

    private final List<ObjectReference> resources;

    @JsonCreator
    public ApplicationStatus(@Nullable @JsonProperty("resources") List<ObjectReference> resources) {
        this.resources = resources;
    }

    public List<ObjectReference> getResources() {
        return resources;
    }

}

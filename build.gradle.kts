plugins {
    id("org.openrewrite.build.recipe-library") version "latest.release"
}

group = "org.openrewrite.recipe"
description = "A rewrite module automating best practices and migrations for Kubernetes"

var rewriteVersion = if(project.hasProperty("releasing")) {
    "latest.release"
} else {
    "latest.integration"
}

dependencies {
    compileOnly("org.projectlombok:lombok:latest.release")
    annotationProcessor("org.projectlombok:lombok:latest.release")

    implementation(platform("org.openrewrite:rewrite-bom:${rewriteVersion}"))
    implementation("org.openrewrite:rewrite-core")
    implementation("org.openrewrite:rewrite-yaml")

    testImplementation("org.openrewrite:rewrite-test")
    testImplementation("org.assertj:assertj-core:latest.release")
    testImplementation("org.junit.jupiter:junit-jupiter-api:latest.release")
    testImplementation("org.junit.jupiter:junit-jupiter-params:latest.release")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:latest.release")
}

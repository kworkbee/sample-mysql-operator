package me.tommy.sample_mysql_operator.resource

import io.fabric8.kubernetes.api.model.OwnerReference
import io.fabric8.kubernetes.api.model.OwnerReferenceBuilder
import me.tommy.sample_mysql_operator.resource.domain.common.ImageSpec

const val LABEL_MANAGED_KEY = "managed"
const val LABEL_MANAGED_VALUE = "true"
const val LABEL_APP_KEY = "app"
const val MANAGED_LABEL_SELECTOR = "managed=true"

fun ServiceComponent.createOwnerReferences(): OwnerReference = OwnerReferenceBuilder()
        .withApiVersion(apiVersion)
        .withKind(kind)
        .withName(metadata.name)
        .withUid(metadata.uid)
        .withBlockOwnerDeletion(true)
        .withController(true)
        .build()

fun ServiceComponent.mysqlSecretName(): String = "${metadata.name}-mysql-secret"

fun ServiceComponent.appLabelValue(): String = metadata.name

fun managedLabels(): Map<String, String> = mapOf(LABEL_MANAGED_KEY to LABEL_MANAGED_VALUE)

fun appLabels(appName: String): Map<String, String> = mapOf(LABEL_APP_KEY to appName)

fun ImageSpec.createImagePath(): String = "${registry}/${imageName}:${tag}"

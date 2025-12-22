package me.tommy.sample_mysql_operator.resource.dependent.mysql

import io.fabric8.kubernetes.api.model.apps.StatefulSet
import io.fabric8.kubernetes.api.model.apps.StatefulSetBuilder
import io.javaoperatorsdk.operator.api.reconciler.Context
import io.javaoperatorsdk.operator.processing.dependent.kubernetes.CRUDKubernetesDependentResource
import me.tommy.sample_mysql_operator.resource.ServiceComponent
import me.tommy.sample_mysql_operator.resource.appLabelValue
import me.tommy.sample_mysql_operator.resource.appLabels
import me.tommy.sample_mysql_operator.resource.createImagePath
import me.tommy.sample_mysql_operator.resource.createOwnerReferences
import me.tommy.sample_mysql_operator.resource.managedLabels
import me.tommy.sample_mysql_operator.resource.mysqlSecretName

class MySQLStatefulSet: CRUDKubernetesDependentResource<StatefulSet, ServiceComponent>() {

    companion object {
        const val MYSQL_CONTAINER_NAME = "mysql"
        const val ENV_MYSQL_ROOT_PASSWORD = "MYSQL_ROOT_PASSWORD"
        const val SECRET_KEY_PASSWORD = "password"
    }

    override fun desired(
        primary: ServiceComponent?,
        context: Context<ServiceComponent?>?
    ): StatefulSet? {
        val resource = requireNotNull(primary)
        val mysqlSpec = resource.spec.mysql
        val imageSpec = mysqlSpec.image
        val appName = resource.appLabelValue()
        val secretName = resource.mysqlSecretName()

        return StatefulSetBuilder()
            .withNewMetadata()
                .withName(resource.metadata.name)
                .withNamespace(resource.metadata.namespace)
                .withOwnerReferences(resource.createOwnerReferences())
                .withLabels<String, String>(managedLabels() + appLabels(appName))
            .endMetadata()
            .withNewSpec()
                .withReplicas(1)
                .withNewSelector()
                    .withMatchLabels<String, String>(appLabels(appName))
                .endSelector()
                .withNewTemplate()
                    .withNewMetadata()
                        .withLabels<String, String>(appLabels(appName))
                    .endMetadata()
                    .withNewSpec()
                        .addNewContainer()
                            .withName(MYSQL_CONTAINER_NAME)
                            .withImage(imageSpec.createImagePath())
                            .addNewEnv()
                                .withName(ENV_MYSQL_ROOT_PASSWORD)
                                .withNewValueFrom()
                                    .withNewSecretKeyRef()
                                        .withName(secretName)
                                        .withKey(SECRET_KEY_PASSWORD)
                                    .endSecretKeyRef()
                                .endValueFrom()
                            .endEnv()
                            .addNewPort()
                            .withContainerPort(mysqlSpec.port)
                            .endPort()
                        .endContainer()
                    .endSpec()
                .endTemplate()
            .endSpec()
            .build()
    }
}

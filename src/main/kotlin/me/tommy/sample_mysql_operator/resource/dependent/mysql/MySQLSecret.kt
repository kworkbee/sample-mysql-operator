package me.tommy.sample_mysql_operator.resource.dependent.mysql

import io.fabric8.kubernetes.api.model.Secret
import io.fabric8.kubernetes.api.model.SecretBuilder
import io.javaoperatorsdk.operator.api.config.informer.Informer
import io.javaoperatorsdk.operator.api.reconciler.Context
import io.javaoperatorsdk.operator.processing.dependent.kubernetes.CRUDKubernetesDependentResource
import io.javaoperatorsdk.operator.processing.dependent.kubernetes.KubernetesDependent
import me.tommy.sample_mysql_operator.resource.ServiceComponent
import me.tommy.sample_mysql_operator.resource.MANAGED_LABEL_SELECTOR
import me.tommy.sample_mysql_operator.resource.createOwnerReferences
import me.tommy.sample_mysql_operator.resource.managedLabels
import me.tommy.sample_mysql_operator.resource.mysqlSecretName

@KubernetesDependent(informer = Informer(labelSelector = MANAGED_LABEL_SELECTOR))
class MySQLSecret : CRUDKubernetesDependentResource<Secret, ServiceComponent>() {

    override fun desired(
        primary: ServiceComponent?,
        context: Context<ServiceComponent?>?
    ): Secret? {
        val resource = requireNotNull(primary)
        val mysqlSpec = resource.spec.mysql

        return SecretBuilder()
            .withNewMetadata()
                .withName(resource.mysqlSecretName())
                .withNamespace(resource.metadata.namespace)
                .withOwnerReferences(resource.createOwnerReferences())
                .withLabels<String, String>(managedLabels())
            .endMetadata()
            .withStringData<String, String>(
                mapOf(
                    "username" to mysqlSpec.username,
                    "password" to mysqlSpec.password,
                )
            )
            .build()
    }
}

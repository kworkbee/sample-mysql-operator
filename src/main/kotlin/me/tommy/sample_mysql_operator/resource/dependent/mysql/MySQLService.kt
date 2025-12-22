package me.tommy.sample_mysql_operator.resource.dependent.mysql

import io.fabric8.kubernetes.api.model.IntOrString
import io.fabric8.kubernetes.api.model.Service
import io.fabric8.kubernetes.api.model.ServiceBuilder
import io.javaoperatorsdk.operator.api.config.informer.Informer
import io.javaoperatorsdk.operator.api.reconciler.Context
import io.javaoperatorsdk.operator.processing.dependent.kubernetes.CRUDKubernetesDependentResource
import io.javaoperatorsdk.operator.processing.dependent.kubernetes.KubernetesDependent
import me.tommy.sample_mysql_operator.resource.ServiceComponent
import me.tommy.sample_mysql_operator.resource.MANAGED_LABEL_SELECTOR
import me.tommy.sample_mysql_operator.resource.appLabelValue
import me.tommy.sample_mysql_operator.resource.appLabels
import me.tommy.sample_mysql_operator.resource.createOwnerReferences
import me.tommy.sample_mysql_operator.resource.managedLabels

@KubernetesDependent(informer = Informer(labelSelector = MANAGED_LABEL_SELECTOR))
class MySQLService: CRUDKubernetesDependentResource<Service, ServiceComponent>() {

    override fun desired(
        primary: ServiceComponent?,
        context: Context<ServiceComponent?>?
    ): Service? {
        val resource = requireNotNull(primary)
        val metadata = requireNotNull(resource.metadata)
        val servicePort = resource.spec.mysql.port
        val appName = resource.appLabelValue()

        return ServiceBuilder()
            .withNewMetadata()
            .withName(metadata.name)
            .withNamespace(metadata.namespace)
            .withOwnerReferences(primary.createOwnerReferences())
            .withLabels<String, String>(managedLabels() + appLabels(appName))
            .endMetadata()
            .withNewSpec()
            .addNewPort()
            .withPort(servicePort)
            .withTargetPort(IntOrString(servicePort))
            .endPort()
            .withSelector<String, String>(appLabels(appName))
            .endSpec()
            .build()
    }
}

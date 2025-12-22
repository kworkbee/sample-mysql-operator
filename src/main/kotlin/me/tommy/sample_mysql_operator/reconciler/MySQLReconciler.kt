package me.tommy.sample_mysql_operator.reconciler

import io.fabric8.kubernetes.api.model.Service
import io.fabric8.kubernetes.api.model.apps.StatefulSet
import io.javaoperatorsdk.operator.api.reconciler.Cleaner
import io.javaoperatorsdk.operator.api.reconciler.Context
import io.javaoperatorsdk.operator.api.reconciler.ControllerConfiguration
import io.javaoperatorsdk.operator.api.reconciler.DeleteControl
import io.javaoperatorsdk.operator.api.reconciler.Reconciler
import io.javaoperatorsdk.operator.api.reconciler.UpdateControl
import io.javaoperatorsdk.operator.api.reconciler.Workflow
import io.javaoperatorsdk.operator.api.reconciler.dependent.Dependent
import me.tommy.sample_mysql_operator.resource.ComponentPhase
import me.tommy.sample_mysql_operator.resource.ComponentStatus
import me.tommy.sample_mysql_operator.resource.ServiceComponent
import me.tommy.sample_mysql_operator.resource.dependent.mysql.MySQLSecret
import me.tommy.sample_mysql_operator.resource.dependent.mysql.MySQLService
import me.tommy.sample_mysql_operator.resource.dependent.mysql.MySQLStatefulSet
import me.tommy.sample_mysql_operator.resource.domain.mysql.MySQLStatus
import org.springframework.stereotype.Component
import java.time.Duration
import java.time.Instant

@Component
@Workflow(
    dependents = [
        Dependent(type = MySQLService::class),
        Dependent(type = MySQLSecret::class),
        Dependent(type = MySQLStatefulSet::class),
    ]
)
@ControllerConfiguration
class MySQLReconciler: Reconciler<ServiceComponent>,
    Cleaner<ServiceComponent> {

    private fun buildStatus(
        phase: ComponentPhase,
        reason: String,
        expiresAt: Instant,
        endpoint: String?
    ): ComponentStatus = ComponentStatus(
        mysql = MySQLStatus(
            phase = phase,
            reason = reason,
            expiresAt = expiresAt,
            endpoint = endpoint,
        ),
    )

    override fun reconcile(
        resource: ServiceComponent?,
        context: Context<ServiceComponent?>?
    ): UpdateControl<ServiceComponent?> {
        val primary = requireNotNull(resource)
        val ctx = requireNotNull(context)

        val createdAt = Instant.parse(requireNotNull(primary.metadata.creationTimestamp))
        val ttl = Duration.ofSeconds(primary.spec.mysql.ttl)
        val expiresAt = createdAt.plus(ttl)
        val now = Instant.now()

        if (now.isAfter(expiresAt)) {
            primary.status = buildStatus(
                phase = ComponentPhase.EXPIRED,
                reason = "Expired",
                expiresAt = expiresAt,
                endpoint = null,
            )

            return UpdateControl.patchStatus(primary)
        }

        val remainingTime = Duration.between(now, expiresAt)
        val statefulSet = ctx.getSecondaryResource(StatefulSet::class.java).orElse(null)
        val isReady = (statefulSet?.status?.readyReplicas ?: 0) > 0

        if (isReady) {
            val service = ctx.getSecondaryResource(Service::class.java).orElse(null)
            val serviceName = service?.metadata?.name ?: primary.metadata.name
            val namespace = service?.metadata?.namespace ?: primary.metadata.namespace
            val endpoint = "$serviceName.$namespace.svc.cluster.local:${primary.spec.mysql.port}"

            primary.status = buildStatus(
                phase = ComponentPhase.READY,
                reason = "Running",
                expiresAt = expiresAt,
                endpoint = endpoint,
            )

            return UpdateControl.patchStatus(primary).rescheduleAfter(remainingTime)
        }

        return UpdateControl.noUpdate<ServiceComponent?>().rescheduleAfter(remainingTime)
    }

    override fun cleanup(
        resource: ServiceComponent?,
        context: Context<ServiceComponent?>?
    ): DeleteControl? = DeleteControl.defaultDelete()
}

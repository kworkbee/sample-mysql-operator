package me.tommy.sample_mysql_operator.resource

import io.fabric8.kubernetes.api.model.Namespaced
import io.fabric8.kubernetes.client.CustomResource
import io.fabric8.kubernetes.model.annotation.Group
import io.fabric8.kubernetes.model.annotation.ShortNames
import io.fabric8.kubernetes.model.annotation.Version
import me.tommy.sample_mysql_operator.resource.domain.mysql.MySQLSpec
import me.tommy.sample_mysql_operator.resource.domain.mysql.MySQLStatus
import java.time.Duration
import java.time.Instant

@Group("tommy.me")
@Version("v1")
@ShortNames("sc")
class ServiceComponent : CustomResource<ComponentSpec, ComponentStatus>(), Namespaced {
    override fun initStatus(): ComponentStatus = ComponentStatus(
        mysql = MySQLStatus(
            expiresAt = Instant.now().plus(Duration.ofSeconds(spec?.mysql?.ttl ?: 3_600L))
        ),
    )
}

data class ComponentSpec(
    val mysql: MySQLSpec = MySQLSpec(),
)

data class ComponentStatus(
    val mysql: MySQLStatus = MySQLStatus(),
)

enum class ComponentPhase {
    PENDING,
    READY,
    EXPIRED,
}

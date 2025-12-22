package me.tommy.sample_mysql_operator.resource.domain.mysql

import me.tommy.sample_mysql_operator.resource.ComponentPhase
import java.time.Instant

data class MySQLStatus(
    val phase: ComponentPhase = ComponentPhase.PENDING,
    val reason: String = "Instance is being created",
    val expiresAt: Instant? = null,
    val endpoint: String? = null,
)

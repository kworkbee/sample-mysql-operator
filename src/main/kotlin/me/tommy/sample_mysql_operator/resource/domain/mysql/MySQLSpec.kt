package me.tommy.sample_mysql_operator.resource.domain.mysql

import me.tommy.sample_mysql_operator.resource.domain.common.ImageSpec

data class MySQLSpec(
    val image: ImageSpec = ImageSpec(
        registry = "docker.io",
        imageName = "mysql",
        tag = "8.0",
    ),
    val username: String = "root",
    val password: String = "password",
    val port: Int = 3306,
    val ttl: Long = 3_600L,
)

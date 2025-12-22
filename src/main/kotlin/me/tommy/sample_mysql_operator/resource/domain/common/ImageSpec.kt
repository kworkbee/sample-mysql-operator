package me.tommy.sample_mysql_operator.resource.domain.common

data class ImageSpec(
    val registry: String = "docker.io",
    val imageName: String = "mysql",
    val tag: String = "8.0",
)

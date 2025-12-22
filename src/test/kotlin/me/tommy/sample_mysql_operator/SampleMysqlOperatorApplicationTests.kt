package me.tommy.sample_mysql_operator

import io.fabric8.kubernetes.client.KubernetesClient
import io.javaoperatorsdk.operator.springboot.starter.test.EnableMockOperator
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest

@SpringBootTest
@EnableMockOperator(crdPaths = [
    "classpath:kubernetes/customresourcedefinitions/servicecomponents.tommy.me-v1.yml",
])
class SampleMysqlOperatorApplicationTests {

    @Autowired
    lateinit var client: KubernetesClient

    @Test
    fun serviceComponent_will_be_found() {
        val crd = client.apiextensions()
            .v1()
            .customResourceDefinitions()
            .withName("servicecomponents.tommy.me")
            .get()

        assertThat(crd).isNotNull
    }
}
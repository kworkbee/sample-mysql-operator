import io.fabric8.crdv2.generator.CRDGenerationInfo
import io.fabric8.crdv2.generator.CRDGenerator
import io.fabric8.crd.generator.collector.CustomResourceCollector
import java.nio.file.Files

plugins {
	kotlin("jvm") version "2.2.21"
	kotlin("plugin.spring") version "2.2.21"
	id("org.springframework.boot") version "4.0.1"
	id("io.spring.dependency-management") version "1.1.7"
}

group = "me.tommy"
version = "0.0.1-SNAPSHOT"
description = "Demo Operator"

java {
	toolchain {
		languageVersion = JavaLanguageVersion.of(21)
	}
}

configurations {
	compileOnly {
		extendsFrom(configurations.annotationProcessor.get())
	}
}

repositories {
	mavenCentral()
}

buildscript {
	repositories {
		mavenCentral()
	}
	dependencies {
		classpath("io.fabric8:crd-generator-api-v2:7.0.0")
		classpath("io.fabric8:crd-generator-collector:7.0.0")
	}
}

dependencies {
	implementation("org.springframework.boot:spring-boot-starter")
	implementation("org.jetbrains.kotlin:kotlin-reflect")
	implementation("io.javaoperatorsdk:operator-framework-spring-boot-starter:6.3.1")
	implementation("io.github.oshai:kotlin-logging-jvm:7.0.13")

	developmentOnly("org.springframework.boot:spring-boot-devtools")
	developmentOnly("org.springframework.boot:spring-boot-testcontainers")
	developmentOnly("org.testcontainers:k3s:1.19.1")

	annotationProcessor("org.springframework.boot:spring-boot-configuration-processor")

	testImplementation("org.springframework.boot:spring-boot-starter-test")
	testImplementation("org.jetbrains.kotlin:kotlin-test-junit5")
	testImplementation("io.kotest:kotest-runner-junit5-jvm:6.0.7")
	testImplementation("io.kotest:kotest-assertions-core-jvm:6.0.7")
	testImplementation("io.kotest:kotest-extensions-spring:6.0.7")
	testImplementation("io.javaoperatorsdk:operator-framework-spring-boot-starter-test:6.3.1")

	testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

kotlin {
	compilerOptions {
		freeCompilerArgs.addAll("-Xjsr305=strict", "-Xannotation-default-target=param-property")
	}
}

tasks.withType<Test> {
	dependsOn("generateCrds")
	dependsOn("copyCrdsToSource")
	dependsOn("ensureLocalCluster")
	useJUnitPlatform()
}

val generatedCrdDir = layout.buildDirectory.dir("customresourcedefinitions")
val targetDir = "src/main/resources/kubernetes/customresourcedefinitions"

tasks.register("generateCrds") {
	group = "kubernetes"
	description = "Generate CRDs from compiled custom resource classes"

	dependsOn(tasks.named("classes"))

	val sourceSet = project.extensions.getByType<SourceSetContainer>()["main"]

	doLast {
		val existingClassesDirs = sourceSet.output.classesDirs.filter { it.exists() }.files.toList()

		if (existingClassesDirs.isEmpty()) {
			logger.warn("No compiled classes found. Make sure to run 'classes' task first.")
			return@doLast
		}

		val compileClasspath = sourceSet.compileClasspath.map { it.absolutePath }
		val outputClasspath = existingClassesDirs.map { it.absolutePath }
		val fullClasspath = outputClasspath + compileClasspath

		val outputDir = generatedCrdDir.get().asFile
		Files.createDirectories(outputDir.toPath())

		val collector = CustomResourceCollector()
			.withParentClassLoader(Thread.currentThread().contextClassLoader)
			.withClasspathElements(fullClasspath)
			.withFilesToScan(existingClassesDirs)

		val crdClasses = collector.findCustomResourceClasses()

		if (crdClasses.isEmpty()) {
			println("No CustomResource classes found with @Group and @Version annotations.")
			return@doLast
		}

		val crdGenerator = CRDGenerator()
			.customResourceClasses(crdClasses)
			.inOutputDir(outputDir)

		val crdGenerationInfo: CRDGenerationInfo = crdGenerator.detailedGenerate()

		crdGenerationInfo.crdDetailsPerNameAndVersion.forEach { (crdName, versionToInfo) ->
			println("✅ Generated CRD: $crdName")
			versionToInfo.forEach { (version, info) ->
				println("   └─ $version -> ${info.filePath}")
			}
		}
	}
}

tasks.register<Copy>("copyCrdsToSource") {
	group = "kubernetes"
	description = "Copies generated CRDs to $targetDir"

	dependsOn("generateCrds")

	from(generatedCrdDir)
	into(targetDir)

	doLast {
		println("CRD YAML Files copied to $targetDir")
	}
}

tasks.register("ensureLocalCluster") {
	group = "kubernetes"
	description = "Checks if local kind cluster exists, creates if not."

	doLast {
		fun runCommand(vararg args: String): String {
			val process = ProcessBuilder(*args)
				.redirectErrorStream(true)
				.start()

			val output = process.inputStream.bufferedReader().use { it.readText() }
			process.waitFor()
			return output.trim()
		}

		val clusterName = "dev-cluster"

		println("Checking Kind clusters...")
		val output = try {
			runCommand("kind", "get", "clusters")
		} catch (e: Exception) {
			println("[Error] 'kind' 명령어를 실행할 수 없습니다. PATH를 확인하세요.")
			""
		}

		if (!output.contains(clusterName)) {
			println("Creating local Kind cluster '$clusterName'...")

			runCommand("kind", "create", "cluster", "--name", clusterName)
		} else {
			println("Local Kind cluster '$clusterName' already exists.")
		}

		println("Applying CRDs...")
		val crdResult = runCommand("kubectl", "apply", "-f", "$targetDir/*.yml", "--context", "kind-$clusterName")
		println(crdResult)

	}
}

tasks.named("bootRun") {
	dependsOn("copyCrdsToSource")
	dependsOn("ensureLocalCluster")
}

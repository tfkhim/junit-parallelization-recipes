package com.github.tfkhim.junitParallelizationRecepies.dynamodb

import com.github.tfkhim.junitParallelizationRecipes.DynamoDbEntry
import org.slf4j.LoggerFactory
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Primary
import org.springframework.test.context.DynamicPropertyRegistrar
import org.springframework.test.context.event.annotation.AfterTestMethod
import org.springframework.test.context.event.annotation.BeforeTestMethod
import org.testcontainers.containers.localstack.LocalStackContainer
import org.testcontainers.utility.DockerImageName
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient
import software.amazon.awssdk.enhanced.dynamodb.TableSchema
import software.amazon.awssdk.regions.Region
import software.amazon.awssdk.services.dynamodb.DynamoDbClient
import java.util.UUID

@TestConfiguration
class LocalStackConfiguration {
    @Bean fun randomDynamoDbTables() = RandomDynamoDbTable()

    @Bean
    fun registerTableNames(table: RandomDynamoDbTable) =
        DynamicPropertyRegistrar { registry ->
            log.info("Configuring DynamoDb table name to ${table.tableName}")
            registry.add("application.dynamoDb.tableName") { table.tableName }
        }

    @Bean
    @Primary
    fun dynamoDbTestClient(): DynamoDbClient = container.createClient()
}

class RandomDynamoDbTable {
    val tableName = UUID.randomUUID().toString()

    private val client = container.createClient()
    private val enhancedClient = DynamoDbEnhancedClient.builder().dynamoDbClient(client).build()
    private val table = enhancedClient.table(tableName, TableSchema.fromBean(DynamoDbEntry::class.java))

    @BeforeTestMethod
    fun beforeEach() {
        log.info("Creating DynamoDb table $tableName")
        table.createTable()
        client.waiter().waitUntilTableExists { it.tableName(tableName) }
    }

    @AfterTestMethod
    fun afterEach() {
        log.info("Deleting DynamoDb table $tableName")
        table.deleteTable()
        // does not work reliably
        client.waiter().waitUntilTableNotExists { it.tableName(tableName) }
    }
}

private val log = LoggerFactory.getLogger(LocalStackConfiguration::class.java)

private val container: LocalStackContainer =
    LocalStackContainer(DockerImageName.parse("localstack/localstack:latest"))
        .withServices(LocalStackContainer.Service.DYNAMODB)
        .withEnv("HOSTNAME_EXTERNAL", "localhost")

private fun LocalStackContainer.createClient(): DynamoDbClient {
    container.start()

    val basicCredentials = AwsBasicCredentials.create(this.accessKey, this.secretKey)

    return DynamoDbClient
        .builder()
        .endpointOverride(this.getEndpointOverride(LocalStackContainer.Service.DYNAMODB))
        .region(Region.of(this.region))
        .credentialsProvider(StaticCredentialsProvider.create(basicCredentials))
        .build()
}

package com.github.tfkhim.junitParallelizationRecipes

import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.stereotype.Service
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient
import software.amazon.awssdk.enhanced.dynamodb.TableSchema
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbBean
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbPartitionKey
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbSortKey
import software.amazon.awssdk.regions.Region
import software.amazon.awssdk.services.dynamodb.DynamoDbClient
import java.util.UUID

@Configuration
class DynamoDbConfiguration {
    @Bean
    fun dynamoDbClient(): DynamoDbClient = DynamoDbClient.builder().region(Region.EU_CENTRAL_1).build()

    @Bean
    fun dynamoDbEnhancedClient(client: DynamoDbClient): DynamoDbEnhancedClient =
        DynamoDbEnhancedClient.builder().dynamoDbClient(client).build()
}

@Service
class DynamoDbService(
    dynamoDbClient: DynamoDbEnhancedClient,
    @Value($$"${application.dynamoDb.tableName}")
    tableName: String,
) {
    private val table =
        dynamoDbClient.table(tableName, TableSchema.fromBean(DynamoDbEntry::class.java))

    fun writeEntry() {
        log.info("Writing entry")
        val entry =
            DynamoDbEntry(
                partitionKey = UUID.randomUUID().toString(),
                sortKey = UUID.randomUUID().toString(),
            )
        table.putItem(entry)
    }

    companion object {
        private val log = LoggerFactory.getLogger(DynamoDbService::class.java)
    }
}

@DynamoDbBean
data class DynamoDbEntry(
    @get:DynamoDbPartitionKey var partitionKey: String? = null,
    @get:DynamoDbSortKey var sortKey: String? = null,
)

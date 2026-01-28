package com.github.tfkhim.junitParallelizationRecepies.dynamodb

import com.github.tfkhim.junitParallelizationRecepies.springContextCaching.SpringContextResourceLocksProvider
import com.github.tfkhim.junitParallelizationRecipes.DynamoDbConfiguration
import com.github.tfkhim.junitParallelizationRecipes.DynamoDbEntry
import com.github.tfkhim.junitParallelizationRecipes.DynamoDbService
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.RepeatedTest
import org.junit.jupiter.api.parallel.ResourceLock
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.annotation.Import
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient
import software.amazon.awssdk.enhanced.dynamodb.TableSchema

@ResourceLock(providers = [SpringContextResourceLocksProvider::class])
@SpringBootTest(classes = [DynamoDbService::class, DynamoDbConfiguration::class, LocalStackConfiguration::class])
class DynamoDbServiceTest {
    @Value($$"${application.dynamoDb.tableName}")
    private lateinit var tableName: String

    @Autowired
    private lateinit var enhancedClient: DynamoDbEnhancedClient

    @Autowired
    private lateinit var dynamoDbService: DynamoDbService

    @RepeatedTest(2)
    fun `can write to DynamoDb`() {
        log.info("Outer class test is using table: $tableName")

        dynamoDbService.writeEntry()

        assertThat(readItems()).hasSize(1)
    }

    @Nested
    @Import(AdditionalBean::class)
    inner class DifferentContextCanRunInParallel {
        @RepeatedTest(2)
        fun `can write to DynamoDb`() {
            log.info("Nested class test is using table: $tableName")

            dynamoDbService.writeEntry()

            assertThat(readItems()).hasSize(1)
        }
    }

    private fun readItems(): List<DynamoDbEntry> {
        val table = enhancedClient.table(tableName, TableSchema.fromBean(DynamoDbEntry::class.java))
        return table.scan().flatMap { it.items() }
    }

    companion object {
        private val log = LoggerFactory.getLogger(DynamoDbServiceTest::class.java)
    }
}

class AdditionalBean

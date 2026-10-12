package com.hanix.waterwatch.data.repo

import com.hanix.waterwatch.data.source.HealthConnectDataSource
import com.hanix.waterwatch.data.source.WearSyncDataSource
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class HydrationRepositoryTest {

    @Test
    fun `조회한 총량을 워치에 발행한다`() = runBlocking {
        val wearSyncDataSource = FakeWearSyncDataSource()
        val repository = HydrationRepository(
            healthConnectDataSource = FakeHealthConnectDataSource(totalMl = 250.0),
            wearSyncDataSource = wearSyncDataSource
        )

        assertEquals(250.0, repository.todayTotalMl())
        assertEquals(250.0, wearSyncDataSource.publishedMl)
    }

    @Test
    fun `워치 발행이 실패해도 조회 결과는 그대로 반환한다`() = runBlocking {
        val repository = HydrationRepository(
            healthConnectDataSource = FakeHealthConnectDataSource(totalMl = 250.0),
            wearSyncDataSource = FakeWearSyncDataSource(failOnPublish = true)
        )

        assertEquals(250.0, repository.todayTotalMl())
    }

    @Test
    fun `기록이 없으면 null 을 반환하고 워치에도 null 을 발행한다`() = runBlocking {
        val wearSyncDataSource = FakeWearSyncDataSource()
        val repository = HydrationRepository(
            healthConnectDataSource = FakeHealthConnectDataSource(totalMl = null),
            wearSyncDataSource = wearSyncDataSource
        )

        assertNull(repository.todayTotalMl())
        assertTrue(wearSyncDataSource.published)
        assertNull(wearSyncDataSource.publishedMl)
    }

    @Test
    fun `기록은 Health Connect 에 그대로 전달된다`() = runBlocking {
        val healthConnectDataSource = FakeHealthConnectDataSource(totalMl = null)
        val repository = HydrationRepository(
            healthConnectDataSource = healthConnectDataSource,
            wearSyncDataSource = FakeWearSyncDataSource()
        )

        repository.recordIntake(250.0)

        assertEquals(250.0, healthConnectDataSource.writtenMl)
    }

    @Test
    fun `기록 결과는 요청한 노드에 그대로 회신된다`() = runBlocking {
        val wearSyncDataSource = FakeWearSyncDataSource()
        val repository = HydrationRepository(
            healthConnectDataSource = FakeHealthConnectDataSource(totalMl = null),
            wearSyncDataSource = wearSyncDataSource
        )

        repository.sendRecordResult(nodeId = "node-1", success = false)

        assertEquals("node-1" to false, wearSyncDataSource.sentResult)
    }

    private class FakeHealthConnectDataSource(
        private val totalMl: Double?
    ) : HealthConnectDataSource {

        var writtenMl: Double? = null

        override val isAvailable: Boolean = true

        override suspend fun hasHydrationPermissions(): Boolean = true

        override suspend fun hasAllRequestedPermissions(): Boolean = true

        override suspend fun readTodayTotalMl(): Double? = totalMl

        override suspend fun writeHydrationMl(ml: Double) {
            writtenMl = ml
        }
    }

    private class FakeWearSyncDataSource(
        private val failOnPublish: Boolean = false
    ) : WearSyncDataSource {

        var published: Boolean = false
        var publishedMl: Double? = null
        var sentResult: Pair<String, Boolean>? = null

        override suspend fun publishTodayTotal(ml: Double?) {
            if (failOnPublish) throw IllegalStateException("Wearable API is not available")
            published = true
            publishedMl = ml
        }

        override suspend fun isWatchConnected(): Boolean = false

        override suspend fun sendRecordResult(nodeId: String, success: Boolean) {
            sentResult = nodeId to success
        }
    }
}

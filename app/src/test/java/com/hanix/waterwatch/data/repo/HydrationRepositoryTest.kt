package com.hanix.waterwatch.data.repo

import com.hanix.waterwatch.data.source.GoalDataSource
import com.hanix.waterwatch.data.source.HealthConnectDataSource
import com.hanix.waterwatch.data.source.WearSyncDataSource
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
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
            wearSyncDataSource = wearSyncDataSource,
            goalDataSource = FakeGoalDataSource()
        )

        assertEquals(250.0, repository.todayTotalMl())
        assertEquals(250.0, wearSyncDataSource.publishedMl)
        assertEquals(GOAL_ML, wearSyncDataSource.publishedGoalMl)
    }

    @Test
    fun `목표를 바꾸면 새 목표를 오늘 총량과 함께 워치에 발행한다`() = runBlocking {
        val wearSyncDataSource = FakeWearSyncDataSource()
        val repository = HydrationRepository(
            healthConnectDataSource = FakeHealthConnectDataSource(totalMl = 250.0),
            wearSyncDataSource = wearSyncDataSource,
            goalDataSource = FakeGoalDataSource()
        )

        repository.setGoalMl(2_500)

        assertEquals(2_500, repository.goalMl.first())
        assertEquals(250.0, wearSyncDataSource.publishedMl)
        assertEquals(2_500, wearSyncDataSource.publishedGoalMl)
    }

    @Test
    fun `워치 발행이 실패해도 조회 결과는 그대로 반환한다`() = runBlocking {
        val repository = HydrationRepository(
            healthConnectDataSource = FakeHealthConnectDataSource(totalMl = 250.0),
            wearSyncDataSource = FakeWearSyncDataSource(failOnPublish = true),
            goalDataSource = FakeGoalDataSource()
        )

        assertEquals(250.0, repository.todayTotalMl())
    }

    @Test
    fun `기록이 없으면 null 을 반환하고 워치에도 null 을 발행한다`() = runBlocking {
        val wearSyncDataSource = FakeWearSyncDataSource()
        val repository = HydrationRepository(
            healthConnectDataSource = FakeHealthConnectDataSource(totalMl = null),
            wearSyncDataSource = wearSyncDataSource,
            goalDataSource = FakeGoalDataSource()
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
            wearSyncDataSource = FakeWearSyncDataSource(),
            goalDataSource = FakeGoalDataSource()
        )

        repository.recordIntake(250.0)

        assertEquals(250.0, healthConnectDataSource.writtenMl)
    }

    @Test
    fun `기록하면 폰 화면이 다시 읽도록 바뀌었다는 신호를 보낸다`() = runBlocking {
        val repository = HydrationRepository(
            healthConnectDataSource = FakeHealthConnectDataSource(totalMl = null),
            wearSyncDataSource = FakeWearSyncDataSource(),
            goalDataSource = FakeGoalDataSource()
        )
        val changed = async(start = CoroutineStart.UNDISPATCHED) { repository.intakeChanged.first() }

        repository.recordIntake(250.0)

        withTimeout(1_000) { changed.await() }
    }

    @Test
    fun `기록 결과는 요청한 노드에 그대로 회신된다`() = runBlocking {
        val wearSyncDataSource = FakeWearSyncDataSource()
        val repository = HydrationRepository(
            healthConnectDataSource = FakeHealthConnectDataSource(totalMl = null),
            wearSyncDataSource = wearSyncDataSource,
            goalDataSource = FakeGoalDataSource()
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

        override suspend fun deleteLatestTodayRecord(): Boolean = false
    }

    private class FakeWearSyncDataSource(
        private val failOnPublish: Boolean = false
    ) : WearSyncDataSource {

        var published: Boolean = false
        var publishedMl: Double? = null
        var publishedGoalMl: Int? = null
        var sentResult: Pair<String, Boolean>? = null

        override suspend fun publishToday(totalMl: Double?, goalMl: Int) {
            if (failOnPublish) throw IllegalStateException("Wearable API is not available")
            published = true
            publishedMl = totalMl
            publishedGoalMl = goalMl
        }

        override suspend fun isWatchConnected(): Boolean = false

        override suspend fun sendRecordResult(nodeId: String, success: Boolean) {
            sentResult = nodeId to success
        }
    }

    private class FakeGoalDataSource : GoalDataSource {
        override val goalMl = MutableStateFlow(GOAL_ML)

        override suspend fun setGoalMl(ml: Int) {
            goalMl.value = ml
        }
    }

    private companion object {
        const val GOAL_ML = 2_000
    }
}

package com.hanix.waterwatch.service

import com.hanix.waterwatch.common.PATH_LOG_HYDRATION
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/**
 * 매니페스트는 Kotlin 상수를 참조할 수 없어 [PATH_LOG_HYDRATION] 이 intent-filter 에 복제돼 있다.
 * 두 값이 어긋나면 컴파일·런타임 에러 없이 서비스가 기동하지 않고 메시지가 조용히 사라지므로,
 * 그 실패를 여기서 잡는다.
 */
class HydrationLogPathTest {

    @Test
    fun `매니페스트의 pathPrefix 가 PATH_LOG_HYDRATION 과 일치한다`() {
        val manifest = File("src/main/AndroidManifest.xml")
        assertTrue("매니페스트를 찾을 수 없음: ${manifest.absolutePath}", manifest.exists())

        val expected = """android:pathPrefix="$PATH_LOG_HYDRATION""""
        assertTrue(
            "매니페스트에 $expected 가 없음. PATH_LOG_HYDRATION 을 바꿨다면 매니페스트도 함께 바꿔야 한다.",
            manifest.readText().contains(expected)
        )
    }
}

package com.hanix.waterwatch.ui.hydration

import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import com.hanix.waterwatch.locator.ServiceLocator
import com.hanix.waterwatch.ui.onboarding.GoalGuideScreen
import com.hanix.waterwatch.ui.onboarding.PermissionGuideRoute
import com.hanix.waterwatch.ui.theme.WaterWatchTheme
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable

class HydrationActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // 테마를 라이트로 고정해서 시스템 바 아이콘도 다크 모드와 상관없이 어두운 색으로 맞춘다.
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.light(Color.TRANSPARENT, Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.light(Color.TRANSPARENT, Color.TRANSPARENT)
        )
        setContent {
            WaterWatchTheme {
                ScreenContents()
            }
        }
    }

    @Composable
    fun ScreenContents() {
        val onboardingCompleted by ServiceLocator.onboardingDataSource.isCompleted
            .collectAsStateWithLifecycle(initialValue = null)
        // 저장된 값을 읽기 전에 그리면 안내 화면이 잠깐 비쳤다가 메인으로 바뀐다.
        val completed = onboardingCompleted ?: return
        // 시작 화면은 처음 정한 값으로 고정한다. 온보딩 중에 값이 바뀌어도 NavHost 를 다시 만들지 않는다.
        val startDestination: Any = remember { if (completed) Main() else PermissionGuide }

        val navController = rememberNavController()
        val scope = rememberCoroutineScope()

        // 배경은 상태 바·내비게이션 바 뒤까지 깔고, 시스템 바 여백은 각 화면이 내용에만 준다.
        NavHost(
            navController = navController,
            startDestination = startDestination
        ) {
            composable<PermissionGuide> {
                PermissionGuideRoute(
                    onGranted = {
                        navController.navigate(GoalGuide) {
                            popUpTo<PermissionGuide> { inclusive = true }
                        }
                    }
                )
            }
            composable<GoalGuide> {
                val goalMl by ServiceLocator.hydrationRepository.goalMl.collectAsStateWithLifecycle(initialValue = null)
                // 저장된 목표(없으면 기본값)를 읽은 뒤에 그려야 시트가 그 값으로 열린다.
                goalMl?.let { initialGoalMl ->
                    GoalGuideScreen(
                        initialGoalMl = initialGoalMl,
                        onGoalSelected = { goal ->
                            // 온보딩은 목표까지 정해야 끝난다. 여기서 앱을 끄면 다음에 권한 안내부터 다시 본다.
                            scope.launch {
                                ServiceLocator.hydrationRepository.setGoalMl(goal)
                                ServiceLocator.onboardingDataSource.complete()
                                navController.navigate(Main(firstEntry = true)) {
                                    popUpTo<GoalGuide> { inclusive = true }
                                }
                            }
                        }
                    )
                }
            }
            composable<Main> { entry ->
                HydrationRoute(firstEntry = entry.toRoute<Main>().firstEntry)
            }
        }
    }
}

@Serializable
private object PermissionGuide

@Serializable
private object GoalGuide

/** @property firstEntry 온보딩을 막 마치고 들어왔는지 */
@Serializable
private data class Main(val firstEntry: Boolean = false)

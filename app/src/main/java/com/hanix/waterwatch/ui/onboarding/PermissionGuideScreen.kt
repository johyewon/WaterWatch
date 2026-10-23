package com.hanix.waterwatch.ui.onboarding

import android.app.Activity
import android.content.Intent
import androidx.activity.compose.LocalActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.app.ActivityCompat
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.PermissionController
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.hanix.waterwatch.data.source.HealthConnectDataSource
import com.hanix.waterwatch.designsystem.Accent
import com.hanix.waterwatch.designsystem.BodyText
import com.hanix.waterwatch.designsystem.CaptionText
import com.hanix.waterwatch.designsystem.HydrationBackground
import com.hanix.waterwatch.designsystem.HydrationBackgroundSpec
import com.hanix.waterwatch.locator.ServiceLocator
import com.hanix.waterwatch.ui.component.HydrationButton
import com.hanix.waterwatch.ui.component.HydrationButtonStyle
import com.hanix.waterwatch.ui.hydration.Duck
import com.hanix.waterwatch.ui.theme.WaterWatchTheme

// 크기는 디자인 프로토타입(Android 360×800 보드) 값. 보드에는 상태 바가 없어서 위쪽 값은 상태 바(24dp)를 뺐다.

private val CONTENT_PADDING_HORIZONTAL = 20.dp
private val TITLE_PADDING_TOP = 96.dp
private val TITLE_SPACING = 12.dp
private val CAPTION_SPACING = 16.dp
private val BUTTON_PADDING_BOTTOM = 16.dp

/**
 * 첫 진입 권한 안내. 버튼을 누르면 Health Connect 권한 화면을 띄운다.
 * 필수 권한을 받으면 [onGranted] 를 부른다. 거부하면 안내 팝업으로 다시 요청하거나 설정으로 보낸다.
 */
@Composable
fun PermissionGuideRoute(
    onGranted: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: PermissionGuideViewModel = viewModel {
        PermissionGuideViewModel(hydrationRepository = ServiceLocator.hydrationRepository)
    }
) {
    val deniedKind by viewModel.dialog.collectAsStateWithLifecycle()

    // 권한 요청 결과와 "다시 묻지 않음" 판단 모두 Activity 가 있어야 한다.
    val activity = checkNotNull(LocalActivity.current)
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = PermissionController.createRequestPermissionResultContract()
    ) { result ->
        val granted = result.containsAll(HealthConnectDataSource.HYDRATION_PERMISSIONS)
        viewModel.onPermissionResult(granted, activity.canAskHydrationPermissionAgain())
    }
    val backgroundPermissionLauncher = rememberLauncherForActivityResult(
        contract = PermissionController.createRequestPermissionResultContract()
    ) { result ->
        viewModel.onBackgroundPermissionResult(result.containsAll(HealthConnectDataSource.REQUESTED_PERMISSIONS))
    }
    // 앱별 권한 화면은 시스템 권한이 있어야 열 수 있어서 Health Connect 첫 화면을 연다. 경로는 안내 팝업이 알려준다.
    // 설정 화면은 결과를 돌려주지 않는다. 돌아온 시점만 알면 된다.
    val settingsLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) {
        viewModel.onSettingsReturned()
    }

    LaunchedEffect(viewModel) {
        viewModel.events.collect { event ->
            when (event) {
                PermissionGuideEvent.RequestPermissions ->
                    permissionLauncher.launch(HealthConnectDataSource.REQUESTED_PERMISSIONS)
                // 이미 받은 권한은 Health Connect 가 다시 묻지 않아 백그라운드 읽기만 뜬다.
                PermissionGuideEvent.RequestBackgroundPermission ->
                    backgroundPermissionLauncher.launch(HealthConnectDataSource.REQUESTED_PERMISSIONS)
                PermissionGuideEvent.OpenSettings ->
                    settingsLauncher.launch(Intent(HealthConnectClient.ACTION_HEALTH_CONNECT_SETTINGS))
                PermissionGuideEvent.Granted -> onGranted()
            }
        }
    }

    PermissionGuideScreen(
        onStartClick = { viewModel.onStartClick(activity.canAskHydrationPermissionAgain()) },
        modifier = modifier
    )

    deniedKind?.let { kind ->
        PermissionDeniedDialog(
            kind = kind,
            onActionClick = viewModel::onDialogActionClick,
            onDismiss = viewModel::onDialogDismiss
        )
    }
}

/**
 * 거부한 뒤 Health Connect 권한 화면을 다시 띄울 수 있는지.
 * Health Connect 권한은 시스템 런타임 권한이라 일반 권한처럼 시스템이 "다시 묻지 않음"을 안다
 * (실기기에서 1번째 거부 뒤 true, 2번째 뒤 false 확인). 처음 묻기 전에도 false 라서 거부한 뒤에만 의미가 있다.
 */
private fun Activity.canAskHydrationPermissionAgain(): Boolean =
    HealthConnectDataSource.HYDRATION_PERMISSIONS.any { ActivityCompat.shouldShowRequestPermissionRationale(this, it) }

@Composable
fun PermissionGuideScreen(onStartClick: () -> Unit, modifier: Modifier = Modifier) {
    Box(modifier = modifier.fillMaxSize()) {
        HydrationBackground(progress = 0f, spec = HydrationBackgroundSpec.Phone, modifier = Modifier.fillMaxSize())
        Duck(progress = 0f, modifier = Modifier.fillMaxSize())

        Column(
            modifier = Modifier
                .fillMaxSize()
                .safeDrawingPadding()
                .padding(horizontal = CONTENT_PADDING_HORIZONTAL),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(TITLE_PADDING_TOP))
            Text(
                text = "반가워요!",
                color = Accent,
                fontSize = 24.sp,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = (-0.5).sp
            )
            Spacer(modifier = Modifier.height(TITLE_SPACING))
            Text(
                text = "물마시기 권한을 허용하고\n꽥과 함께 매일 물마셔요!",
                color = BodyText,
                fontSize = 20.sp,
                fontWeight = FontWeight.Medium,
                lineHeight = 30.sp,
                letterSpacing = (-0.5).sp,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.weight(1f))

            Text(
                text = "마신 물을 Health Connect에 기록하고 불러오기 위해\n물마시기 권한이 필요해요.",
                color = CaptionText,
                fontSize = 14.sp,
                lineHeight = 21.sp,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(CAPTION_SPACING))
            HydrationButton(
                text = "권한 허용하고 시작하기",
                onClick = onStartClick,
                style = HydrationButtonStyle.Primary,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = BUTTON_PADDING_BOTTOM)
            )
        }
    }
}

@Preview(showBackground = true, widthDp = 360, heightDp = 776)
@Composable
fun PermissionGuideScreenPreview() {
    WaterWatchTheme(dynamicColor = false) {
        PermissionGuideScreen(onStartClick = {})
    }
}

package com.hanix.waterwatch.ui.onboarding

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hanix.waterwatch.designsystem.Accent
import com.hanix.waterwatch.designsystem.CaptionText
import com.hanix.waterwatch.designsystem.HydrationBackground
import com.hanix.waterwatch.designsystem.HydrationBackgroundSpec
import com.hanix.waterwatch.designsystem.Spacing
import com.hanix.waterwatch.designsystem.TitleText
import com.hanix.waterwatch.ui.goal.GoalSheet
import com.hanix.waterwatch.ui.hydration.Duck
import com.hanix.waterwatch.ui.theme.WaterWatchTheme

// 크기는 디자인 프로토타입(Android 360×800 보드) 값. 보드에는 상태 바가 없어서 위쪽 값은 상태 바(24dp)를 뺐다.

private val CONTENT_PADDING_HORIZONTAL = 20.dp
private val TITLE_PADDING_TOP = 70.dp

/**
 * 첫 진입 하루 목표 설정. 화면에 들어오면 목표 시트가 바로 열린다.
 * 시트에서 고르면 그 값으로, 고르지 않고 닫으면 [initialGoalMl](저장된 값이 없으면 기본 2,000ml)로 [onGoalSelected] 를 부른다.
 */
@Composable
fun GoalGuideScreen(
    initialGoalMl: Int,
    onGoalSelected: (goalMl: Int) -> Unit,
    modifier: Modifier = Modifier
) {
    Box(modifier = modifier.fillMaxSize()) {
        HydrationBackground(progress = 0f, spec = HydrationBackgroundSpec.Phone, modifier = Modifier.fillMaxSize())
        Duck(progress = 0f, modifier = Modifier.fillMaxSize())

        Column(
            modifier = Modifier
                .fillMaxSize()
                .safeDrawingPadding()
                .padding(horizontal = CONTENT_PADDING_HORIZONTAL),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(Spacing.Small)
        ) {
            Spacer(modifier = Modifier.height(TITLE_PADDING_TOP))
            Text(
                text = "하루 목표 설정",
                color = Accent,
                fontSize = 22.sp,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = (-0.5).sp
            )
            Text(
                text = "하루에 마실 물의 양을 정하고\n꽥을 시작해보세요",
                color = TitleText,
                fontSize = 20.sp,
                fontWeight = FontWeight.Medium,
                lineHeight = 30.sp,
                textAlign = TextAlign.Center
            )
            Text(
                text = "목표는 나중에 설정에서 언제든 바꿀 수 있어요.",
                color = CaptionText,
                fontSize = 14.sp,
                lineHeight = 21.sp,
                textAlign = TextAlign.Center
            )
        }
    }

    GoalSheet(
        initialGoalMl = initialGoalMl,
        confirmText = "이 목표로 시작하기",
        onConfirm = onGoalSelected,
        onDismissRequest = { onGoalSelected(initialGoalMl) }
    )
}

@Preview(showBackground = true, widthDp = 360, heightDp = 776)
@Composable
fun GoalGuideScreenPreview() {
    WaterWatchTheme(dynamicColor = false) {
        GoalGuideScreen(initialGoalMl = 2_000, onGoalSelected = {})
    }
}

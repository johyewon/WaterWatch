package com.hanix.waterwatch.ui.onboarding

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.hanix.waterwatch.R
import com.hanix.waterwatch.designsystem.BodyText
import com.hanix.waterwatch.designsystem.DescriptionText
import com.hanix.waterwatch.designsystem.Radius
import com.hanix.waterwatch.designsystem.Spacing
import com.hanix.waterwatch.designsystem.SubtleSurface
import com.hanix.waterwatch.designsystem.TitleText
import com.hanix.waterwatch.ui.component.HydrationButton
import com.hanix.waterwatch.ui.component.HydrationButtonStyle

// 크기는 디자인 프로토타입 값 그대로. 색·모서리는 :designsystem 에서 가져온다.

private val DIALOG_MARGIN_HORIZONTAL = 20.dp
private val CARD_PADDING = PaddingValues(top = 28.dp, start = 20.dp, end = 20.dp, bottom = 20.dp)
private val PATH_SPACING = 14.dp
private val ACTIONS_SPACING = 20.dp

/** 닫기 : 동작 버튼 너비 비율 */
private const val CLOSE_WEIGHT = 1f
private const val ACTION_WEIGHT = 1.4f

/**
 * 필수 권한을 거부했을 때 띄우는 안내.
 * [PermissionDeniedKind.Settings] 면 설정 화면 경로를 같이 보여준다.
 */
@Composable
fun PermissionDeniedDialog(
    kind: PermissionDeniedKind,
    onActionClick: () -> Unit,
    onDismiss: () -> Unit
) {
    // ponytail: 뒤 배경은 시스템 Dialog 기본 어둡게 처리. 프로토타입의 남색 0.4 로 맞추려면 창 dim 을 끄고 직접 그린다.
    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Column(
            modifier = Modifier
                .padding(horizontal = DIALOG_MARGIN_HORIZONTAL)
                .fillMaxWidth()
                .background(Color.White, RoundedCornerShape(Radius.XLarge))
                .padding(CARD_PADDING)
        ) {
            Text(
                text = "물마시기 권한이 필요해요",
                modifier = Modifier.fillMaxWidth(),
                color = TitleText,
                fontSize = 20.sp,
                fontWeight = FontWeight.SemiBold,
                lineHeight = 28.sp,
                textAlign = TextAlign.Center
            )
            Text(
                text = when (kind) {
                    PermissionDeniedKind.Reask ->
                        "마신 물을 Health Connect에 기록하려면 물마시기 권한이 꼭 필요해요. 한 번 더 요청할게요."
                    PermissionDeniedKind.Settings -> "권한을 다시 요청할 수 없어 설정에서 직접 켜야 해요."
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = Spacing.Small),
                color = DescriptionText,
                fontSize = 15.sp,
                lineHeight = 22.sp,
                textAlign = TextAlign.Center
            )
            if (kind == PermissionDeniedKind.Settings) {
                Text(
                    text = "Health Connect › 앱 권한 › ${stringResource(R.string.app_name)}에서 ‘물마시기’를 켜 주세요.",
                    modifier = Modifier
                        .padding(top = PATH_SPACING)
                        .fillMaxWidth()
                        .background(SubtleSurface, RoundedCornerShape(Radius.Medium))
                        .padding(horizontal = 14.dp, vertical = Spacing.Medium),
                    color = BodyText,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    lineHeight = 20.sp,
                    textAlign = TextAlign.Center
                )
            }
            Row(
                modifier = Modifier.padding(top = ACTIONS_SPACING),
                horizontalArrangement = Arrangement.spacedBy(Spacing.Small)
            ) {
                HydrationButton(
                    text = "닫기",
                    onClick = onDismiss,
                    style = HydrationButtonStyle.Secondary,
                    modifier = Modifier.weight(CLOSE_WEIGHT)
                )
                HydrationButton(
                    text = when (kind) {
                        PermissionDeniedKind.Reask -> "권한 재요청"
                        PermissionDeniedKind.Settings -> "설정으로 이동"
                    },
                    onClick = onActionClick,
                    style = HydrationButtonStyle.Primary,
                    modifier = Modifier.weight(ACTION_WEIGHT)
                )
            }
        }
    }
}

package com.hanix.waterwatch.ui.component

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hanix.waterwatch.designsystem.Accent
import com.hanix.waterwatch.designsystem.SubtleSurface
import com.hanix.waterwatch.designsystem.ButtonBorder
import com.hanix.waterwatch.designsystem.ButtonContainer
import com.hanix.waterwatch.designsystem.ButtonContent
import com.hanix.waterwatch.designsystem.ButtonIcon
import com.hanix.waterwatch.designsystem.Radius
import com.hanix.waterwatch.designsystem.Spacing

// 크기는 디자인 프로토타입 값 그대로. 색·모서리는 :designsystem 에서 가져온다.

private val ICON_SIZE = 20.dp
private val ICON_SPACING = 6.dp
private const val DISABLED_ALPHA = 0.4f

// 기본 여백(좌우 24dp)이면 세 칸으로 나눈 기록 버튼에서 "120ml" 의 ml 이 잘린다.
private val CONTENT_PADDING = PaddingValues(horizontal = Spacing.Small)

enum class HydrationButtonStyle(
    val height: Dp,
    val containerColor: Color,
    val contentColor: Color,
    val border: BorderStroke?,
    val fontWeight: FontWeight
) {
    /** 메인 화면 기록 버튼 */
    Outlined(52.dp, ButtonContainer, ButtonContent, BorderStroke(1.dp, ButtonBorder), FontWeight.Bold),

    /** 바텀시트 취소 */
    Secondary(56.dp, SubtleSurface, ButtonContent, null, FontWeight.SemiBold),

    /** 바텀시트 저장·기록 */
    Primary(56.dp, Accent, Color.White, null, FontWeight.SemiBold)
}

/** 막히면 프로토타입처럼 전체를 흐리게 한다. */
@Composable
fun HydrationButton(
    text: String,
    onClick: () -> Unit,
    style: HydrationButtonStyle,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    icon: Painter? = null
) {
    Button(
        onClick = onClick,
        modifier = modifier.height(style.height),
        enabled = enabled,
        shape = RoundedCornerShape(Radius.Large),
        colors = ButtonDefaults.buttonColors(
            containerColor = style.containerColor,
            contentColor = style.contentColor,
            disabledContainerColor = style.containerColor.copy(alpha = DISABLED_ALPHA),
            disabledContentColor = style.contentColor.copy(alpha = DISABLED_ALPHA)
        ),
        border = style.border,
        contentPadding = CONTENT_PADDING
    ) {
        if (icon != null) {
            Icon(painter = icon, contentDescription = null, tint = ButtonIcon, modifier = Modifier.size(ICON_SIZE))
            Spacer(modifier = Modifier.width(ICON_SPACING))
        }
        Text(text = text, fontSize = 17.sp, fontWeight = style.fontWeight, maxLines = 1)
    }
}

@Preview(showBackground = true)
@Composable
fun HydrationButtonPreview() {
    Column(modifier = Modifier.padding(Spacing.Large), verticalArrangement = Arrangement.spacedBy(Spacing.Small)) {
        HydrationButton(text = "직접입력", onClick = {}, style = HydrationButtonStyle.Outlined, modifier = Modifier.fillMaxWidth())
        HydrationButton(
            text = "직접입력",
            onClick = {},
            style = HydrationButtonStyle.Outlined,
            modifier = Modifier.fillMaxWidth(),
            enabled = false
        )
        Row(horizontalArrangement = Arrangement.spacedBy(Spacing.Small)) {
            HydrationButton(text = "취소", onClick = {}, style = HydrationButtonStyle.Secondary, modifier = Modifier.weight(1f))
            HydrationButton(text = "저장하기", onClick = {}, style = HydrationButtonStyle.Primary, modifier = Modifier.weight(1.6f))
        }
    }
}

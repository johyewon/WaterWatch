package com.hanix.waterwatch.designsystem

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.FirstBaseline
import androidx.compose.ui.layout.layout
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.IntOffset
import kotlin.math.roundToInt

private const val ROLL_MILLIS = 220
private val ROLL_TWEEN = tween<IntOffset>(ROLL_MILLIS)

/**
 * 숫자가 바뀌면 바뀐 자리만 슬롯처럼 굴러간다. 늘면 아래에서 올라오고 줄면 위에서 내려온다.
 * 자리는 오른쪽부터 세어 묶어서, 1,000 처럼 자릿수가 늘어도 일의 자리부터 같은 슬롯을 쓴다.
 * 폰·워치가 같이 쓰므로 Material 없이 그린다. 글꼴은 [Pretendard], 자리마다 따로 그려도 폭이 흔들리지 않게 고정폭 숫자(tnum)를 쓴다.
 *
 * [color] 를 [style] 과 따로 받는 건, 색만 애니메이션할 때 글자 밑선을 매 프레임 다시 재지 않기 위해서다.
 */
@Composable
fun RollingNumber(value: Int, style: TextStyle, color: Color, modifier: Modifier = Modifier) {
    var previousValue by remember { mutableIntStateOf(value) }
    val increasing = value >= previousValue
    SideEffect { previousValue = value }

    val digitStyle = TextStyle(fontFamily = Pretendard).merge(style).copy(fontFeatureSettings = "tnum")
    val textMeasurer = rememberTextMeasurer()
    // 굴러가는 동안 칸 밖으로 밀려난 숫자의 글자 밑선이 잡혀 옆 글자가 튀지 않도록, 밑선을 고정값으로 알려준다.
    val baseline = remember(textMeasurer, digitStyle) { textMeasurer.measure("0", digitStyle).firstBaseline.roundToInt() }

    val text = "%,d".format(value)
    Row(
        modifier = modifier.layout { measurable, constraints ->
            val placeable = measurable.measure(constraints)
            layout(placeable.width, placeable.height, mapOf(FirstBaseline to baseline)) {
                placeable.place(0, 0)
            }
        }
    ) {
        text.forEachIndexed { index, char ->
            key(text.length - index) {
                AnimatedContent(
                    targetState = char,
                    // 슬롯 창처럼 자기 칸 밖은 보이지 않게 자른다.
                    modifier = Modifier.clipToBounds(),
                    transitionSpec = {
                        val direction = if (increasing) 1 else -1
                        slideInVertically(ROLL_TWEEN) { height -> height * direction }
                            .togetherWith(slideOutVertically(ROLL_TWEEN) { height -> -height * direction })
                    },
                    label = "rollingDigit"
                ) { digit ->
                    BasicText(text = digit.toString(), style = digitStyle, color = { color })
                }
            }
        }
    }
}

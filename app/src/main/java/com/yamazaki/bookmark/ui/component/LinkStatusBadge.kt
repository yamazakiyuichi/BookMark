package com.yamazaki.bookmark.ui.component

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.yamazaki.bookmark.domain.model.LinkStatus
import com.yamazaki.bookmark.ui.theme.LinkBroken
import com.yamazaki.bookmark.ui.theme.LinkChecking
import com.yamazaki.bookmark.ui.theme.LinkOk
import com.yamazaki.bookmark.ui.theme.LinkUnknown

@Composable
fun LinkStatusBadge(
    status: LinkStatus,
    modifier: Modifier = Modifier
) {
    when (status) {
        LinkStatus.CHECKING -> {
            CircularProgressIndicator(
                modifier = modifier.size(16.dp),
                color = LinkChecking,
                strokeWidth = 2.dp
            )
        }
        else -> {
            val (color, label) = when (status) {
                LinkStatus.OK -> LinkOk to "OK"
                LinkStatus.BROKEN -> LinkBroken to "NG"
                LinkStatus.UNKNOWN -> LinkUnknown to "?"
                else -> LinkUnknown to "?"
            }
            Box(
                modifier = modifier
                    .clip(CircleShape)
                    .background(color)
                    .padding(horizontal = 6.dp, vertical = 2.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = label,
                    color = Color.White,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

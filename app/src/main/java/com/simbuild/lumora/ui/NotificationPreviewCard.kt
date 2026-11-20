package com.simbuild.lumora.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.simbuild.lumora.core.notifications.PreviewState

/** Temporary card drawn over the clock; the clock, date and battery stay visible underneath. */
@Composable
fun NotificationPreviewCard(state: PreviewState, modifier: Modifier = Modifier) {
    val card = (state as? PreviewState.Showing)?.card ?: return
    Column(
        modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(Color(0xFF0D1B2A))
            .padding(16.dp)
            .semantics { liveRegion = LiveRegionMode.Polite },
    ) {
        Text(card.appName, color = Color(0xFF00E5FF), fontSize = 13.sp, fontWeight = FontWeight.Medium)
        card.sender?.let {
            Text(it, color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        card.text?.let {
            Text(it, color = Color.White.copy(alpha = 0.85f), fontSize = 14.sp, maxLines = 3, overflow = TextOverflow.Ellipsis)
        }
    }
}

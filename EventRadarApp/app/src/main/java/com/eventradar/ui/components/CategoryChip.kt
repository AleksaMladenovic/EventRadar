package com.eventradar.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.eventradar.data.model.EventCategory

@Composable
fun CategoryChip(category: EventCategory) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(50)) // Potpuno zaobljene ivice
            .background(category.color.copy(alpha = 0.15f)) // Svetlija pozadina
            .padding(horizontal = 10.dp, vertical = 4.dp)
    ) {
        Text(
            text = stringResource(id = category.displayNameResId).uppercase(),
            color = category.color, // Boja teksta je glavna boja kategorije
            fontWeight = FontWeight.Bold,
            fontSize = 12.sp,
            letterSpacing = 0.5.sp
        )
    }
}
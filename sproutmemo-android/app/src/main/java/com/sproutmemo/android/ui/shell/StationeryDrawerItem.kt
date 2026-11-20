package com.sproutmemo.android.ui.shell

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sproutmemo.android.ui.components.StationeryIcon
import com.sproutmemo.android.ui.theme.LocalAppColors

@Composable
internal fun StationeryDrawerItem(
    iconName: String,
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val colors = LocalAppColors.current
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 3.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(if (isSelected) colors.edge else Color.Transparent)
            .padding(bottom = if (isSelected) 3.dp else 0.dp)
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(46.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(if (isSelected) colors.cardBg else Color.Transparent)
                .then(
                    if (isSelected) Modifier.border(1.5.dp, colors.neonBlue.copy(alpha = 0.65f), RoundedCornerShape(14.dp))
                    else Modifier
                )
                .padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (isSelected) {
                Box(
                    modifier = Modifier
                        .width(4.dp)
                        .height(20.dp)
                        .clip(RoundedCornerShape(999.dp))
                        .background(colors.neonBlue)
                )
                Spacer(modifier = Modifier.width(10.dp))
            }
            StationeryIcon(
                name = iconName,
                size = 18.dp,
                tint = if (isSelected) colors.neonBlue else colors.textSecondary
            )
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = label,
                fontSize = 15.5.sp,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.SemiBold,
                color = if (isSelected) colors.neonBlue else colors.textPrimary
            )
        }
    }
}


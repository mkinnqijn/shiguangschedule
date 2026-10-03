package com.xingheyuzhuan.shiguangschedule.ui.settings.holiday

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.xingheyuzhuan.shiguangschedule.ui.settings.notification.NotificationSettingsUiState
import com.xingheyuzhuan.shiguangschedule.ui.settings.notification.SettingItemRow
import org.jetbrains.compose.resources.stringResource
import shiguangschedule.shared.generated.resources.Res
import shiguangschedule.shared.generated.resources.item_add_skipped_dates
import shiguangschedule.shared.generated.resources.item_clear_skipped_dates
import shiguangschedule.shared.generated.resources.item_update_holiday_info
import shiguangschedule.shared.generated.resources.item_view_skipped_dates
import shiguangschedule.shared.generated.resources.skipped_dates_count_format
import shiguangschedule.shared.generated.resources.skipped_dates_none
import shiguangschedule.shared.generated.resources.update_holiday_info_hint

@Composable
fun HolidayDateSettingsCard(
    uiState: NotificationSettingsUiState,
    onUpdateHolidays: () -> Unit,
    onAddDates: () -> Unit,
    onViewDates: () -> Unit,
    onClearDates: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
            contentColor = MaterialTheme.colorScheme.onSurfaceVariant
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = modifier.fillMaxWidth()
    ) {
        Column {
            SettingItemRow(
                title = stringResource(Res.string.item_update_holiday_info),
                onClick = onUpdateHolidays,
                trailing = {
                    if (uiState.isLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(26.dp),
                            strokeWidth = 4.dp
                        )
                    }
                }
            )
            Text(
                text = stringResource(Res.string.update_holiday_info_hint),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 12.dp)
            )
            HorizontalDivider()

            SettingItemRow(
                title = stringResource(Res.string.item_add_skipped_dates),
                onClick = onAddDates
            )
            HorizontalDivider()

            SettingItemRow(
                title = stringResource(Res.string.item_view_skipped_dates),
                currentValue = if (uiState.skippedDates.isNotEmpty()) {
                    stringResource(Res.string.skipped_dates_count_format, uiState.skippedDates.size)
                } else {
                    stringResource(Res.string.skipped_dates_none)
                },
                onClick = onViewDates
            )
            HorizontalDivider()

            SettingItemRow(
                title = stringResource(Res.string.item_clear_skipped_dates),
                onClick = onClearDates
            )
        }
    }
}

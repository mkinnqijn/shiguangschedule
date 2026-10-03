package com.xingheyuzhuan.shiguangschedule.ui.settings.holiday

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.xingheyuzhuan.shiguangschedule.ui.settings.notification.NotificationDialogType
import com.xingheyuzhuan.shiguangschedule.ui.settings.notification.NotificationSettingsViewModel
import com.xingheyuzhuan.shiguangschedule.ui.settings.notification.PlatformNotificationDialogDispatcher
import org.jetbrains.compose.resources.stringResource
import org.jetbrains.compose.resources.vectorResource
import org.koin.compose.viewmodel.koinViewModel
import shiguangschedule.shared.generated.resources.Res
import shiguangschedule.shared.generated.resources.arrow_back_24px
import shiguangschedule.shared.generated.resources.title_holiday_date_records

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HolidayDateSettingsScreen(
    onBack: () -> Unit,
    viewModel: NotificationSettingsViewModel = koinViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(Res.string.title_holiday_date_records)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(vectorResource(Res.drawable.arrow_back_24px), contentDescription = null)
                    }
                }
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp),
            contentPadding = PaddingValues(bottom = 16.dp)
        ) {
            item {
                HolidayDateSettingsCard(
                    uiState = uiState,
                    onUpdateHolidays = { viewModel.updateHolidays() },
                    onAddDates = { viewModel.showDialog(NotificationDialogType.AddSkippedDates) },
                    onViewDates = { viewModel.showDialog(NotificationDialogType.ViewSkippedDates) },
                    onClearDates = { viewModel.showDialog(NotificationDialogType.ClearConfirmation) }
                )
            }
        }
    }

    PlatformNotificationDialogDispatcher(
        uiState = uiState,
        viewModel = viewModel
    )
}

package dev.coupleapp.android.features.lovecounter

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import dev.coupleapp.shared.LoveCounterFacade
import java.util.TimeZone

@Composable
fun LoveCounterScreen() {
    val counter = remember { LoveCounterFacade() }
    var date by rememberSaveable { mutableStateOf("") }
    val zone = rememberSaveable { TimeZone.getDefault().id }
    var message by rememberSaveable { mutableStateOf("Chọn ngày câu chuyện của hai bạn bắt đầu.") }
    Scaffold { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            Text("Ngày bên nhau")
            Text(message)
            OutlinedTextField(
                value = date,
                onValueChange = { date = it },
                label = { Text("Ngày bắt đầu — YYYY-MM-DD") },
                singleLine = true,
            )
            Text("Múi giờ: $zone")
            Button(onClick = {
                val result = counter.calculate(date.trim(), zone)
                message = when (result.errorCode) {
                    null -> "Đã bên nhau ${result.days} ngày"
                    "future_date" -> "Ngày bắt đầu không thể ở tương lai."
                    "invalid_time_zone" -> "Múi giờ chưa được hỗ trợ."
                    else -> "Hãy nhập ngày hợp lệ theo YYYY-MM-DD."
                }
            }) { Text("Xem số ngày") }
            Text("Bản thử • Chưa lưu dữ liệu")
        }
    }
}

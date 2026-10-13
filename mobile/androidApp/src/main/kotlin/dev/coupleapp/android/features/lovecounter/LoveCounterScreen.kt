package dev.coupleapp.android.features.lovecounter

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.coupleapp.shared.CounterConflictView
import dev.coupleapp.shared.CounterSession
import dev.coupleapp.shared.CounterViewState
import dev.coupleapp.android.FirebaseObservability
import java.time.LocalDate
import java.time.ZoneOffset
import java.time.Instant

@Composable
private fun Page(title: String, back: (() -> Unit)? = null, content: @Composable ColumnScope.() -> Unit) {
    Scaffold { insets ->
        Column(Modifier.fillMaxSize().padding(insets).verticalScroll(rememberScrollState()).padding(24.dp), verticalArrangement = Arrangement.spacedBy(20.dp)) {
            if (back != null) TextButton(onClick = back) { Text("‹ Quay lại") }
            Text(title, style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Bold)
            content()
        }
    }
}

@Composable fun StorageStatus(error: String?, retry: () -> Unit) = Page("Ngày bên nhau") {
    if (error == null) CircularProgressIndicator() else Text("Chưa mở được dữ liệu trên máy. Hãy kiểm tra dung lượng và mở lại ứng dụng. Dữ liệu chưa bị xóa.")
    if (error != null) Button(onClick = retry) { Text("Thử lại") }
}

@Composable fun WelcomeScreen(start: () -> Unit, account: () -> Unit) = Page("Ngày bên nhau") {
    Spacer(Modifier.height(56.dp))
    Text("Chuyện của mình,\nbắt đầu từ một ngày.", style = MaterialTheme.typography.displaySmall)
    Text("Giữ những ngày đáng nhớ, ngay cả khi không có mạng.")
    Spacer(Modifier.height(24.dp))
    Button(onClick = start, modifier = Modifier.fillMaxWidth()) { Text("Bắt đầu") }
    TextButton(onClick = account) { Text("Tôi đã có tài khoản") }
    Text("Dữ liệu được lưu trên thiết bị trước.", style = MaterialTheme.typography.bodySmall)
}

@Composable fun CounterHomeScreen(state: CounterViewState, edit: () -> Unit, account: () -> Unit, sync: () -> Unit) = Page("Ngày bên nhau") {
    if (state.displayName.isNotBlank()) Text(listOf(state.displayName, state.partnerName).filter { it.isNotBlank() }.joinToString(" & "))
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer), modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.fillMaxWidth().padding(28.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(state.days.toString(), fontSize = 76.sp, fontWeight = FontWeight.Light)
            Text("ngày bên nhau", style = MaterialTheme.typography.titleLarge)
            Text("Từ ngày "+state.startDateIso)
        }
    }
    if (state.errorCode != null) Text(errorText(state.errorCode), color = MaterialTheme.colorScheme.error)
    TextButton(onClick = sync) { Text("Đã lưu trên máy · ${state.pendingCount} thay đổi chờ đồng bộ") }
    Button(onClick = edit, modifier = Modifier.fillMaxWidth()) { Text("Chỉnh thông tin") }
    OutlinedButton(onClick = account, modifier = Modifier.fillMaxWidth()) { Text("Tài khoản và dữ liệu") }
    Text(syncText(state.syncState), style = MaterialTheme.typography.bodySmall)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable fun EditCounterScreen(state: CounterViewState, session: CounterSession, initial: Boolean, back: () -> Unit, saved: () -> Unit) {
    var baseRevision by rememberSaveable { mutableStateOf(state.localRevision) }
    var reload by remember { mutableStateOf(false) }
    var name by rememberSaveable { mutableStateOf(state.displayName) }
    var partner by rememberSaveable { mutableStateOf(state.partnerName) }
    var date by rememberSaveable { mutableStateOf(state.startDateIso.ifBlank { LocalDate.now().toString() }) }
    var zone by rememberSaveable { mutableStateOf(state.timeZoneId) }
    var picker by rememberSaveable { mutableStateOf(false) }
    var zonePicker by rememberSaveable { mutableStateOf(false) }
    var zoneQuery by rememberSaveable { mutableStateOf("") }
    var error by rememberSaveable { mutableStateOf<String?>(null) }
    var busy by remember { mutableStateOf(false) }
    var leave by remember { mutableStateOf(false) }
    val dirty = name != state.displayName || partner != state.partnerName || date != state.startDateIso || zone != state.timeZoneId
    BackHandler(enabled = dirty || busy) { if (!busy) leave = true }
    Page(if (initial) "Ngày mình bắt đầu?" else "Chỉnh thông tin", { if (!busy) { if (dirty) leave = true else back() } }) {
        if (!initial) {
            OutlinedTextField(name, { name = it }, label = { Text("Tên hiển thị") }, singleLine = true, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(partner, { partner = it }, label = { Text("Tên người ấy (không bắt buộc)") }, singleLine = true, modifier = Modifier.fillMaxWidth())
        }
        Text("Chọn ngày hai bạn muốn ghi nhớ.")
        OutlinedButton(onClick = { picker = true }, modifier = Modifier.fillMaxWidth()) { Text(date) }
        if (!initial) OutlinedButton(onClick = { zonePicker = true }, modifier = Modifier.fillMaxWidth()) { Text("Múi giờ: " + zoneLabel(zone)) }
        Text("Ngày đầu tiên được tính là ngày 1.")
        if (error != null) Text(errorText(error), color = MaterialTheme.colorScheme.error)
        if (baseRevision != state.localRevision) {
            Text("Có thông tin mới: ${state.displayName} · ${state.partnerName}\n${state.startDateIso} · ${zoneLabel(state.timeZoneId)}")
            OutlinedButton(onClick = { reload = true }, enabled = !busy) { Text("Dùng bản mới để sửa") }
        }
        Button(enabled = !busy, modifier = Modifier.fillMaxWidth(), onClick = {
            busy = true; error = null
            session.save(name, partner, date, zone, baseRevision) { result ->
                busy = false
                if (result.errorCode == null) saved() else error = result.errorCode
            }
        }) { Text(if (busy) "Đang lưu…" else if (initial) "Lưu và tiếp tục" else "Lưu thay đổi") }
        Text("Lưu trên máy · Không cần Internet", style = MaterialTheme.typography.bodySmall)
    }
    if (picker) {
        val selected = rememberDatePickerState(initialSelectedDateMillis = LocalDate.parse(date).atStartOfDay().toInstant(ZoneOffset.UTC).toEpochMilli())
        DatePickerDialog(onDismissRequest = { picker = false }, confirmButton = {
            TextButton(onClick = {
                selected.selectedDateMillis?.let { date = Instant.ofEpochMilli(it).atZone(ZoneOffset.UTC).toLocalDate().toString() }
                picker = false
            }) { Text("Chọn") }
        }, dismissButton = { TextButton(onClick = { picker = false }) { Text("Hủy") } }) { DatePicker(selected) }
    }
    if (zonePicker) ModalBottomSheet(onDismissRequest = { zonePicker = false }) {
        Column(Modifier.padding(24.dp)) {
            Text("Chọn múi giờ", style = MaterialTheme.typography.titleLarge)
            OutlinedTextField(zoneQuery, { zoneQuery = it }, label = { Text("Tìm địa điểm") }, modifier = Modifier.fillMaxWidth())
            val zones = remember { java.time.ZoneId.getAvailableZoneIds().sorted() }
            LazyColumn(Modifier.fillMaxWidth().heightIn(max = 440.dp)) {
                items(zones.filter { zoneLabel(it).contains(zoneQuery, ignoreCase = true) }) { value ->
                    TextButton(onClick = { zone = value; zonePicker = false }) { Text(zoneLabel(value)) }
                }
            }
        }
    }
    if (reload) AlertDialog(onDismissRequest = { reload = false }, title = { Text("Dùng bản mới để sửa?") },
        text = { Text("Nội dung đang nhập sẽ được thay bằng thông tin mới nhất đã lưu trên máy.") },
        confirmButton = { TextButton(onClick = {
            name = state.displayName; partner = state.partnerName
            date = state.startDateIso.ifBlank { LocalDate.now().toString() }; zone = state.timeZoneId
            baseRevision = state.localRevision; error = null; reload = false
        }) { Text("Dùng bản mới") } }, dismissButton = { TextButton(onClick = { reload = false }) { Text("Giữ bản đang nhập") } })
    if (leave) AlertDialog(onDismissRequest = { leave = false }, title = { Text("Bỏ thay đổi chưa lưu?") }, text = { Text("Thông tin đã lưu trước đó vẫn được giữ.") }, confirmButton = { TextButton(onClick = back) { Text("Bỏ thay đổi") } }, dismissButton = { TextButton(onClick = { leave = false }) { Text("Tiếp tục sửa") } })
}

@Composable fun AccountScreen(state: CounterViewState, back: () -> Unit, sync: () -> Unit, observability: FirebaseObservability) = Page("Tài khoản", back) {
    var analyticsEnabled by rememberSaveable { mutableStateOf(observability.analyticsEnabled) }
    var crashReportsEnabled by rememberSaveable { mutableStateOf(observability.crashReportsEnabled) }
    Text(state.displayName.ifBlank { "Không gian của bạn" }, style = MaterialTheme.typography.titleLarge)
    Text("Đang dùng trên thiết bị")
    Text("Bạn chưa cần đăng nhập để lưu dữ liệu trên máy.")
    Text("Google/Apple và khôi phục từ cloud đang được triển khai.")
    Text(syncText(state.syncState))
    OutlinedButton(onClick = sync) { Text("Xem trạng thái đồng bộ") }
    Text("Dữ liệu chưa đồng bộ có thể mất khi xóa ứng dụng.", style = MaterialTheme.typography.bodySmall)
    HorizontalDivider()
    Text("Chẩn đoán và cải thiện", style = MaterialTheme.typography.titleMedium)
    if (!observability.available) {
        Text("Bản local chưa kết nối Firebase Analytics/Crashlytics. Không có dữ liệu chẩn đoán nào được gửi.")
    }
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.weight(1f)) {
            Text("Chia sẻ dữ liệu sử dụng")
            Text("Chỉ sự kiện kỹ thuật đã giới hạn; không gồm tên, ngày, UID hay nội dung.", style = MaterialTheme.typography.bodySmall)
        }
        Switch(
            checked = analyticsEnabled,
            onCheckedChange = { value ->
                observability.setAnalyticsEnabled(value)
                analyticsEnabled = observability.analyticsEnabled
            },
            enabled = observability.available,
        )
    }
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.weight(1f)) {
            Text("Gửi báo cáo lỗi")
            Text("Gửi crash kỹ thuật để sửa lỗi; mặc định tắt.", style = MaterialTheme.typography.bodySmall)
        }
        Switch(
            checked = crashReportsEnabled,
            onCheckedChange = { value ->
                observability.setCrashReportsEnabled(value)
                crashReportsEnabled = observability.crashReportsEnabled
            },
            enabled = observability.available,
        )
    }
}

@Composable fun SyncScreen(state: CounterViewState, back: () -> Unit, retry: () -> Unit, session: CounterSession) = Page("Đồng bộ dữ liệu", back) {
    Text(if (state.hasCounter) "Đã lưu trên máy" else "Chưa có dữ liệu", style = MaterialTheme.typography.headlineSmall)
    Text("${state.pendingCount} thay đổi đang chờ")
    Text(syncText(state.syncState))
    Text("Thông tin đã lưu trên máy vẫn dùng được khi mất mạng.")
    var choice by remember { mutableStateOf<Pair<CounterConflictView, Boolean>?>(null) }
    var resolutionError by remember { mutableStateOf<String?>(null) }
    state.conflict?.let { conflict ->
        Text("Chọn thông tin bạn muốn giữ", style = MaterialTheme.typography.titleLarge)
        Text("Trên máy: ${state.displayName} · ${state.partnerName}\n${state.startDateIso} · ${zoneLabel(state.timeZoneId)}")
        Text("Trên server: ${conflict.displayName} · ${conflict.partnerName}\n${conflict.startDateIso} · ${zoneLabel(conflict.timeZoneId)}")
        if (!conflict.isCurrent) Text("Bạn vừa sửa dữ liệu. Bấm Thử đồng bộ để cập nhật bản so sánh.")
        Button(onClick = { choice = conflict to true }, enabled = conflict.isCurrent && !state.saving) { Text("Giữ bản trên máy") }
        OutlinedButton(onClick = { choice = conflict to false }, enabled = conflict.isCurrent && !state.saving) { Text("Dùng bản server") }
    }
    resolutionError?.let { Text(it, color = MaterialTheme.colorScheme.error) }
    choice?.let { (conflict, keepLocal) ->
        AlertDialog(onDismissRequest = { choice = null },
            title = { Text(if (keepLocal) "Giữ bản trên máy?" else "Dùng bản server?") },
            text = { Text(if (keepLocal) "Bản trên máy sẽ được gửi lên thay thế bản server đã hiển thị khi có mạng. Nếu server có thay đổi mới, bạn sẽ được chọn lại." else "Thông tin trên máy và các thay đổi đang chờ sẽ được thay bằng bản server đã hiển thị.") },
            confirmButton = { TextButton(onClick = {
                choice = null
                session.resolveConflict(conflict.localRevision, conflict.serverVersion, keepLocal) { result ->
                    resolutionError = result.errorCode?.let { if (it == "conflict_changed") "Dữ liệu đã thay đổi. Hãy cập nhật bản so sánh và chọn lại." else "Chưa áp dụng được. Hãy thử lại." }
                }
            }) { Text("Xác nhận") } },
            dismissButton = { TextButton(onClick = { choice = null }) { Text("Hủy") } })
    }
    if (state.syncState != "not_configured") {
        Text("Bản kiểm thử: kết nối máy chủ local, chưa phải bản sao lưu cloud.", style = MaterialTheme.typography.bodySmall)
        OutlinedButton(onClick = retry, enabled = state.syncState != "syncing") { Text("Thử đồng bộ") }
    }
    if (state.hasCounter) ListItem(headlineContent = { Text("Thông tin bộ đếm") }, supportingContent = { Text("Ngày bắt đầu và tên hiển thị") })
    Button(onClick = back) { Text("Tiếp tục dùng app") }
}

private fun errorText(code: String?) = when(code) {
    "edit_changed" -> "Thông tin đã thay đổi từ lúc bạn bắt đầu sửa. Nội dung đang nhập vẫn được giữ; hãy xem bản mới trước khi lưu."
    "invalid_date" -> "Hãy chọn ngày hợp lệ."
    "future_date" -> "Ngày bắt đầu không thể ở tương lai."
    "invalid_time_zone" -> "Múi giờ chưa hợp lệ."
    "invalid_input" -> "Tên tối đa 80 ký tự. Hãy kiểm tra thông tin."
    else -> "Chưa lưu được. Dữ liệu đang nhập vẫn được giữ; hãy kiểm tra dung lượng và thử lại."
}

private fun zoneLabel(id: String): String = if (id == "Asia/Ho_Chi_Minh") "Việt Nam (UTC+7)" else id.replace('_', ' ')

private fun syncText(state: String) = when (state) {
    "not_configured" -> "Bản phát triển: đồng bộ chưa được kết nối."
    "syncing" -> "Đang đồng bộ…"
    "synced" -> "Đã đồng bộ với máy chủ kiểm thử."
    "retry_later" -> "Chưa kết nối được. Dữ liệu vẫn được giữ trên máy."
    "conflict" -> "Có bản thay đổi trên máy chủ. Dữ liệu trên máy được giữ để xử lý xung đột."
    "account_conflict" -> "Tài khoản hiện tại không khớp dữ liệu trên máy. Đã dừng gửi."
    "auth_required" -> "Chưa xác thực được tài khoản. Hãy thử lại khi có mạng."
    "forbidden" -> "Tài khoản chưa có quyền đồng bộ dữ liệu này."
    "sync_error", "storage_unavailable" -> "Chưa đồng bộ được. Thay đổi vẫn được giữ lại."
    else -> "Sẵn sàng đồng bộ khi có kết nối."
}

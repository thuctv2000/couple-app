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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.shape.RoundedCornerShape
import dev.coupleapp.shared.CounterConflictView
import dev.coupleapp.shared.CounterSession
import dev.coupleapp.shared.CounterViewState
import dev.coupleapp.android.FirebaseObservability
import java.time.LocalDate
import java.time.ZoneOffset
import java.time.Instant

private val AppBackground = Color(0xFFFFFAF5)
private val Rose = Color(0xFFB65F66)
private val RoseDark = Color(0xFF47292B)
private val RoseSoft = Color(0xFFF5E4E2)
private val SageSoft = Color(0xFFE8EDE5)
private val Sage = Color(0xFF526650)

@Composable
private fun Page(title: String, back: (() -> Unit)? = null, content: @Composable ColumnScope.() -> Unit) {
    Scaffold(containerColor = AppBackground) { insets ->
        Box(Modifier.fillMaxSize().padding(insets), contentAlignment = Alignment.TopCenter) {
            Column(
                Modifier.widthIn(max = 640.dp).fillMaxWidth().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp, vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(24.dp),
            ) {
                if (back != null) TextButton(onClick = back, contentPadding = PaddingValues(horizontal = 0.dp)) { Text("‹  Quay lại", fontSize = 16.sp) }
                Text(title, color = RoseDark, fontSize = 36.sp, lineHeight = 42.sp, fontWeight = FontWeight.Bold)
                content()
                Spacer(Modifier.height(12.dp))
            }
        }
    }
}

@Composable
private fun PrimaryAction(label: String, onClick: () -> Unit, enabled: Boolean = true) {
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp),
        shape = RoundedCornerShape(18.dp),
        colors = ButtonDefaults.buttonColors(containerColor = Rose),
    ) { Text(label, fontSize = 18.sp, fontWeight = FontWeight.SemiBold) }
}

@Composable
private fun SecondaryAction(label: String, onClick: () -> Unit, enabled: Boolean = true) {
    OutlinedButton(
        onClick = onClick,
        enabled = enabled,
        modifier = Modifier.fillMaxWidth().heightIn(min = 54.dp),
        shape = RoundedCornerShape(18.dp),
        colors = ButtonDefaults.outlinedButtonColors(contentColor = RoseDark),
    ) { Text(label, fontSize = 17.sp, fontWeight = FontWeight.Medium) }
}

@Composable
private fun SectionSurface(content: @Composable ColumnScope.() -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = Color.White.copy(alpha = 0.76f),
        shape = RoundedCornerShape(24.dp),
        tonalElevation = 1.dp,
    ) {
        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp), content = content)
    }
}

@Composable fun StorageStatus(error: String?, retry: () -> Unit) = Page("Ngày bên nhau") {
    if (error == null) CircularProgressIndicator() else Text("Chưa mở được dữ liệu trên máy. Hãy kiểm tra dung lượng và mở lại ứng dụng. Dữ liệu chưa bị xóa.")
    if (error != null) Button(onClick = retry) { Text("Thử lại") }
}

@Composable fun WelcomeScreen(start: () -> Unit, account: () -> Unit) = Page("Ngày bên nhau") {
    Surface(
        modifier = Modifier.align(Alignment.CenterHorizontally).size(112.dp),
        color = RoseSoft,
        shape = RoundedCornerShape(40.dp),
    ) {
        Box(contentAlignment = Alignment.Center) { Text("♡", color = Rose, fontSize = 64.sp, fontWeight = FontWeight.Light) }
    }
    Text(
        "Chuyện của mình,\nbắt đầu từ một ngày.",
        modifier = Modifier.fillMaxWidth(),
        color = RoseDark,
        fontSize = 38.sp,
        lineHeight = 45.sp,
        fontWeight = FontWeight.Bold,
        textAlign = TextAlign.Center,
    )
    Text(
        "Giữ những ngày đáng nhớ, ngay cả khi không có mạng.",
        modifier = Modifier.fillMaxWidth(),
        style = MaterialTheme.typography.titleMedium,
        textAlign = TextAlign.Center,
    )
    Spacer(Modifier.height(8.dp))
    PrimaryAction("Bắt đầu", start)
    TextButton(onClick = account, modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp)) {
        Text("Tôi đã có tài khoản", color = Rose, fontSize = 17.sp, fontWeight = FontWeight.SemiBold)
    }
    Text("Dữ liệu được lưu trên thiết bị trước.", modifier = Modifier.fillMaxWidth(), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
}

@Composable fun CounterHomeScreen(state: CounterViewState, edit: () -> Unit, account: () -> Unit, sync: () -> Unit) = Page("Ngày bên nhau") {
    if (state.displayName.isNotBlank()) {
        Text(
            listOf(state.displayName, state.partnerName).filter { it.isNotBlank() }.joinToString("  &  ").uppercase(),
            color = Rose,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.5.sp,
        )
    }
    Card(colors = CardDefaults.cardColors(containerColor = Rose), shape = RoundedCornerShape(30.dp), modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 42.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Text(state.days.toString(), color = Color.White, fontSize = 104.sp, lineHeight = 108.sp, fontWeight = FontWeight.Light)
            Text("ngày bên nhau", color = Color.White, fontSize = 26.sp, fontWeight = FontWeight.Medium)
            HorizontalDivider(Modifier.width(72.dp), color = Color.White.copy(alpha = 0.72f))
            Text("Từ ngày ${state.startDateIso}", color = Color.White.copy(alpha = 0.9f), fontSize = 18.sp)
        }
    }
    if (state.errorCode != null) Text(errorText(state.errorCode), color = MaterialTheme.colorScheme.error)
    Surface(onClick = sync, modifier = Modifier.fillMaxWidth(), color = SageSoft, shape = RoundedCornerShape(22.dp)) {
        Row(Modifier.padding(horizontal = 18.dp, vertical = 17.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            Surface(color = Sage, shape = RoundedCornerShape(16.dp), modifier = Modifier.size(48.dp)) {
                Box(contentAlignment = Alignment.Center) { Text("✓", color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Bold) }
            }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text("Đã lưu trên máy", color = Sage, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                Text(if (state.pendingCount == 0) "Không có thay đổi đang chờ" else "${state.pendingCount} thay đổi chờ đồng bộ", color = Sage.copy(alpha = 0.82f), fontSize = 14.sp)
            }
            Text("›", color = Sage, fontSize = 28.sp)
        }
    }
    PrimaryAction("Chỉnh thông tin", edit)
    SectionSurface {
        Text("Giữ lại câu chuyện của mình", color = RoseDark, fontSize = 20.sp, fontWeight = FontWeight.Bold)
        Text("Thêm cách đăng nhập để khôi phục dữ liệu đã đồng bộ khi đổi máy.", style = MaterialTheme.typography.bodyLarge)
        SecondaryAction("Tài khoản và dữ liệu", account)
    }
    Text(syncText(state.syncState), modifier = Modifier.fillMaxWidth(), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
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
        SecondaryAction(date, { picker = true })
        if (!initial) SecondaryAction("Múi giờ: " + zoneLabel(zone), { zonePicker = true })
        Text("Ngày đầu tiên được tính là ngày 1.")
        if (error != null) Text(errorText(error), color = MaterialTheme.colorScheme.error)
        if (baseRevision != state.localRevision) {
            Text("Có thông tin mới: ${state.displayName} · ${state.partnerName}\n${state.startDateIso} · ${zoneLabel(state.timeZoneId)}")
            OutlinedButton(onClick = { reload = true }, enabled = !busy) { Text("Dùng bản mới để sửa") }
        }
        PrimaryAction(if (busy) "Đang lưu…" else if (initial) "Lưu và tiếp tục" else "Lưu thay đổi", enabled = !busy, onClick = {
            busy = true; error = null
            session.save(name, partner, date, zone, baseRevision) { result ->
                busy = false
                if (result.errorCode == null) saved() else error = result.errorCode
            }
        })
        Text("Lưu trên máy · Không cần Internet", modifier = Modifier.fillMaxWidth(), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
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
    Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Surface(color = RoseSoft, shape = RoundedCornerShape(36.dp), modifier = Modifier.size(104.dp)) {
            Box(contentAlignment = Alignment.Center) { Text("♡", color = Rose, fontSize = 58.sp) }
        }
        Text(state.displayName.ifBlank { "Không gian của bạn" }, color = RoseDark, fontSize = 28.sp, fontWeight = FontWeight.Bold)
        Surface(color = SageSoft, shape = RoundedCornerShape(50)) { Text("Đang dùng trên thiết bị", modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp), color = Sage, fontWeight = FontWeight.SemiBold) }
    }
    SectionSurface {
        Text("Tài khoản và dữ liệu", color = RoseDark, fontSize = 22.sp, fontWeight = FontWeight.Bold)
        Text("Bạn chưa cần đăng nhập để lưu dữ liệu trên máy.", style = MaterialTheme.typography.bodyLarge)
        Text("Google/Apple và khôi phục từ cloud đang được triển khai.", color = MaterialTheme.colorScheme.onSurfaceVariant)
        HorizontalDivider()
        Text(syncText(state.syncState), style = MaterialTheme.typography.bodyLarge)
        SecondaryAction("Xem trạng thái đồng bộ", sync)
        Text("Dữ liệu chưa đồng bộ có thể mất khi xóa ứng dụng.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
    SectionSurface {
        Text("Chẩn đoán và cải thiện", color = RoseDark, fontSize = 22.sp, fontWeight = FontWeight.Bold)
        if (!observability.available) {
            Text("Bản local chưa kết nối Analytics/Crashlytics. Không có dữ liệu chẩn đoán nào được gửi.", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().heightIn(min = 64.dp)) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("Chia sẻ dữ liệu sử dụng", fontSize = 17.sp, fontWeight = FontWeight.SemiBold)
                Text("Không gồm tên, ngày, UID hay nội dung.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Switch(checked = analyticsEnabled, onCheckedChange = { value -> observability.setAnalyticsEnabled(value); analyticsEnabled = observability.analyticsEnabled }, enabled = observability.available)
        }
        HorizontalDivider()
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().heightIn(min = 64.dp)) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("Gửi báo cáo lỗi", fontSize = 17.sp, fontWeight = FontWeight.SemiBold)
                Text("Mặc định tắt. Khi bật, Firebase có thể gửi cả báo cáo crash đã lưu trên máy từ trước.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Switch(checked = crashReportsEnabled, onCheckedChange = { value -> observability.setCrashReportsEnabled(value); crashReportsEnabled = observability.crashReportsEnabled }, enabled = observability.available)
        }
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
        PrimaryAction("Giữ bản trên máy", { choice = conflict to true }, enabled = conflict.isCurrent && !state.saving)
        SecondaryAction("Dùng bản server", { choice = conflict to false }, enabled = conflict.isCurrent && !state.saving)
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
        SecondaryAction("Thử đồng bộ", retry, enabled = state.syncState != "syncing")
    }
    if (state.hasCounter) ListItem(headlineContent = { Text("Thông tin bộ đếm") }, supportingContent = { Text("Ngày bắt đầu và tên hiển thị") })
    PrimaryAction("Tiếp tục dùng app", back)
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

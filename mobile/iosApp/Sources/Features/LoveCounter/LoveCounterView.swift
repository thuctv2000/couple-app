import SwiftUI
import Combine
@preconcurrency import CoupleShared

@MainActor
final class CounterModel: ObservableObject {
    @Published var state: CounterViewState?
    let session = FirebaseCounterIdentity.makeSession()
    private var observation: CounterObservation?

    func start() {
        guard observation == nil else { return }
        observation = session.observe { [weak self] value in
            MainActor.assumeIsolated { self?.state = value }
        }
        session.refresh()
    }
    func stop() { observation?.cancel(); observation = nil }
    func retry() { stop(); state = nil; start() }
}

private enum CounterRoute: Hashable { case setup, edit, account, sync }

struct LoveCounterView: View {
    @Environment(\.scenePhase) private var scenePhase
    @StateObject private var model = CounterModel()
    @State private var path: [CounterRoute] = []
    private let rose = Color(red: 0.68, green: 0.37, blue: 0.39)

    var body: some View {
        NavigationStack(path: $path) {
            Group {
                if let state = model.state {
                    if state.errorCode == "storage_unavailable" {
                        VStack {
                            ContentUnavailableView("Chưa mở được dữ liệu", systemImage: "externaldrive.badge.exclamationmark", description: Text("Hãy kiểm tra dung lượng và mở lại ứng dụng. Dữ liệu chưa bị xóa."))
                            Button("Thử lại") { model.retry() }.buttonStyle(.borderedProminent)
                        }
                    } else if state.hasCounter { home(state) }
                    else { welcome }
                } else { ProgressView("Đang mở dữ liệu trên máy…") }
            }
            .navigationDestination(for: CounterRoute.self) { route in
                switch route {
                case .setup, .edit:
                    CounterEditor(model: model, initial: route == .setup) { path.removeAll() }
                case .account: account
                case .sync: sync
                }
            }
        }
        .tint(rose)
        .onAppear { model.start() }
        .onDisappear { model.stop() }
        .onChange(of: scenePhase) { _, phase in
            if phase == .active { model.session.refresh() } else { model.session.pauseSync() }
        }
    }

    private var welcome: some View {
        ScrollView {
            VStack(alignment: .leading, spacing: 28) {
                Image(systemName: "heart").font(.system(size: 48)).foregroundStyle(rose).accessibilityHidden(true)
                Text("Chuyện của mình,\nbắt đầu từ một ngày.").font(.largeTitle.bold())
                Text("Giữ những ngày đáng nhớ, ngay cả khi không có mạng.")
                Button("Bắt đầu") { path.append(.setup) }.buttonStyle(.borderedProminent)
                Button("Tôi đã có tài khoản") { path.append(.account) }
                Text("Dữ liệu được lưu trên thiết bị trước.").font(.footnote).foregroundStyle(.secondary)
            }.padding(24)
        }.navigationTitle("Ngày bên nhau")
    }

    private func home(_ state: CounterViewState) -> some View {
        ScrollView {
            VStack(spacing: 24) {
                if !state.displayName.isEmpty {
                    Text([state.displayName, state.partnerName].filter { !$0.isEmpty }.joined(separator: " & ")).font(.title2)
                }
                VStack(spacing: 16) {
                    Text("\(state.days)").font(.system(size: 76, weight: .light, design: .rounded)).minimumScaleFactor(0.5)
                    Text("ngày bên nhau").font(.title2)
                    Text("Từ ngày \(state.startDateIso)")
                }
                .frame(maxWidth: .infinity).padding(28)
                .background(rose.opacity(0.15), in: RoundedRectangle(cornerRadius: 24))
                if state.errorCode != nil { Text("Kiểm tra lại ngày bắt đầu và múi giờ.").foregroundStyle(.red) }
                Button { path.append(.sync) } label: {
                    Label("Đã lưu trên máy · \(state.pendingCount) thay đổi chờ đồng bộ", systemImage: "iphone")
                }
                Button("Chỉnh thông tin") { path.append(.edit) }.buttonStyle(.borderedProminent)
                Button("Tài khoản và dữ liệu") { path.append(.account) }.buttonStyle(.bordered)
                Text(syncText(state.syncState)).font(.footnote).foregroundStyle(.secondary)
            }.padding(24)
        }.navigationTitle("Ngày bên nhau")
    }

    private var account: some View {
        Form {
            Section {
                Text(model.state?.displayName.isEmpty == false ? model.state!.displayName : "Không gian của bạn").font(.title2)
                Label("Đang dùng trên thiết bị", systemImage: "iphone")
            }
            Section("Tài khoản") {
                Text("Bạn chưa cần đăng nhập để lưu dữ liệu trên máy.")
                Text("Google/Apple và khôi phục từ cloud đang được triển khai.")
                Text(syncText(model.state?.syncState ?? "not_configured"))
            }
            Section("Dữ liệu của bạn") {
                Button("Xem trạng thái đồng bộ") { path.append(.sync) }
                Text("Dữ liệu chưa đồng bộ có thể mất khi xóa ứng dụng.").font(.footnote)
            }
        }.navigationTitle("Tài khoản")
    }

    private var sync: some View {
        Form {
            Section {
                Label(model.state?.hasCounter == true ? "Đã lưu trên máy" : "Chưa có dữ liệu", systemImage: "internaldrive")
                Text("\(model.state?.pendingCount ?? 0) thay đổi đang chờ")
                Text(syncText(model.state?.syncState ?? "not_configured"))
            }
            if let state = model.state, let conflict = state.conflict {
                CounterConflictSection(state: state, conflict: conflict, session: model.session)
            }
            Section {
                Text("Thông tin đã lưu trên máy vẫn dùng được khi mất mạng.")
                if let state = model.state, state.syncState != "not_configured" {
                    Text("Bản kiểm thử: kết nối máy chủ local, chưa phải bản sao lưu cloud.").font(.footnote)
                    Button("Thử đồng bộ") { model.session.requestSync() }.disabled(state.syncState == "syncing")
                }
                Button("Tiếp tục dùng app") { path.removeLast() }
            }
        }.navigationTitle("Đồng bộ dữ liệu")
    }
}

private func syncText(_ state: String) -> String {
    switch state {
    case "not_configured": return "Bản phát triển: đồng bộ chưa được kết nối."
    case "syncing": return "Đang đồng bộ…"
    case "synced": return "Đã đồng bộ với máy chủ kiểm thử."
    case "retry_later": return "Chưa kết nối được. Dữ liệu vẫn được giữ trên máy."
    case "conflict": return "Có bản thay đổi trên máy chủ. Dữ liệu trên máy được giữ để xử lý xung đột."
    case "account_conflict": return "Tài khoản hiện tại không khớp dữ liệu trên máy. Đã dừng gửi."
    case "auth_required": return "Chưa xác thực được tài khoản. Hãy thử lại khi có mạng."
    case "forbidden": return "Tài khoản chưa có quyền đồng bộ dữ liệu này."
    case "sync_error", "storage_unavailable": return "Chưa đồng bộ được. Thay đổi vẫn được giữ lại."
    default: return "Sẵn sàng đồng bộ khi có kết nối."
    }
}

private struct CounterEditor: View {
    @ObservedObject var model: CounterModel
    let initial: Bool
    let saved: () -> Void
    @State private var displayName = ""
    @State private var partnerName = ""
    @State private var date = Date()
    @State private var zoneId = TimeZone.current.identifier
    @State private var baseRevision: Int64 = 0
    @State private var confirmReload = false
    @State private var loaded = false
    @State private var saving = false
    @State private var error: String?
    @State private var confirmLeave = false
    @Environment(\.dismiss) private var dismiss

    private func load(_ state: CounterViewState) {
        displayName = state.displayName; partnerName = state.partnerName; zoneId = state.timeZoneId
        date = formatter().date(from: state.startDateIso) ?? Date()
        baseRevision = state.localRevision
    }

    private func formatter(zone: String? = nil) -> DateFormatter {
        let formatter = DateFormatter()
        formatter.calendar = Calendar(identifier: .gregorian)
        formatter.locale = Locale(identifier: "en_US_POSIX")
        formatter.timeZone = TimeZone(identifier: zone ?? zoneId) ?? .current
        formatter.dateFormat = "yyyy-MM-dd"
        return formatter
    }

    var body: some View {
        Form {
            if !initial {
                Section("Tên hiển thị") { TextField("Tên của bạn", text: $displayName) }
                Section("Tên người ấy (không bắt buộc)") { TextField("Tên người ấy", text: $partnerName) }
            }
            Section("Ngày bắt đầu") {
                DatePicker("Ngày mình bắt đầu", selection: $date, in: ...Date(), displayedComponents: .date)
                    .datePickerStyle(.graphical)
                    .environment(\.calendar, Calendar(identifier: .gregorian))
                    .environment(\.timeZone, TimeZone(identifier: zoneId) ?? .current)
                Text("Ngày đầu tiên được tính là ngày 1.").font(.footnote)
            }
            if !initial {
                Section("Múi giờ") {
                    Picker("Địa điểm", selection: $zoneId) {
                        ForEach(TimeZone.knownTimeZoneIdentifiers, id: \.self) { id in
                            Text(id == "Asia/Ho_Chi_Minh" ? "Việt Nam (UTC+7)" : id.replacingOccurrences(of: "_", with: " ")).tag(id)
                        }
                    }.pickerStyle(.navigationLink)
                }
            }
            Section {
                if let error { Text(error).foregroundStyle(.red) }
                if let state = model.state, state.localRevision != baseRevision {
                    Text("Có thông tin mới: \(state.displayName) · \(state.partnerName)\n\(state.startDateIso) · \(state.timeZoneId)")
                    Button("Dùng bản mới để sửa") { confirmReload = true }
                        .disabled(saving)
                        .alert("Dùng bản mới để sửa?", isPresented: $confirmReload) {
                            Button("Dùng bản mới") {
                                if let latest = model.state { load(latest) }
                                error = nil
                            }
                            Button("Giữ bản đang nhập", role: .cancel) { }
                        } message: { Text("Nội dung đang nhập sẽ được thay bằng thông tin mới nhất đã lưu trên máy.") }
                }
                Button(saving ? "Đang lưu…" : initial ? "Lưu và tiếp tục" : "Lưu thay đổi") {
                    saving = true; error = nil
                    model.session.save(displayName: displayName, partnerName: partnerName, startDateIso: formatter().string(from: date), timeZoneId: zoneId, expectedRevision: baseRevision) { result in
                        MainActor.assumeIsolated {
                            saving = false
                            if result.errorCode == nil { saved() }
                            else {
                                switch result.errorCode {
                                case "edit_changed": error = "Thông tin đã thay đổi từ lúc bạn bắt đầu sửa. Nội dung đang nhập vẫn được giữ; hãy xem bản mới trước khi lưu."
                                case "future_date": error = "Ngày bắt đầu không thể ở tương lai."
                                case "invalid_time_zone": error = "Múi giờ chưa hợp lệ."
                                case "invalid_input": error = "Tên tối đa 80 ký tự. Hãy kiểm tra thông tin."
                                default: error = "Chưa lưu được. Nội dung đang nhập vẫn được giữ; hãy thử lại."
                                }
                            }
                        }
                    }
                }.disabled(saving)
                Text("Lưu trên máy · Không cần Internet").font(.footnote).foregroundStyle(.secondary)
            }
        }
        .navigationTitle(initial ? "Ngày mình bắt đầu?" : "Chỉnh thông tin")
        .navigationBarBackButtonHidden(true)
        .toolbar {
            ToolbarItem(placement: .topBarLeading) {
                Button("Quay lại") {
                    let original = model.state
                    let dirty = displayName != original?.displayName || partnerName != original?.partnerName || zoneId != original?.timeZoneId || formatter().string(from: date) != original?.startDateIso
                    if dirty { confirmLeave = true } else { dismiss() }
                }.disabled(saving)
            }
        }
        .confirmationDialog("Bỏ thay đổi chưa lưu?", isPresented: $confirmLeave, titleVisibility: .visible) {
            Button("Bỏ thay đổi", role: .destructive) { dismiss() }
            Button("Tiếp tục sửa", role: .cancel) { }
        }
        .onChange(of: zoneId) { old, new in
            guard TimeZone(identifier: old) != nil, TimeZone(identifier: new) != nil else { return }
            let sameCalendarDate = formatter(zone: old).string(from: date)
            if let adjusted = formatter(zone: new).date(from: sameCalendarDate) { date = adjusted }
        }
        .onAppear {
            guard !loaded else { return }
            loaded = true
            if let state = model.state {
                load(state)
            }
        }
    }
}

private struct CounterConflictSection: View {
    let state: CounterViewState
    let conflict: CounterConflictView
    let session: CounterSession
    @State private var choice: ConflictChoice?
    @State private var error: String?

    private struct ConflictChoice: Identifiable {
        let id = UUID()
        let snapshot: CounterConflictView
        let keepLocal: Bool
    }

    var body: some View {
        Section("Chọn thông tin bạn muốn giữ") {
            Text("Trên máy: \(state.displayName) · \(state.partnerName)\n\(state.startDateIso) · \(state.timeZoneId)")
            Text("Trên server: \(conflict.displayName) · \(conflict.partnerName)\n\(conflict.startDateIso) · \(conflict.timeZoneId)")
            if !conflict.isCurrent { Text("Bạn vừa sửa dữ liệu. Bấm Thử đồng bộ để cập nhật bản so sánh.") }
            Button("Giữ bản trên máy") { choice = ConflictChoice(snapshot: conflict, keepLocal: true) }
                .disabled(!conflict.isCurrent || state.saving)
            Button("Dùng bản server") { choice = ConflictChoice(snapshot: conflict, keepLocal: false) }
                .disabled(!conflict.isCurrent || state.saving)
            if let error { Text(error).foregroundStyle(.red) }
        }
        .alert(item: $choice) { selected in
            Alert(title: Text(selected.keepLocal ? "Giữ bản trên máy?" : "Dùng bản server?"),
                  message: Text(selected.keepLocal ? "Bản trên máy sẽ được gửi lên thay thế bản server đã hiển thị khi có mạng. Nếu server có thay đổi mới, bạn sẽ được chọn lại." : "Thông tin trên máy và các thay đổi đang chờ sẽ được thay bằng bản server đã hiển thị."),
                  primaryButton: .default(Text("Xác nhận")) {
                    session.resolveConflict(localRevision: selected.snapshot.localRevision, serverVersion: selected.snapshot.serverVersion, keepLocal: selected.keepLocal) { result in
                        error = result.errorCode.map { $0 == "conflict_changed" ? "Dữ liệu đã thay đổi. Hãy cập nhật bản so sánh và chọn lại." : "Chưa áp dụng được. Hãy thử lại." }
                    }
                  }, secondaryButton: .cancel(Text("Hủy")))
        }
    }
}

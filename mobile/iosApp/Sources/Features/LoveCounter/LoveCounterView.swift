import SwiftUI
import CoupleShared

struct LoveCounterView: View {
    @State private var startDate = Date()
    @State private var message = "Chọn ngày câu chuyện của hai bạn bắt đầu."
    @State private var timeZone = TimeZone.current
    private let counter = LoveCounterFacade()

    var body: some View {
        Form {
            Section {
                Text(message).font(.title2)
                DatePicker("Ngày bắt đầu", selection: $startDate, in: ...Date(), displayedComponents: .date)
                    .environment(\.timeZone, timeZone)
                Text("Múi giờ: \(timeZone.identifier)")
                    .font(.footnote)
                Button("Xem số ngày") { calculate() }
            }
            Section {
                Text("Bản thử • Chưa lưu dữ liệu").foregroundStyle(.secondary)
            }
        }
        .navigationTitle("Ngày bên nhau")
    }

    private func calculate() {
        let formatter = DateFormatter()
        formatter.calendar = Calendar(identifier: .gregorian)
        formatter.locale = Locale(identifier: "en_US_POSIX")
        formatter.timeZone = timeZone
        formatter.dateFormat = "yyyy-MM-dd"
        let result = counter.calculate(startDateIso: formatter.string(from: startDate), timeZoneId: timeZone.identifier)
        switch result.errorCode {
        case nil: message = "Đã bên nhau \(result.days) ngày"
        case "future_date": message = "Ngày bắt đầu không thể ở tương lai."
        case "invalid_time_zone": message = "Múi giờ chưa được hỗ trợ."
        default: message = "Hãy chọn ngày hợp lệ."
        }
    }
}

import type { Metadata } from "next";
import Link from "next/link";
export const metadata: Metadata = { title: "Trợ giúp" };

const questions = [
  { id: "availability", title: "Tôi có thể tải app ở đâu?", answer: "CoupleApp đang được phát triển và chưa có đường dẫn tải công khai trên App Store hay Google Play. Luồng lưu offline và đồng bộ đã được thử trên Android giả lập. Phiên bản iOS vẫn đang được kiểm chứng trước khi mở thử nghiệm." },
  { id: "start", title: "Có cần đăng nhập để bắt đầu không?", answer: "Không. Bản hiện tại cho phép chọn ngày và lưu trên thiết bị trước. Khi bản thử nghiệm được cấu hình đồng bộ và có mạng, app có thể tạo phiên khách để gửi dữ liệu. Google/Apple và liên kết tài khoản đang được hoàn thiện." },
  { id: "days", title: "Ngày bên nhau được tính như thế nào?", answer: "Ngày bắt đầu được tính là ngày 1. App tính theo ngày lịch và múi giờ bạn đã chọn; không cần mở app mỗi ngày để bộ đếm tăng. Nếu thấy ngày chưa đúng, hãy kiểm tra ngày bắt đầu, múi giờ và ngày giờ của thiết bị." },
  { id: "offline", title: "Mất mạng có dùng được không?", answer: "Bạn vẫn có thể xem và sửa thông tin đã lưu trên máy. Thay đổi chờ đồng bộ được giữ lại. Khi có mạng, mở lại app hoặc vào màn Đồng bộ và chọn Thử đồng bộ. Đồng bộ khi app đóng hoàn toàn vẫn đang được hoàn thiện." },
  { id: "backup", title: "Đã lưu trên máy có nghĩa là đã sao lưu chưa?", answer: "Chưa. Đã lưu trên máy chỉ xác nhận dữ liệu có trên thiết bị hiện tại. Bản đồng bộ đang dùng môi trường thử nghiệm, chưa phải dịch vụ sao lưu công khai. Gỡ app có thể làm mất dữ liệu chỉ lưu trên máy; không xem phiên khách là cách khôi phục dữ liệu sau khi cài lại." },
  { id: "conflict", title: "Vì sao app hỏi chọn bản trên máy hay server?", answer: "Có thay đổi khác với bản bạn đang sửa. App giữ cả thông tin trên máy và bản nhận về để bạn so sánh. Giữ bản trên máy sẽ gửi lại bản đó khi có mạng; dùng bản server sẽ thay thông tin local và bỏ các thay đổi đang chờ. Hãy đọc bản so sánh trước khi xác nhận." },
  { id: "draft", title: "Tại sao bản đang sửa chưa lưu được?", answer: "Thông tin có thể đã được cập nhật từ lúc bạn mở màn sửa. App giữ nội dung bạn đang nhập để tránh ghi đè bản mới. Bạn có thể xem thông tin mới và chọn Dùng bản mới để sửa; thao tác này chỉ thay bản nhập sau khi bạn xác nhận." },
  { id: "delete", title: "Tôi có thể xóa dữ liệu và tài khoản chưa?", answer: "Luồng xóa trong app đang được hoàn thiện. Gỡ app không phải thao tác xóa dữ liệu đã gửi đến môi trường đồng bộ. Kênh yêu cầu hỗ trợ và hướng dẫn xóa đầy đủ sẽ được công bố trước khi mở bản thử nghiệm cho người dùng." },
];

export default function HelpPage() {
  return <main id="main-content" className="document-page"><header className="document-header"><p className="eyebrow">MỘT CHÚT HƯỚNG DẪN</p><h1>Dùng app,<br /><em>thật nhẹ nhàng.</em></h1><p>Những điều cần biết về bộ đếm và dữ liệu trong bản đang phát triển.</p></header><div className="faq-list">{questions.map(q => <details id={q.id} key={q.id}><summary>{q.title}</summary><p>{q.answer}</p></details>)}</div><aside className="notice"><h2>Về thông tin của bạn</h2><p>Xem những dữ liệu bản thử nghiệm lưu, gửi và các phần đang được hoàn thiện.</p><Link className="text-link" href="/privacy">Dữ liệu & quyền riêng tư ↗</Link></aside></main>;
}

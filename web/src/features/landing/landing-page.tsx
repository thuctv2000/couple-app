import Link from "next/link";

const features = [
  { mark: "01", title: "Bắt đầu từ ngày của bạn", body: "Chọn ngày hai bạn muốn ghi nhớ, thêm tên và múi giờ. Ngày đầu tiên cũng là một ngày bên nhau." },
  { mark: "02", title: "Mở lên, thấy ngay", body: "Bộ đếm đọc thông tin đã lưu trên máy. Bạn vẫn xem và chỉnh sửa được khi không có mạng." },
  { mark: "03", title: "Bạn chọn điều muốn giữ", body: "Trong bản thử nghiệm đồng bộ, nếu có hai bản khác nhau, app hiển thị để bạn chọn trước khi thay thế." },
];

export function LandingPage() {
  return <main id="main-content">
    <section className="hero" aria-labelledby="hero-title">
      <div className="hero-copy"><p className="eyebrow">NGÀY BÊN NHAU</p><h1 id="hero-title">Những ngày nhỏ.<br /><em>Một tình yêu lớn.</em></h1><p className="lead">Một nơi nhẹ nhàng để nhớ ngày bắt đầu và nhìn lại quãng đường hai bạn đã bên nhau.</p><div className="hero-actions"><a className="button primary" href="#features">Khám phá CoupleApp <span aria-hidden="true">↗</span></a><Link className="text-link" href="/help">App hoạt động thế nào?</Link></div><p className="release-note"><span className="status-dot" aria-hidden="true" />Đang phát triển cho Android và iOS.<br />Chưa có bản tải công khai.</p></div>
      <figure className="counter-illustration"><div className="keepsake"><div className="keepsake-top"><span>CHUYỆN CỦA CHÚNG MÌNH</span><span aria-hidden="true">♡</span></div><p className="sample-names">Linh & Nam</p><p className="sample-count">365</p><p className="sample-label">ngày bên nhau</p><div className="sample-rule" /><p className="sample-caption">Từ một ngày thật đặc biệt.</p></div><figcaption>Minh họa bộ đếm · Giao diện app đang hoàn thiện</figcaption></figure>
    </section>
    <section id="features" className="section" aria-labelledby="features-title"><div className="section-intro"><p className="eyebrow">ÍT THAO TÁC, NHIỀU Ý NGHĨA</p><h2 id="features-title">Dành chỗ cho điều quan trọng.</h2><p>Bản đầu tiên tập trung vào bộ đếm và thông tin của hai bạn.</p></div><div className="feature-grid">{features.map(feature => <article className="feature-card" key={feature.mark}><span className="feature-number">{feature.mark}</span><h3>{feature.title}</h3><p>{feature.body}</p></article>)}</div></section>
    <section className="offline-panel" aria-labelledby="offline-title"><div><p className="eyebrow">CỨ BẮT ĐẦU THÔI</p><h2 id="offline-title">Không cần tài khoản<br />để lưu ngày đầu tiên.</h2></div><div><p>Thông tin được lưu trên thiết bị trước. Bạn có thể dùng bộ đếm ngay cả khi chưa kết nối Internet.</p><p className="muted">Dữ liệu chỉ có trên máy có thể mất khi gỡ app. Đăng nhập Google/Apple và khôi phục trên thiết bị khác đang được hoàn thiện.</p><Link className="text-link" href="/help#backup">Hiểu về lưu dữ liệu <span aria-hidden="true">↗</span></Link></div></section>
    <section className="section roadmap" aria-labelledby="next-title"><p className="eyebrow">TỪNG BƯỚC, CÙNG NHAU</p><h2 id="next-title">Còn nhiều điều phía trước.</h2><p>Kỷ niệm, kết nối hai người và những gợi ý quan tâm nhau nằm trong định hướng tiếp theo. Các tính năng chia sẻ và AI chưa có trong bản hiện tại.</p><Link className="button secondary" href="/help#availability">Xem trạng thái bản thử nghiệm</Link></section>
  </main>;
}

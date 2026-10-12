import Link from "next/link";

export function SiteHeader() {
  return <header className="site-header"><Link className="brand" href="/" aria-label="CoupleApp — trang chủ"><span aria-hidden="true">♡</span> CoupleApp</Link><nav aria-label="Điều hướng chính"><Link href="/#features">Trải nghiệm</Link><Link href="/help">Trợ giúp</Link><Link href="/privacy">Dữ liệu của bạn</Link></nav></header>;
}

export function SiteFooter() {
  return <footer className="site-footer"><div><Link className="brand" href="/">CoupleApp</Link><p>Một ngày bình thường. Một điều đáng nhớ.</p></div><div className="footer-links"><Link href="/help">Trợ giúp</Link><Link href="/privacy">Dữ liệu & quyền riêng tư</Link><span>Bản thử nghiệm · Chưa phát hành công khai</span></div></footer>;
}

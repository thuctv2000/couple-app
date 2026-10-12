import type { Metadata } from "next";
import { SiteHeader, SiteFooter } from "@/components/site-shell";
import "./globals.css";

export const metadata: Metadata = {
  title: { default: "CoupleApp — Ngày bên nhau", template: "%s | CoupleApp" },
  description: "Ghi nhớ ngày bắt đầu, đếm ngày bên nhau và lưu thông tin ngay trên thiết bị. CoupleApp đang được phát triển cho Android và iOS.",
};

export default function RootLayout({ children }: { children: React.ReactNode }) {
  return <html lang="vi"><body><a className="skip-link" href="#main-content">Đến nội dung chính</a><SiteHeader />{children}<SiteFooter /></body></html>;
}

import type { Metadata } from "next";
import { AdminLogin } from "@/features/admin/components/admin-login";
import { readAdminConfig, readAdminLoginConfig } from "@/features/admin/server/config";

export const metadata: Metadata = { title: "Đăng nhập quản trị", robots: { index: false, follow: false } };
export const dynamic = "force-dynamic";
export default function AdminLoginPage() {
  const config = readAdminConfig(process.env) && readAdminLoginConfig(process.env);
  return <main id="main-content" className="document-page"><header className="document-header"><p className="eyebrow">COUPLEAPP · QUẢN TRỊ</p><h1>Đăng nhập<br /><em>quản trị.</em></h1><p>Khu vực dành cho người vận hành ứng dụng.</p></header>{config ? <AdminLogin config={config} /> : <section className="notice"><h2>Đăng nhập chưa được cấu hình</h2><p>Môi trường này chưa sẵn sàng cho đăng nhập quản trị. Không có dữ liệu người dùng được hiển thị.</p></section>}</main>;
}

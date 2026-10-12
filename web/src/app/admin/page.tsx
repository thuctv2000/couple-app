import type { Metadata } from "next";
import { cookies } from "next/headers";
import { redirect } from "next/navigation";
import Link from "next/link";
import { readAdminConfig } from "@/features/admin/server/config";
import { verifyAdmin } from "@/features/admin/server/verify";
import { AdminLookup } from "@/features/admin/components/admin-lookup";

export const metadata: Metadata = { title: "Quản trị", robots: { index: false, follow: false } };
export const dynamic = "force-dynamic";

export default async function AdminPage() {
  const config = readAdminConfig(process.env);
  if (!config) redirect("/admin/login");
  const result = await verifyAdmin(config, (await cookies()).get(config.cookieName)?.value);
  if (result === "unauthenticated") redirect("/admin/login");
  if (result !== "allowed") return <main id="main-content" className="document-page"><header className="document-header"><h1>{result === "forbidden" ? "Không có quyền quản trị." : "Chưa xác minh được quyền."}</h1><p>{result === "forbidden" ? "Tài khoản này chưa được cấp quyền hoặc quyền đã bị thu hồi." : "Dịch vụ xác thực đang không sẵn sàng. Hãy thử lại sau."}</p><Link href="/admin/login">Quay lại đăng nhập</Link></header></main>;
  return <main id="main-content" className="document-page"><header className="document-header"><p className="eyebrow">KHU VỰC QUẢN TRỊ</p><h1>Phiên quản trị<br /><em>đã xác minh.</em></h1><p>Server vừa kiểm tra quyền truy cập của bạn. Phiên có thời hạn tối đa 55 phút; khi hết hạn, hãy đăng nhập lại.</p></header><AdminLookup/><form action="/api/admin/logout" method="post"><button className="button secondary" type="submit">Đăng xuất</button></form></main>;
}

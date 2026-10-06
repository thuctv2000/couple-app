import type { Metadata } from "next";
import "./globals.css";

export const metadata: Metadata = {
  title: "CoupleApp — Ngày bên nhau",
  description: "Một nơi dành cho câu chuyện của hai bạn.",
};

export default function RootLayout({ children }: { children: React.ReactNode }) {
  return <html lang="vi"><body>{children}</body></html>;
}

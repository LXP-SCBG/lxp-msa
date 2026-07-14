import type { Metadata } from "next";
import { Geist, Geist_Mono } from "next/font/google";
import "./globals.css";
import Navbar from "@/components/Navbar";
import FloatingGoalButton from "@/components/FloatingGoalButton";
import { AuthProvider } from "@/store/authStore";

const geistSans = Geist({
  variable: "--font-geist-sans",
  subsets: ["latin"],
});

const geistMono = Geist_Mono({
  variable: "--font-geist-mono",
  subsets: ["latin"],
});

export const metadata: Metadata = {
  title: "Loop",
  description: "Learning Experience Platform",
};

export default function RootLayout({
  children,
}: Readonly<{
  children: React.ReactNode;
}>) {
  return (
    <html lang="ko" className={`${geistSans.variable} ${geistMono.variable}`}>
      <body className="bg-gray-50 min-h-screen flex flex-col">
        <AuthProvider>
          <Navbar />
          <main className="pt-14 flex-1 flex flex-col">{children}</main>
          <FloatingGoalButton />
        </AuthProvider>
      </body>
    </html>
  );
}

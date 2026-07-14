import FloatingGoalButton from "@/components/FloatingGoalButton";

export default function MainLayout({
  children,
}: {
  children: React.ReactNode;
}) {
  return (
    <>
      {children}
      <FloatingGoalButton />
    </>
  );
}

"use client";

type ButtonProps = {
  children: React.ReactNode;
  variant?: "primary" | "outline";
  type?: "button" | "submit" | "reset";
  fullWidth?: boolean;
  disabled?: boolean;
  className?: string;
  onClick?: () => void;
};

export default function Button({
  children,
  variant = "primary",
  type = "button",
  fullWidth = false,
  disabled = false,
  className = "",
  onClick,
}: ButtonProps) {
  const base =
    "inline-flex items-center justify-center px-4 py-2 rounded-lg font-medium text-sm transition-colors focus:outline-none disabled:opacity-50 disabled:cursor-not-allowed";
  const variants = {
    primary: "bg-violet-600 text-white hover:bg-violet-700 cursor-pointer",
    outline:
      "border border-violet-600 text-violet-600 bg-white hover:bg-violet-50 cursor-pointer",
  };

  return (
    <button
      type={type}
      onClick={onClick}
      disabled={disabled}
      className={`${base} ${variants[variant]} ${fullWidth ? "w-full" : ""} ${className}`}
    >
      {children}
    </button>
  );
}

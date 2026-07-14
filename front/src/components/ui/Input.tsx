"use client";

type InputProps = {
  label?: string;
  id?: string;
  type?: string;
  placeholder?: string;
  value?: string;
  onChange?: (e: React.ChangeEvent<HTMLInputElement>) => void;
  onBlur?: (e: React.FocusEvent<HTMLInputElement>) => void;
  error?: string;
  className?: string;
};

export default function Input({
  label,
  id,
  type = "text",
  placeholder,
  value,
  onChange,
  onBlur,
  error,
  className = "",
}: InputProps) {
  return (
    <div className="flex flex-col gap-1.5">
      {label && (
        <label htmlFor={id} className="text-sm font-medium text-gray-700">
          {label}
        </label>
      )}
      <input
        id={id}
        type={type}
        placeholder={placeholder}
        value={value}
        onChange={onChange}
        onBlur={onBlur}
        className={`w-full rounded-lg border px-3 py-2.5 text-sm text-gray-900 placeholder:text-gray-400 outline-none focus:ring-2 focus:border-transparent transition ${error ? "border-red-400 focus:ring-red-400" : "border-gray-300 focus:ring-violet-500"} ${className}`}
      />
      {error && <p className="text-xs text-red-500">{error}</p>}
    </div>
  );
}

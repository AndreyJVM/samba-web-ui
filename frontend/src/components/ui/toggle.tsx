interface ToggleProps {
  checked: boolean;
  onChange: (checked: boolean) => void;
  label?: string;
}

export function Toggle({ checked, onChange, label }: ToggleProps) {
  return (
    <label className="flex items-center gap-3 cursor-pointer group">
      <button
        type="button"
        role="switch"
        aria-checked={checked}
        onClick={(e) => {
          e.preventDefault();
          onChange(!checked);
        }}
        className={`relative inline-flex h-5 w-9 shrink-0 cursor-pointer items-center justify-center rounded-full transition-colors duration-200 ease-in-out focus:outline-none focus-visible:ring-2 focus-visible:ring-accent ${
          checked ? 'bg-accent' : 'bg-track border border-border-strong'
        }`}
      >
        <span className="sr-only">{label || "Toggle"}</span>
        <span
          className={`pointer-events-none inline-block h-4 w-4 transform rounded-full ring-0 transition duration-200 ease-in-out ${
            checked ? 'translate-x-2 bg-on-accent' : '-translate-x-2 bg-text-muted'
          }`}
        />
      </button>
      {label && <span className="text-[13px] font-medium text-foreground group-hover:opacity-80 transition-opacity">{label}</span>}
    </label>
  );
}

import React, { useState, useRef, useEffect } from 'react';

interface AutocompleteInputProps extends React.InputHTMLAttributes<HTMLInputElement> {
  suggestions: string[];
}

export function AutocompleteInput({ suggestions, value, onChange, className, ...props }: AutocompleteInputProps) {
  const [isOpen, setIsOpen] = useState(false);
  const [activeIndex, setActiveIndex] = useState(-1);
  const wrapperRef = useRef<HTMLDivElement>(null);

  const strValue = (value as string) || "";
  const filtered = strValue 
    ? suggestions.filter(s => s.toLowerCase().includes(strValue.toLowerCase()) && s !== strValue)
    : suggestions;

  useEffect(() => {
    function handleClickOutside(event: MouseEvent) {
      if (wrapperRef.current && !wrapperRef.current.contains(event.target as Node)) {
        setIsOpen(false);
      }
    }
    document.addEventListener("mousedown", handleClickOutside);
    return () => document.removeEventListener("mousedown", handleClickOutside);
  }, []);

  const handleSelect = (suggestion: string) => {
    if (onChange) {
      onChange({ target: { value: suggestion } } as any);
    }
    setIsOpen(false);
    setActiveIndex(-1);
  };

  const handleKeyDown = (e: React.KeyboardEvent<HTMLInputElement>) => {
    if (!isOpen) return;
    if (e.key === 'ArrowDown') {
      e.preventDefault();
      setActiveIndex(prev => (prev < filtered.length - 1 ? prev + 1 : prev));
    } else if (e.key === 'ArrowUp') {
      e.preventDefault();
      setActiveIndex(prev => (prev > 0 ? prev - 1 : -1));
    } else if (e.key === 'Enter' && activeIndex >= 0) {
      e.preventDefault();
      handleSelect(filtered[activeIndex]);
    } else if (e.key === 'Escape') {
      setIsOpen(false);
    }
  };

  return (
    <div className={`relative w-full ${className || ''}`} ref={wrapperRef}>
      <input
        {...props}
        className="w-full bg-transparent outline-none text-foreground placeholder:text-status-disabled py-2.5 px-3.5"
        style={{ width: '100%' }}
        value={value}
        onChange={(e) => {
          onChange && onChange(e);
          setIsOpen(true);
          setActiveIndex(-1);
        }}
        onFocus={() => setIsOpen(true)}
        onKeyDown={(e) => {
          if (props.onKeyDown) props.onKeyDown(e);
          handleKeyDown(e);
        }}
        autoComplete="off"
      />
      
      {isOpen && filtered.length > 0 && (
        <ul className="absolute z-[100] w-full bg-surface border border-border mt-1 rounded-md shadow-lg max-h-60 overflow-y-auto">
          {filtered.map((suggestion, idx) => (
            <li
              key={suggestion}
              className={'px-3 py-2 text-[13px] font-mono cursor-pointer transition-colors ' + (idx === activeIndex ? 'bg-surface-hover text-brand' : 'hover:bg-surface-hover text-foreground')}
              onClick={() => handleSelect(suggestion)}
            >
              {suggestion}
            </li>
          ))}
        </ul>
      )}
    </div>
  );
}

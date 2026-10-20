import React, { useState, useRef, useEffect } from 'react';
import { Check, X, ChevronDown } from 'lucide-react';
import { useId } from 'react';

interface MultiSelectProps {
  options: string[];
  value: string;
  onChange: (value: string) => void;
  placeholder?: string;
  className?: string;
  inputId?: string;
  optionDescriptions?: Record<string, string>;
}

export function MultiSelect({ options, value, onChange, placeholder, className, inputId, optionDescriptions }: MultiSelectProps) {
  const [isOpen, setIsOpen] = useState(false);
  const [search, setSearch] = useState('');
  const [activeIndex, setActiveIndex] = useState(-1);
  const listId = useId();
  const wrapperRef = useRef<HTMLDivElement>(null);
  
  const selected = value ? value.split(',').map(s => s.trim()).filter(Boolean) : [];
  
  const filteredOptions = options.filter(o => o.toLowerCase().includes(search.toLowerCase()));

  useEffect(() => {
    function handleClickOutside(event: MouseEvent) {
      if (wrapperRef.current && !wrapperRef.current.contains(event.target as Node)) {
        setIsOpen(false);
      }
    }
    document.addEventListener("mousedown", handleClickOutside);
    return () => document.removeEventListener("mousedown", handleClickOutside);
  }, []);

  const toggleOption = (opt: string) => {
    let next;
    if (selected.includes(opt)) {
      next = selected.filter(s => s !== opt);
    } else {
      next = [...selected, opt];
    }
    onChange(next.join(', '));
    setSearch('');
    setActiveIndex(-1);
  };

  const removeOption = (e: React.MouseEvent, opt: string) => {
    e.stopPropagation();
    onChange(selected.filter(s => s !== opt).join(', '));
  };

  return (
    <div className={`relative w-full ${className || ''}`} ref={wrapperRef}>
      <div 
        className="min-h-[42px] w-full bg-surface border border-border-strong focus-within:border-accent transition-all py-1.5 px-2 rounded-lg flex flex-wrap gap-1.5 items-center cursor-text"
        onClick={() => setIsOpen(true)}
      >
        {selected.map(opt => (
          <span key={opt} title={optionDescriptions?.[opt]} className="bg-surface-2 border border-border text-foreground px-2 py-0.5 rounded-md text-[12px] font-mono flex items-center gap-1.5">
            {opt}
            <button type="button" onClick={(e) => removeOption(e, opt)} className="text-text-muted hover:text-status-error transition-colors">
              <X className="w-3 h-3" />
            </button>
          </span>
        ))}
        <div className="flex-1 min-w-[80px] flex items-center gap-2">
          <input
            id={inputId}
            role="combobox"
            aria-expanded={isOpen}
            aria-controls={listId}
            aria-autocomplete="list"
            aria-activedescendant={isOpen && filteredOptions[activeIndex] ? `${listId}-${activeIndex}` : undefined}
            className="w-full bg-transparent outline-none text-[13px] font-mono text-foreground placeholder:text-text-muted"
            placeholder={selected.length === 0 ? placeholder : ''}
            value={search}
            onChange={e => {
              setSearch(e.target.value);
              setActiveIndex(-1);
              setIsOpen(true);
            }}
            onFocus={() => setIsOpen(true)}
            onBlur={event => {
              if (!wrapperRef.current?.contains(event.relatedTarget)) setIsOpen(false);
            }}
            onKeyDown={event => {
              if (event.key === 'ArrowDown' || event.key === 'ArrowUp') {
                event.preventDefault();
                setIsOpen(true);
                setActiveIndex(index => event.key === 'ArrowDown'
                  ? Math.min(index + 1, filteredOptions.length - 1)
                  : Math.max(index - 1, -1));
              } else if (event.key === 'Enter' && isOpen) {
                event.preventDefault();
                const option = filteredOptions[activeIndex];
                if (option) toggleOption(option);
              } else if (event.key === 'Escape') {
                event.preventDefault();
                setIsOpen(false);
                setActiveIndex(-1);
              }
            }}
          />
          {!isOpen && <ChevronDown className="w-4 h-4 text-text-muted shrink-0 mr-1" />}
        </div>
      </div>
      
      {isOpen && (
        <ul id={listId} role="listbox" aria-multiselectable="true" onMouseDown={event => event.preventDefault()} className="absolute z-[100] w-full bg-surface border border-border mt-1 rounded-md max-h-60 overflow-y-auto">
          {filteredOptions.length === 0 ? (
            <li className="px-3 py-4 text-center text-[12px] text-text-muted">No matching options</li>
          ) : (
            filteredOptions.map((opt, index) => {
              const isSelected = selected.includes(opt);
              return (
                <li
                  key={opt}
                  id={`${listId}-${index}`}
                  role="option"
                  aria-selected={isSelected}
                  className={`px-3 py-2 text-[13px] font-mono cursor-pointer transition-colors hover:bg-surface-2 text-foreground flex items-center justify-between group ${index === activeIndex ? 'bg-surface-2' : ''}`}
                  onClick={() => toggleOption(opt)}
                >
                  <div className="flex items-center gap-2.5">
                    <div className={`w-4 h-4 rounded border flex items-center justify-center transition-colors ${isSelected ? 'bg-accent border-accent text-on-accent' : 'border-border-strong group-hover:border-foreground'}`}>
                      {isSelected && <Check className="w-3 h-3" />}
                    </div>
                    {opt}
                    {optionDescriptions?.[opt] && <span className="text-[11px] text-text-muted font-sans">{optionDescriptions[opt]}</span>}
                  </div>
                </li>
              );
            })
          )}
        </ul>
      )}
    </div>
  );
}

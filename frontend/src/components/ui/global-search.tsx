import React, { useState, useEffect, useRef } from "react";
import { Search, FolderKanban, Users, Shield } from "lucide-react";
import { useNavigate } from "react-router-dom";
import { api } from "../../lib/api";
import { useTranslation } from "../../lib/i18n";

export function GlobalSearch() {
  const [isOpen, setIsOpen] = useState(false);
  const [query, setQuery] = useState("");
  const [activeIndex, setActiveIndex] = useState(-1);
  const [results, setResults] = useState<{ type: string; title: string; subtitle?: string; path: string; icon: any }[]>([]);
  const wrapperRef = useRef<HTMLDivElement>(null);
  const navigate = useNavigate();
  const { t } = useTranslation();

  const [cache, setCache] = useState<{shares: any[], users: any[], groups: any[]} | null>(null);

  useEffect(() => {
    function handleClickOutside(event: MouseEvent) {
      if (wrapperRef.current && !wrapperRef.current.contains(event.target as Node)) {
        setIsOpen(false);
      }
    }
    document.addEventListener("mousedown", handleClickOutside);
    return () => document.removeEventListener("mousedown", handleClickOutside);
  }, []);

  const loadData = async () => {
    setIsOpen(true);
    if (cache) return;
    try {
      const [sh, us, gr] = await Promise.all([
        api.get<any[]>("/api/shares").catch(() => []),
        api.get<any[]>("/api/users").catch(() => []),
        api.get<any[]>("/api/groups").catch(() => [])
      ]);
      setCache({ shares: sh || [], users: us || [], groups: gr || [] });
    } catch {}
  };

  useEffect(() => {
    if (!query.trim() || !cache) {
      setResults([]);
      return;
    }
    const q = query.toLowerCase();
    const matches: typeof results = [];

    cache.shares.forEach(s => {
      if (s.name.toLowerCase().includes(q) || s.path.toLowerCase().includes(q)) {
        matches.push({ type: t("sidebar.shares"), title: s.name, subtitle: s.path, path: `/shares`, icon: FolderKanban });
      }
    });
    cache.users.forEach(u => {
      if (u.username.toLowerCase().includes(q) || (u.fullName && u.fullName.toLowerCase().includes(q))) {
        matches.push({ type: t("sidebar.users"), title: u.username, subtitle: u.fullName || 'Local user', path: `/users`, icon: Users });
      }
    });
    cache.groups.forEach(g => {
      if (g.name.toLowerCase().includes(q)) {
        matches.push({ type: t("sidebar.groups"), title: g.name, subtitle: `${g.members?.length || 0} members`, path: `/groups`, icon: Shield });
      }
    });

    setResults(matches.slice(0, 8));
    setActiveIndex(-1);
  }, [query, cache, t]);

  const handleSelect = (path: string) => {
    setIsOpen(false);
    setQuery("");
    navigate(path);
  };

  const handleKeyDown = (e: React.KeyboardEvent) => {
    if (!isOpen) return;
    if (e.key === 'ArrowDown') {
      e.preventDefault();
      setActiveIndex(prev => (prev < results.length - 1 ? prev + 1 : prev));
    } else if (e.key === 'ArrowUp') {
      e.preventDefault();
      setActiveIndex(prev => (prev > 0 ? prev - 1 : -1));
    } else if (e.key === 'Enter' && activeIndex >= 0) {
      e.preventDefault();
      handleSelect(results[activeIndex].path);
    } else if (e.key === 'Escape') {
      setIsOpen(false);
    }
  };

  return (
    <div className="relative w-full max-w-md" ref={wrapperRef}>
      <div className="relative flex items-center">
        <Search className="w-4 h-4 absolute left-3 text-status-disabled" />
        <input 
          className="w-full bg-surface-hover hover:bg-surface border border-transparent focus:border-border-strong focus:bg-surface focus:ring-4 focus:ring-ring transition-all py-2 pl-9 pr-4 text-[13px] font-medium rounded-lg text-foreground placeholder:text-status-disabled outline-none"
          placeholder=t("common.search")
          value={query}
          onChange={(e) => {
            setQuery(e.target.value);
            setIsOpen(true);
          }}
          onFocus={loadData}
          onKeyDown={handleKeyDown}
        />
      </div>

      {isOpen && query.trim() !== "" && (
        <div className="absolute top-full left-0 right-0 mt-2 bg-surface border border-border rounded-lg shadow-lg z-[100] overflow-hidden animate-in fade-in slide-in-from-top-2 duration-200">
          {results.length === 0 ? (
            <div className="p-4 text-center text-[13px] text-status-disabled">
              No results found for "{query}"
            </div>
          ) : (
            <ul className="max-h-80 overflow-y-auto custom-scrollbar py-1">
              {results.map((res, idx) => {
                const Icon = res.icon;
                return (
                  <li 
                    key={`${res.type}-${res.title}-${idx}`}
                    className={`px-3 py-2 cursor-pointer flex items-center gap-3 transition-colors ${idx === activeIndex ? 'bg-surface-hover' : 'hover:bg-surface-hover'}`}
                    onClick={() => handleSelect(res.path)}
                    onMouseEnter={() => setActiveIndex(idx)}
                  >
                    <div className="bg-background border border-border p-2 rounded-md shrink-0">
                      <Icon className="w-4 h-4 text-brand" />
                    </div>
                    <div className="flex-1 min-w-0">
                      <div className="text-[13px] font-semibold text-foreground truncate">{res.title}</div>
                      <div className="text-[11px] text-status-disabled truncate">{res.subtitle}</div>
                    </div>
                    <div className="shrink-0 text-[10px] font-bold uppercase tracking-wider text-status-disabled bg-background px-2 py-1 rounded border border-border/50">
                      {res.type}
                    </div>
                  </li>
                );
              })}
            </ul>
          )}
        </div>
      )}
    </div>
  );
}

import { useEffect, useState } from "react";
import { Link, useLocation, Outlet } from "react-router-dom";
import { Server, FolderKanban, Users, Settings, LogOut, Play, Square, RefreshCw, Moon, Sun } from "lucide-react";
import { api } from "../lib/api";
import { useToast } from "./ui/toast";
import { useTranslation } from "../lib/i18n";

export default function Layout({ children }: { children?: React.ReactNode }) {
  const location = useLocation();
  const [userInfo, setUserInfo] = useState({ host: "", user: "" });
  const [serverStatus, setServerStatus] = useState<boolean | null>(null);
  const [isDark, setIsDark] = useState(() => document.documentElement.classList.contains("dark"));
  const { toast } = useToast();
  const { t, lang, setLanguage } = useTranslation();

  useEffect(() => {
    api.get<{ host: string; user: string }>("/api/auth/me")
      .then(res => setUserInfo(res))
      .catch(() => {});
  }, []);

  const fetchStatus = () => {
    api.get<{ isRunning: boolean }>("/api/monitoring/dashboard")
      .then(res => {
        if (typeof res?.isRunning === "boolean") {
          setServerStatus(res.isRunning);
        }
      })
      .catch(() => {});
  };

  useEffect(() => {
    fetchStatus();
    const interval = setInterval(fetchStatus, 5000); 
    return () => clearInterval(interval);
  }, []);

  const handleLogout = async () => {
    try {
      await api.post("/api/auth/logout");
    } finally {
      window.location.href = "/ui/login";
    }
  };

  const handleServiceControl = async (action: string) => {
    try {
      await api.post(`/api/monitoring/control?action=${action}`);
      toast("info", "Command Sent", `Action: ${action}`);
      setTimeout(fetchStatus, 1000);
    } catch (err: any) {
      toast("error", "Error", err.message);
    }
  };

  const toggleDark = () => {
    const next = !isDark;
    setIsDark(next);
    document.documentElement.classList.toggle("dark", next);
    localStorage.setItem("theme", next ? "dark" : "light");
  };

  const toggleLang = () => {
    setLanguage(lang === "ru" ? "en" : "ru");
  };

  useEffect(() => {
    if (localStorage.getItem("theme") === "dark") {
      setIsDark(true);
      document.documentElement.classList.add("dark");
    }
  }, []);

  const navItems = [
    { name: t("sidebar.dashboard"), path: "/dashboard", icon: Server },
    { name: t("sidebar.shares"), path: "/shares", icon: FolderKanban },
    { name: t("sidebar.users"), path: "/users", icon: Users },
    { name: t("sidebar.groups"), path: "/groups", icon: Users },
    { name: t("sidebar.settings"), path: "/config", icon: Settings },
  ];

  return (
    <div className="min-h-screen flex text-foreground bg-background font-sans selection:bg-brand/20">
      <aside className="w-[240px] bg-surface flex-col hidden md:flex border-r border-border shrink-0 z-20">
        <div className="h-14 px-4 flex items-center justify-between border-b border-border shrink-0">
          <div className="flex items-center gap-2.5 font-semibold text-[15px] tracking-wide">
            <div className="bg-brand text-brand-text p-1.5 rounded-md shadow-sm">
              <Server className="w-4 h-4" />
            </div>
            <span>SAMBA<span className="opacity-50 font-bold ml-1 text-[11px] uppercase tracking-widest leading-none align-middle">ADM</span></span>
          </div>
          <div className="flex items-center gap-1">
            <button onClick={toggleLang} className="p-1.5 text-status-disabled hover:text-foreground hover:bg-surface-hover rounded-md transition-colors text-[10px] font-bold uppercase tracking-widest hidden sm:flex items-center gap-1">
              {lang}
            </button>
            <button onClick={toggleDark} className="p-1.5 text-status-disabled hover:text-foreground hover:bg-surface-hover rounded-md transition-colors hidden sm:block">
              {isDark ? <Sun className="w-4 h-4" /> : <Moon className="w-4 h-4" />}
            </button>
          </div>
        </div>

        {userInfo.host && (
          <div className="p-4 border-b border-border space-y-3">
            <div className="text-[11px] font-mono flex flex-col gap-1">
              <div className="flex items-center justify-between">
                <span className="text-status-disabled font-sans font-medium">{t("sidebar.server")}</span>
                <span className="truncate max-w-[120px] bg-surface-hover px-1.5 py-0.5 rounded-sm border border-border" title={userInfo.host}>{userInfo.host}</span>
              </div>
              <div className="flex items-center justify-between">
                <span className="text-status-disabled font-sans font-medium">{t("sidebar.user")}</span>
                <span className="text-foreground font-medium truncate max-w-[120px] bg-surface-hover px-1.5 py-0.5 rounded-sm border border-border" title={userInfo.user}>{userInfo.user}</span>
              </div>
            </div>
            
            <div className="flex items-center justify-between bg-surface-hover p-2.5 rounded-md border border-border shadow-sm-subtle">
              <div className="flex items-center gap-2">
                <div className="relative flex h-2.5 w-2.5">
                  {serverStatus === true && <span className="animate-ping absolute inline-flex h-full w-full rounded-full bg-status-active opacity-40"></span>}
                  <span className={`relative inline-flex rounded-full h-2.5 w-2.5 ${serverStatus === true ? 'bg-status-active' : serverStatus === false ? 'bg-status-error' : 'bg-status-disabled'}`}></span>
                </div>
                <span className="text-[11px] font-semibold text-foreground">
                  {serverStatus === true ? t("sidebar.running") : serverStatus === false ? t("sidebar.stopped") : t("sidebar.wait")}
                </span>
              </div>
              <div className="flex gap-0.5">
                {serverStatus === false && (
                  <button onClick={() => handleServiceControl("start")} className="p-1.5 text-foreground hover:bg-surface border border-transparent hover:border-border rounded-md shadow-sm hover:shadow-sm-subtle transition-all" title="Start">
                    <Play className="w-3.5 h-3.5" />
                  </button>
                )}
                {serverStatus === true && (
                  <button onClick={() => handleServiceControl("stop")} className="p-1.5 text-status-error hover:bg-surface border border-transparent hover:border-border rounded-md shadow-sm hover:shadow-sm-subtle transition-all" title="Stop">
                    <Square className="w-3.5 h-3.5" />
                  </button>
                )}
                <button onClick={() => handleServiceControl("restart")} className="p-1.5 text-foreground hover:bg-surface border border-transparent hover:border-border rounded-md shadow-sm hover:shadow-sm-subtle transition-all" title="Restart">
                  <RefreshCw className="w-3.5 h-3.5" />
                </button>
              </div>
            </div>
          </div>
        )}

        <nav className="flex-1 px-3 py-4 flex flex-col gap-1 overflow-y-auto custom-scrollbar">
          {navItems.map((item) => {
            const isActive = location.pathname.includes(item.path);
            const Icon = item.icon;
            return (
              <Link
                key={item.path}
                to={item.path}
                className={`flex items-center gap-3 px-3 py-2 rounded-md transition-all text-[13px] font-medium ${
                  isActive
                    ? "bg-brand text-brand-text shadow-sm"
                    : "text-foreground hover:bg-surface-hover"
                }`}
              >
                <Icon className={`w-4 h-4 shrink-0 ${isActive ? 'opacity-100' : 'opacity-70'}`} />
                {item.name}
              </Link>
            );
          })}
        </nav>

        <div className="p-3 border-t border-border shrink-0">
          <button
            onClick={handleLogout}
            className="w-full flex items-center justify-center gap-2 text-status-disabled hover:text-foreground hover:bg-surface-hover rounded-md transition-colors text-[13px] py-2 px-3 font-medium border border-transparent hover:border-border shadow-sm-subtle"
          >
            <LogOut className="w-4 h-4" /> {t("sidebar.logout")}
          </button>
        </div>
      </aside>

      <main className="flex-1 flex flex-col min-w-0 bg-background relative overflow-hidden">
        <div className="h-14 border-b border-border bg-surface px-4 sm:px-6 flex items-center justify-between md:hidden shrink-0">
           <div className="font-semibold text-sm flex items-center gap-2">
             <div className="bg-brand text-brand-text p-1 rounded-sm"><Server className="w-3.5 h-3.5" /></div>
             SAMBA<span className="text-status-disabled ml-1 text-[10px] uppercase font-bold">ADM</span>
           </div>
           <button onClick={toggleLang} className="text-xs font-bold uppercase">{lang}</button>
        </div>
        <div className="flex-1 p-4 sm:p-6 lg:p-8 overflow-y-auto custom-scrollbar">
          <div className="max-w-[1100px] w-full mx-auto pb-10">
            {children || <Outlet />}
          </div>
        </div>
      </main>
    </div>
  );
}

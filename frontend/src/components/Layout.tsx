import { useEffect, useState } from "react";
import { Link, useLocation, Outlet } from "react-router-dom";
import { Server, FolderKanban, Users, Settings, LogOut, UserCircle2, Play, Square, RefreshCw } from "lucide-react";
import { api } from "../lib/api";
import { useToast } from "./ui/toast";

export default function Layout({ children }: { children?: React.ReactNode }) {
  const location = useLocation();
  const [userInfo, setUserInfo] = useState({ host: "", user: "" });
  const [serverStatus, setServerStatus] = useState<boolean | null>(null);
  const { toast } = useToast();

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
    const interval = setInterval(fetchStatus, 10000); 
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
      toast("info", "Команда отправлена", `Сервис Samba получил команду ${action}`);
      setTimeout(fetchStatus, 1500);
    } catch (err: any) {
      toast("error", "Ошибка управления", err.message);
    }
  };

  const navItems = [
    { name: "Дашборд / Файлы", path: "/dashboard", icon: Server },
    { name: "Общие папки", path: "/shares", icon: FolderKanban },
    { name: "Пользователи", path: "/users", icon: UserCircle2 },
    { name: "Группы ОС", path: "/groups", icon: Users },
    { name: "Системный конфиг", path: "/config", icon: Settings },
  ];

  return (
    <div className="min-h-screen bg-[#f8fafc] flex font-sans text-slate-900 selection:bg-blue-100 selection:text-blue-900">
      {/* Боковая панель (Темная современная) */}
      <aside className="w-[280px] bg-[#0b1120] flex flex-col hidden md:flex border-r border-slate-800/60 z-20 shadow-2xl relative shrink-0">
        <div className="absolute top-0 left-0 right-0 h-32 bg-gradient-to-b from-blue-600/10 to-transparent pointer-events-none"></div>

        <div className="p-6 flex flex-col gap-6 relative z-10">
          <div className="flex items-center gap-3 font-extrabold text-2xl text-white tracking-tight">
            <div className="bg-gradient-to-br from-blue-500 to-indigo-600 p-2 rounded-xl shadow-lg shadow-blue-500/20 ring-1 ring-white/10">
              <Server className="w-5 h-5 text-white" />
            </div>
            SambaUI
          </div>

          {userInfo.host && (
            <div className="bg-white/5 border border-white/10 rounded-2xl p-4 backdrop-blur-md relative overflow-hidden group">
              <div className="absolute -right-4 -top-4 w-16 h-16 bg-blue-500/10 rounded-full blur-xl group-hover:bg-blue-500/20 transition-all"></div>
              
              <div className="text-[15px] text-white font-medium truncate flex items-center gap-2 mb-1 drop-shadow-sm" title={userInfo.user}>
                <UserCircle2 className="w-4 h-4 text-blue-400" /> {userInfo.user}
              </div>
              <div className="text-[13px] text-slate-400 font-mono truncate px-1 py-0.5" title={userInfo.host}>
                {userInfo.host}
              </div>
              
              <div className="mt-4 flex items-center justify-between border-t border-white/10 pt-4">
                <div className="flex items-center gap-2">
                  <div className="relative flex h-2.5 w-2.5">
                    {serverStatus === true && (
                      <span className="animate-ping absolute inline-flex h-full w-full rounded-full bg-emerald-400 opacity-75"></span>
                    )}
                    <span className={`relative inline-flex rounded-full h-2.5 w-2.5 ${serverStatus === true ? 'bg-emerald-500' : serverStatus === false ? 'bg-rose-500' : 'bg-slate-500'}`}></span>
                  </div>
                  <span className="text-xs font-medium text-slate-300 uppercase tracking-wider">
                    {serverStatus === true ? "Работает" : serverStatus === false ? "Остановлен" : "Связь..."}
                  </span>
                </div>
                <div className="flex gap-1.5 opacity-80 hover:opacity-100 transition-opacity">
                  {serverStatus === false && (
                    <button onClick={() => handleServiceControl("start")} className="p-1.5 text-emerald-400 hover:bg-emerald-400/20 rounded-md transition-colors" title="Start">
                      <Play className="w-3.5 h-3.5 fill-current" />
                    </button>
                  )}
                  {serverStatus === true && (
                    <button onClick={() => handleServiceControl("stop")} className="p-1.5 text-rose-400 hover:bg-rose-400/20 rounded-md transition-colors" title="Stop">
                      <Square className="w-3.5 h-3.5 fill-current" />
                    </button>
                  )}
                  <button onClick={() => handleServiceControl("restart")} className="p-1.5 text-blue-400 hover:bg-blue-400/20 rounded-md transition-colors" title="Restart">
                    <RefreshCw className="w-3.5 h-3.5" />
                  </button>
                </div>
              </div>
            </div>
          )}
        </div>

        <nav className="flex-1 px-4 pb-4 flex flex-col gap-1.5 relative z-10 overflow-y-auto custom-scrollbar">
          <div className="text-xs font-semibold text-slate-500 uppercase tracking-widest mb-2 px-2 mt-2">Управление</div>
          {navItems.map((item) => {
            const isActive = location.pathname.includes(item.path);
            const Icon = item.icon;
            return (
              <Link
                key={item.path}
                to={item.path}
                className={`flex items-center gap-3 px-3.5 py-3 rounded-xl transition-all duration-200 text-[15px] font-medium group ${
                  isActive
                    ? "bg-blue-600/15 text-blue-400"
                    : "text-slate-400 hover:bg-white/5 hover:text-slate-200"
                }`}
              >
                <Icon className={`w-5 h-5 transition-transform duration-200 ${isActive ? 'scale-110 drop-shadow-md' : 'group-hover:scale-110'}`} />
                {item.name}
              </Link>
            );
          })}
        </nav>

        <div className="p-4 border-t border-slate-800/60 relative z-10">
          <button
            onClick={handleLogout}
            className="w-full flex items-center justify-center gap-2 bg-transparent border border-slate-700/50 text-slate-300 hover:bg-rose-500/10 hover:text-rose-400 hover:border-rose-500/30 rounded-xl transition-all text-[15px] py-3.5 font-medium"
          >
            <LogOut className="w-4 h-4" /> Выйти
          </button>
        </div>
      </aside>

      {/* Основной контент */}
      <main className="flex-1 flex flex-col min-w-0 bg-[#f8fafc] relative overflow-hidden">
        <div className="absolute inset-0 bg-[url('https://grainy-gradients.vercel.app/noise.svg')] opacity-[0.015] mix-blend-overlay pointer-events-none z-0"></div>
        <div className="p-6 md:p-10 lg:p-12 overflow-y-auto h-screen relative z-10 w-full">
          <div className="max-w-[1200px] mx-auto">
            {children || <Outlet />}
          </div>
        </div>
      </main>
    </div>
  );
}

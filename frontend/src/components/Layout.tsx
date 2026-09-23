import { useEffect, useState } from "react";
import { Link, useLocation, Outlet } from "react-router-dom";
import { Server, FolderKanban, Users, Settings, LogOut, UserCircle2, HardDrive, Play, Square, RefreshCw } from "lucide-react";
import { Button } from "./ui/button";

export default function Layout({ children }: { children?: React.ReactNode }) {
  const location = useLocation();
  const [userInfo, setUserInfo] = useState({ host: "", user: "" });
  const [serverStatus, setServerStatus] = useState<boolean | null>(null);

  useEffect(() => {
    fetch("/api/auth/me")
      .then((res) => {
        if (res.status === 401) {
          window.location.href = "/ui/login";
        }
        return res.json();
      })
      .then((json) => {
        if (json && json.success && json.data) {
          setUserInfo(json.data);
        }
      })
      .catch(() => {});
  }, []);

  const fetchStatus = () => {
    fetch("/api/monitoring/dashboard")
      .then((res) => res.json())
      .then((json) => {
        if (json.success && typeof json.data?.isRunning === "boolean") {
          setServerStatus(json.data.isRunning);
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
    await fetch("/api/auth/logout", { method: "POST" });
    window.location.href = "/ui/login";
  };

  const handleServiceControl = async (action: string) => {
    try {
      const res = await fetch(`/api/monitoring/control?action=${action}`, { method: "POST" });
      if (res.ok) {
        setTimeout(fetchStatus, 1500);
      }
    } catch (err) {}
  };

  const navItems = [
    { name: "Папки", path: "/shares", icon: FolderKanban },
    { name: "Файлы", path: "/files", icon: HardDrive },
    { name: "Пользователи", path: "/users", icon: UserCircle2 },
    { name: "Группы", path: "/groups", icon: Users },
    { name: "Конфигурация", path: "/config", icon: Settings },
  ];

  return (
    <div className="min-h-screen bg-slate-50 flex">
      {/* Боковая панель (Sidebar) */}
      <aside className="w-72 bg-white shadow-xl flex flex-col hidden md:flex border-r border-slate-200 z-10">
        <div className="p-6 flex flex-col gap-3 border-b border-slate-100 bg-slate-50/50">
          <div className="flex items-center gap-3 font-bold text-xl text-slate-800">
            <Server className="w-8 h-8 text-blue-600" />
            <span>Samba Web UI</span>
          </div>

          {userInfo.host && (
            <div className="bg-white border border-slate-200 rounded-lg p-4 mt-2 shadow-sm">
              <div className="text-base text-slate-700 font-semibold truncate flex items-center gap-2 mb-1" title={userInfo.user}>
                <UserCircle2 className="w-4 h-4 text-slate-400" /> {userInfo.user}
              </div>
              <div className="text-sm text-slate-500 font-mono truncate bg-slate-50 px-2 py-1 rounded" title={userInfo.host}>
                {userInfo.host}
              </div>
              <div className="mt-4 flex items-center justify-between border-t border-slate-100 pt-4">
                <div className="flex items-center gap-2">
                  <div
                    className={`w-3 h-3 rounded-full ${
                      serverStatus === true
                        ? "bg-green-500"
                        : serverStatus === false
                        ? "bg-red-500"
                        : "bg-slate-300"
                    }`}
                  ></div>
                  <span className="text-sm font-semibold text-slate-600">
                    {serverStatus === true
                      ? "Active"
                      : serverStatus === false
                      ? "Stopped"
                      : "Connecting..."}
                  </span>
                </div>
                <div className="flex gap-1">
                  {serverStatus === false && (
                    <button
                      onClick={() => handleServiceControl("start")}
                      className="p-1.5 text-green-600 hover:bg-green-100 rounded"
                      title="Start"
                    >
                      <Play className="w-4 h-4" />
                    </button>
                  )}
                  {serverStatus === true && (
                    <button
                      onClick={() => handleServiceControl("stop")}
                      className="p-1.5 text-red-600 hover:bg-red-100 rounded"
                      title="Stop"
                    >
                      <Square className="w-4 h-4" />
                    </button>
                  )}
                  <button
                    onClick={() => handleServiceControl("restart")}
                    className="p-1.5 text-blue-600 hover:bg-blue-100 rounded"
                    title="Restart"
                  >
                    <RefreshCw className="w-4 h-4" />
                  </button>
                </div>
              </div>
            </div>
          )}
        </div>

        <nav className="flex-1 p-4 flex flex-col gap-1.5">
          {navItems.map((item) => {
            const isActive = location.pathname.includes(item.path);
            const Icon = item.icon;
            return (
              <Link
                key={item.path}
                to={item.path}
                className={`flex items-center gap-3 px-4 py-3 rounded-lg transition-all text-base font-semibold tracking-wide ${
                  isActive
                    ? "bg-blue-600 text-white shadow-md shadow-blue-200"
                    : "text-slate-600 hover:bg-blue-50 hover:text-blue-700"
                }`}
              >
                <Icon className="w-5 h-5" />
                {item.name}
              </Link>
            );
          })}
        </nav>

        <div className="p-6 border-t border-slate-100">
          <Button
            onClick={handleLogout}
            className="w-full flex justify-center gap-2 bg-slate-100 border border-slate-200 text-slate-700 hover:bg-slate-200 text-base py-6 font-semibold"
          >
            <LogOut className="w-5 h-5" /> Выйти
          </Button>
        </div>
      </aside>

      {/* Основной контент */}
      <main className="flex-1 flex flex-col min-w-0 bg-slate-50/50">
        <div className="p-6 md:p-12 overflow-y-auto h-screen">
          <div className="max-w-6xl mx-auto">
            {/* Отрисовка вложенных маршрутов от React Router (эквивалент <Outlet />) */}
            {children || <Outlet />}
          </div>
        </div>
      </main>
    </div>
  );
}

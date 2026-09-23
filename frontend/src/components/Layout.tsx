import { useEffect, useState } from "react";
import { Outlet, Link, useLocation } from "react-router-dom";
import { Server, LayoutDashboard, FolderKanban, Users, Settings, LogOut, UserCircle2 } from "lucide-react";
import { Button } from "./ui/button";

export default function Layout() {
  const location = useLocation();
  const [userInfo, setUserInfo] = useState({ host: "", user: "" });

  useEffect(() => {
    fetch("/api/auth/me")
      .then(res => res.json())
      .then(json => {
        if (json.success && json.data) {
          setUserInfo(json.data);
        }
      })
      .catch(() => {});
  }, []);

  const handleLogout = async () => {
    await fetch("/api/auth/logout", { method: "POST" });
    window.location.href = "/ui/login";
  };

  const navItems = [
    { name: "Дашборд", path: "/dashboard", icon: LayoutDashboard },
    { name: "Общие папки", path: "/shares", icon: FolderKanban },
    { name: "Группы (Скоро)", path: "/groups", icon: Users }, // Добавим заглушку для групп
    { name: "Пользователи", path: "/users", icon: UserCircle2 },
    { name: "Конфиг", path: "/config", icon: Settings },
  ];

  return (
    <div className="min-h-screen bg-muted/30 flex">
      {/* Боковая панель (Sidebar) */}
      <aside className="w-64 bg-background border-r border-border flex flex-col hidden md:flex">
        <div className="p-6 flex flex-col gap-1 border-b border-border">
          <div className="flex items-center gap-3 font-bold text-lg">
            <Server className="w-6 h-6 text-primary" />
            <span>Samba Web UI</span>
          </div>
          {userInfo.host && (
            <div className="text-xs text-muted-foreground mt-2 pl-9">
              <div><span className="font-semibold">{userInfo.user}</span> @ {userInfo.host}</div>
            </div>
          )}
        </div>
        
        <nav className="flex-1 p-4 flex flex-col gap-2">
          {navItems.map((item) => {
            const isActive = location.pathname.includes(item.path);
            const Icon = item.icon;
            return (
              <Link
                key={item.path}
                to={item.path}
                className={`flex items-center gap-3 px-4 py-2 rounded-md transition-colors ${
                  isActive 
                  ? "bg-primary text-primary-foreground font-medium" 
                  : "text-muted-foreground hover:bg-muted"
                }`}
              >
                <Icon className="w-5 h-5" />
                {item.name}
              </Link>
            );
          })}
        </nav>

        <div className="p-4 border-t border-border">
          <Button 
            onClick={handleLogout} 
            className="w-full flex justify-start gap-3 !bg-transparent border border-border text-foreground hover:bg-muted"
          >
            <LogOut className="w-4 h-4" /> Выйти
          </Button>
        </div>
      </aside>

      {/* Основной контент */}
      <main className="flex-1 flex flex-col min-w-0">
        <div className="p-6 md:p-12 overflow-y-auto h-screen">
          <div className="max-w-6xl mx-auto">
            <Outlet />
          </div>
        </div>
      </main>
    </div>
  );
}

import { useEffect, useState } from "react";
import { useNavigate } from "react-router-dom";
import { HardDrive, Server, Users, FileStack, LogOut, Power } from "lucide-react";
import { Button } from "../components/ui/button";
import { Card, CardContent, CardHeader, CardTitle } from "../components/ui/card";

interface DashboardData {
  isRunning: boolean;
  diskUsage: string[];
  connections: any[];
  openFiles: any[];
}

export default function DashboardPage() {
  const navigate = useNavigate();
  const [data, setData] = useState<DashboardData | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");

  const fetchDashboard = async () => {
    try {
      setLoading(true);
      const res = await fetch("/api/monitoring/dashboard");
      
      if (res.status === 401) {
        navigate("/login");
        return;
      }
      
      const json = await res.json();
      if (json.success) {
        setData(json.data);
        setError("");
      } else {
        setError(json.message || "Ошибка загрузки дашборда");
      }
    } catch (err) {
      setError("Ошибка соединения с сервером");
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchDashboard();
    const interval = setInterval(fetchDashboard, 10000);
    return () => clearInterval(interval);
  }, []);

  const handleLogout = async () => {
    await fetch("/api/auth/logout", { method: "POST" });
    navigate("/login");
  };

  const handleServiceControl = async (action: string) => {
    try {
      const res = await fetch(`/api/monitoring/control?action=${action}`, { method: "POST" });
      if (res.ok) fetchDashboard();
    } catch (err) {}
  };

  return (
    <div className="min-h-screen bg-muted/30 p-6 md:p-12">
      <div className="max-w-6xl mx-auto space-y-6">
        
        {/* Header */}
        <header className="flex justify-between items-center mb-8 bg-background p-4 rounded-xl border border-border shadow-sm">
          <div className="flex items-center gap-3 text-lg font-bold">
            <Server className="w-6 h-6 text-primary" />
            Samba Web UI
          </div>
          <Button onClick={handleLogout} className="flex gap-2 !bg-transparent border border-border text-foreground hover:bg-muted">
            <LogOut className="w-4 h-4" /> Выйти
          </Button>
        </header>

        {error && (
          <div className="bg-red-50 text-red-600 p-4 rounded-md border border-red-100 mb-6">
            {error}
          </div>
        )}

        {/* Loading State */}
        {loading && !data && (
          <div className="flex justify-center py-20 text-muted-foreground animate-pulse">
            Синхронизация с сервером...
          </div>
        )}

        {/* Dashboard Grid */}
        {data && (
          <div className="grid grid-cols-1 md:grid-cols-3 gap-6">
            
            {/* Статус Сервиса */}
            <Card>
              <CardHeader className="flex flex-row items-center justify-between pb-2">
                <CardTitle className="text-sm font-medium text-muted-foreground">Статус Службы (smbd)</CardTitle>
                <Power className={`w-4 h-4 ${data.isRunning ? "text-green-500" : "text-red-500"}`} />
              </CardHeader>
              <CardContent>
                <div className="text-2xl font-bold mb-4">
                  {data.isRunning ? "Активен" : "Остановлен"}
                </div>
                <div className="flex gap-2">
                  {!data.isRunning && (
                    <Button className="w-full bg-green-600 hover:bg-green-700 text-white h-8 text-xs" onClick={() => handleServiceControl("start")}>
                      Запустить
                    </Button>
                  )}
                  {data.isRunning && (
                    <Button className="w-full !bg-transparent border border-border text-foreground hover:bg-muted h-8 text-xs" onClick={() => handleServiceControl("restart")}>
                      Перезапустить
                    </Button>
                  )}
                </div>
              </CardContent>
            </Card>

            {/* Соединения */}
            <Card>
              <CardHeader className="flex flex-row items-center justify-between pb-2">
                <CardTitle className="text-sm font-medium text-muted-foreground">Активные Юзеры</CardTitle>
                <Users className="w-4 h-4 text-blue-500" />
              </CardHeader>
              <CardContent>
                <div className="text-2xl font-bold">{data.connections.length}</div>
                <p className="text-xs text-muted-foreground mt-1">Клиентов подключено сейчас</p>
              </CardContent>
            </Card>

            {/* Диски */}
            <Card>
              <CardHeader className="flex flex-row items-center justify-between pb-2">
                <CardTitle className="text-sm font-medium text-muted-foreground">Место на дисках</CardTitle>
                <HardDrive className="w-4 h-4 text-purple-500" />
              </CardHeader>
              <CardContent>
                {data.diskUsage.length > 0 ? (
                  <div className="space-y-1">
                    {data.diskUsage.slice(0, 3).map((disk, i) => (
                      <div key={i} className="text-sm font-mono truncate bg-muted/50 px-2 py-1 rounded">
                        {disk}
                      </div>
                    ))}
                  </div>
                ) : (
                  <div className="text-2xl font-bold text-muted-foreground">Н/Д</div>
                )}
              </CardContent>
            </Card>

            {/* Логика для Открытых файлов */}
            <Card className="md:col-span-3">
              <CardHeader className="flex flex-row items-center justify-between">
                <CardTitle>Открытые файлы</CardTitle>
                <FileStack className="w-4 h-4 text-orange-500" />
              </CardHeader>
              <CardContent>
                {data.openFiles.length === 0 ? (
                  <p className="text-sm text-center py-6 text-muted-foreground bg-muted/20 rounded-md">
                    В данный момент никто не скачивает и не открывает файлы
                  </p>
                ) : (
                  <div className="overflow-x-auto">
                    <table className="w-full text-sm text-left whitespace-nowrap">
                      <thead className="text-muted-foreground border-b border-border">
                        <tr>
                          <th className="font-medium pb-2 pr-4">PID</th>
                          <th className="font-medium pb-2 pr-4">Пользователь</th>
                          <th className="font-medium pb-2 pr-4">Файл</th>
                          <th className="font-medium pb-2 pr-4">Доступ</th>
                        </tr>
                      </thead>
                      <tbody className="divide-y divide-border">
                        {data.openFiles.map((file, i) => (
                          <tr key={i} className="hover:bg-muted/30">
                            <td className="py-2 pr-4">{typeof file === 'object' ? file.pid : '-'}</td>
                            <td className="py-2 pr-4">{typeof file === 'object' ? file.user : '-'}</td>
                            <td className="py-2 pr-4 truncate max-w-[300px]" title={typeof file === 'object' ? file.path : file.toString()}>
                                {typeof file === 'object' ? file.path : file.toString()}
                            </td>
                            <td className="py-2 pr-4">{typeof file === 'object' ? file.rw : '-'}</td>
                          </tr>
                        ))}
                      </tbody>
                    </table>
                  </div>
                )}
              </CardContent>
            </Card>

          </div>
        )}
      </div>
    </div>
  );
}

import { useEffect, useState } from "react";
import { useNavigate } from "react-router-dom";
import { HardDrive, Users, FileStack, Power } from "lucide-react";
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
        setError(json.message || "╨Ю╤И╨╕╨▒╨║╨░ ╨╖╨░╨│╤А╤Г╨╖╨║╨╕ ╨┤╨░╤И╨▒╨╛╤А╨┤╨░");
      }
    } catch (err) {
      setError("╨Ю╤И╨╕╨▒╨║╨░ ╤Б╨╛╨╡╨┤╨╕╨╜╨╡╨╜╨╕╤П ╤Б ╤Б╨╡╤А╨▓╨╡╤А╨╛╨╝");
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchDashboard();
    const interval = setInterval(fetchDashboard, 10000);
    return () => clearInterval(interval);
  }, []);

  const handleServiceControl = async (action: string) => {
    try {
      const res = await fetch(`/api/monitoring/control?action=${action}`, { method: "POST" });
      if (res.ok) fetchDashboard();
    } catch (err) {}
  };

  return (
    <div className="space-y-6">
      
      <div>
        <h1 className="text-3xl font-bold tracking-tight">╨Ф╨░╤И╨▒╨╛╤А╨┤</h1>
        <p className="text-muted-foreground">╨Ь╨╛╨╜╨╕╤В╨╛╤А╨╕╨╜╨│ ╤А╨╡╤Б╤Г╤А╤Б╨╛╨▓ ╨╕ ╤Б╨╗╤Г╨╢╨▒╤Л Samba.</p>
      </div>

      {error && (
        <div className="bg-red-50 text-red-600 p-4 rounded-md border border-red-100 mb-6">
          {error}
        </div>
      )}

      {loading && !data && (
        <div className="flex justify-center py-20 text-muted-foreground animate-pulse">
          ╨б╨╕╨╜╤Е╤А╨╛╨╜╨╕╨╖╨░╤Ж╨╕╤П ╤Б ╤Б╨╡╤А╨▓╨╡╤А╨╛╨╝...
        </div>
      )}

      {data && (
        <div className="grid grid-cols-1 md:grid-cols-3 gap-6">
          
          <Card>
            <CardHeader className="flex flex-row items-center justify-between pb-2">
              <CardTitle className="text-sm font-medium text-muted-foreground">╨б╤В╨░╤В╤Г╤Б ╨б╨╗╤Г╨╢╨▒╤Л (smbd)</CardTitle>
              <Power className={`w-4 h-4 ${data.isRunning ? "text-green-500" : "text-red-500"}`} />
            </CardHeader>
            <CardContent>
              <div className="text-2xl font-bold mb-4">
                {data.isRunning ? "╨Р╨║╤В╨╕╨▓╨╡╨╜" : "╨Ю╤Б╤В╨░╨╜╨╛╨▓╨╗╨╡╨╜"}
              </div>
              <div className="flex gap-2">
                {!data.isRunning && (
                  <Button className="w-full bg-green-600 hover:bg-green-700 text-white h-8 text-xs" onClick={() => handleServiceControl("start")}>
                    ╨Ч╨░╨┐╤Г╤Б╤В╨╕╤В╤М
                  </Button>
                )}
                {data.isRunning && (
                  <Button className="w-full !bg-transparent border border-border text-foreground hover:bg-muted h-8 text-xs" onClick={() => handleServiceControl("restart")}>
                    ╨Я╨╡╤А╨╡╨╖╨░╨┐╤Г╤Б╤В╨╕╤В╤М
                  </Button>
                )}
              </div>
            </CardContent>
          </Card>

          <Card>
            <CardHeader className="flex flex-row items-center justify-between pb-2">
              <CardTitle className="text-sm font-medium text-muted-foreground">╨Р╨║╤В╨╕╨▓╨╜╤Л╨╡ ╨о╨╖╨╡╤А╤Л</CardTitle>
              <Users className="w-4 h-4 text-blue-500" />
            </CardHeader>
            <CardContent>
              <div className="text-2xl font-bold">{data.connections.length}</div>
              <p className="text-xs text-muted-foreground mt-1">╨Ъ╨╗╨╕╨╡╨╜╤В╨╛╨▓ ╨┐╨╛╨┤╨║╨╗╤О╤З╨╡╨╜╨╛ ╤Б╨╡╨╣╤З╨░╤Б</p>
            </CardContent>
          </Card>

          <Card>
            <CardHeader className="flex flex-row items-center justify-between pb-2">
              <CardTitle className="text-sm font-medium text-muted-foreground">╨Ь╨╡╤Б╤В╨╛ ╨╜╨░ ╨┤╨╕╤Б╨║╨░╤Е</CardTitle>
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
                <div className="text-2xl font-bold text-muted-foreground">╨Э/╨Ф</div>
              )}
            </CardContent>
          </Card>

          <Card className="md:col-span-3">
            <CardHeader className="flex flex-row items-center justify-between">
              <CardTitle>╨Ю╤В╨║╤А╤Л╤В╤Л╨╡ ╤Д╨░╨╣╨╗╤Л</CardTitle>
              <FileStack className="w-4 h-4 text-orange-500" />
            </CardHeader>
            <CardContent>
              {data.openFiles.length === 0 ? (
                <p className="text-sm text-center py-6 text-muted-foreground bg-muted/20 rounded-md">
                  ╨Т ╨┤╨░╨╜╨╜╤Л╨╣ ╨╝╨╛╨╝╨╡╨╜╤В ╨╜╨╕╨║╤В╨╛ ╨╜╨╡ ╤Б╨║╨░╤З╨╕╨▓╨░╨╡╤В ╨╕ ╨╜╨╡ ╨╛╤В╨║╤А╤Л╨▓╨░╨╡╤В ╤Д╨░╨╣╨╗╤Л
                </p>
              ) : (
                <div className="overflow-x-auto">
                  <table className="w-full text-sm text-left whitespace-nowrap">
                    <thead className="text-muted-foreground border-b border-border">
                      <tr>
                        <th className="font-medium pb-2 pr-4">PID</th>
                        <th className="font-medium pb-2 pr-4">╨Я╨╛╨╗╤М╨╖╨╛╨▓╨░╤В╨╡╨╗╤М</th>
                        <th className="font-medium pb-2 pr-4">╨д╨░╨╣╨╗</th>
                        <th className="font-medium pb-2 pr-4">╨Ф╨╛╤Б╤В╤Г╨┐</th>
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
  );
}


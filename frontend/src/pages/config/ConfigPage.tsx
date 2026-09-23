import { useEffect, useState } from "react";
import { Settings, Save, Archive, RotateCcw } from "lucide-react";
import { Button } from "../../components/ui/button";
import { Input } from "../../components/ui/input";
import { Card, CardContent, CardHeader, CardTitle } from "../../components/ui/card";

interface GlobalConfig {
  workgroup: string;
  serverString: string;
  netbiosName: string;
  security: string;
  mapToGuest: string;
  interfaces: string;
  bindInterfacesOnly: boolean;
  loadPrinters: boolean;
  disableNetbios: boolean;
  serverMinProtocol: string;
  serverMaxProtocol: string;
}

interface Backup {
  filename: string;
  createdAt: string;
  sizeBytes: number;
}

export default function ConfigPage() {
  const [config, setConfig] = useState<GlobalConfig | null>(null);
  const [backups, setBackups] = useState<Backup[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");
  const [isSaving, setIsSaving] = useState(false);

  const fetchData = async () => {
    try {
      setLoading(true);
      const [configRes, backupRes] = await Promise.all([
        fetch("/api/config/global"),
        fetch("/api/config/backups")
      ]);
      const configJson = await configRes.json();
      const backupJson = await backupRes.json();

      if (configJson.success) setConfig(configJson.data);
      else setError(configJson.message || "Ошибка загрузки конфигурации");

      if (backupJson.success) setBackups(backupJson.data);
    } catch (err) {
      setError("Ошибка API");
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchData();
  }, []);

  const handleSave = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!config) return;
    setIsSaving(true);
    try {
      const res = await fetch("/api/config/global", {
        method: "PUT",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify(config)
      });
      if (res.ok) {
        alert("Конфигурация [global] успешно сохранена! Служба Samba перезапущена.");
        fetchData(); // reload backups list
      } else {
        const json = await res.json();
        alert(json.message || "Ошибка сохранения");
      }
    } catch (err) {
      alert("Ошибка сети");
    } finally {
      setIsSaving(false);
    }
  };

  const handleRestore = async (filename: string) => {
    if (!confirm(`Вы действительно хотите восстановить конфигурацию из файла ${filename}? Текущие настройки будут потеряны!`)) return;
    
    try {
      const res = await fetch(`/api/config/backups/${filename}/restore`, { method: "POST" });
      if (res.ok) {
        alert("Конфигурация успешно восстановлена!");
        fetchData();
      } else {
        const json = await res.json();
        alert(json.message || "Ошибка восстановления из резервной копии");
      }
    } catch (err) {
      alert("Ошибка сети");
    }
  };

  if (loading) {
    return <div className="animate-pulse text-muted-foreground p-12 text-center">Загрузка настроек...</div>;
  }

  return (
    <div className="space-y-6">
      <div>
        <h1 className="text-3xl font-bold tracking-tight">Глобальные настройки</h1>
        <p className="text-muted-foreground">Управление параметрами секции [global] в smb.conf и бэкапами конфигурации.</p>
      </div>

      {error && <div className="text-red-500 bg-red-50 p-4 rounded-lg">{error}</div>}

      <div className="grid grid-cols-1 md:grid-cols-3 gap-6">
        
        {/* Main Config Form */}
        <div className="md:col-span-2">
          <Card>
            <CardHeader>
              <CardTitle className="flex items-center gap-2">
                <Settings className="w-5 h-5 text-primary" /> 
                Секция [global]
              </CardTitle>
            </CardHeader>
            <CardContent>
              {config && (
                <form onSubmit={handleSave} className="space-y-4">
                  <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
                    <div className="space-y-2">
                      <label className="text-sm font-medium">Рабочая группа (Workgroup)</label>
                      <Input 
                        required 
                        value={config.workgroup} 
                        onChange={(e) => setConfig({ ...config, workgroup: e.target.value })} 
                      />
                    </div>
                    <div className="space-y-2">
                      <label className="text-sm font-medium">NetBIOS Имя</label>
                      <Input 
                        value={config.netbiosName || ""} 
                        onChange={(e) => setConfig({ ...config, netbiosName: e.target.value })} 
                      />
                    </div>
                  </div>

                  <div className="space-y-2">
                    <label className="text-sm font-medium">Описание (Server string)</label>
                    <Input 
                      value={config.serverString || ""} 
                      onChange={(e) => setConfig({ ...config, serverString: e.target.value })} 
                    />
                  </div>

                  <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
                    <div className="space-y-2">
                      <label className="text-sm font-medium">Security</label>
                      <select 
                        className="flex h-9 w-full rounded-md border border-border bg-transparent px-3 py-1 text-sm shadow-sm"
                        value={config.security} 
                        onChange={(e) => setConfig({ ...config, security: e.target.value })}
                      >
                        <option value="user">user (по умолчанию)</option>
                        <option value="ads">ads (Active Directory)</option>
                      </select>
                    </div>
                    <div className="space-y-2">
                      <label className="text-sm font-medium">Map to Guest</label>
                      <select 
                        className="flex h-9 w-full rounded-md border border-border bg-transparent px-3 py-1 text-sm shadow-sm"
                        value={config.mapToGuest} 
                        onChange={(e) => setConfig({ ...config, mapToGuest: e.target.value })}
                      >
                        <option value="Bad User">Bad User (гость если юзер не найден)</option>
                        <option value="Never">Never (без гостей)</option>
                      </select>
                    </div>
                  </div>

                  <div className="grid grid-cols-1 md:grid-cols-2 gap-4 pt-2">
                    <div className="space-y-2">
                      <label className="text-sm font-medium">Min Protocol</label>
                      <select 
                        className="flex h-9 w-full rounded-md border border-border bg-transparent px-3 py-1 text-sm shadow-sm"
                        value={config.serverMinProtocol} 
                        onChange={(e) => setConfig({ ...config, serverMinProtocol: e.target.value })}
                      >
                        <option value="NT1">NT1 (SMBv1, небезопасно)</option>
                        <option value="SMB2">SMB2 (по умолчанию)</option>
                        <option value="SMB3">SMB3</option>
                      </select>
                    </div>
                    <div className="space-y-2">
                      <label className="text-sm font-medium">Max Protocol</label>
                      <select 
                        className="flex h-9 w-full rounded-md border border-border bg-transparent px-3 py-1 text-sm shadow-sm"
                        value={config.serverMaxProtocol} 
                        onChange={(e) => setConfig({ ...config, serverMaxProtocol: e.target.value })}
                      >
                        <option value="SMB2">SMB2</option>
                        <option value="SMB3">SMB3 (по умолчанию)</option>
                      </select>
                    </div>
                  </div>

                  <div className="space-y-4 py-4 border-t border-b border-border text-sm">
                    <div className="flex items-center gap-2">
                      <input type="checkbox" id="printers" 
                        checked={config.loadPrinters} 
                        onChange={(e) => setConfig({ ...config, loadPrinters: e.target.checked })} 
                      />
                      <label htmlFor="printers">Загружать принтеры (load printers)</label>
                    </div>
                    <div className="flex items-center gap-2">
                      <input type="checkbox" id="netbios" 
                        checked={config.disableNetbios} 
                        onChange={(e) => setConfig({ ...config, disableNetbios: e.target.checked })} 
                      />
                      <label htmlFor="netbios">Отключить NetBIOS</label>
                    </div>
                  </div>
                  
                  <div className="flex justify-end pt-2">
                    <Button type="submit" disabled={isSaving} className="flex gap-2 w-full md:w-auto">
                      <Save className="w-4 h-4" /> {isSaving ? "Сохраняем..." : "Сохранить и Перезапустить"}
                    </Button>
                  </div>
                </form>
              )}
            </CardContent>
          </Card>
        </div>

        {/* Backups Panel */}
        <div className="md:col-span-1 space-y-4">
          <Card>
            <CardHeader>
              <CardTitle className="flex items-center gap-2 text-primary">
                <Archive className="w-5 h-5" /> 
                Резервные копии
              </CardTitle>
            </CardHeader>
            <CardContent>
              {backups.length === 0 ? (
                <p className="text-sm text-muted-foreground text-center py-4">Бэкапов пока нет</p>
              ) : (
                <div className="space-y-3">
                  {backups.map(b => (
                    <div key={b.filename} className="p-3 bg-muted/30 rounded-lg border border-border text-sm flex flex-col gap-2">
                      <div className="font-mono text-xs overflow-hidden text-ellipsis font-bold">
                        {b.filename}
                      </div>
                      <div className="flex justify-between items-center text-xs text-muted-foreground">
                        <span>{new Date(b.createdAt).toLocaleDateString()} {new Date(b.createdAt).toLocaleTimeString()}</span>
                        <span>{Math.round(b.sizeBytes / 1024)} KB</span>
                      </div>
                      <Button onClick={() => handleRestore(b.filename)} className="w-full !bg-transparent text-foreground border border-border hover:bg-muted text-xs h-7 gap-2 mt-1">
                        <RotateCcw className="w-3 h-3" /> Восстановить конфигурацию
                      </Button>
                    </div>
                  ))}
                </div>
              )}
            </CardContent>
          </Card>
        </div>

      </div>
    </div>
  );
}

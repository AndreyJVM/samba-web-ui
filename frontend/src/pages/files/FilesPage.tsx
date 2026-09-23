import { useEffect, useState } from "react";
import { Folder, HardDrive, ArrowUp, FolderPlus, ChevronRight } from "lucide-react";
import { Button } from "../../components/ui/button";
import { Input } from "../../components/ui/input";
import { Card, CardContent, CardHeader, CardTitle } from "../../components/ui/card";

interface DirectoryItem {
  name: string;
  fullPath: string;
}

interface BrowseResult {
  currentPath: string;
  parentPath: string | null;
  directories: DirectoryItem[];
}

interface DiskUsage {
  total: string;
  used: string;
  available: string;
  usePercent: number;
  mountPoint: string;
}

export default function FilesPage() {
  const [currentPath, setCurrentPath] = useState("/");
  const [browseData, setBrowseData] = useState<BrowseResult | null>(null);
  const [diskUsage, setDiskUsage] = useState<DiskUsage | null>(null);
  
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState("");

  const [isCreatingDir, setIsCreatingDir] = useState(false);
  const [newDirName, setNewDirName] = useState("");

  const fetchData = async (path: string) => {
    setLoading(true);
    setError("");
    try {
      const [browseRes, diskRes] = await Promise.all([
        fetch(`/api/fs/browse?path=${encodeURIComponent(path)}`),
        fetch(`/api/fs/disk-usage?path=${encodeURIComponent(path)}`)
      ]);

      const browseJson = await browseRes.json();
      const diskJson = await diskRes.json();

      if (browseJson.success) {
        setBrowseData(browseJson.data);
      } else {
        setError(browseJson.message || "Ошибка загрузки папок");
      }

      if (diskJson.success) {
        setDiskUsage(diskJson.data);
      }
    } catch (err) {
      setError("Ошибка API при работе с ФС");
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchData(currentPath);
  }, [currentPath]);

  const handleCreateDirectory = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!newDirName) return;
    
    try {
      const res = await fetch(`/api/fs/mkdir?parentPath=${encodeURIComponent(currentPath)}&name=${encodeURIComponent(newDirName)}`, {
        method: "POST"
      });
      if (res.ok) {
        setIsCreatingDir(false);
        setNewDirName("");
        fetchData(currentPath); // reload
      } else {
        const json = await res.json();
        alert(json.message || "Ошибка создания каталога");
      }
    } catch (err) {
      alert("Ошибка сети");
    }
  };

  const navigateTo = (path: string) => {
    setCurrentPath(path);
  };

  return (
    <div className="space-y-6">
      <div className="flex justify-between items-center">
        <div>
          <h1 className="text-3xl font-bold tracking-tight">Файловый менеджер</h1>
          <p className="text-muted-foreground">Просмотр директорий и статистика дисков.</p>
        </div>
        <Button onClick={() => setIsCreatingDir(true)} className="flex gap-2">
          <FolderPlus className="w-4 h-4" /> Создать папку
        </Button>
      </div>

      {error && <div className="text-red-500 bg-red-50 p-4 rounded-lg">{error}</div>}

      <div className="grid grid-cols-1 md:grid-cols-4 gap-6">
        
        {/* Панель диска */}
        <div className="md:col-span-1 space-y-4">
          <Card>
            <CardHeader className="pb-3">
              <CardTitle className="flex items-center gap-2 text-primary text-lg">
                <HardDrive className="w-5 h-5" /> 
                Хранилище
              </CardTitle>
            </CardHeader>
            <CardContent>
              {diskUsage ? (
                <div className="space-y-4">
                  <div className="space-y-1">
                    <div className="flex justify-between text-sm">
                      <span className="text-muted-foreground">Точка:</span>
                      <span className="font-medium text-foreground text-xs">{diskUsage.mountPoint}</span>
                    </div>
                    <div className="flex justify-between text-sm">
                      <span className="text-muted-foreground">Занято:</span>
                      <span className="font-medium text-foreground text-xs">{diskUsage.used} / {diskUsage.total}</span>
                    </div>
                    <div className="flex justify-between text-sm">
                      <span className="text-muted-foreground">Свободно:</span>
                      <span className="font-medium text-green-600 text-xs">{diskUsage.available}</span>
                    </div>
                  </div>
                  
                  <div className="space-y-1">
                    <div className="h-2 w-full bg-muted rounded-full overflow-hidden">
                      <div 
                        className={`h-full ${diskUsage.usePercent > 90 ? 'bg-red-500' : diskUsage.usePercent > 75 ? 'bg-yellow-500' : 'bg-primary'}`} 
                        style={{ width: `${diskUsage.usePercent}%` }}
                      ></div>
                    </div>
                    <div className="text-right text-xs text-muted-foreground">{diskUsage.usePercent}% заполнено</div>
                  </div>
                </div>
              ) : (
                <div className="text-sm text-muted-foreground">Информация недоступна</div>
              )}
            </CardContent>
          </Card>
        </div>

        {/* Основная панель ФС */}
        <div className="md:col-span-3">
          <Card className="h-full flex flex-col">
            <div className="bg-muted/40 p-4 border-b border-border flex flex-col md:flex-row gap-4 items-start md:items-center justify-between rounded-t-xl">
              <div className="flex items-center text-sm font-mono flex-wrap bg-background border border-border px-3 py-1.5 rounded-md min-w-[50%]">
                <span className="text-muted-foreground mr-2">Путь:</span> 
                {currentPath}
              </div>
              
              {isCreatingDir && (
                <form onSubmit={handleCreateDirectory} className="flex gap-2 w-full md:w-auto">
                  <Input 
                    autoFocus
                    placeholder="Имя новой папки..." 
                    className="h-8 text-sm max-w-[200px]"
                    value={newDirName}
                    onChange={(e) => setNewDirName(e.target.value)}
                  />
                  <Button type="submit" className="h-8 text-xs py-1 px-3">Создать</Button>
                  <Button type="button" className="h-8 text-xs py-1 px-3 !bg-transparent text-foreground border border-border hover:bg-muted" onClick={() => setIsCreatingDir(false)}>Отмена</Button>
                </form>
              )}
            </div>

            <CardContent className="p-0 flex-1 overflow-auto min-h-[400px]">
              {loading ? (
                <div className="p-8 text-center text-muted-foreground animate-pulse">Загрузка директорий...</div>
              ) : (
                <div className="flex flex-col">
                  {/* Родительская папка */}
                  {browseData?.parentPath && (
                    <button 
                      onClick={() => navigateTo(browseData.parentPath!)}
                      className="flex items-center gap-3 p-4 border-b border-border hover:bg-muted/50 transition-colors text-left"
                    >
                      <div className="p-2 bg-muted rounded-md text-muted-foreground">
                        <ArrowUp className="w-5 h-5" />
                      </div>
                      <span className="font-medium text-foreground">.. (На уровень вверх)</span>
                    </button>
                  )}

                  {/* Список папок */}
                  {browseData?.directories.length === 0 ? (
                    <div className="p-12 text-center text-muted-foreground">
                      В этой директории нет вложенных папок
                    </div>
                  ) : (
                    browseData?.directories.map((dir) => (
                      <button 
                        key={dir.fullPath}
                        onClick={() => navigateTo(dir.fullPath)}
                        className="flex items-center gap-3 p-4 border-b border-border hover:bg-muted/30 transition-colors text-left group"
                      >
                        <div className="p-2 bg-blue-500/10 rounded-md text-blue-500 group-hover:bg-blue-500 group-hover:text-white transition-colors">
                          <Folder className="w-5 h-5" />
                        </div>
                        <span className="font-medium text-foreground flex-1 truncate">{dir.name}</span>
                        <ChevronRight className="w-4 h-4 text-muted-foreground opacity-50 group-hover:opacity-100 group-hover:text-primary transition-all group-hover:translate-x-1" />
                      </button>
                    ))
                  )}
                </div>
              )}
            </CardContent>
          </Card>
        </div>
      </div>
    </div>
  );
}

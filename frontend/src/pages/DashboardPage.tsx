import { useEffect, useState } from "react";
import { useNavigate } from "react-router-dom";
import { HardDrive, Users, FileStack, Power, Folder, ArrowUp, FolderPlus, ChevronRight, Server } from "lucide-react";

// --- Interfaces ---
interface DashboardData {
  isRunning: boolean;
  diskUsage: string[];
  connections: any[];
  openFiles: any[];
}

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

export default function DashboardPage() {
  const navigate = useNavigate();

  // Dashboard State
  const [data, setData] = useState<DashboardData | null>(null);
  const [dashLoading, setDashLoading] = useState(true);
  const [dashError, setDashError] = useState("");

  // Files State
  const [currentPath, setCurrentPath] = useState("/");
  const [browseData, setBrowseData] = useState<BrowseResult | null>(null);
  const [diskUsage, setDiskUsage] = useState<DiskUsage | null>(null);
  const [filesLoading, setFilesLoading] = useState(false);
  const [filesError, setFilesError] = useState("");
  const [isCreatingDir, setIsCreatingDir] = useState(false);
  const [newDirName, setNewDirName] = useState("");

  const fetchDashboard = async () => {
    try {
      const res = await fetch("/api/monitoring/dashboard");
      if (res.status === 401) {
        navigate("/login");
        return;
      }
      const json = await res.json();
      if (json.success) {
        setData(json.data);
        setDashError("");
      } else {
        setDashError(json.message || "Ошибка загрузки дашборда");
      }
    } catch (err) {
      setDashError("Ошибка соединения с сервером");
    } finally {
      setDashLoading(false);
    }
  };

  const fetchFiles = async (path: string) => {
    setFilesLoading(true);
    setFilesError("");
    try {
      const [browseRes, diskRes] = await Promise.all([
        fetch(`/api/fs/browse?path=${encodeURIComponent(path)}`),
        fetch(`/api/fs/disk-usage?path=${encodeURIComponent(path)}`)
      ]);
      const browseJson = await browseRes.json();
      const diskJson = await diskRes.json();

      if (browseJson.success) setBrowseData(browseJson.data);
      else setFilesError(browseJson.message || "Ошибка загрузки пути");

      if (diskJson.success) setDiskUsage(diskJson.data);
    } catch (err) {
      setFilesError("Ошибка API файловой системы");
    } finally {
      setFilesLoading(false);
    }
  };

  useEffect(() => {
    fetchDashboard();
    const interval = setInterval(fetchDashboard, 10000);
    return () => clearInterval(interval);
  }, [navigate]);

  useEffect(() => {
    fetchFiles(currentPath);
  }, [currentPath]);

  const handleServiceControl = async (action: string) => {
    try {
      const res = await fetch(`/api/monitoring/control?action=${action}`, { method: "POST" });
      if (res.ok) fetchDashboard();
    } catch (err) {}
  };

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
        fetchFiles(currentPath);
      } else {
        const json = await res.json();
        alert(json.message || "Ошибка создания папки");
      }
    } catch (err) {
      alert("Сетевая ошибка");
    }
  };

  const navigateTo = (path: string) => setCurrentPath(path);

  return (
    <div className="space-y-8 animate-in fade-in duration-500 pb-10">
      
      {/* HEADER */}
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4">
        <div>
          <h1 className="text-3xl font-extrabold tracking-tight text-slate-900 flex items-center gap-3">
            <div className="bg-blue-100/50 p-2 rounded-2xl ring-1 ring-blue-500/10">
              <Server className="w-7 h-7 text-blue-600" />
            </div>
            Дашборд / Файлы
          </h1>
          <p className="text-slate-500 text-[15px] mt-2">Мониторинг ресурсов службы Samba и файловый менеджер</p>
        </div>
      </div>

      {dashError && (
        <div className="bg-rose-50 text-rose-600 p-4 rounded-xl text-[14px] font-medium border border-rose-100/50">
          ⚠️ {dashError}
        </div>
      )}

      {dashLoading && !data && (
        <div className="grid grid-cols-1 md:grid-cols-3 gap-6">
          {[1,2,3].map(i => (
             <div key={i} className="h-32 bg-white/50 rounded-[24px] border border-slate-100 p-6 animate-pulse flex flex-col justify-between">
                <div className="w-1/3 h-4 bg-slate-200/50 rounded-full mb-4"></div>
                <div className="w-full h-2 bg-slate-100 rounded-full mb-2"></div>
             </div>
          ))}
        </div>
      )}

      {/* MONITORING / DASHBOARD WIDGETS */}
      {data && (
        <div className="grid grid-cols-1 md:grid-cols-3 lg:grid-cols-4 gap-6">
          
          {/* Service Status */}
          <div className="bg-white rounded-[24px] shadow-sm border border-slate-200/60 p-6 flex flex-col">
            <div className="flex items-center justify-between mb-4">
              <h3 className="text-[13px] font-bold text-slate-500 uppercase tracking-wider">Служба smbd</h3>
              <Power className={`w-5 h-5 ${data.isRunning ? "text-emerald-500" : "text-rose-500"}`} />
            </div>
            <div className="text-3xl font-black text-slate-800 mb-6">
              {data.isRunning ? "Онлайн" : "Остановлен"}
            </div>
            <div className="flex gap-3 mt-auto">
              {!data.isRunning ? (
                <button 
                  className="flex-1 bg-emerald-500 hover:bg-emerald-600 text-white font-bold py-2.5 rounded-xl transition-colors shadow-sm"
                  onClick={() => handleServiceControl("start")}
                >
                  Запуск
                </button>
              ) : (
                <>
                  <button 
                    className="flex-1 bg-rose-50 hover:bg-rose-100 text-rose-600 font-bold py-2.5 rounded-xl transition-colors"
                    onClick={() => handleServiceControl("stop")}
                  >
                    Остановка
                  </button>
                  <button 
                    className="flex-1 bg-slate-50 border border-slate-200 hover:bg-slate-100 text-slate-700 font-bold py-2.5 rounded-xl transition-colors"
                    onClick={() => handleServiceControl("restart")}
                  >
                    Рестарт
                  </button>
                </>
              )}
            </div>
          </div>

          {/* Connections */}
          <div className="bg-gradient-to-br from-blue-600 to-indigo-700 rounded-[24px] shadow-md border border-blue-500 p-6 flex flex-col text-white relative overflow-hidden">
            <div className="absolute top-0 right-0 w-32 h-32 bg-white/10 rounded-full blur-2xl -mr-10 -mt-10 pointer-events-none"></div>
            <div className="flex items-center justify-between mb-4 relative z-10">
              <h3 className="text-[13px] font-bold text-blue-200 uppercase tracking-wider">Активные сессии</h3>
              <Users className="w-5 h-5 text-blue-300" />
            </div>
            <div className="text-4xl font-black mb-1 relative z-10">{data.connections.length}</div>
            <p className="text-[13px] text-blue-200 font-medium relative z-10 mt-auto">подключенных клиентов</p>
          </div>

          {/* Open Files */}
          <div className="md:col-span-1 lg:col-span-2 bg-white rounded-[24px] shadow-sm border border-slate-200/60 p-6 flex flex-col">
            <div className="flex items-center justify-between mb-4">
              <h3 className="text-[13px] font-bold text-slate-500 uppercase tracking-wider">Открытые файлы</h3>
              <FileStack className="w-5 h-5 text-orange-500" />
            </div>
            <div className="text-3xl font-black text-slate-800 mb-2">{data.openFiles.length}</div>
            <div className="flex-1 overflow-y-auto max-h-[80px] custom-scrollbar text-sm mt-2">
              {data.openFiles.length === 0 ? (
                 <p className="text-slate-400 font-medium italic mt-2">Нет открытых клиентами файлов</p>
              ) : (
                 <div className="flex flex-col gap-1">
                   {data.openFiles.slice(0, 3).map((f, i) => (
                     <div key={i} className="flex justify-between items-center bg-slate-50 px-3 py-1.5 rounded-lg border border-slate-100">
                       <span className="font-mono text-[12px] truncate max-w-[70%] text-slate-600" title={typeof f === 'object' ? f.path : f.toString()}>{typeof f === 'object' ? f.path : f.toString()}</span>
                       <span className="text-[11px] font-bold text-slate-400 bg-white px-2 py-0.5 rounded-md border border-slate-200">{typeof f === 'object' ? f.user : '-'}</span>
                     </div>
                   ))}
                 </div>
              )}
            </div>
          </div>
          
        </div>
      )}

      {/* Divider */}
      <div className="border-t border-slate-200/70 pt-8 mt-12 mb-4 relative">
         <span className="absolute -top-3 left-6 bg-[#f8fafc] px-3 font-bold text-[11px] uppercase tracking-widest text-slate-400">Менеджер файлов</span>
      </div>

      {filesError && (
        <div className="bg-rose-50 text-rose-600 p-4 rounded-xl text-[14px] font-medium border border-rose-100/50">
          ⚠️ {filesError}
        </div>
      )}

      {/* Disk Usage Widget (File Browser Level) */}
      {diskUsage && (
        <div className="grid grid-cols-1 md:grid-cols-4 gap-4 bg-white rounded-[24px] p-6 shadow-sm border border-slate-200/60">
          <div className="col-span-1 md:col-span-4 flex items-center justify-between mb-2">
            <h3 className="font-bold text-slate-800 tracking-tight flex items-center gap-2"><HardDrive className="w-5 h-5 text-blue-500" /> Использование хранилища</h3>
            <span className="text-[13px] font-bold text-slate-500 bg-slate-100 px-2 py-1 rounded-md">{diskUsage.usePercent}%</span>
          </div>
          <div className="col-span-1 md:col-span-4 w-full bg-slate-100 rounded-full h-2 mb-4 overflow-hidden">
             <div 
               className={`h-2 rounded-full ${diskUsage.usePercent > 90 ? 'bg-rose-500' : diskUsage.usePercent > 70 ? 'bg-amber-500' : 'bg-blue-500'}`} 
               style={{ width: `${diskUsage.usePercent}%` }}
             ></div>
          </div>
          
          <div className="bg-slate-50 rounded-xl p-4 border border-slate-100">
            <div className="text-[12px] font-semibold text-slate-400 uppercase tracking-wider mb-1">Всего</div>
            <div className="text-xl font-extrabold text-slate-700">{diskUsage.total}</div>
          </div>
          <div className="bg-slate-50 rounded-xl p-4 border border-slate-100">
            <div className="text-[12px] font-semibold text-slate-400 uppercase tracking-wider mb-1">Занято</div>
            <div className="text-xl font-extrabold text-slate-700">{diskUsage.used}</div>
          </div>
          <div className="bg-slate-50 rounded-xl p-4 border border-slate-100">
            <div className="text-[12px] font-semibold text-slate-400 uppercase tracking-wider mb-1">Свободно</div>
            <div className="text-xl font-extrabold text-emerald-600">{diskUsage.available}</div>
          </div>
          <div className="bg-slate-50 rounded-xl p-4 border border-slate-100">
            <div className="text-[12px] font-semibold text-slate-400 uppercase tracking-wider mb-1">Точка монтирования</div>
            <div className="text-lg font-bold text-slate-700 truncate" title={diskUsage.mountPoint}>{diskUsage.mountPoint}</div>
          </div>
        </div>
      )}

      {/* Breadcrumb & Create Dir */}
      <div className="bg-white rounded-[24px] shadow-sm border border-slate-200/60 overflow-hidden flex flex-col">
        <div className="px-6 py-4 border-b border-slate-100 flex flex-col gap-4 sm:flex-row sm:items-center justify-between bg-slate-50/50">
          <div className="flex flex-wrap items-center gap-1 text-[15px] font-medium text-slate-600">
            <button onClick={() => navigateTo("/")} className="hover:text-blue-600 transition-colors">root</button>
            {browseData?.currentPath.split("/").filter(Boolean).map((part, index, array) => {
              const p = "/" + array.slice(0, index + 1).join("/");
              return (
                <div key={p} className="flex items-center gap-1">
                  <ChevronRight className="w-4 h-4 text-slate-400" />
                  <button onClick={() => navigateTo(p)} className="hover:text-blue-600 transition-colors">{part}</button>
                </div>
              );
            })}
          </div>

          <div className="flex items-center gap-2">
            {!isCreatingDir ? (
              <button onClick={() => setIsCreatingDir(true)} className="text-[13px] font-bold text-slate-600 bg-white border border-slate-200 px-4 py-2.5 rounded-xl hover:bg-slate-50 hover:text-slate-900 transition-colors flex items-center gap-2 shadow-sm">
                <FolderPlus className="w-4 h-4" /> Создать папку
              </button>
            ) : (
              <form onSubmit={handleCreateDirectory} className="flex gap-2">
                <input 
                  autoFocus
                  required
                  pattern="[a-zA-Z0-9_.-]+"
                  className="text-[13px] border border-slate-200 px-3 py-2 rounded-xl focus:outline-none focus:border-blue-500 bg-white shadow-sm w-40"
                  placeholder="Новая папка..."
                  value={newDirName}
                  onChange={e => setNewDirName(e.target.value)}
                />
                <button type="submit" className="text-[13px] font-bold text-white bg-blue-600 px-4 py-2 rounded-xl hover:bg-blue-700 transition-colors shadow-sm">
                  ОК
                </button>
                <button type="button" onClick={() => setIsCreatingDir(false)} className="text-[13px] font-bold text-slate-600 bg-white border border-slate-200 px-3 py-2 rounded-xl hover:bg-slate-50 transition-colors shadow-sm">
                  Отмена
                </button>
              </form>
            )}
          </div>
        </div>

        {/* Directory List */}
        <div className="p-2 sm:p-4 min-h-[150px]">
           {filesLoading ? (
             <div className="flex justify-center p-10">
               <div className="animate-spin rounded-full h-8 w-8 border-b-2 border-blue-600"></div>
             </div>
           ) : (
             <div className="grid grid-cols-1 sm:grid-cols-2 md:grid-cols-3 gap-2">
               {browseData?.parentPath && (
                 <button 
                   onClick={() => navigateTo(browseData.parentPath!)}
                   className="flex items-center gap-3 p-3 rounded-xl hover:bg-slate-50 text-left transition-colors group"
                 >
                   <div className="bg-slate-100 p-2 rounded-lg group-hover:bg-blue-100 transition-colors">
                     <ArrowUp className="w-5 h-5 text-slate-500 group-hover:text-blue-600" />
                   </div>
                   <span className="font-medium text-slate-700">.. (наверх)</span>
                 </button>
               )}

               {browseData?.directories.map(dir => (
                 <button 
                   key={dir.fullPath}
                   onClick={() => navigateTo(dir.fullPath)}
                   className="flex items-center gap-3 p-3 rounded-xl hover:bg-slate-50 text-left transition-colors group border border-transparent hover:border-slate-100"
                 >
                   <div className="bg-blue-50 p-2 rounded-lg group-hover:bg-blue-100 transition-colors">
                     <Folder className="w-5 h-5 text-blue-500 group-hover:text-blue-600 fill-blue-100" />
                   </div>
                   <span className="font-medium text-slate-700 truncate" title={dir.name}>{dir.name}</span>
                 </button>
               ))}
               
               {(!browseData?.directories || browseData.directories.length === 0) && !browseData?.parentPath && (
                 <div className="col-span-full text-center text-slate-400 py-10 italic">
                   Нет вложенных папок
                 </div>
               )}
             </div>
           )}
        </div>
      </div>
    </div>
  );
}

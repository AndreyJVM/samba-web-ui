import { useEffect, useState } from "react";
import { Folder, HardDrive, ArrowUp, FolderPlus, ChevronRight } from "lucide-react";

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
    <div className="space-y-8 animate-in fade-in duration-500">
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4">
        <div>
          <h1 className="text-3xl font-extrabold tracking-tight text-slate-900 flex items-center gap-3">
            <div className="bg-blue-100/50 p-2 rounded-2xl ring-1 ring-blue-500/10">
              <HardDrive className="w-7 h-7 text-blue-600" />
            </div>
            Файловый менеджер
          </h1>
          <p className="text-slate-500 text-[15px] mt-2">Реальная файловая система сервера ({diskUsage?.mountPoint || '/'})</p>
        </div>
      </div>

      {error && (
        <div className="bg-rose-50 text-rose-600 p-4 rounded-xl text-[14px] font-medium border border-rose-100/50">
          ⚠️ {error}
        </div>
      )}

      {/* Disk Usage Widget */}
      {diskUsage && (
        <div className="grid grid-cols-1 md:grid-cols-4 gap-4 bg-white rounded-[24px] p-6 shadow-sm border border-slate-200/60">
          <div className="col-span-1 md:col-span-4 flex items-center justify-between mb-2">
            <h3 className="font-bold text-slate-800 tracking-tight">Использование диска</h3>
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
            <div className="text-[12px] font-semibold text-slate-400 uppercase tracking-wider mb-1">Использовано</div>
            <div className="text-xl font-extrabold text-slate-700">{diskUsage.used}</div>
          </div>
          <div className="bg-slate-50 rounded-xl p-4 border border-slate-100">
            <div className="text-[12px] font-semibold text-slate-400 uppercase tracking-wider mb-1">Доступно</div>
            <div className="text-xl font-extrabold text-emerald-600">{diskUsage.available}</div>
          </div>
          <div className="bg-slate-50 rounded-xl p-4 border border-slate-100">
            <div className="text-[12px] font-semibold text-slate-400 uppercase tracking-wider mb-1">Монтирование</div>
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
                  placeholder="Имя папки..."
                  value={newDirName}
                  onChange={e => setNewDirName(e.target.value)}
                />
                <button type="submit" className="text-[13px] font-bold text-white bg-blue-600 px-4 py-2 rounded-xl hover:bg-blue-700 transition-colors shadow-sm">
                  ✓
                </button>
                <button type="button" onClick={() => setIsCreatingDir(false)} className="text-[13px] font-bold text-slate-600 bg-white border border-slate-200 px-3 py-2 rounded-xl hover:bg-slate-50 transition-colors shadow-sm">
                  ✕
                </button>
              </form>
            )}
          </div>
        </div>

        {/* Directory List */}
        <div className="p-2 sm:p-4">
           {loading ? (
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
                   <span className="font-medium text-slate-700">.. (Наверх)</span>
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
                   Папка пуста
                 </div>
               )}
             </div>
           )}
        </div>
      </div>
    </div>
  );
}

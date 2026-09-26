import { useEffect, useState, useCallback } from "react";
import { FileStack, Power, Folder, ArrowUp, FolderPlus, ChevronRight } from "lucide-react";
import { api } from "../lib/api";
import { useToast } from "../components/ui/toast";
import { useTranslation } from "../lib/i18n";

interface DashboardData {
  isRunning: boolean;
  diskUsage: string[];
  connections: any[];
  openFiles: any[];
}
interface DirectoryItem { name: string; fullPath: string; }
interface BrowseResult { currentPath: string; parentPath: string | null; directories: DirectoryItem[]; }
interface DiskUsage { total: string; used: string; available: string; usePercent: number; mountPoint: string; }

export default function DashboardPage() {
  const [data, setData] = useState<DashboardData | null>(null);
  const [dashLoading, setDashLoading] = useState(true);

  const [currentPath, setCurrentPath] = useState("/");
  const [browseData, setBrowseData] = useState<BrowseResult | null>(null);
  const [diskUsage, setDiskUsage] = useState<DiskUsage | null>(null);
  const [filesLoading, setFilesLoading] = useState(false);
  
  const [isCreatingDir, setIsCreatingDir] = useState(false);
  const [newDirName, setNewDirName] = useState("");

  const { info, error: toastError, success } = useToast();
  const { t } = useTranslation();

  const navigateTo = (path: string) => setCurrentPath(path);

  const fetchDashboard = useCallback(async () => {
    try {
      const res = await api.get<DashboardData>("/api/monitoring/dashboard");
      setData(res);
    } catch (err: any) {
      if (!data) toastError("Error", err.message);
    } finally {
      setDashLoading(false);
    }
  }, [data, toastError]);

  const fetchFiles = async (path: string) => {
    setFilesLoading(true);
    try {
      const [browseRes, diskRes] = await Promise.all([
        api.get<BrowseResult>(`/api/fs/browse?path=${encodeURIComponent(path)}`).catch(() => null),
        api.get<DiskUsage>(`/api/fs/disk-usage?path=${encodeURIComponent(path)}`).catch(() => null)
      ]);
      if (browseRes) setBrowseData(browseRes);
      if (diskRes) setDiskUsage(diskRes);
    } catch (err: any) {
      toastError("FS Error", err.message);
    } finally {
      setFilesLoading(false);
    }
  };

  useEffect(() => {
    fetchDashboard();
    const interval = setInterval(fetchDashboard, 10000);
    return () => clearInterval(interval);
  }, [fetchDashboard]);

  useEffect(() => {
    fetchFiles(currentPath);
  }, [currentPath]);

  const handleServiceControl = async (action: string) => {
    try {
      await api.post(`/api/monitoring/control?action=${action}`);
      info("Command Sent", `Action: ${action} initiated`);
      setTimeout(fetchDashboard, 1500);
    } catch (err: any) {
      toastError("Control Error", err.message);
    }
  };

  const handleCreateDirectory = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!newDirName) return;
    try {
      await api.post(`/api/fs/mkdir?parentPath=${encodeURIComponent(currentPath)}&name=${encodeURIComponent(newDirName)}`);
      success("Created", `Directory ${newDirName} created successfully`);
      setIsCreatingDir(false);
      setNewDirName("");
      fetchFiles(currentPath);
    } catch (err: any) {
      toastError("Creation Error", err.message);
    }
  };

  return (
    <div className="space-y-6 animate-in fade-in duration-500">
      <div className="flex items-center gap-3 pb-4">
        <h1 className="text-2xl font-semibold text-foreground tracking-tight">{t("dashboard.title")}</h1>
      </div>

      {dashLoading && !data ? (
        <div className="grid grid-cols-1 md:grid-cols-3 gap-5">
          {[1,2,3].map(i => (
             <div key={i} className="h-32 bg-surface shadow-subtle border border-border/50 rounded-lg animate-pulse" />
          ))}
        </div>
      ) : data ? (
        <div className="grid grid-cols-1 md:grid-cols-3 gap-5">
          
          <div className="bg-surface border border-border shadow-sm-subtle p-5 rounded-lg flex flex-col justify-between">
            <div className="flex items-center justify-between mb-4">
              <h3 className="text-[13px] font-medium text-status-disabled">{t("dashboard.serviceStatus")}</h3>
              <Power className={`w-4 h-4 ${data.isRunning ? "text-status-active" : "text-status-error"}`} />
            </div>
            <div className="flex items-baseline gap-2 mb-5">
              <span className={`text-2xl font-semibold ${data.isRunning ? "text-status-active" : "text-foreground"}`}>
                {data.isRunning ? t("sidebar.running") : t("sidebar.stopped")}
              </span>
              <span className="text-[11px] text-status-disabled uppercase tracking-widest bg-surface-hover px-1.5 py-0.5 rounded-sm border border-border">smbd</span>
            </div>
            <div className="grid grid-cols-2 gap-3 mt-auto">
              {!data.isRunning ? (
                <button onClick={() => handleServiceControl("start")} className="col-span-2 text-[13px] font-medium bg-brand text-brand-text hover:bg-brand-hover py-2 rounded-md shadow-sm transition-colors">
                  {t("dashboard.startService")}
                </button>
              ) : (
                <>
                  <button onClick={() => handleServiceControl("stop")} className="text-[13px] font-medium bg-surface text-status-error hover:bg-surface-hover py-2 rounded-md transition-colors border border-border shadow-sm-subtle">
                    {t("dashboard.stop")}
                  </button>
                  <button onClick={() => handleServiceControl("restart")} className="text-[13px] font-medium bg-surface text-foreground hover:bg-surface-hover py-2 rounded-md transition-colors border border-border shadow-sm-subtle">
                    {t("dashboard.restart")}
                  </button>
                </>
              )}
            </div>
          </div>

          <div className="bg-surface border border-border shadow-sm-subtle p-5 rounded-lg flex flex-col justify-between">
            <div className="flex items-center justify-between mb-4">
              <h3 className="text-[13px] font-medium text-status-disabled">{t("dashboard.activeSessions")}</h3>
            </div>
            <div className="flex items-end gap-2 mb-2">
              <span className="text-4xl font-semibold text-foreground tracking-tight">{data.connections?.length || 0}</span>
              <span className="text-sm text-status-disabled mb-1.5 font-medium">{t("dashboard.clients")}</span>
            </div>
          </div>

          <div className="bg-surface border border-border shadow-sm-subtle p-5 rounded-lg flex flex-col justify-between">
            <div className="flex items-center justify-between mb-4">
              <h3 className="text-[13px] font-medium text-status-disabled">{t("dashboard.openFiles")}</h3>
              <FileStack className="w-4 h-4 text-status-disabled" />
            </div>
            <div className="flex items-end gap-2">
              <span className="text-4xl font-semibold text-foreground tracking-tight">{data.openFiles?.length || 0}</span>
              <span className="text-sm text-status-disabled mb-1.5 font-medium">{t("dashboard.locked")}</span>
            </div>
            <div className="h-[44px] mt-2 overflow-y-auto custom-scrollbar flex flex-col gap-1.5">
               {data.openFiles?.length > 0 ? data.openFiles.slice(0, 2).map((f: any, idx: number) => (
                 <div key={idx} className="flex justify-between items-center text-[11px] font-mono bg-surface-hover px-2 py-1 rounded-sm border border-border">
                   <span className="truncate max-w-[70%]">{typeof f === 'object' ? f.path : f.toString()}</span>
                   <span className="text-foreground font-semibold">{typeof f === 'object' ? f.user : '-'}</span>
                 </div>
               )) : (
                 <span className="text-[13px] text-status-disabled mt-1">{t("dashboard.noFilesInUse")}</span>
               )}
            </div>
          </div>
        </div>
      ) : null}

      <div className="pt-6">
         <div className="flex items-center gap-3 pb-4">
            <h2 className="text-lg font-semibold text-foreground tracking-tight">{t("dashboard.storageExplorer")}</h2>
         </div>
      </div>

      {diskUsage && (
        <div className="grid grid-cols-2 lg:grid-cols-4 gap-4 bg-surface rounded-lg p-5 border border-border shadow-sm-subtle relative overflow-hidden">
          <div className="col-span-2 lg:col-span-4 flex items-center gap-4 mb-2">
            <div className="flex-1 bg-surface-hover border border-border rounded-full h-3 overflow-hidden">
               <div 
                 className={`h-full transition-all duration-500 rounded-full ${diskUsage.usePercent > 90 ? 'bg-status-error' : diskUsage.usePercent > 70 ? 'bg-status-warning' : 'bg-status-active'}`} 
                 style={{ width: `${diskUsage.usePercent}%` }}
               />
            </div>
            <span className="text-sm font-semibold w-12 text-right">{diskUsage.usePercent}%</span>
          </div>

          <div className="flex flex-col">
            <span className="text-xs text-status-disabled font-medium mb-1">{t("dashboard.total")}</span>
            <span className="text-sm font-mono font-medium">{diskUsage.total}</span>
          </div>
          <div className="flex flex-col">
            <span className="text-xs text-status-disabled font-medium mb-1">{t("dashboard.used")}</span>
            <span className="text-sm font-mono font-medium">{diskUsage.used}</span>
          </div>
          <div className="flex flex-col">
            <span className="text-xs text-status-disabled font-medium mb-1">{t("dashboard.free")}</span>
            <span className="text-sm font-mono font-semibold text-status-active">{diskUsage.available}</span>
          </div>
          <div className="flex flex-col">
            <span className="text-xs text-status-disabled font-medium mb-1">{t("dashboard.mount")}</span>
            <span className="text-sm font-mono text-foreground truncate" title={diskUsage.mountPoint}>{diskUsage.mountPoint}</span>
          </div>
        </div>
      )}

      <div className="bg-surface rounded-lg border border-border shadow-sm-subtle flex flex-col overflow-hidden">
        <div className="px-5 py-4 border-b border-border flex flex-col sm:flex-row sm:items-center justify-between gap-4 bg-surface-hover/50">
          <div className="flex flex-wrap items-center gap-1.5 text-[13px] font-mono">
            <button onClick={() => navigateTo("/")} className="text-foreground hover:text-brand px-1.5 py-0.5 rounded transition-colors hover:bg-border">{t("common.root")}</button>
            {browseData?.currentPath.split("/").filter(Boolean).map((part, index, array) => {
              const p = "/" + array.slice(0, index + 1).join("/");
              return (
                <div key={p} className="flex items-center gap-1.5">
                  <ChevronRight className="w-3.5 h-3.5 text-status-disabled" />
                  <button onClick={() => navigateTo(p)} className="text-foreground hover:text-brand px-1.5 py-0.5 rounded transition-colors hover:bg-border">{part}</button>
                </div>
              );
            })}
          </div>

          <div className="flex items-center gap-2">
            {!isCreatingDir ? (
              <button 
                onClick={() => setIsCreatingDir(true)} 
                className="text-[12px] font-medium text-foreground bg-surface border border-border shadow-sm-subtle px-3 py-1.5 rounded-md hover:bg-surface-hover transition-colors flex items-center gap-1.5"
              >
                <FolderPlus className="w-3.5 h-3.5" /> {t("dashboard.mkdir")}
              </button>
            ) : (
              <form onSubmit={handleCreateDirectory} className="flex gap-2 isolate">
                <input 
                  autoFocus required pattern="[a-zA-Z0-9_.-]+"
                  className="text-xs font-mono border border-border bg-surface px-3 py-1.5 rounded-md shadow-sm-subtle focus:outline-none focus:ring-2 focus:ring-ring focus:border-border-strong w-48 text-foreground"
                  placeholder={t("dashboard.dirname")}
                  value={newDirName}
                  onChange={e => setNewDirName(e.target.value)}
                />
                <button type="submit" className="text-xs font-medium text-brand-text bg-brand px-3 py-1.5 rounded-md hover:bg-brand-hover shadow-sm transition-colors">
                  {t("common.save")}
                </button>
                <button type="button" onClick={() => setIsCreatingDir(false)} className="text-xs font-medium text-foreground bg-surface border border-border px-3 py-1.5 rounded-md hover:bg-surface-hover shadow-sm-subtle transition-colors">
                  {t("common.cancel")}
                </button>
              </form>
            )}
          </div>
        </div>

        <div className="min-h-[240px]">
           {filesLoading ? (
             <div className="flex justify-center items-center h-48">
               <div className="w-5 h-5 border-2 border-border border-t-foreground rounded-full animate-spin"></div>
             </div>
           ) : (
             <div className="grid grid-cols-1 sm:grid-cols-2 md:grid-cols-3 lg:grid-cols-4 gap-0 divide-y sm:divide-y-0 sm:gap-[1px] bg-border p-[1px]">
               {browseData?.parentPath && (
                 <button 
                   onClick={() => navigateTo(browseData.parentPath!)}
                   className="flex items-center gap-3 px-4 py-3 bg-surface hover:bg-surface-hover text-left transition-colors group"
                 >
                   <ArrowUp className="w-4 h-4 text-status-disabled group-hover:text-foreground transition-colors" />
                   <span className="text-[13px] font-mono font-medium text-foreground">..</span>
                 </button>
               )}
               {browseData?.directories.map(dir => (
                 <button 
                   key={dir.fullPath}
                   onClick={() => navigateTo(dir.fullPath)}
                   className="flex items-center gap-3 px-4 py-3 bg-surface hover:bg-surface-hover text-left transition-colors group"
                 >
                   <Folder className="w-4 h-4 text-status-disabled group-hover:text-foreground transition-colors" />
                   <span className="text-[13px] font-mono font-medium text-foreground truncate" title={dir.name}>{dir.name}</span>
                 </button>
               ))}
               {(!browseData?.directories || browseData.directories.length === 0) && !browseData?.parentPath && (
                 <div className="col-span-full bg-surface text-center text-[13px] text-status-disabled font-mono py-16">
                   {t("dashboard.dirEmpty")}
                 </div>
               )}
             </div>
           )}
        </div>
      </div>
    </div>
  );
}

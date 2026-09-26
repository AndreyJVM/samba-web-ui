import { useEffect, useState, useCallback } from "react";
import { HardDrive, Users, FileStack, Power, Folder, ArrowUp, FolderPlus, ChevronRight, Server } from "lucide-react";
import { api } from "../lib/api";
import { useToast } from "../components/ui/toast";

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
    <div className="space-y-6 animate-in fade-in duration-500 pb-10 max-w-6xl">
      <div className="flex items-center gap-3 pb-4 border-b border-border">
        <Server className="w-5 h-5 text-foreground" />
        <h1 className="text-xl font-semibold text-foreground tracking-tight">System Overview</h1>
      </div>

      {dashLoading && !data ? (
        <div className="grid grid-cols-1 md:grid-cols-3 gap-4">
          {[1,2,3].map(i => (
             <div key={i} className="h-28 bg-surface border border-border p-4 animate-pulse rounded-md" />
          ))}
        </div>
      ) : data ? (
        <div className="grid grid-cols-1 md:grid-cols-3 gap-4">
          
          <div className="bg-surface border border-border p-5 rounded-md shadow-sm-subtle flex flex-col justify-between group">
            <div className="flex items-center justify-between mb-4">
              <h3 className="text-xs font-bold text-status-disabled uppercase tracking-wide">Service Status</h3>
              <Power className={`w-4 h-4 ${data.isRunning ? "text-status-active" : "text-status-error"}`} />
            </div>
            <div className="flex items-baseline gap-2 mb-5">
              <span className={`text-2xl font-mono font-bold ${data.isRunning ? "text-status-active" : "text-status-error"}`}>
                {data.isRunning ? "RUNNING" : "STOPPED"}
              </span>
              <span className="text-[10px] text-status-disabled uppercase tracking-widest">smbd</span>
            </div>
            <div className="grid grid-cols-2 gap-2 mt-auto">
              {!data.isRunning ? (
                <button onClick={() => handleServiceControl("start")} className="col-span-2 text-xs font-semibold bg-status-active/10 text-status-active hover:bg-status-active/20 py-2 rounded transition-colors border border-status-active/20">
                  START SERVICE
                </button>
              ) : (
                <>
                  <button onClick={() => handleServiceControl("stop")} className="text-xs font-semibold bg-status-error/10 text-status-error hover:bg-status-error/20 py-2 rounded transition-colors border border-status-error/20">
                    STOP
                  </button>
                  <button onClick={() => handleServiceControl("restart")} className="text-xs font-semibold bg-surface-hover text-foreground hover:bg-border-strong py-2 rounded transition-colors border border-border">
                    RESTART
                  </button>
                </>
              )}
            </div>
          </div>

          <div className="bg-surface border border-border p-5 rounded-md shadow-sm-subtle flex flex-col justify-between">
            <div className="flex items-center justify-between mb-4">
              <h3 className="text-xs font-bold text-status-disabled uppercase tracking-wide">Active Sessions</h3>
              <Users className="w-4 h-4 text-brand" />
            </div>
            <div className="flex items-end gap-2 mb-2">
              <span className="text-3xl font-mono font-bold text-foreground">{data.connections?.length || 0}</span>
              <span className="text-xs text-status-disabled mb-1 font-medium">clients</span>
            </div>
          </div>

          <div className="bg-surface border border-border p-5 rounded-md shadow-sm-subtle flex flex-col justify-between">
            <div className="flex items-center justify-between mb-4">
              <h3 className="text-xs font-bold text-status-disabled uppercase tracking-wide">Open Files</h3>
              <FileStack className="w-4 h-4 text-status-warning" />
            </div>
            <div className="flex items-end gap-2">
              <span className="text-3xl font-mono font-bold text-foreground">{data.openFiles?.length || 0}</span>
              <span className="text-xs text-status-disabled mb-1 font-medium">locked</span>
            </div>
            <div className="h-[40px] mt-2 overflow-y-auto custom-scrollbar flex flex-col gap-1">
               {data.openFiles?.length > 0 ? data.openFiles.slice(0, 2).map((f: any, idx: number) => (
                 <div key={idx} className="flex justify-between items-center text-[10px] font-mono bg-background px-2 py-1 rounded border border-border">
                   <span className="truncate max-w-[70%]">{typeof f === 'object' ? f.path : f.toString()}</span>
                   <span className="text-brand font-bold">{typeof f === 'object' ? f.user : '-'}</span>
                 </div>
               )) : (
                 <span className="text-xs text-status-disabled italic mt-1">No files in use</span>
               )}
            </div>
          </div>
        </div>
      ) : null}

      <div className="pt-6">
         <div className="flex items-center gap-3 pb-3 border-b border-border">
            <HardDrive className="w-4 h-4 text-status-disabled" />
            <h2 className="text-sm font-bold text-foreground uppercase tracking-widest">Storage & Explorer</h2>
         </div>
      </div>

      {diskUsage && (
        <div className="grid grid-cols-2 lg:grid-cols-4 gap-4 bg-surface rounded-md p-5 border border-border shadow-sm-subtle relative overflow-hidden">
          <div className="col-span-2 lg:col-span-4 flex items-center gap-4 mb-2">
            <div className="flex-1 bg-background border border-border rounded-sm h-3 overflow-hidden">
               <div 
                 className={`h-full transition-all duration-500 ${diskUsage.usePercent > 90 ? 'bg-status-error' : diskUsage.usePercent > 70 ? 'bg-status-warning' : 'bg-status-active'}`} 
                 style={{ width: `${diskUsage.usePercent}%` }}
               />
            </div>
            <span className="text-xs font-mono font-bold w-10 text-right">{diskUsage.usePercent}%</span>
          </div>

          <div className="flex flex-col">
            <span className="text-[10px] text-status-disabled uppercase font-bold tracking-widest mb-0.5">Total</span>
            <span className="text-sm font-mono font-medium">{diskUsage.total}</span>
          </div>
          <div className="flex flex-col">
            <span className="text-[10px] text-status-disabled uppercase font-bold tracking-widest mb-0.5">Used</span>
            <span className="text-sm font-mono font-medium">{diskUsage.used}</span>
          </div>
          <div className="flex flex-col">
            <span className="text-[10px] text-status-disabled uppercase font-bold tracking-widest mb-0.5">Free</span>
            <span className="text-sm font-mono font-medium text-status-active">{diskUsage.available}</span>
          </div>
          <div className="flex flex-col">
            <span className="text-[10px] text-status-disabled uppercase font-bold tracking-widest mb-0.5">Mount</span>
            <span className="text-sm font-mono text-status-disabled truncate" title={diskUsage.mountPoint}>{diskUsage.mountPoint}</span>
          </div>
        </div>
      )}

      <div className="bg-surface rounded-md border border-border shadow-sm-subtle flex flex-col">
        <div className="px-4 py-3 border-b border-border flex flex-col sm:flex-row sm:items-center justify-between gap-3 bg-surface-hover/30">
          <div className="flex flex-wrap items-center gap-1.5 text-xs font-mono">
            <button onClick={() => navigateTo("/")} className="text-brand hover:underline font-semibold">root</button>
            {browseData?.currentPath.split("/").filter(Boolean).map((part, index, array) => {
              const p = "/" + array.slice(0, index + 1).join("/");
              return (
                <div key={p} className="flex items-center gap-1.5">
                  <ChevronRight className="w-3 h-3 text-status-disabled" />
                  <button onClick={() => navigateTo(p)} className="text-brand hover:underline font-semibold">{part}</button>
                </div>
              );
            })}
          </div>

          <div className="flex items-center gap-2">
            {!isCreatingDir ? (
              <button 
                onClick={() => setIsCreatingDir(true)} 
                className="text-[11px] font-bold uppercase tracking-wider text-foreground bg-background border border-border px-3 py-1.5 rounded-sm hover:bg-surface-hover transition-colors flex items-center gap-1.5"
              >
                <FolderPlus className="w-3.5 h-3.5" /> MKDIR
              </button>
            ) : (
              <form onSubmit={handleCreateDirectory} className="flex gap-2 isolate">
                <input 
                  autoFocus required pattern="[a-zA-Z0-9_.-]+"
                  className="text-xs font-mono border border-border bg-background px-2.5 py-1.5 rounded-sm focus:outline-none focus:border-brand w-48 text-foreground"
                  placeholder="dirname"
                  value={newDirName}
                  onChange={e => setNewDirName(e.target.value)}
                />
                <button type="submit" className="text-xs font-bold text-brand-text bg-brand px-3 py-1.5 rounded-sm hover:bg-brand-hover transition-colors">
                  OK
                </button>
                <button type="button" onClick={() => setIsCreatingDir(false)} className="text-xs font-bold text-foreground bg-background border border-border px-3 py-1.5 rounded-sm hover:bg-surface-hover transition-colors">
                  Cancel
                </button>
              </form>
            )}
          </div>
        </div>

        <div className="min-h-[200px]">
           {filesLoading ? (
             <div className="flex justify-center items-center h-40">
               <div className="w-5 h-5 border-2 border-border border-t-brand rounded-full animate-spin"></div>
             </div>
           ) : (
             <div className="grid grid-cols-1 sm:grid-cols-2 md:grid-cols-3 xl:grid-cols-4 gap-[1px] bg-border p-[1px]">
               {browseData?.parentPath && (
                 <button 
                   onClick={() => navigateTo(browseData.parentPath!)}
                   className="flex items-center gap-3 p-3 bg-surface hover:bg-surface-hover text-left transition-colors group"
                 >
                   <ArrowUp className="w-4 h-4 text-status-disabled group-hover:text-foreground" />
                   <span className="text-xs font-mono font-medium text-foreground">..</span>
                 </button>
               )}
               {browseData?.directories.map(dir => (
                 <button 
                   key={dir.fullPath}
                   onClick={() => navigateTo(dir.fullPath)}
                   className="flex items-center gap-3 p-3 bg-surface hover:bg-surface-hover text-left transition-colors group"
                 >
                   <Folder className="w-4 h-4 text-status-disabled group-hover:text-brand" />
                   <span className="text-xs font-mono font-medium text-foreground truncate" title={dir.name}>{dir.name}</span>
                 </button>
               ))}
               {(!browseData?.directories || browseData.directories.length === 0) && !browseData?.parentPath && (
                 <div className="col-span-full bg-surface text-center text-xs text-status-disabled font-mono py-12">
                   Directory is empty
                 </div>
               )}
             </div>
           )}
        </div>
      </div>
    </div>
  );
}

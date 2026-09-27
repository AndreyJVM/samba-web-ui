import { useEffect, useState } from "react";
import { Save, RotateCcw, Clock, ArrowLeft } from "lucide-react";
import { api } from "../../lib/api";
import { useToast } from "../../components/ui/toast";
import { useConfirm } from "../../components/ui/confirm";
import { Toggle } from "../../components/ui/toggle";
import { useTranslation } from "../../lib/i18n";

interface SmbGlobalConfig {
  workgroup: string;
  netbiosName: string;
  serverString: string;
  security: string;
  mapToGuest: string;
  interfaces: string;
  bindInterfacesOnly: boolean;
  minProtocol: string;
  maxProtocol: string;
  disableNetbios: boolean;
  loadPrinters: boolean;
  passdbBackend: string;
  guestAccount: string;
}

interface Backup {
  filename: string;
  createdAt: string;
  size: string;
}

export default function ConfigPage() {
  const [config, setConfig] = useState<SmbGlobalConfig | null>(null);
  const [backups, setBackups] = useState<Backup[]>([]);
  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);
  const { error: toastError, success } = useToast();
  const { confirm } = useConfirm();
  const { t } = useTranslation();

  const fetchData = async () => {
    try {
      setLoading(true);
      const [resConfig, resBackups] = await Promise.all([
        api.get<SmbGlobalConfig>("/api/config/global"),
        api.get<Backup[]>("/api/config/backups").catch(() => []) 
      ]);
      setConfig(resConfig);
      setBackups(resBackups || []);
    } catch (err: any) {
      toastError("API Error", err.message);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => { fetchData(); }, []);

  const handleSave = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!config) return;

    try {
      setSaving(true);
      await api.put("/api/config/global", config);
      success("Saved", "Configuration applied. Restart service to apply fully.");
      // Refetch to pull latest backups
      fetchData();
    } catch (err: any) {
      toastError("Save Error", err.message);
    } finally {
      setSaving(false);
    }
  };

  const handleReset = async () => {
    const ok = await confirm({
      title: t("config.defaults"),
      message: "Restore defaults? This will erase current settings.",
      destructive: true,
      confirmText: "Restore"
    });
    if (!ok) return;

    setConfig({
      workgroup: "WORKGROUP",
      netbiosName: "",
      serverString: "Samba Server",
      security: "user",
      mapToGuest: "never",
      interfaces: "",
      bindInterfacesOnly: false,
      minProtocol: "SMB2",
      maxProtocol: "SMB3",
      disableNetbios: true,
      loadPrinters: false,
      passdbBackend: "tdbsam",
      guestAccount: "nobody"
    });
    success("Restored", "Default values set. Save to apply.");
  };

  const handleRestoreBackup = async (filename: string) => {
    const ok = await confirm({
      title: t("config.restore"),
      message: `Restore configuration from backup: ${filename}?`,
      destructive: true,
      confirmText: t("config.restore")
    });
    if (!ok) return;

    try {
      await api.post(`/api/config/backups/${filename}/restore`);
      success("Restored", `Backup ${filename} restored successfully. Restart service applied.`);
      fetchData(); // reload UI config values
    } catch (err: any) {
      toastError("Restore Error", err.message);
    }
  };

  if (loading) {
    return (
      <div className="space-y-6 animate-in fade-in duration-500 max-w-4xl">
         <div className="h-10 w-48 bg-surface-hover animate-pulse rounded-md"></div>
         <div className="h-64 bg-surface shadow-sm-subtle border border-border/50 rounded-lg animate-pulse"></div>
      </div>
    );
  }

  if (!config) return null;

  return (
    <div className="space-y-6 animate-in fade-in duration-500 max-w-4xl pb-10">
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4 pb-4">
        <h1 className="text-2xl font-semibold text-foreground tracking-tight">{t("config.title")}</h1>
        <div className="flex gap-2">
          <button onClick={handleReset} type="button" className="text-sm font-medium text-foreground bg-surface border border-border px-4 py-2.5 rounded-md hover:bg-surface-hover shadow-sm-subtle transition-colors flex items-center gap-2">
            <RotateCcw className="w-4 h-4" /> {t("config.defaults")}
          </button>
        </div>
      </div>

      <form onSubmit={handleSave} className="space-y-8">
        
        <div className="bg-surface rounded-lg shadow-sm-subtle border border-border overflow-hidden">
           <div className="bg-surface-hover/50 px-5 py-3 border-b border-border/50">
             <h2 className="text-[13px] font-semibold tracking-wide uppercase text-status-disabled">{t("config.general")}</h2>
           </div>
           <div className="p-5 grid grid-cols-1 md:grid-cols-2 gap-6">
              <div className="space-y-2">
                <label className="text-[13px] font-medium text-foreground">{t("config.workgroup")}</label>
                <input 
                  value={config.workgroup} onChange={(e) => setConfig({...config, workgroup: e.target.value})} 
                  className="w-full bg-surface border border-border focus:border-border-strong focus:outline-none focus:ring-4 focus:ring-ring text-foreground transition-all py-2.5 px-3.5 text-sm font-medium rounded-lg shadow-sm-subtle uppercase"
                />
              </div>
              <div className="space-y-2">
                <label className="flex flex-col gap-0.5">
                  <span className="text-[13px] font-medium text-foreground">{t("config.netbiosName")}</span>
                </label>
                <input 
                  value={config.netbiosName} onChange={(e) => setConfig({...config, netbiosName: e.target.value})} 
                  className="w-full bg-surface border border-border focus:border-border-strong focus:outline-none focus:ring-4 focus:ring-ring text-foreground transition-all py-2.5 px-3.5 text-sm font-medium rounded-lg shadow-sm-subtle"
                  placeholder={t("config.autoGenerated")}
                />
              </div>
              <div className="space-y-2 md:col-span-2">
                <label className="text-[13px] font-medium text-foreground">{t("config.serverString")}</label>
                <input 
                  value={config.serverString} onChange={(e) => setConfig({...config, serverString: e.target.value})} 
                  className="w-full bg-surface border border-border focus:border-border-strong focus:outline-none focus:ring-4 focus:ring-ring text-foreground transition-all py-2.5 px-3.5 text-sm rounded-lg shadow-sm-subtle"
                />
              </div>
              <div className="md:col-span-2 flex items-center bg-surface-hover p-4 rounded-lg border border-border/50">
                <Toggle checked={config.loadPrinters} onChange={c => setConfig({ ...config, loadPrinters: c })} label={t("config.loadPrinters")} />
              </div>
           </div>
        </div>

        <div className="bg-surface rounded-lg shadow-sm-subtle border border-border overflow-hidden">
           <div className="bg-surface-hover/50 px-5 py-3 border-b border-border/50">
             <h2 className="text-[13px] font-semibold tracking-wide uppercase text-status-disabled">{t("config.network")}</h2>
           </div>
           <div className="p-5 grid grid-cols-1 md:grid-cols-2 gap-6">
              <div className="space-y-2">
                <label className="text-[13px] font-medium text-foreground">{t("config.interfaces")}</label>
                <input 
                  value={config.interfaces} onChange={(e) => setConfig({...config, interfaces: e.target.value})} 
                  className="w-full bg-surface border border-border focus:border-border-strong focus:outline-none focus:ring-4 focus:ring-ring text-foreground transition-all py-2.5 px-3.5 text-sm font-mono rounded-lg shadow-sm-subtle"
                  placeholder="lo eth0 192.168.1.10"
                />
              </div>
              <div className="flex flex-col gap-4 pt-1">
                <div className="bg-surface-hover p-4 border border-border/50 rounded-lg">
                  <Toggle checked={config.bindInterfacesOnly} onChange={c => setConfig({ ...config, bindInterfacesOnly: c })} label={t("config.bindInterfacesOnly")} />
                </div>
                <div className="bg-surface-hover p-4 border border-border/50 rounded-lg">
                  <Toggle checked={config.disableNetbios} onChange={c => setConfig({ ...config, disableNetbios: c })} label={t("config.disableNetbios")} />
                </div>
              </div>
           </div>
        </div>

        <div className="bg-surface rounded-lg shadow-sm-subtle border border-border overflow-hidden">
           <div className="bg-surface-hover/50 px-5 py-3 border-b border-border/50">
             <h2 className="text-[13px] font-semibold tracking-wide uppercase text-status-disabled">{t("config.security")}</h2>
           </div>
           <div className="p-5 grid grid-cols-1 md:grid-cols-2 gap-6">
              <div className="space-y-2">
                <label className="text-[13px] font-medium text-foreground">{t("config.securityModel")}</label>
                <select 
                  value={config.security} onChange={(e) => setConfig({...config, security: e.target.value})}
                  className="w-full bg-surface border border-border focus:border-border-strong focus:outline-none focus:ring-4 focus:ring-ring text-foreground transition-all py-2.5 px-3.5 text-sm rounded-lg shadow-sm-subtle appearance-none cursor-pointer"
                >
                  <option value="user">User</option>
                  <option value="ads">{t("config.activeDirectory")}</option>
                  <option value="domain">Domain</option>
                </select>
              </div>
              <div className="space-y-2">
                <label className="text-[13px] font-medium text-foreground">{t("config.mapToGuest")}</label>
                <select 
                  value={config.mapToGuest} onChange={(e) => setConfig({...config, mapToGuest: e.target.value})}
                  className="w-full bg-surface border border-border focus:border-border-strong focus:outline-none focus:ring-4 focus:ring-ring text-foreground transition-all py-2.5 px-3.5 text-sm rounded-lg shadow-sm-subtle appearance-none cursor-pointer"
                >
                  <option value="never">{t("config.never")}</option>
                  <option value="bad user">{t("config.badUser")}</option>
                  <option value="bad password">{t("config.badPassword")}</option>
                </select>
              </div>
              <div className="space-y-2">
                <label className="text-[13px] font-medium text-foreground">{t("config.minProtocol")}</label>
                <select 
                  value={config.minProtocol} onChange={(e) => setConfig({...config, minProtocol: e.target.value})}
                  className="w-full bg-surface border border-border focus:border-border-strong focus:outline-none focus:ring-4 focus:ring-ring text-foreground transition-all py-2.5 px-3.5 text-sm rounded-lg shadow-sm-subtle appearance-none cursor-pointer"
                >
                  <option value="CORE">CORE (Legacy)</option>
                  <option value="NT1">NT1 (SMB1)</option>
                  <option value="SMB2">SMB2</option>
                  <option value="SMB3">SMB3</option>
                </select>
              </div>
              <div className="space-y-2">
                <label className="text-[13px] font-medium text-foreground">{t("config.maxProtocol")}</label>
                <select 
                  value={config.maxProtocol} onChange={(e) => setConfig({...config, maxProtocol: e.target.value})}
                  className="w-full bg-surface border border-border focus:border-border-strong focus:outline-none focus:ring-4 focus:ring-ring text-foreground transition-all py-2.5 px-3.5 text-sm rounded-lg shadow-sm-subtle appearance-none cursor-pointer"
                >
                  <option value="NT1">NT1 (SMB1)</option>
                  <option value="SMB2">SMB2</option>
                  <option value="SMB3">SMB3</option>
                </select>
              </div>
           </div>
        </div>

        {backups.length > 0 && (
          <div className="bg-surface rounded-lg shadow-sm-subtle border border-border overflow-hidden">
             <div className="bg-surface-hover/50 px-5 py-3 border-b border-border/50 flex items-center gap-2">
               <h2 className="text-[13px] font-semibold tracking-wide uppercase text-status-disabled">{t("config.backups")}</h2>
             </div>
             <div className="p-5">
                <p className="text-sm text-status-disabled mb-4">{t("config.backupsDesc")}</p>
                <div className="grid gap-3 select-none">
                  {backups.map((bkp) => (
                    <div key={bkp.filename} className="flex flex-col sm:flex-row sm:items-center justify-between gap-4 p-4 border border-border/70 rounded-md bg-surface-hover/30 hover:border-border transition-colors group">
                      <div className="flex items-center gap-3">
                         <div className="bg-surface border border-border/50 p-2 rounded-md">
                           <Clock className="w-5 h-5 text-status-disabled group-hover:text-foreground transition-colors" />
                         </div>
                         <div>
                            <div className="text-sm font-semibold text-foreground font-mono">{bkp.filename}</div>
                            <div className="text-xs text-status-disabled font-medium mt-0.5">{bkp.createdAt} • {bkp.size}</div>
                         </div>
                      </div>
                      <button 
                        type="button"
                        onClick={() => handleRestoreBackup(bkp.filename)}
                        className="text-xs font-medium bg-surface text-foreground hover:bg-surface-hover py-1.5 px-3 rounded-md transition-colors border border-border shadow-sm-subtle flex gap-1.5 items-center shrink-0"
                      >
                        <ArrowLeft className="w-3.5 h-3.5" />
                        {t("config.restore")}
                      </button>
                    </div>
                  ))}
                </div>
             </div>
          </div>
        )}

        <div className="flex justify-end pt-4 border-t border-border">
          <button 
            type="submit" 
            disabled={saving}
            className="text-sm font-medium text-brand-text bg-brand px-6 py-2.5 rounded-md hover:bg-brand-hover shadow-sm transition-colors flex items-center gap-2 disabled:opacity-50"
          >
            <Save className="w-4 h-4" /> {saving ? t("config.saving") : t("config.applyConfig")}
          </button>
        </div>
      </form>
    </div>
  );
}

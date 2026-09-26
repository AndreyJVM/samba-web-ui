import { useEffect, useState } from "react";
import { Settings, Save, RotateCcw, Fingerprint, Shield, Network, Zap } from "lucide-react";
import { api } from "../../lib/api";
import { useToast } from "../../components/ui/toast";
import { useConfirm } from "../../components/ui/confirm";

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

export default function ConfigPage() {
  const [config, setConfig] = useState<GlobalConfig | null>(null);
  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);
  const [activeTab, setActiveTab] = useState<"general" | "network" | "security">("general");

  const { info, error: toastError, success } = useToast();
  const { confirm } = useConfirm();

  const loadConfig = async () => {
    try {
      setLoading(true);
      const res = await api.get<GlobalConfig>("/api/config/global");
      setConfig(res);
    } catch (err: any) {
      toastError("Load Error", err.message);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => { loadConfig(); }, []);

  const handleSave = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!config) return;
    try {
      setSaving(true);
      await api.put("/api/config/global", config);
      success("Saved", "Configuration applied. Restart Samba to take effect.");
    } catch (err: any) {
      toastError("Save Error", err.message);
    } finally {
      setSaving(false);
    }
  };

  const handleRestore = async () => {
    const ok = await confirm({
      title: "Reset Configuration",
      message: "Are you sure you want to restore default smb.conf values? This will overwrite your settings.",
      destructive: true,
      confirmText: "Reset to Defaults"
    });
    if (!ok) return;
    info("Not Implemented", "Factory reset is in development.");
  };

  if (loading) return <div className="text-status-disabled text-sm animate-pulse p-4">Loading configuration...</div>;
  if (!config) return <div className="text-status-error text-sm p-4">Failed to load configuration. Make sure API is accessible.</div>;

  return (
    <div className="space-y-6 animate-in fade-in duration-500 max-w-4xl">
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4 pb-4 border-b border-border">
        <div className="flex items-center gap-3">
          <Settings className="w-5 h-5 text-foreground" />
          <h1 className="text-xl font-semibold text-foreground tracking-tight">Global Configuration</h1>
        </div>
      </div>

      <div className="flex bg-border rounded-sm p-[1px] shadow-sm-subtle gap-[1px]">
        {[
          { id: 'general', icon: Fingerprint, label: 'General' },
          { id: 'network', icon: Network, label: 'Network' },
          { id: 'security', icon: Shield, label: 'Security' }
        ].map(t => (
          <button 
            key={t.id}
            type="button"
            onClick={() => setActiveTab(t.id as any)} 
            className={`flex-1 flex items-center justify-center gap-2 py-2 text-xs font-bold uppercase tracking-widest transition-colors ${
              activeTab === t.id ? 'bg-surface text-brand' : 'bg-surface hover:bg-surface-hover text-status-disabled'
            }`}
          >
            <t.icon className="w-3.5 h-3.5" /> {t.label}
          </button>
        ))}
      </div>

      <form onSubmit={handleSave} className="bg-surface border border-border shadow-sm-subtle rounded-md flex flex-col overflow-hidden">
        <div className="p-6">
          {activeTab === 'general' && (
            <div className="space-y-5">
              <div className="grid grid-cols-1 md:grid-cols-2 gap-5">
                <div className="space-y-1.5">
                  <label className="text-[10px] font-bold text-status-disabled uppercase tracking-widest">Workgroup</label>
                  <input required value={config.workgroup} onChange={e => setConfig({...config, workgroup: e.target.value})} className="w-full bg-background border border-border focus:border-brand text-foreground py-2 px-3 text-sm rounded-sm outline-none" />
                </div>
                <div className="space-y-1.5">
                  <label className="text-[10px] font-bold text-status-disabled uppercase tracking-widest">NetBIOS Name</label>
                  <input value={config.netbiosName || ""} onChange={e => setConfig({...config, netbiosName: e.target.value})} placeholder="Auto-generated if empty" className="w-full bg-background border border-border focus:border-brand text-foreground py-2 px-3 text-sm rounded-sm outline-none" />
                </div>
                <div className="space-y-1.5 md:col-span-2">
                  <label className="text-[10px] font-bold text-status-disabled uppercase tracking-widest">Server String</label>
                  <input value={config.serverString} onChange={e => setConfig({...config, serverString: e.target.value})} className="w-full bg-background border border-border focus:border-brand text-foreground py-2 px-3 text-sm rounded-sm outline-none" />
                </div>
              </div>
              <div className="pt-4 border-t border-border">
                <label className="flex items-center gap-2 cursor-pointer">
                  <input type="checkbox" checked={config.loadPrinters} onChange={(e) => setConfig({...config, loadPrinters: e.target.checked})} className="w-3.5 h-3.5 text-brand bg-background border-border rounded-sm focus:ring-brand accent-brand" />
                  <span className="text-xs font-semibold text-foreground uppercase tracking-wide">Load Printers</span>
                </label>
              </div>
            </div>
          )}

          {activeTab === 'network' && (
            <div className="space-y-5">
              <div className="space-y-1.5">
                <label className="text-[10px] font-bold text-status-disabled uppercase tracking-widest">Interfaces (Listen IP/Eth)</label>
                <input value={config.interfaces || ""} onChange={e => setConfig({...config, interfaces: e.target.value})} placeholder="127.0.0.0/8 eth0" className="w-full bg-background border border-border focus:border-brand text-foreground font-mono py-2 px-3 text-sm rounded-sm outline-none" />
              </div>
              <div className="pt-4 border-t border-border space-y-3">
                <label className="flex items-center gap-2 cursor-pointer">
                  <input type="checkbox" checked={config.bindInterfacesOnly} onChange={(e) => setConfig({...config, bindInterfacesOnly: e.target.checked})} className="w-3.5 h-3.5 text-brand bg-background border-border rounded-sm focus:ring-brand accent-brand" />
                  <span className="text-xs font-semibold text-foreground uppercase tracking-wide">Bind Interfaces Only</span>
                </label>
                <label className="flex items-center gap-2 cursor-pointer">
                  <input type="checkbox" checked={config.disableNetbios} onChange={(e) => setConfig({...config, disableNetbios: e.target.checked})} className="w-3.5 h-3.5 text-brand bg-background border-border rounded-sm focus:ring-brand accent-brand" />
                  <span className="text-xs font-semibold text-foreground uppercase tracking-wide">Disable NetBIOS</span>
                </label>
              </div>
            </div>
          )}

          {activeTab === 'security' && (
            <div className="space-y-5">
              <div className="grid grid-cols-1 md:grid-cols-2 gap-5">
                <div className="space-y-1.5">
                  <label className="text-[10px] font-bold text-status-disabled uppercase tracking-widest">Security Model</label>
                  <select value={config.security} onChange={e => setConfig({...config, security: e.target.value})} className="w-full bg-background border border-border focus:border-brand text-foreground py-2 px-3 text-sm rounded-sm outline-none">
                    <option value="user">User</option>
                    <option value="ads">Active Directory</option>
                  </select>
                </div>
                <div className="space-y-1.5">
                  <label className="text-[10px] font-bold text-status-disabled uppercase tracking-widest">Map To Guest</label>
                  <select value={config.mapToGuest} onChange={e => setConfig({...config, mapToGuest: e.target.value})} className="w-full bg-background border border-border focus:border-brand text-foreground py-2 px-3 text-sm rounded-sm outline-none">
                    <option value="Never">Never</option>
                    <option value="Bad User">Bad User</option>
                    <option value="Bad Password">Bad Password</option>
                  </select>
                </div>
              </div>
              <div className="pt-4 border-t border-border grid grid-cols-1 md:grid-cols-2 gap-5">
                <div className="space-y-1.5">
                  <label className="text-[10px] font-bold text-status-disabled uppercase tracking-widest flex items-center gap-1"><Zap className="w-3 h-3"/> Min Protocol</label>
                  <select value={config.serverMinProtocol} onChange={e => setConfig({...config, serverMinProtocol: e.target.value})} className="w-full bg-background border border-border focus:border-brand text-foreground py-2 px-3 text-sm rounded-sm outline-none">
                    <option value="CORE">CORE (Legacy)</option>
                    <option value="NT1">NT1 (SMB 1)</option>
                    <option value="SMB2">SMB 2</option>
                    <option value="SMB3">SMB 3</option>
                  </select>
                </div>
                <div className="space-y-1.5">
                  <label className="text-[10px] font-bold text-status-disabled uppercase tracking-widest flex items-center gap-1"><Zap className="w-3 h-3"/> Max Protocol</label>
                  <select value={config.serverMaxProtocol} onChange={e => setConfig({...config, serverMaxProtocol: e.target.value})} className="w-full bg-background border border-border focus:border-brand text-foreground py-2 px-3 text-sm rounded-sm outline-none">
                    <option value="NT1">NT1 (SMB 1)</option>
                    <option value="SMB2">SMB 2</option>
                    <option value="SMB3">SMB 3</option>
                  </select>
                </div>
              </div>
            </div>
          )}
        </div>

        <div className="bg-surface-hover/30 p-4 border-t border-border flex justify-between gap-3 mt-auto">
          <button type="button" onClick={handleRestore} className="text-xs font-bold uppercase tracking-widest text-foreground bg-background border border-border px-5 py-2 rounded-sm hover:bg-surface transition-colors flex items-center gap-2">
            <RotateCcw className="w-3.5 h-3.5" /> Defaults
          </button>
          
          <button disabled={saving} type="submit" className="text-xs font-bold uppercase tracking-widest text-brand-text bg-brand px-6 py-2 rounded-sm hover:bg-brand-hover transition-colors flex items-center gap-2">
            <Save className="w-3.5 h-3.5" /> {saving ? "Saving..." : "Apply Config"}
          </button>
        </div>
      </form>
    </div>
  );
}

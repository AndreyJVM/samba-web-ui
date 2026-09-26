import { useEffect, useState } from "react";
import { FolderKanban, Plus, Trash2, Settings2, ShieldCheck, Users, ChevronDown, ChevronUp } from "lucide-react";
import { api } from "../../lib/api";
import { useToast } from "../../components/ui/toast";
import { useConfirm } from "../../components/ui/confirm";
import { Modal } from "../../components/ui/modal";
import { Toggle } from "../../components/ui/toggle";
import { useTranslation } from "../../lib/i18n";

interface Share {
  name: string;
  path: string;
  comment?: string;
  readOnly: boolean;
  guestOk: boolean;
  browseable: boolean;
  validUsers?: string;
  writeList?: string;
  createMask?: string;
  directoryMask?: string;
  forceUser?: string;
  forceGroup?: string;
  maxConnections?: string;
  hostsAllow?: string;
  hostsDeny?: string;
  isNew?: boolean;
}

interface User { username: string; }
interface Group { name: string; }

export default function SharesPage() {
  const [shares, setShares] = useState<Share[]>([]);
  const [users, setUsers] = useState<User[]>([]);
  const [groups, setGroups] = useState<Group[]>([]);
  
  const [loading, setLoading] = useState(true);

  const [isEditing, setIsEditing] = useState(false);
  const [currentShare, setCurrentShare] = useState<Partial<Share> | null>(null);
  const [showAdvanced, setShowAdvanced] = useState(false);
  
  const { error: toastError, success } = useToast();
  const { confirm } = useConfirm();
  const { t } = useTranslation();

  const fetchAll = async () => {
    try {
      setLoading(true);
      const [shRes, usRes, grRes] = await Promise.all([
        api.get<Share[]>("/api/shares").catch(() => []),
        api.get<User[]>("/api/users").catch(() => []),
        api.get<Group[]>("/api/groups").catch(() => [])
      ]);
      setShares(shRes || []);
      setUsers(usRes || []);
      setGroups(grRes || []);
    } catch (err: any) {
      toastError("API Error", err.message);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchAll();
  }, []);

  const handleDelete = async (name: string) => {
    const ok = await confirm({
      title: t("common.delete"),
      message: `${t("common.confirm")} [${name}]?`,
      destructive: true,
      confirmText: t("common.delete")
    });
    if (!ok) return;

    try {
      await api.delete(`/api/shares/${name}`);
      success(t("common.delete"), `Share ${name} removed`);
      fetchAll();
    } catch (err: any) {
      toastError("Delete Error", err.message);
    }
  };

  const handleSave = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!currentShare?.name || !currentShare?.path) return;

    const isNew = currentShare.isNew === true;
    const url = isNew ? "/api/shares" : `/api/shares/${currentShare.name}`;

    try {
      const dto = {
        name: currentShare.name,
        path: currentShare.path,
        comment: currentShare.comment || "",
        readOnly: currentShare.readOnly,
        browseable: currentShare.browseable,
        guestOk: currentShare.guestOk,
        validUsers: currentShare.validUsers || "",
        writeList: currentShare.writeList || "",
        createMask: currentShare.createMask || "",
        directoryMask: currentShare.directoryMask || "",
        forceUser: currentShare.forceUser || "",
        forceGroup: currentShare.forceGroup || "",
        maxConnections: currentShare.maxConnections || "",
        hostsAllow: currentShare.hostsAllow || "",
        hostsDeny: currentShare.hostsDeny || ""
      };

      if (isNew) {
        await api.post(url, dto);
      } else {
        await api.put(url, dto);
      }
      
      success(isNew ? t("common.create") : t("common.save"), "Configuration saved");
      setIsEditing(false);
      fetchAll();
    } catch (err: any) {
      toastError("Save Error", err.message);
    }
  };

  const openEditor = (share?: Share) => {
    if (share) {
      setCurrentShare({ ...share, isNew: false });
    } else {
      setCurrentShare({
        name: "",
        path: "",
        browseable: true,
        readOnly: true,
        guestOk: false,
        isNew: true
      });
    }
    setShowAdvanced(false);
    setIsEditing(true);
  };

  const toggleToValidUsers = (token: string) => {
    if (!currentShare) return;
    let current = (currentShare.validUsers || "").split(",").map(s => s.trim()).filter(Boolean);
    if (current.includes(token)) {
      current = current.filter(s => s !== token);
    } else {
      current.push(token);
    }
    setCurrentShare({ ...currentShare, validUsers: current.join(", ") });
  };

  const activeTokens = (currentShare?.validUsers || "").split(",").map(t => t.trim());

  return (
    <div className="space-y-6 animate-in fade-in duration-500 pb-10">
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4 pb-4">
        <h1 className="text-2xl font-semibold text-foreground tracking-tight">{t("shares.title")}</h1>
        <button onClick={() => openEditor()} className="text-[13px] font-medium text-brand-text bg-brand px-4 py-2 rounded-md hover:bg-brand-hover shadow-sm transition-colors flex items-center gap-2">
          <Plus className="w-4 h-4" /> {t("shares.newShare")}
        </button>
      </div>

      {loading ? (
        <div className="flex flex-col gap-3">
          {[1,2,3].map(i => (
             <div key={i} className="h-20 bg-surface shadow-sm-subtle border border-border/50 rounded-lg animate-pulse" />
          ))}
        </div>
      ) : shares.filter(s => s.name !== 'global').length === 0 ? (
        <div className="text-center py-24 bg-surface rounded-lg border border-border border-dashed shadow-sm-subtle">
          <FolderKanban className="w-12 h-12 text-status-disabled mx-auto mb-4 opacity-50" />
          <h3 className="text-base font-semibold text-foreground">{t("shares.noShares")}</h3>
          <p className="text-[13px] text-status-disabled mt-1 mb-6">{t("shares.noSharesDesc")}</p>
          <button onClick={() => openEditor()} className="text-[13px] font-medium text-foreground bg-surface border border-border px-4 py-2 rounded-md hover:bg-surface-hover shadow-sm-subtle transition-colors">
            {t("shares.createShare")}
          </button>
        </div>
      ) : (
        <div className="bg-surface rounded-lg border border-border shadow-sm-subtle overflow-hidden overflow-x-auto">
          <table className="w-full text-left min-w-[800px] border-collapse">
            <thead className="bg-surface-hover/50 text-[11px] font-medium text-status-disabled uppercase">
              <tr>
                <th className="py-3 px-5 w-[25%] font-medium">{t("shares.shareName")}</th>
                <th className="py-3 px-5 w-[35%] font-medium">{t("shares.diskPath")}</th>
                <th className="py-3 px-5 w-[25%] font-medium">{t("shares.access")}</th>
                <th className="py-3 px-5 text-right w-[15%] font-medium">{t("common.actions")}</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-border">
              {shares.filter(s => s.name !== 'global').map((share) => (
                <tr key={share.name} className="hover:bg-surface-hover/50 transition-colors group">
                  <td className="py-4 px-5">
                    <div className="flex items-center gap-2.5">
                       <div className="bg-surface-hover border border-border/50 p-1.5 rounded-md">
                         <FolderKanban className="w-4 h-4 text-foreground" />
                       </div>
                       <span className="text-foreground font-semibold text-sm">{share.name}</span>
                    </div>
                    {share.comment && <div className="text-[12px] text-status-disabled mt-1.5 truncate max-w-[220px]" title={share.comment}>{share.comment}</div>}
                  </td>
                  <td className="py-4 px-5">
                    <span className="text-[13px] font-mono text-status-disabled bg-surface-hover px-2 py-1 rounded-md border border-border/50" title={share.path}>
                      {share.path || '/'}
                    </span>
                  </td>
                  <td className="py-4 px-5">
                    <div className="flex flex-wrap gap-2">
                      {share.readOnly && <span className="bg-status-warning/10 text-status-warning border border-status-warning/20 text-[10px] font-bold px-2 py-0.5 rounded-full uppercase tracking-wider">RO</span>}
                      {share.guestOk && <span className="bg-status-active/10 text-status-active border border-status-active/20 text-[10px] font-bold px-2 py-0.5 rounded-full uppercase tracking-wider">GUEST</span>}
                      {!share.readOnly && !share.guestOk && <span className="text-status-disabled text-[10px] bg-surface-hover border border-border font-bold px-2 py-0.5 rounded-full uppercase tracking-wider">{t("shares.private")}</span>}
                    </div>
                  </td>
                  <td className="py-4 px-5 text-right">
                    <div className="flex items-center justify-end gap-1 opacity-0 group-hover:opacity-100 transition-opacity">
                      <button onClick={() => openEditor(share)} className="p-2 text-status-disabled hover:text-foreground hover:bg-surface-hover rounded-md transition-colors border border-transparent hover:border-border" title={t("shares.configure")}>
                        <Settings2 className="w-4 h-4" />
                      </button>
                      <button onClick={() => handleDelete(share.name)} className="p-2 text-status-disabled hover:text-status-error hover:bg-surface-hover rounded-md transition-colors border border-transparent hover:border-border" title={t("common.delete")}>
                        <Trash2 className="w-4 h-4" />
                      </button>
                    </div>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}

      {currentShare && (
        <Modal 
          isOpen={isEditing} 
          onClose={() => setIsEditing(false)}
          title={currentShare.isNew ? t("shares.newShare") : `${t("shares.configure")}: ${currentShare.name}`}
          maxWidth="2xl"
        >
          <form onSubmit={handleSave} className="space-y-6">
            <div className="grid grid-cols-1 md:grid-cols-2 gap-5">
              <div className="space-y-2">
                <label className="text-[12px] font-medium text-foreground">{t("shares.shareName")}</label>
                <input 
                  required 
                  value={currentShare.name} 
                  disabled={!currentShare.isNew}
                  onChange={(e) => setCurrentShare({ ...currentShare, name: e.target.value })} 
                  className={`w-full bg-surface border border-border focus:border-border-strong focus:outline-none focus:ring-4 focus:ring-ring text-foreground transition-all py-2.5 px-3.5 text-[13px] rounded-lg shadow-sm-subtle ${!currentShare.isNew ? 'opacity-60 cursor-not-allowed' : ''}`}
                />
              </div>
              <div className="space-y-2">
                <label className="text-[12px] font-medium text-foreground">{t("shares.diskPath")}</label>
                <input 
                  required 
                  value={currentShare.path || ""} 
                  onChange={(e) => setCurrentShare({ ...currentShare, path: e.target.value })} 
                  className="w-full bg-surface border border-border focus:border-border-strong focus:outline-none focus:ring-4 focus:ring-ring text-foreground transition-all py-2.5 px-3.5 text-[13px] rounded-lg shadow-sm-subtle font-mono"
                  placeholder="/mnt/disk1/data"
                />
              </div>
            </div>

            <div className="space-y-2">
              <label className="text-[12px] font-medium text-foreground">{t("shares.comment")}</label>
              <input 
                value={currentShare.comment || ""} 
                onChange={(e) => setCurrentShare({ ...currentShare, comment: e.target.value })} 
                className="w-full bg-surface border border-border focus:border-border-strong focus:outline-none focus:ring-4 focus:ring-ring text-foreground transition-all py-2.5 px-3.5 text-[13px] rounded-lg shadow-sm-subtle"
              />
            </div>

            <div className="grid grid-cols-1 sm:grid-cols-3 gap-4 pt-2">
               <div className="bg-surface-hover p-4 rounded-lg border border-border/50">
                 <Toggle checked={currentShare.browseable || false} onChange={c => setCurrentShare({ ...currentShare, browseable: c })} label={t("shares.browseable")} />
               </div>
               <div className="bg-surface-hover p-4 rounded-lg border border-border/50">
                 <Toggle checked={currentShare.readOnly || false} onChange={c => setCurrentShare({ ...currentShare, readOnly: c })} label={t("shares.readOnly")} />
               </div>
               <div className="bg-surface-hover p-4 rounded-lg border border-border/50">
                 <Toggle checked={currentShare.guestOk || false} onChange={c => setCurrentShare({ ...currentShare, guestOk: c })} label={t("shares.guestOk")} />
               </div>
            </div>

            <div className="space-y-3 pt-4 border-t border-border">
              <div className="flex flex-col gap-1">
                <label className="text-[12px] font-medium text-foreground">{t("shares.validUsers")}</label>
                <p className="text-[12px] text-status-disabled">{t("shares.validUsersHint")}</p>
              </div>
              
              <input 
                value={currentShare.validUsers || ""} 
                onChange={(e) => setCurrentShare({ ...currentShare, validUsers: e.target.value })} 
                className="w-full bg-surface border border-border focus:border-border-strong focus:outline-none focus:ring-4 focus:ring-ring text-foreground transition-all py-2.5 px-3.5 text-[13px] font-mono rounded-lg shadow-sm-subtle"
                placeholder="user1, user2, @admins"
              />

              <div className="bg-surface rounded-lg border border-border shadow-sm-subtle p-4 space-y-4">
                {groups.length > 0 && (
                  <div>
                    <div className="text-[11px] font-semibold text-status-disabled uppercase tracking-wide mb-2 flex items-center gap-1.5">
                      <Users className="w-3.5 h-3.5" /> {t("shares.externalGroups")}
                    </div>
                    <div className="flex flex-wrap gap-2">
                       {groups.map(g => {
                         const tk = `@${g.name}`;
                         const active = activeTokens.includes(tk);
                         return (
                           <button 
                              key={g.name} type="button" onClick={() => toggleToValidUsers(tk)}
                              className={`px-2.5 py-1 rounded-md text-[11px] font-mono font-medium transition-colors border ${active ? 'bg-foreground text-background border-foreground shadow-sm' : 'bg-surface border-border text-foreground hover:bg-surface-hover'}`}
                           >
                             {tk}
                           </button>
                         )
                       })}
                    </div>
                  </div>
                )}
                {users.length > 0 && (
                  <div>
                    <div className="text-[11px] font-semibold text-status-disabled uppercase tracking-wide mb-2 flex items-center gap-1.5">
                      <ShieldCheck className="w-3.5 h-3.5" /> {t("shares.localUsers")}
                    </div>
                    <div className="flex flex-wrap gap-2">
                       {users.map(u => {
                         const tk = u.username;
                         const active = activeTokens.includes(tk);
                         return (
                           <button 
                              key={u.username} type="button" onClick={() => toggleToValidUsers(tk)}
                              className={`px-2.5 py-1 rounded-md text-[11px] font-mono font-medium transition-colors border ${active ? 'bg-foreground text-background border-foreground shadow-sm' : 'bg-surface border-border text-foreground hover:bg-surface-hover'}`}
                           >
                             {tk}
                           </button>
                         )
                       })}
                    </div>
                  </div>
                )}
              </div>
            </div>

            <div className="pt-2">
              <button 
                type="button"
                onClick={() => setShowAdvanced(!showAdvanced)}
                className="w-full flex items-center justify-between text-[13px] font-medium text-foreground py-3 border-b border-border/50 hover:bg-surface-hover/50 transition-colors rounded-t-lg px-2"
              >
                <div className="flex items-center gap-2">
                  <Settings2 className="w-4 h-4 text-status-disabled" /> {t("shares.advOptions")}
                </div>
                {showAdvanced ? <ChevronUp className="w-4 h-4 text-status-disabled" /> : <ChevronDown className="w-4 h-4 text-status-disabled" />}
              </button>
              
              {showAdvanced && (
                 <div className="grid grid-cols-1 md:grid-cols-2 gap-4 mt-4 animate-in slide-in-from-top-2 duration-300">
                    {[
                      { l: t("shares.writeList"), k: "writeList", p: "@admins, user1" },
                      { l: t("shares.maxConn"), k: "maxConnections", p: "0" },
                      { l: t("shares.createMask"), k: "createMask", p: "0644" },
                      { l: t("shares.dirMask"), k: "directoryMask", p: "0755" },
                      { l: t("shares.forceUser"), k: "forceUser", p: "user1" },
                      { l: t("shares.forceGroup"), k: "forceGroup", p: "users" },
                      { l: t("shares.hostsAllow"), k: "hostsAllow", p: "192.168.1." },
                      { l: t("shares.hostsDeny"), k: "hostsDeny", p: "ALL" },
                    ].map(field => (
                      <div key={field.k} className="flex flex-col gap-1.5">
                        <label className="text-[11px] font-semibold text-status-disabled uppercase" title={field.l}>{field.l}</label>
                        <input 
                          value={(currentShare as any)[field.k] || ""} 
                          onChange={(e) => setCurrentShare({ ...currentShare, [field.k]: e.target.value })} 
                          className="bg-surface border border-border focus:border-border-strong focus:outline-none focus:ring-4 focus:ring-ring transition-all py-1.5 px-2.5 text-[12px] font-mono rounded-md shadow-sm-subtle text-foreground"
                          placeholder={field.p}
                        />
                      </div>
                    ))}
                 </div>
              )}
            </div>

            <div className="pt-6 flex justify-end gap-3 border-t border-border">
               <button type="button" onClick={() => setIsEditing(false)} className="text-[13px] font-medium text-foreground bg-surface border border-border px-5 py-2.5 rounded-md hover:bg-surface-hover shadow-sm-subtle transition-colors">
                 {t("common.cancel")}
               </button>
               <button type="submit" className="text-[13px] font-medium text-brand-text bg-brand px-6 py-2.5 rounded-md hover:bg-brand-hover shadow-sm transition-colors">
                 {t("common.save")}
               </button>
            </div>
          </form>
        </Modal>
      )}
    </div>
  );
}

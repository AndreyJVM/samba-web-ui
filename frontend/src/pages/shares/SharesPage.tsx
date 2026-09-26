import { useEffect, useState } from "react";
import { FolderKanban, Plus, Trash2, Settings2, ShieldCheck, Users, ChevronDown, ChevronUp } from "lucide-react";
import { api } from "../../lib/api";
import { useToast } from "../../components/ui/toast";
import { useConfirm } from "../../components/ui/confirm";
import { Modal } from "../../components/ui/modal";

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
      title: "Delete Share",
      message: `Permanently delete the network share [${name}]?`,
      destructive: true,
      confirmText: "Delete"
    });
    if (!ok) return;

    try {
      await api.delete(`/api/shares/${name}`);
      success("Deleted", `Share ${name} removed`);
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
      
      success(isNew ? "Created" : "Updated", "Configuration saved");
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
    <div className="space-y-6 animate-in fade-in duration-500 pb-10 max-w-6xl">
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4 pb-4 border-b border-border">
        <div className="flex items-center gap-3">
          <FolderKanban className="w-5 h-5 text-foreground" />
          <h1 className="text-xl font-semibold text-foreground tracking-tight">Network Shares</h1>
        </div>
        
        <button onClick={() => openEditor()} className="text-[11px] font-bold uppercase tracking-wider text-brand-text bg-brand px-4 py-2 rounded-sm hover:bg-brand-hover transition-colors flex items-center gap-2">
          <Plus className="w-3.5 h-3.5" /> New Share
        </button>
      </div>

      {loading ? (
        <div className="flex flex-col gap-[1px] bg-border p-[1px] rounded-md overflow-hidden">
          {[1,2,3,4].map(i => (
             <div key={i} className="h-12 bg-surface animate-pulse" />
          ))}
        </div>
      ) : shares.filter(s => s.name !== 'global').length === 0 ? (
        <div className="text-center py-20 bg-surface rounded-md border border-border border-dashed">
          <FolderKanban className="w-10 h-10 text-status-disabled mx-auto mb-3 opacity-50" />
          <h3 className="text-sm font-bold text-foreground">No shares available</h3>
          <p className="text-xs text-status-disabled mt-1 mb-4">Create your first directory to share over the network.</p>
          <button onClick={() => openEditor()} className="text-xs font-bold text-brand hover:underline">
             + Create Share
          </button>
        </div>
      ) : (
        <div className="bg-border rounded-md p-[1px] overflow-hidden overflow-x-auto shadow-sm-subtle">
          <table className="w-full text-left min-w-[800px] border-collapse bg-surface">
            <thead className="bg-surface-hover/50 text-[10px] font-bold uppercase tracking-widest text-status-disabled">
              <tr>
                <th className="py-3 px-4 w-[20%]">Share Name</th>
                <th className="py-3 px-4 w-[30%]">Disk Path</th>
                <th className="py-3 px-4 w-[20%]">Access</th>
                <th className="py-3 px-4 text-right w-[10%]">Actions</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-border">
              {shares.filter(s => s.name !== 'global').map((share) => (
                <tr key={share.name} className="hover:bg-surface-hover/50 transition-colors group">
                  <td className="py-3 px-4">
                    <div className="flex items-center gap-2">
                       <FolderKanban className="w-3.5 h-3.5 text-brand" />
                       <span className="text-foreground font-semibold text-sm">{share.name}</span>
                    </div>
                    {share.comment && <div className="text-[10px] text-status-disabled mt-0.5 truncate max-w-[200px]" title={share.comment}>{share.comment}</div>}
                  </td>
                  <td className="py-3 px-4">
                    <span className="text-xs font-mono text-status-disabled" title={share.path}>
                      {share.path || '/'}
                    </span>
                  </td>
                  <td className="py-3 px-4">
                    <div className="flex flex-wrap gap-1.5">
                      {share.readOnly && <span className="bg-status-warning/10 text-status-warning border border-status-warning/20 text-[9px] font-bold px-1.5 py-0.5 rounded-sm uppercase tracking-widest">RO</span>}
                      {share.guestOk && <span className="bg-status-active/10 text-status-active border border-status-active/20 text-[9px] font-bold px-1.5 py-0.5 rounded-sm uppercase tracking-widest">GUEST</span>}
                      {!share.readOnly && !share.guestOk && <span className="text-status-disabled text-[9px] font-bold uppercase tracking-widest px-1.5 py-0.5">PRIVATE</span>}
                    </div>
                  </td>
                  <td className="py-3 px-4 text-right">
                    <div className="flex items-center justify-end gap-1 opacity-0 group-hover:opacity-100 transition-opacity">
                      <button onClick={() => openEditor(share)} className="p-1.5 text-status-disabled hover:text-foreground hover:bg-surface-hover rounded" title="Configure">
                        <Settings2 className="w-4 h-4" />
                      </button>
                      <button onClick={() => handleDelete(share.name)} className="p-1.5 text-status-disabled hover:text-status-error hover:bg-surface-hover rounded" title="Delete">
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
          title={currentShare.isNew ? "New Share" : `Configure: ${currentShare.name}`}
          maxWidth="2xl"
        >
          <form onSubmit={handleSave} className="space-y-5">
            <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
              <div className="space-y-1.5">
                <label className="text-[10px] font-bold text-status-disabled uppercase tracking-widest">Network Name</label>
                <input 
                  required 
                  value={currentShare.name} 
                  disabled={!currentShare.isNew}
                  onChange={(e) => setCurrentShare({ ...currentShare, name: e.target.value })} 
                  className={`w-full bg-background border border-border focus:border-brand text-foreground transition-colors py-2 px-3 text-sm rounded-sm outline-none ${!currentShare.isNew ? 'opacity-60 cursor-not-allowed' : ''}`}
                />
              </div>
              <div className="space-y-1.5">
                <label className="text-[10px] font-bold text-status-disabled uppercase tracking-widest">Server Path</label>
                <input 
                  required 
                  value={currentShare.path || ""} 
                  onChange={(e) => setCurrentShare({ ...currentShare, path: e.target.value })} 
                  className="w-full bg-background border border-border focus:border-brand text-foreground transition-colors py-2 px-3 text-sm rounded-sm outline-none font-mono"
                  placeholder="/mnt/disk1/data"
                />
              </div>
            </div>

            <div className="space-y-1.5">
              <label className="text-[10px] font-bold text-status-disabled uppercase tracking-widest">Comment / Description</label>
              <input 
                value={currentShare.comment || ""} 
                onChange={(e) => setCurrentShare({ ...currentShare, comment: e.target.value })} 
                className="w-full bg-background border border-border focus:border-brand text-foreground transition-colors py-2 px-3 text-sm rounded-sm outline-none"
              />
            </div>

            <div className="grid grid-cols-1 sm:grid-cols-3 gap-3 pt-3">
              <label className="flex items-center gap-2 bg-background p-3 rounded-sm border border-border cursor-pointer hover:bg-surface-hover transition-colors">
                <input type="checkbox" checked={currentShare.browseable} onChange={(e) => setCurrentShare({ ...currentShare, browseable: e.target.checked })} className="w-3.5 h-3.5 text-brand bg-background border-border rounded-sm focus:ring-brand accent-brand" />
                <span className="text-xs font-semibold text-foreground uppercase tracking-wide">Browseable</span>
              </label>
              <label className="flex items-center gap-2 bg-background p-3 rounded-sm border border-border cursor-pointer hover:bg-surface-hover transition-colors">
                <input type="checkbox" checked={currentShare.readOnly} onChange={(e) => setCurrentShare({ ...currentShare, readOnly: e.target.checked })} className="w-3.5 h-3.5 text-brand bg-background border-border rounded-sm focus:ring-brand accent-brand" />
                <span className="text-xs font-semibold text-foreground uppercase tracking-wide">Read Only</span>
              </label>
              <label className="flex items-center gap-2 bg-background p-3 rounded-sm border border-border cursor-pointer hover:bg-surface-hover transition-colors">
                <input type="checkbox" checked={currentShare.guestOk} onChange={(e) => setCurrentShare({ ...currentShare, guestOk: e.target.checked })} className="w-3.5 h-3.5 text-brand bg-background border-border rounded-sm focus:ring-brand accent-brand" />
                <span className="text-xs font-semibold text-foreground uppercase tracking-wide">Guest Ok</span>
              </label>
            </div>

            <div className="space-y-3 pt-3 border-t border-border">
              <div className="flex flex-col gap-0.5">
                <label className="text-[10px] font-bold text-status-disabled uppercase tracking-widest">Valid Users / Access</label>
                <p className="text-[11px] text-status-disabled">Groups start with '@'. Comma separated.</p>
              </div>
              
              <input 
                value={currentShare.validUsers || ""} 
                onChange={(e) => setCurrentShare({ ...currentShare, validUsers: e.target.value })} 
                className="w-full bg-background border border-border focus:border-brand text-foreground transition-colors py-2 px-3 text-sm font-mono rounded-sm outline-none"
                placeholder="user1, user2, @admins"
              />

              <div className="bg-background p-3 rounded-sm border border-border space-y-3">
                {groups.length > 0 && (
                  <div>
                    <div className="text-[9px] font-bold text-status-disabled uppercase tracking-widest mb-1.5 flex items-center gap-1">
                      <Users className="w-3 h-3" /> External Groups
                    </div>
                    <div className="flex flex-wrap gap-1.5">
                       {groups.map(g => {
                         const tk = `@${g.name}`;
                         const active = activeTokens.includes(tk);
                         return (
                           <button 
                              key={g.name} type="button" onClick={() => toggleToValidUsers(tk)}
                              className={`px-2 py-1 rounded-sm text-[10px] font-mono font-bold uppercase transition-colors border ${active ? 'bg-brand text-brand-text border-brand' : 'bg-surface border-border text-foreground hover:border-border-strong'}`}
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
                    <div className="text-[9px] font-bold text-status-disabled uppercase tracking-widest mb-1.5 flex items-center gap-1">
                      <ShieldCheck className="w-3 h-3" /> Local Users
                    </div>
                    <div className="flex flex-wrap gap-1.5">
                       {users.map(u => {
                         const tk = u.username;
                         const active = activeTokens.includes(tk);
                         return (
                           <button 
                              key={u.username} type="button" onClick={() => toggleToValidUsers(tk)}
                              className={`px-2 py-1 rounded-sm text-[10px] font-mono font-bold uppercase transition-colors border ${active ? 'bg-foreground text-background border-foreground' : 'bg-surface border-border text-foreground hover:border-border-strong'}`}
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
                className="w-full flex items-center justify-between text-xs font-bold uppercase tracking-wider text-status-disabled hover:text-foreground py-2 border-b border-border transition-colors"
              >
                <div className="flex items-center gap-2">
                  <Settings2 className="w-4 h-4" /> Advanced Options
                </div>
                {showAdvanced ? <ChevronUp className="w-4 h-4" /> : <ChevronDown className="w-4 h-4" />}
              </button>
              
              {showAdvanced && (
                 <div className="grid grid-cols-1 md:grid-cols-2 gap-3 mt-3 animate-in slide-in-from-top-2 duration-200 bg-surface p-3 rounded-sm border border-border">
                    {[
                      { l: "Write List", k: "writeList", p: "@admins, user1" },
                      { l: "Max Connections", k: "maxConnections", p: "0" },
                      { l: "Create Mask", k: "createMask", p: "0644" },
                      { l: "Directory Mask", k: "directoryMask", p: "0755" },
                      { l: "Force User", k: "forceUser", p: "user1" },
                      { l: "Force Group", k: "forceGroup", p: "users" },
                      { l: "Hosts Allow", k: "hostsAllow", p: "192.168.1." },
                      { l: "Hosts Deny", k: "hostsDeny", p: "ALL" },
                    ].map(field => (
                      <div key={field.k} className="flex items-center gap-2">
                        <label className="text-[10px] font-bold text-status-disabled uppercase w-24 shrink-0 truncate" title={field.l}>{field.l}</label>
                        <input 
                          value={(currentShare as any)[field.k] || ""} 
                          onChange={(e) => setCurrentShare({ ...currentShare, [field.k]: e.target.value })} 
                          className="flex-1 bg-background border border-border py-1 px-2 text-xs font-mono rounded-sm focus:outline-none focus:border-brand text-foreground"
                          placeholder={field.p}
                        />
                      </div>
                    ))}
                 </div>
              )}
            </div>

            <div className="pt-4 flex justify-end gap-2 border-t border-border mt-6">
               <button type="button" onClick={() => setIsEditing(false)} className="text-xs font-bold uppercase tracking-widest text-foreground bg-background border border-border px-5 py-2 rounded-sm hover:bg-surface-hover transition-colors">
                 Cancel
               </button>
               <button type="submit" className="text-xs font-bold uppercase tracking-widest text-brand-text bg-brand px-6 py-2 rounded-sm hover:bg-brand-hover transition-colors">
                 Save
               </button>
            </div>
          </form>
        </Modal>
      )}
    </div>
  );
}

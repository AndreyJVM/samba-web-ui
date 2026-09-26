import { useEffect, useState } from "react";
import { Users, Plus, Trash2, UserPlus, UserMinus } from "lucide-react";
import { api } from "../../lib/api";
import { useToast } from "../../components/ui/toast";
import { useConfirm } from "../../components/ui/confirm";
import { Modal } from "../../components/ui/modal";
import { useTranslation } from "../../lib/i18n";

interface Group { name: string; members: string[]; }

export default function GroupsPage() {
  const [groups, setGroups] = useState<Group[]>([]);
  const [loading, setLoading] = useState(true);

  const [isCreating, setIsCreating] = useState(false);
  const [newGroupName, setNewGroupName] = useState("");

  const [isAddingUser, setIsAddingUser] = useState(false);
  const [targetGroup, setTargetGroup] = useState("");
  const [usernameToAdd, setUsernameToAdd] = useState("");

  const { error: toastError, success } = useToast();
  const { confirm } = useConfirm();
  const { t } = useTranslation();

  const fetchGroups = async () => {
    try {
      setLoading(true);
      const res = await api.get<Group[]>("/api/groups");
      setGroups(res);
    } catch (err: any) {
      toastError("API Error", err.message);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => { fetchGroups(); }, []);

  const handleDeleteGroup = async (groupName: string) => {
    const ok = await confirm({
      title: t("common.delete"),
      message: `${t("common.confirm")} ${groupName}?`,
      destructive: true,
      confirmText: t("common.delete")
    });
    if (!ok) return;

    try {
      await api.delete(`/api/groups/${groupName}`);
      success(t("common.delete"), `Group ${groupName} removed`);
      fetchGroups();
    } catch (err: any) {
      toastError("Delete Error", err.message);
    }
  };

  const handleCreateGroup = async (e: React.FormEvent) => {
    e.preventDefault();
    try {
      await api.post("/api/groups", { groupName: newGroupName });
      success(t("common.create"), `Group ${newGroupName} created`);
      setIsCreating(false);
      setNewGroupName("");
      fetchGroups();
    } catch (err: any) {
      toastError("Creation Error", err.message);
    }
  };

  const handleAddUser = async (e: React.FormEvent) => {
    e.preventDefault();
    try {
      await api.post(`/api/groups/${targetGroup}/users`, { username: usernameToAdd });
      success(t("common.add"), `User ${usernameToAdd} added to ${targetGroup}`);
      setIsAddingUser(false);
      setUsernameToAdd("");
      fetchGroups();
    } catch (err: any) {
      toastError("Add Error", err.message);
    }
  };

  const handleRemoveUser = async (groupName: string, username: string) => {
    const ok = await confirm({
      title: t("common.remove"),
      message: `${t("common.remove")} ${username}?`,
      destructive: true,
      confirmText: t("common.remove")
    });
    if (!ok) return;

    try {
      await api.delete(`/api/groups/${groupName}/users/${username}`);
      success(t("common.remove"), `User ${username} removed from ${groupName}`);
      fetchGroups();
    } catch (err: any) {
      toastError("Remove Error", err.message);
    }
  };

  const openAddUser = (groupName: string) => {
    setTargetGroup(groupName);
    setUsernameToAdd("");
    setIsAddingUser(true);
  };

  return (
    <div className="space-y-6 animate-in fade-in duration-500">
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4 pb-4">
        <h1 className="text-2xl font-semibold text-foreground tracking-tight">{t("groups.title")}</h1>
        <button onClick={() => setIsCreating(true)} className="text-[13px] font-medium text-brand-text bg-brand px-4 py-2 rounded-md hover:bg-brand-hover shadow-sm transition-colors flex items-center gap-2">
          <Plus className="w-4 h-4" /> {t("groups.newGroup")}
        </button>
      </div>
      
      {loading ? (
        <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
          {[1,2,3,4].map(i => (
             <div key={i} className="h-32 bg-surface shadow-sm-subtle border border-border/50 rounded-lg animate-pulse" />
          ))}
        </div>
      ) : groups.length === 0 ? (
        <div className="text-center py-24 bg-surface rounded-lg border border-border border-dashed shadow-sm-subtle">
          <Users className="w-12 h-12 text-status-disabled mx-auto mb-4 opacity-50" />
          <h3 className="text-base font-semibold text-foreground">{t("groups.noGroups")}</h3>
        </div>
      ) : (
        <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
          {groups.map((group) => (
            <div key={group.name} className="bg-surface rounded-lg flex flex-col border border-border shadow-sm-subtle overflow-hidden group">
              <div className="p-5 flex-1 flex flex-col">
                <div className="flex items-start justify-between mb-5">
                  <div className="flex flex-col">
                    <h3 className="font-medium text-[15px] text-foreground font-mono">@{group.name}</h3>
                    <p className="text-[11px] font-semibold tracking-wider text-status-disabled uppercase mt-1">{t("groups.posixGroup")}</p>
                  </div>
                  
                  <button onClick={() => handleDeleteGroup(group.name)} className="p-1.5 text-status-disabled hover:text-status-error hover:bg-surface-hover rounded-md transition-colors opacity-0 group-hover:opacity-100" title={t("common.delete")}>
                    <Trash2 className="w-4 h-4" />
                  </button>
                </div>

                <div className="border border-border/50 rounded-md bg-surface-hover/30 mt-auto overflow-hidden">
                  <div className="bg-surface-hover/80 px-3.5 py-2.5 flex items-center justify-between border-b border-border/50">
                    <span className="text-[11px] font-semibold uppercase tracking-wider text-status-disabled">{t("common.members")} ({group.members?.length || 0})</span>
                    <button 
                      onClick={() => openAddUser(group.name)}
                      className="text-brand hover:text-brand-hover text-[11px] font-bold uppercase flex items-center gap-1"
                    >
                      <Plus className="w-3.5 h-3.5" /> {t("common.add")}
                    </button>
                  </div>
                  
                  <div className="p-3 bg-surface">
                    {!group.members || group.members.length === 0 ? (
                      <div className="text-[12px] text-status-disabled">{t("groups.emptyGroup")}</div>
                    ) : (
                      <div className="flex flex-wrap gap-2">
                        {group.members.map(member => (
                          <div key={member} className="flex items-center gap-1.5 bg-surface-hover px-2.5 py-1 rounded-md text-[12px] font-mono border border-border text-foreground hover:border-border-strong transition-colors">
                            <span>{member}</span>
                            <button 
                              title={t("common.remove")} 
                              onClick={() => handleRemoveUser(group.name, member)}
                              className="text-status-disabled hover:text-status-error transition-colors"
                            >
                              <UserMinus className="w-3 h-3" />
                            </button>
                          </div>
                        ))}
                      </div>
                    )}
                  </div>
                </div>
              </div>
            </div>
          ))}
        </div>
      )}

      <Modal 
        isOpen={isCreating} 
        onClose={() => setIsCreating(false)}
        title={t("groups.newOsGroup")}
        maxWidth="sm"
      >
        <form onSubmit={handleCreateGroup} className="space-y-5">
          <div className="space-y-1.5">
            <label className="text-[12px] font-medium text-foreground">{t("groups.groupName")}</label>
            <input 
              required autoFocus pattern="^[a-z_][a-z0-9_-]*[$]?$"
              className="w-full bg-surface border border-border focus:border-border-strong focus:outline-none focus:ring-4 focus:ring-ring text-foreground transition-all py-2.5 px-3.5 text-[13px] font-mono rounded-lg shadow-sm-subtle"
              placeholder="smb_users"
              value={newGroupName} 
              onChange={(e) => setNewGroupName(e.target.value)} 
            />
          </div>
          <div className="pt-4 flex justify-end gap-3 border-t border-border mt-2">
             <button type="button" onClick={() => setIsCreating(false)} className="text-[13px] font-medium text-foreground bg-surface border border-border px-5 py-2.5 rounded-md hover:bg-surface-hover shadow-sm-subtle transition-colors">
               {t("common.cancel")}
             </button>
            <button type="submit" className="text-[13px] font-medium text-brand-text bg-brand px-6 py-2.5 rounded-md hover:bg-brand-hover shadow-sm transition-colors">
               {t("common.create")}
            </button>
          </div>
        </form>
      </Modal>

      <Modal 
        isOpen={isAddingUser} 
        onClose={() => setIsAddingUser(false)}
        title={`${t("groups.addToGroup")}@${targetGroup}`}
        maxWidth="sm"
      >
        <form onSubmit={handleAddUser} className="space-y-5">
          <div className="space-y-1.5">
            <label className="text-[12px] font-medium text-foreground">{t("users.username")}</label>
            <input 
              required autoFocus
              className="w-full bg-surface border border-border focus:border-border-strong focus:outline-none focus:ring-4 focus:ring-ring text-foreground transition-all py-2.5 px-3.5 text-[13px] font-mono rounded-lg shadow-sm-subtle"
              value={usernameToAdd} 
              onChange={(e) => setUsernameToAdd(e.target.value)} 
              placeholder="john_doe"
            />
          </div>
          <div className="pt-4 flex justify-end gap-3 border-t border-border mt-2">
             <button type="button" onClick={() => setIsAddingUser(false)} className="text-[13px] font-medium text-foreground bg-surface border border-border px-5 py-2.5 rounded-md hover:bg-surface-hover shadow-sm-subtle transition-colors">
               {t("common.cancel")}
             </button>
            <button type="submit" className="text-[13px] font-medium text-brand-text bg-brand px-6 py-2.5 rounded-md hover:bg-brand-hover shadow-sm transition-colors flex items-center gap-1.5">
              <UserPlus className="w-4 h-4" /> {t("common.add")}
            </button>
          </div>
        </form>
      </Modal>
    </div>
  );
}

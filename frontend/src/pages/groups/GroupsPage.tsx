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
    <div className="space-y-6 animate-in fade-in duration-500 pb-10">
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4 pb-4">
        <h1 className="text-2xl font-semibold text-foreground tracking-tight">{t("groups.title")}</h1>
        <button onClick={() => setIsCreating(true)} className="text-sm font-medium text-brand-text bg-brand px-4 py-2 rounded-md hover:bg-brand-hover shadow-sm transition-colors flex items-center gap-2">
          <Plus className="w-4 h-4" /> {t("groups.newGroup")}
        </button>
      </div>
      
      {loading ? (
        <div className="flex flex-col gap-3">
          {[1,2,3,4].map(i => (
             <div key={i} className="h-16 bg-surface shadow-sm-subtle border border-border/50 rounded-lg animate-pulse" />
          ))}
        </div>
      ) : groups.length === 0 ? (
        <div className="text-center py-24 bg-surface rounded-lg border border-border border-dashed shadow-sm-subtle">
          <Users className="w-12 h-12 text-status-disabled mx-auto mb-4 opacity-50" />
          <h3 className="text-base font-semibold text-foreground">{t("groups.noGroups")}</h3>
        </div>
      ) : (
        <div className="bg-surface rounded-lg border border-border shadow-sm-subtle overflow-hidden overflow-x-auto">
          <table className="w-full text-left min-w-[700px] border-collapse">
            <thead className="bg-surface-hover/50 text-[13px] font-semibold text-status-disabled uppercase tracking-wide">
              <tr>
                <th className="py-4 px-6 w-[35%]">{t("groups.groupName")}</th>
                <th className="py-4 px-6 w-[45%]">{t("common.members")}</th>
                <th className="py-4 px-6 text-right w-[20%]">{t("common.actions")}</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-border text-sm">
              {groups.map((group) => (
                <tr key={group.name} className="hover:bg-surface-hover/50 transition-colors group/row">
                  <td className="py-4 px-6 align-top">
                    <div className="flex flex-col gap-1">
                      <div className="flex items-center gap-2.5">
                         <div className="bg-surface-hover border border-border/50 p-2 rounded-md">
                           <Users className="w-4 h-4 text-foreground opacity-80" />
                         </div>
                         <span className="text-foreground font-semibold font-mono tracking-wide text-[15px]">@{group.name}</span>
                      </div>
                      <span className="text-[11px] font-semibold tracking-wider text-status-disabled uppercase mt-1 ml-12">{t("groups.posixGroup")}</span>
                    </div>
                  </td>
                  <td className="py-4 px-6 align-top">
                    <div className="flex flex-col gap-2">
                      <div className="flex items-center justify-between max-w-[400px]">
                        <span className="text-xs font-semibold uppercase tracking-wider text-status-disabled">{group.members?.length || 0} {t("common.members")} </span>
                        <button 
                          onClick={() => openAddUser(group.name)}
                          className="text-brand hover:text-brand-hover text-xs font-bold uppercase flex items-center gap-1 opacity-0 group-hover/row:opacity-100 transition-opacity bg-transparent"
                        >
                          <Plus className="w-3 h-3" /> {t("common.add")}
                        </button>
                      </div>
                      
                      {!group.members || group.members.length === 0 ? (
                        <div className="text-[13px] text-status-disabled my-1">{t("groups.emptyGroup")}</div>
                      ) : (
                        <div className="flex flex-wrap gap-2 max-w-[400px]">
                          {group.members.map(member => (
                            <div key={member} className="flex items-center gap-1.5 bg-background px-2.5 py-1 rounded-md text-[13px] font-mono border border-border text-foreground group/mem shadow-sm-subtle">
                              <span>{member}</span>
                              <button 
                                title={t("common.remove")} 
                                onClick={() => handleRemoveUser(group.name, member)}
                                className="text-status-disabled/50 hover:text-status-error transition-colors opacity-0 group-hover/mem:opacity-100"
                              >
                                <UserMinus className="w-3.5 h-3.5" />
                              </button>
                            </div>
                          ))}
                        </div>
                      )}
                    </div>
                  </td>
                  <td className="py-4 px-6 text-right align-top">
                    <div className="flex items-center justify-end gap-2 opacity-0 group-hover/row:opacity-100 transition-opacity">
                      <button onClick={() => handleDeleteGroup(group.name)} className="p-2 text-status-disabled hover:text-status-error hover:bg-surface-hover rounded-md transition-colors border border-transparent hover:border-border" title={t("common.delete")}>
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

      <Modal 
        isOpen={isCreating} 
        onClose={() => setIsCreating(false)}
        title={t("groups.newOsGroup")}
        maxWidth="sm"
      >
        <form onSubmit={handleCreateGroup} className="space-y-5">
          <div className="space-y-1.5">
            <label className="text-[13px] font-medium text-foreground">{t("groups.groupName")}</label>
            <input 
              required autoFocus pattern="^[a-z_][a-z0-9_-]*[$]?$"
              className="w-full bg-surface border border-border focus:border-border-strong focus:outline-none focus:ring-4 focus:ring-ring text-foreground transition-all py-2.5 px-3.5 text-sm font-mono rounded-lg shadow-sm-subtle"
              placeholder="smb_users"
              value={newGroupName} 
              onChange={(e) => setNewGroupName(e.target.value)} 
            />
          </div>
          <div className="pt-4 flex justify-end gap-3 border-t border-border mt-2">
             <button type="button" onClick={() => setIsCreating(false)} className="text-sm font-medium text-foreground bg-surface border border-border px-5 py-2.5 rounded-md hover:bg-surface-hover shadow-sm-subtle transition-colors">
               {t("common.cancel")}
             </button>
            <button type="submit" className="text-sm font-medium text-brand-text bg-brand px-6 py-2.5 rounded-md hover:bg-brand-hover shadow-sm transition-colors">
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
            <label className="text-[13px] font-medium text-foreground">{t("users.username")}</label>
            <input 
              required autoFocus
              className="w-full bg-surface border border-border focus:border-border-strong focus:outline-none focus:ring-4 focus:ring-ring text-foreground transition-all py-2.5 px-3.5 text-sm font-mono rounded-lg shadow-sm-subtle"
              value={usernameToAdd} 
              onChange={(e) => setUsernameToAdd(e.target.value)} 
              placeholder="john_doe"
            />
          </div>
          <div className="pt-4 flex justify-end gap-3 border-t border-border mt-2">
             <button type="button" onClick={() => setIsAddingUser(false)} className="text-sm font-medium text-foreground bg-surface border border-border px-5 py-2.5 rounded-md hover:bg-surface-hover shadow-sm-subtle transition-colors">
               {t("common.cancel")}
             </button>
            <button type="submit" className="text-sm font-medium text-brand-text bg-brand px-6 py-2.5 rounded-md hover:bg-brand-hover shadow-sm transition-colors flex items-center gap-1.5">
              <UserPlus className="w-4 h-4" /> {t("common.add")}
            </button>
          </div>
        </form>
      </Modal>
    </div>
  );
}

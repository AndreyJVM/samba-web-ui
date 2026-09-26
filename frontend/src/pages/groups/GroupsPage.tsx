import { useEffect, useState } from "react";
import { Users, Plus, Trash2, UserPlus, UserMinus } from "lucide-react";
import { api } from "../../lib/api";
import { useToast } from "../../components/ui/toast";
import { useConfirm } from "../../components/ui/confirm";
import { Modal } from "../../components/ui/modal";

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
      title: "Delete Group",
      message: `Delete POSIX group ${groupName}?`,
      destructive: true,
      confirmText: "Delete"
    });
    if (!ok) return;

    try {
      await api.delete(`/api/groups/${groupName}`);
      success("Deleted", `Group ${groupName} removed`);
      fetchGroups();
    } catch (err: any) {
      toastError("Delete Error", err.message);
    }
  };

  const handleCreateGroup = async (e: React.FormEvent) => {
    e.preventDefault();
    try {
      await api.post("/api/groups", { groupName: newGroupName });
      success("Created", `Group ${newGroupName} created`);
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
      success("Added", `User ${usernameToAdd} added to ${targetGroup}`);
      setIsAddingUser(false);
      setUsernameToAdd("");
      fetchGroups();
    } catch (err: any) {
      toastError("Add Error", err.message);
    }
  };

  const handleRemoveUser = async (groupName: string, username: string) => {
    const ok = await confirm({
      title: "Remove User",
      message: `Remove ${username} from ${groupName}?`,
      destructive: true,
      confirmText: "Remove"
    });
    if (!ok) return;

    try {
      await api.delete(`/api/groups/${groupName}/users/${username}`);
      success("Removed", `User ${username} removed from ${groupName}`);
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
    <div className="space-y-6 animate-in fade-in duration-500 max-w-6xl">
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4 pb-4 border-b border-border">
        <div className="flex items-center gap-3">
          <Users className="w-5 h-5 text-foreground" />
          <h1 className="text-xl font-semibold text-foreground tracking-tight">System Groups</h1>
        </div>
        <button onClick={() => setIsCreating(true)} className="text-[11px] font-bold uppercase tracking-wider text-brand-text bg-brand px-4 py-2 rounded-sm hover:bg-brand-hover transition-colors flex items-center gap-2">
          <Plus className="w-3.5 h-3.5" /> New Group
        </button>
      </div>
      
      {loading ? (
        <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
          {[1,2,3,4].map(i => (
             <div key={i} className="h-32 bg-surface animate-pulse rounded-md border border-border" />
          ))}
        </div>
      ) : groups.length === 0 ? (
        <div className="text-center py-20 bg-surface rounded-md border border-border border-dashed">
          <Users className="w-10 h-10 text-status-disabled mx-auto mb-3 opacity-50" />
          <h3 className="text-sm font-bold text-foreground">No OS Groups</h3>
        </div>
      ) : (
        <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
          {groups.map((group) => (
            <div key={group.name} className="bg-surface rounded-md flex flex-col border border-border overflow-hidden shadow-sm-subtle group">
              <div className="p-4 flex-1 flex flex-col">
                <div className="flex items-start justify-between mb-4">
                  <div className="flex flex-col">
                    <h3 className="font-bold text-sm text-foreground font-mono">@{group.name}</h3>
                    <p className="text-[10px] font-bold tracking-widest text-status-disabled uppercase mt-0.5">POSIX Group</p>
                  </div>
                  
                  <button onClick={() => handleDeleteGroup(group.name)} className="p-1.5 text-status-disabled hover:text-status-error hover:bg-surface-hover rounded transition-colors opacity-0 group-hover:opacity-100" title="Delete Group">
                    <Trash2 className="w-4 h-4" />
                  </button>
                </div>

                <div className="border border-border rounded-sm bg-background mt-auto overflow-hidden">
                  <div className="bg-surface-hover/50 px-3 py-1.5 flex items-center justify-between border-b border-border">
                    <span className="text-[9px] font-bold uppercase tracking-widest text-status-disabled">Members ({group.members?.length || 0})</span>
                    <button 
                      onClick={() => openAddUser(group.name)}
                      className="text-brand hover:text-brand-hover hover:underline text-[10px] uppercase font-bold flex items-center gap-1"
                    >
                      <Plus className="w-3 h-3" /> Add
                    </button>
                  </div>
                  
                  <div className="p-2">
                    {!group.members || group.members.length === 0 ? (
                      <div className="text-xs text-status-disabled italic">Empty group</div>
                    ) : (
                      <div className="flex flex-wrap gap-1.5">
                        {group.members.map(member => (
                          <div key={member} className="flex items-center gap-1 bg-surface px-1.5 py-0.5 rounded-sm text-[11px] font-mono border border-border text-foreground">
                            <span>{member}</span>
                            <button 
                              title="Remove" 
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
        title="New OS Group"
        maxWidth="sm"
      >
        <form onSubmit={handleCreateGroup} className="space-y-4">
          <div className="space-y-1.5">
            <label className="text-[10px] font-bold text-status-disabled uppercase tracking-widest">Group Name</label>
            <input 
              required autoFocus pattern="^[a-z_][a-z0-9_-]*[$]?$"
              className="w-full bg-background border border-border focus:border-brand text-foreground font-mono transition-colors py-2 px-3 text-sm rounded-sm outline-none"
              placeholder="smb_users"
              value={newGroupName} 
              onChange={(e) => setNewGroupName(e.target.value)} 
            />
          </div>
          <div className="pt-4 flex justify-end gap-2 border-t border-border mt-6">
             <button type="button" onClick={() => setIsCreating(false)} className="text-xs font-bold uppercase tracking-widest text-foreground bg-background border border-border px-5 py-2 rounded-sm hover:bg-surface-hover transition-colors">
               Cancel
             </button>
            <button type="submit" className="text-xs font-bold uppercase tracking-widest text-brand-text bg-brand px-6 py-2 rounded-sm hover:bg-brand-hover transition-colors">
               Create
            </button>
          </div>
        </form>
      </Modal>

      <Modal 
        isOpen={isAddingUser} 
        onClose={() => setIsAddingUser(false)}
        title={`Add to @${targetGroup}`}
        maxWidth="sm"
      >
        <form onSubmit={handleAddUser} className="space-y-4">
          <div className="space-y-1.5">
            <label className="text-[10px] font-bold text-status-disabled uppercase tracking-widest">Username</label>
            <input 
              required autoFocus
              className="w-full bg-background border border-border focus:border-brand text-foreground font-mono transition-colors py-2 px-3 text-sm rounded-sm outline-none"
              value={usernameToAdd} 
              onChange={(e) => setUsernameToAdd(e.target.value)} 
              placeholder="john_doe"
            />
          </div>
          <div className="pt-4 flex justify-end gap-2 border-t border-border mt-6">
             <button type="button" onClick={() => setIsAddingUser(false)} className="text-xs font-bold uppercase tracking-widest text-foreground bg-background border border-border px-5 py-2 rounded-sm hover:bg-surface-hover transition-colors">
               Cancel
             </button>
            <button type="submit" className="text-xs font-bold uppercase tracking-widest text-brand-text bg-brand px-6 py-2 rounded-sm hover:bg-brand-hover transition-colors flex items-center gap-1.5">
              <UserPlus className="w-4 h-4" /> Add
            </button>
          </div>
        </form>
      </Modal>
    </div>
  );
}

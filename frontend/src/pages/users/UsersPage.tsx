import { useEffect, useState } from "react";
import { Users, Plus, Trash2, Key, UserIcon } from "lucide-react";
import { api } from "../../lib/api";
import { useToast } from "../../components/ui/toast";
import { useConfirm } from "../../components/ui/confirm";
import { Modal } from "../../components/ui/modal";

interface User {
  username: string;
  fullName: string;
}

export default function UsersPage() {
  const [users, setUsers] = useState<User[]>([]);
  const [loading, setLoading] = useState(true);

  const [isCreating, setIsCreating] = useState(false);
  const [newUser, setNewUser] = useState({ username: "", fullName: "", password: "" });

  const [passwordModalOpen, setPasswordModalOpen] = useState(false);
  const [passwordTarget, setPasswordTarget] = useState("");
  const [newPassword, setNewPassword] = useState("");

  const { error: toastError, success } = useToast();
  const { confirm } = useConfirm();

  const fetchUsers = async () => {
    try {
      setLoading(true);
      const res = await api.get<User[]>("/api/users");
      setUsers(res);
    } catch (err: any) {
      toastError("API Error", err.message);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchUsers();
  }, []);

  const handleDelete = async (username: string) => {
    const ok = await confirm({
      title: "Delete User",
      message: `Permanently delete user ${username} from OS and Samba?`,
      destructive: true,
      confirmText: "Delete"
    });
    if (!ok) return;

    try {
      await api.delete(`/api/users/${username}`);
      success("Deleted", `User ${username} removed`);
      fetchUsers();
    } catch (err: any) {
      toastError("Delete Error", err.message);
    }
  };

  const handleCreate = async (e: React.FormEvent) => {
    e.preventDefault();
    try {
      await api.post("/api/users", newUser);
      success("Created", `User ${newUser.username} added successfully`);
      setIsCreating(false);
      setNewUser({ username: "", fullName: "", password: "" });
      fetchUsers();
    } catch (err: any) {
      toastError("Creation Error", err.message);
    }
  };

  const handleChangePassword = async (e: React.FormEvent) => {
    e.preventDefault();
    try {
      await api.put(`/api/users/${passwordTarget}/password`, { newPassword });
      success("Updated", `Password for ${passwordTarget} changed`);
      setPasswordModalOpen(false);
      setNewPassword("");
    } catch (err: any) {
      toastError("Password Error", err.message);
    }
  };

  const openPasswordModal = (username: string) => {
    setPasswordTarget(username);
    setNewPassword("");
    setPasswordModalOpen(true);
  };

  return (
    <div className="space-y-6 animate-in fade-in duration-500 max-w-6xl">
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4 pb-4 border-b border-border">
        <div className="flex items-center gap-3">
          <Users className="w-5 h-5 text-foreground" />
          <h1 className="text-xl font-semibold text-foreground tracking-tight">System Users</h1>
        </div>
        <button onClick={() => setIsCreating(true)} className="text-[11px] font-bold uppercase tracking-wider text-brand-text bg-brand px-4 py-2 rounded-sm hover:bg-brand-hover transition-colors flex items-center gap-2">
          <Plus className="w-3.5 h-3.5" /> Add User
        </button>
      </div>
      
      {loading ? (
        <div className="grid grid-cols-1 sm:grid-cols-2 md:grid-cols-3 xl:grid-cols-4 gap-[1px] bg-border p-[1px] rounded-md">
          {[1,2,3,4].map(i => (
             <div key={i} className="h-20 bg-surface animate-pulse" />
          ))}
        </div>
      ) : users.length === 0 ? (
        <div className="text-center py-20 bg-surface rounded-md border border-border border-dashed">
          <UserIcon className="w-10 h-10 text-status-disabled mx-auto mb-3 opacity-50" />
          <h3 className="text-sm font-bold text-foreground">No users found</h3>
          <p className="text-xs text-status-disabled mt-1 mb-4">You have no synchronized Samba users yet.</p>
        </div>
      ) : (
        <div className="grid grid-cols-1 sm:grid-cols-2 md:grid-cols-3 xl:grid-cols-4 gap-[1px] bg-border p-[1px] rounded-md shadow-sm-subtle">
          {users.map((user) => (
            <div key={user.username} className="bg-surface p-4 flex flex-col group hover:bg-surface-hover transition-colors">
              <div className="flex items-start justify-between">
                <div className="flex flex-col overflow-hidden w-full">
                  <div className="font-mono font-bold text-foreground text-sm flex items-center gap-2 truncate">
                    <span>@{user.username}</span>
                  </div>
                  <div className="text-xs text-status-disabled mt-1 truncate">{user.fullName || "—"}</div>
                </div>
                <div className="flex flex-col gap-1 -mt-1 -mr-2 opacity-0 group-hover:opacity-100 transition-opacity">
                  <button onClick={() => openPasswordModal(user.username)} className="p-1.5 text-status-disabled hover:text-brand hover:bg-background rounded transition-colors" title="Change Password">
                    <Key className="w-3.5 h-3.5" />
                  </button>
                  <button onClick={() => handleDelete(user.username)} className="p-1.5 text-status-disabled hover:text-status-error hover:bg-background rounded transition-colors" title="Delete">
                    <Trash2 className="w-3.5 h-3.5" />
                  </button>
                </div>
              </div>
            </div>
          ))}
        </div>
      )}

      <Modal 
        isOpen={isCreating} 
        onClose={() => setIsCreating(false)}
        title="New User"
        description="Add a system and Samba (smbpasswd) user"
        maxWidth="md"
      >
        <form onSubmit={handleCreate} className="space-y-4">
          <div className="space-y-1.5">
            <label className="text-[10px] font-bold text-status-disabled uppercase tracking-widest">Username</label>
            <input 
              required 
              pattern="^[a-z_][a-z0-9_-]*[$]?$"
              className="w-full bg-background border border-border focus:border-brand text-foreground transition-colors py-2 px-3 text-sm font-mono rounded-sm outline-none"
              value={newUser.username} 
              onChange={(e) => setNewUser({...newUser, username: e.target.value})} 
              placeholder="john_doe"
            />
          </div>
          <div className="space-y-1.5">
            <label className="text-[10px] font-bold text-status-disabled uppercase tracking-widest">Full Name (Optional)</label>
            <input 
              className="w-full bg-background border border-border focus:border-brand text-foreground transition-colors py-2 px-3 text-sm rounded-sm outline-none"
              value={newUser.fullName} 
              onChange={(e) => setNewUser({...newUser, fullName: e.target.value})} 
              placeholder="John Doe"
            />
          </div>
          <div className="space-y-1.5">
            <label className="text-[10px] font-bold text-status-disabled uppercase tracking-widest">Password</label>
            <input 
              required 
              type="password"
              className="w-full bg-background border border-border focus:border-brand text-foreground transition-colors py-2 px-3 text-sm rounded-sm outline-none"
              value={newUser.password} 
              onChange={(e) => setNewUser({...newUser, password: e.target.value})} 
              placeholder="••••••••"
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
        isOpen={passwordModalOpen} 
        onClose={() => setPasswordModalOpen(false)}
        title="Change Password"
        description={`Set new SMB password for @${passwordTarget}`}
        maxWidth="sm"
      >
        <form onSubmit={handleChangePassword} className="space-y-4">
          <div className="space-y-1.5">
            <label className="text-[10px] font-bold text-status-disabled uppercase tracking-widest">New Password</label>
            <input 
              required 
              type="password"
              className="w-full bg-background border border-border focus:border-brand text-foreground transition-colors py-2 px-3 text-sm rounded-sm outline-none"
              value={newPassword} 
              onChange={(e) => setNewPassword(e.target.value)} 
              placeholder="••••••••"
            />
          </div>
          <div className="pt-4 flex justify-end gap-2 border-t border-border mt-6">
             <button type="button" onClick={() => setPasswordModalOpen(false)} className="text-xs font-bold uppercase tracking-widest text-foreground bg-background border border-border px-5 py-2 rounded-sm hover:bg-surface-hover transition-colors">
               Cancel
             </button>
            <button type="submit" className="text-xs font-bold uppercase tracking-widest text-brand-text bg-brand px-6 py-2 rounded-sm hover:bg-brand-hover transition-colors">
              Save
            </button>
          </div>
        </form>
      </Modal>
    </div>
  );
}

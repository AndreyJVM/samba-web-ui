import { useEffect, useState } from "react";
import { Plus, Trash2, Key, UserIcon } from "lucide-react";
import { api } from "../../lib/api";
import { useToast } from "../../components/ui/toast";
import { useConfirm } from "../../components/ui/confirm";
import { Modal } from "../../components/ui/modal";
import { useTranslation } from "../../lib/i18n";

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
  const { t } = useTranslation();

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
      title: t("common.delete"),
      message: `Permanently delete user ${username} from OS and Samba?`,
      destructive: true,
      confirmText: t("common.delete")
    });
    if (!ok) return;

    try {
      await api.delete(`/api/users/${username}`);
      success(t("common.delete"), `User ${username} removed`);
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
    <div className="space-y-6 animate-in fade-in duration-500">
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4 pb-4">
        <h1 className="text-2xl font-semibold text-foreground tracking-tight">{t("users.title")}</h1>
        <button onClick={() => setIsCreating(true)} className="text-[13px] font-medium text-brand-text bg-brand px-4 py-2 rounded-md hover:bg-brand-hover shadow-sm transition-colors flex items-center gap-2">
          <Plus className="w-4 h-4" /> {t("users.addUser")}
        </button>
      </div>
      
      {loading ? (
        <div className="grid grid-cols-1 sm:grid-cols-2 md:grid-cols-3 xl:grid-cols-4 gap-4">
          {[1,2,3,4].map(i => (
             <div key={i} className="h-24 bg-surface shadow-sm-subtle border border-border/50 rounded-lg animate-pulse" />
          ))}
        </div>
      ) : users.length === 0 ? (
        <div className="text-center py-24 bg-surface rounded-lg border border-border border-dashed shadow-sm-subtle">
          <UserIcon className="w-12 h-12 text-status-disabled mx-auto mb-4 opacity-50" />
          <h3 className="text-base font-semibold text-foreground">{t("users.noUsers")}</h3>
          <p className="text-[13px] text-status-disabled mt-1 mb-6">{t("users.noUsersDesc")}</p>
        </div>
      ) : (
        <div className="grid grid-cols-1 sm:grid-cols-2 md:grid-cols-3 xl:grid-cols-4 gap-4">
          {users.map((user) => (
            <div key={user.username} className="bg-surface rounded-lg border border-border shadow-sm-subtle p-5 flex flex-col group hover:border-border-strong transition-colors relative overflow-hidden">
              <div className="flex items-start justify-between">
                <div className="flex flex-col overflow-hidden w-full">
                  <div className="font-mono font-medium text-foreground text-[14px] flex items-center gap-2 truncate">
                    <span>@{user.username}</span>
                  </div>
                  <div className="text-[13px] text-status-disabled mt-1 truncate">{user.fullName || "—"}</div>
                </div>
              </div>
              <div className="mt-4 pt-4 border-t border-border/50 flex gap-2 justify-end opacity-0 group-hover:opacity-100 transition-opacity">
                <button onClick={() => openPasswordModal(user.username)} className="p-1.5 text-status-disabled hover:text-foreground hover:bg-surface-hover rounded-md transition-colors" title={t("users.changePwd")}>
                  <Key className="w-4 h-4" />
                </button>
                <button onClick={() => handleDelete(user.username)} className="p-1.5 text-status-disabled hover:text-status-error hover:bg-surface-hover rounded-md transition-colors" title={t("common.delete")}>
                  <Trash2 className="w-4 h-4" />
                </button>
              </div>
            </div>
          ))}
        </div>
      )}

      <Modal 
        isOpen={isCreating} 
        onClose={() => setIsCreating(false)}
        title={t("users.addUser")}
        description="Add a system and Samba (smbpasswd) user"
        maxWidth="md"
      >
        <form onSubmit={handleCreate} className="space-y-5">
          <div className="space-y-1.5">
            <label className="text-[12px] font-medium text-foreground">{t("users.username")}</label>
            <input 
              required 
              pattern="^[a-z_][a-z0-9_-]*[$]?$"
              className="w-full bg-surface border border-border focus:border-border-strong focus:outline-none focus:ring-4 focus:ring-ring text-foreground transition-all py-2.5 px-3.5 text-[13px] font-mono rounded-lg shadow-sm-subtle"
              value={newUser.username} 
              onChange={(e) => setNewUser({...newUser, username: e.target.value})} 
              placeholder="john_doe"
            />
          </div>
          <div className="space-y-1.5">
            <label className="text-[12px] font-medium text-foreground">{t("users.fullName")}</label>
            <input 
              className="w-full bg-surface border border-border focus:border-border-strong focus:outline-none focus:ring-4 focus:ring-ring text-foreground transition-all py-2.5 px-3.5 text-[13px] rounded-lg shadow-sm-subtle"
              value={newUser.fullName} 
              onChange={(e) => setNewUser({...newUser, fullName: e.target.value})} 
              placeholder="John Doe"
            />
          </div>
          <div className="space-y-1.5">
            <label className="text-[12px] font-medium text-foreground">{t("users.password")}</label>
            <input 
              required 
              type="password"
              className="w-full bg-surface border border-border focus:border-border-strong focus:outline-none focus:ring-4 focus:ring-ring text-foreground transition-all py-2.5 px-3.5 text-[13px] rounded-lg shadow-sm-subtle"
              value={newUser.password} 
              onChange={(e) => setNewUser({...newUser, password: e.target.value})} 
              placeholder="••••••••"
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
        isOpen={passwordModalOpen} 
        onClose={() => setPasswordModalOpen(false)}
        title={t("users.changePwd")}
        description={`${t("users.pwdDesc")}@${passwordTarget}`}
        maxWidth="sm"
      >
        <form onSubmit={handleChangePassword} className="space-y-5">
          <div className="space-y-1.5">
            <label className="text-[12px] font-medium text-foreground">{t("users.newPassword")}</label>
            <input 
              required 
              type="password"
              className="w-full bg-surface border border-border focus:border-border-strong focus:outline-none focus:ring-4 focus:ring-ring text-foreground transition-all py-2.5 px-3.5 text-[13px] rounded-lg shadow-sm-subtle"
              value={newPassword} 
              onChange={(e) => setNewPassword(e.target.value)} 
              placeholder="••••••••"
            />
          </div>
          <div className="pt-4 flex justify-end gap-3 border-t border-border mt-2">
             <button type="button" onClick={() => setPasswordModalOpen(false)} className="text-[13px] font-medium text-foreground bg-surface border border-border px-5 py-2.5 rounded-md hover:bg-surface-hover shadow-sm-subtle transition-colors">
               {t("common.cancel")}
             </button>
            <button type="submit" className="text-[13px] font-medium text-brand-text bg-brand px-6 py-2.5 rounded-md hover:bg-brand-hover shadow-sm transition-colors">
               {t("common.save")}
            </button>
          </div>
        </form>
      </Modal>
    </div>
  );
}

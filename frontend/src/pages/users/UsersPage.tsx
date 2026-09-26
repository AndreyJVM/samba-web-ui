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

  const { toast, error: toastError, success } = useToast();
  const { confirm } = useConfirm();

  const fetchUsers = async () => {
    try {
      setLoading(true);
      const res = await api.get<User[]>("/api/users");
      setUsers(res);
    } catch (err: any) {
      toastError("Ошибка API", err.message);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchUsers();
  }, []);

  const handleDelete = async (username: string) => {
    const ok = await confirm({
      title: "Удаление пользователя",
      message: `Вы действительно хотите удалить пользователя ${username} из системы (Linux + Samba)? Это действие необратимо.`,
      destructive: true,
      confirmText: "Да, удалить"
    });
    if (!ok) return;

    try {
      await api.delete(`/api/users/${username}`);
      success("Удалено", `Пользователь ${username} успешно удален`);
      fetchUsers();
    } catch (err: any) {
      toastError("Ошибка удаления", err.message);
    }
  };

  const handleCreate = async (e: React.FormEvent) => {
    e.preventDefault();
    try {
      await api.post("/api/users", newUser);
      success("Пользователь создан", `Пользователь ${newUser.username} успешно создан`);
      setIsCreating(false);
      setNewUser({ username: "", fullName: "", password: "" });
      fetchUsers();
    } catch (err: any) {
      toastError("Ошибка создания", err.message);
    }
  };

  const handleChangePassword = async (e: React.FormEvent) => {
    e.preventDefault();
    try {
      await api.put(`/api/users/${passwordTarget}/password`, { newPassword });
      success("Успех", `Пароль для ${passwordTarget} успешно изменен`);
      setPasswordModalOpen(false);
      setNewPassword("");
    } catch (err: any) {
      toastError("Ошибка смены пароля", err.message);
    }
  };

  const openPasswordModal = (username: string) => {
    setPasswordTarget(username);
    setNewPassword("");
    setPasswordModalOpen(true);
  };

  return (
    <div className="space-y-8 animate-in fade-in duration-500">
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4">
        <div>
          <h1 className="text-3xl font-extrabold tracking-tight text-slate-900 flex items-center gap-3">
            <div className="bg-amber-100/50 p-2 rounded-2xl ring-1 ring-amber-500/10">
              <Users className="w-7 h-7 text-amber-600" />
            </div>
            Пользователи
          </h1>
          <p className="text-slate-500 text-[15px] mt-2">Управление пользователями ОС и доступом Samba</p>
        </div>
        <button onClick={() => setIsCreating(true)} className="bg-slate-900 hover:bg-slate-800 text-white px-5 py-2.5 rounded-xl font-medium tracking-wide flex items-center gap-2 shadow-lg shadow-slate-900/20 transition-all hover:shadow-xl hover:-translate-y-0.5">
          <Plus className="w-5 h-5" /> Добавить пользователя
        </button>
      </div>
      
      {loading ? (
        <div className="grid grid-cols-1 md:grid-cols-3 xl:grid-cols-4 gap-6">
          {[1,2,3,4].map(i => (
             <div key={i} className="h-40 bg-white/50 rounded-[20px] border border-slate-100 p-6 animate-pulse flex flex-col justify-between">
                <div className="w-1/3 h-6 bg-slate-200/50 rounded-full mb-4"></div>
                <div className="w-full h-3 bg-slate-100 rounded-full mb-2"></div>
                <div className="w-2/3 h-3 bg-slate-100 rounded-full"></div>
             </div>
          ))}
        </div>
      ) : users.length === 0 ? (
        <div className="text-center py-24 bg-white rounded-[24px] border border-dashed border-slate-200/60 shadow-sm">
          <UserIcon className="w-16 h-16 text-amber-100 mx-auto mb-4" />
          <h3 className="text-xl font-bold text-slate-800 tracking-tight">Пользователей нет</h3>
          <p className="text-slate-500 mt-2">Синхронизированные пользователи отсутствуют.</p>
        </div>
      ) : (
        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 xl:grid-cols-4 gap-4">
          {users.map((user) => (
            <div key={user.username} className="bg-white rounded-[20px] p-5 shadow-sm border border-slate-200/60 flex items-center justify-between group hover:shadow-md transition-all">
              <div className="flex flex-col overflow-hidden w-full">
                <div className="font-bold text-slate-800 text-lg flex items-center gap-2 truncate">
                  <div className="w-8 h-8 rounded-full bg-slate-100 flex items-center justify-center shrink-0">
                    <UserIcon className="w-4 h-4 text-slate-400" />
                  </div>
                  <span className="truncate">@{user.username}</span>
                </div>
                <div className="text-slate-500 text-sm mt-1 ml-10 truncate">{user.fullName || "—"}</div>
              </div>
              <div className="flex gap-2 opacity-0 group-hover:opacity-100 transition-opacity pl-2">
                <button onClick={() => openPasswordModal(user.username)} className="p-2.5 text-amber-500 hover:text-amber-600 hover:bg-amber-50 rounded-xl transition-colors bg-white border border-slate-100 shadow-sm hover:shadow" title="Сменить пароль">
                  <Key className="w-4 h-4" />
                </button>
                <button onClick={() => handleDelete(user.username)} className="p-2.5 text-rose-500 hover:text-rose-600 hover:bg-rose-50 rounded-xl transition-colors bg-white border border-slate-100 shadow-sm hover:shadow" title="Удалить">
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
        title="Новый пользователь"
        description="Добавление системного пользователя ОС и базы Samba (smbpasswd)"
        maxWidth="md"
      >
        <form onSubmit={handleCreate} className="space-y-4">
          <div className="space-y-2">
            <label className="text-[13px] font-bold text-slate-700 uppercase tracking-wide">Логин</label>
            <input 
              required 
              pattern="^[a-z_][a-z0-9_-]*[$]?$"
              className="w-full bg-slate-50 border border-slate-200 focus:border-blue-500 focus:ring-4 focus:ring-blue-500/10 hover:border-slate-300 transition-all py-3 px-4 text-base rounded-xl outline-none"
              value={newUser.username} 
              onChange={(e) => setNewUser({...newUser, username: e.target.value})} 
              placeholder="john_doe"
            />
          </div>
          <div className="space-y-2">
            <label className="text-[13px] font-bold text-slate-700 uppercase tracking-wide">Полное имя</label>
            <input 
              className="w-full bg-slate-50 border border-slate-200 focus:border-blue-500 focus:ring-4 focus:ring-blue-500/10 hover:border-slate-300 transition-all py-3 px-4 text-base rounded-xl outline-none"
              value={newUser.fullName} 
              onChange={(e) => setNewUser({...newUser, fullName: e.target.value})} 
              placeholder="John Doe"
            />
          </div>
          <div className="space-y-2">
            <label className="text-[13px] font-bold text-slate-700 uppercase tracking-wide">Пароль (Для SMB и ОС)</label>
            <input 
              required 
              type="password"
              className="w-full bg-slate-50 border border-slate-200 focus:border-blue-500 focus:ring-4 focus:ring-blue-500/10 hover:border-slate-300 transition-all py-3 px-4 text-base rounded-xl outline-none"
              value={newUser.password} 
              onChange={(e) => setNewUser({...newUser, password: e.target.value})} 
              placeholder="••••••••"
            />
          </div>
          <div className="pt-4 flex justify-end gap-3">
             <button type="button" onClick={() => setIsCreating(false)} className="px-5 py-2.5 bg-white border border-slate-200 hover:bg-slate-50 rounded-xl font-medium text-slate-700 transition-colors">
               Отмена
             </button>
            <button type="submit" className="bg-blue-600 hover:bg-blue-700 text-white px-6 py-2.5 rounded-xl font-bold tracking-wide shadow-lg shadow-blue-500/30 transition-all">
              Создать
            </button>
          </div>
        </form>
      </Modal>

      <Modal 
        isOpen={passwordModalOpen} 
        onClose={() => setPasswordModalOpen(false)}
        title="Смена пароля"
        description={`Изменение пароля для доступа ${passwordTarget}`}
        maxWidth="sm"
      >
        <form onSubmit={handleChangePassword} className="space-y-4">
          <div className="space-y-2">
            <label className="text-[13px] font-bold text-slate-700 uppercase tracking-wide">Новый пароль</label>
            <input 
              required 
              type="password"
              className="w-full bg-slate-50 border border-slate-200 focus:border-amber-500 focus:ring-4 focus:ring-amber-500/10 hover:border-slate-300 transition-all py-3 px-4 text-base rounded-xl outline-none"
              value={newPassword} 
              onChange={(e) => setNewPassword(e.target.value)} 
              placeholder="••••••••"
            />
          </div>
          <div className="pt-4 flex justify-end gap-3">
             <button type="button" onClick={() => setPasswordModalOpen(false)} className="px-5 py-2.5 bg-white border border-slate-200 hover:bg-slate-50 rounded-xl font-medium text-slate-700 transition-colors">
               Отмена
             </button>
            <button type="submit" className="bg-amber-500 hover:bg-amber-600 text-white px-6 py-2.5 rounded-xl font-bold tracking-wide shadow-lg shadow-amber-500/30 transition-all">
              Сохранить
            </button>
          </div>
        </form>
      </Modal>
    </div>
  );
}

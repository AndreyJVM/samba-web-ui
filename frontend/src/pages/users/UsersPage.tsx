import { useEffect, useState } from "react";
import { Users, Plus, Trash2, Key, UserIcon } from "lucide-react";

interface User {
  username: string;
  fullName: string;
}

export default function UsersPage() {
  const [users, setUsers] = useState<User[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");

  const [isCreating, setIsCreating] = useState(false);
  const [newUser, setNewUser] = useState({ username: "", fullName: "", password: "" });

  const [passwordModalOpen, setPasswordModalOpen] = useState(false);
  const [passwordTarget, setPasswordTarget] = useState("");
  const [newPassword, setNewPassword] = useState("");

  const fetchUsers = async () => {
    try {
      setLoading(true);
      const res = await fetch("/api/users");
      const json = await res.json();
      if (json.success) {
        setUsers(json.data);
      } else {
        setError(json.message || "Ошибка загрузки списка пользователей");
      }
    } catch (err) {
      setError("Ошибка API");
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchUsers();
  }, []);

  const handleDelete = async (username: string) => {
    if (!confirm(`Вы действительно хотите удалить пользователя ${username} из системы (Linux + Samba)?`)) return;
    try {
      const res = await fetch(`/api/users/${username}`, { method: "DELETE" });
      if (res.ok) fetchUsers();
      else alert("Ошибка удаления");
    } catch (err) {
      alert("Ошибка сети");
    }
  };

  const handleCreate = async (e: React.FormEvent) => {
    e.preventDefault();
    try {
      const res = await fetch("/api/users", {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify(newUser)
      });
      if (res.ok) {
        setIsCreating(false);
        setNewUser({ username: "", fullName: "", password: "" });
        fetchUsers();
      } else {
        const json = await res.json();
        alert(json.message || "Ошибка создания");
      }
    } catch (err) {
      alert("Ошибка сети");
    }
  };

  const handleChangePassword = async (e: React.FormEvent) => {
    e.preventDefault();
    try {
      const res = await fetch(`/api/users/${passwordTarget}/password`, {
        method: "PUT",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ newPassword })
      });
      if (res.ok) {
        setPasswordModalOpen(false);
        setNewPassword("");
        alert(`Пароль для ${passwordTarget} успешно изменен`);
      } else {
        const json = await res.json();
        alert(json.message || "Ошибка смены пароля");
      }
    } catch (err) {
      alert("Ошибка сети");
    }
  };

  const openPasswordModal = (username: string) => {
    setPasswordTarget(username);
    setNewPassword("");
    setPasswordModalOpen(true);
  };

  if (isCreating) {
    return (
      <div className="space-y-8 animate-in slide-in-from-bottom-4 duration-500 max-w-2xl mx-auto mt-10">
        <div className="flex items-center justify-between">
          <div>
            <h1 className="text-3xl font-extrabold tracking-tight text-slate-900">Новый пользователь</h1>
            <p className="text-slate-500 text-[15px] mt-2">Добавление системного пользвателя ОС и Samba (smbpasswd)</p>
          </div>
          <button onClick={() => setIsCreating(false)} className="px-5 py-2.5 bg-white border border-slate-200 hover:border-slate-300 hover:bg-slate-50 text-slate-700 font-medium rounded-xl transition-all shadow-sm">
            Отмена
          </button>
        </div>
        <div className="bg-white rounded-[24px] shadow-sm border border-slate-200/60 p-8 sm:p-10">
          <form onSubmit={handleCreate} className="space-y-6">
            <div className="space-y-3">
              <label className="text-[13px] font-bold text-slate-700 uppercase tracking-wide">Логин</label>
              <input 
                required 
                pattern="^[a-z_][a-z0-9_-]*[$]?$"
                className="w-full bg-slate-50 border border-slate-200 focus:border-blue-500 focus:ring-4 focus:ring-blue-500/10 hover:border-slate-300 transition-all py-4 px-4 text-base rounded-xl outline-none"
                value={newUser.username} 
                onChange={(e) => setNewUser({...newUser, username: e.target.value})} 
                placeholder="john_doe"
              />
            </div>
            <div className="space-y-3">
              <label className="text-[13px] font-bold text-slate-700 uppercase tracking-wide">Полное имя</label>
              <input 
                className="w-full bg-slate-50 border border-slate-200 focus:border-blue-500 focus:ring-4 focus:ring-blue-500/10 hover:border-slate-300 transition-all py-4 px-4 text-base rounded-xl outline-none"
                value={newUser.fullName} 
                onChange={(e) => setNewUser({...newUser, fullName: e.target.value})} 
                placeholder="John Doe"
              />
            </div>
            <div className="space-y-3">
              <label className="text-[13px] font-bold text-slate-700 uppercase tracking-wide">Пароль (Для SMB и ОС)</label>
              <input 
                required 
                type="password"
                className="w-full bg-slate-50 border border-slate-200 focus:border-blue-500 focus:ring-4 focus:ring-blue-500/10 hover:border-slate-300 transition-all py-4 px-4 text-base rounded-xl outline-none"
                value={newUser.password} 
                onChange={(e) => setNewUser({...newUser, password: e.target.value})} 
                placeholder="••••••••"
              />
            </div>
            <button type="submit" className="w-full bg-blue-600 hover:bg-blue-700 text-white px-8 py-4 rounded-xl font-bold tracking-wide shadow-lg shadow-blue-500/30 transition-all hover:-translate-y-0.5 mt-2">
              Создать пользователя
            </button>
          </form>
        </div>
      </div>
    );
  }

  if (passwordModalOpen) {
    return (
      <div className="space-y-8 animate-in slide-in-from-bottom-4 duration-500 max-w-2xl mx-auto mt-10">
        <div className="flex items-center justify-between">
          <div>
            <h1 className="text-3xl font-extrabold tracking-tight text-slate-900">Пароль SMB</h1>
            <p className="text-slate-500 text-[15px] mt-2">Смена пароля доступа к ресурсам для {passwordTarget}</p>
          </div>
          <button onClick={() => setPasswordModalOpen(false)} className="px-5 py-2.5 bg-white border border-slate-200 hover:border-slate-300 hover:bg-slate-50 text-slate-700 font-medium rounded-xl transition-all shadow-sm">
            Отмена
          </button>
        </div>
        <div className="bg-white rounded-[24px] shadow-sm border border-slate-200/60 p-8 sm:p-10">
          <form onSubmit={handleChangePassword} className="space-y-6">
            <div className="space-y-3">
              <label className="text-[13px] font-bold text-slate-700 uppercase tracking-wide">Новый пароль</label>
              <input 
                required 
                type="password"
                className="w-full bg-slate-50 border border-slate-200 focus:border-amber-500 focus:ring-4 focus:ring-amber-500/10 hover:border-slate-300 transition-all py-4 px-4 text-base rounded-xl outline-none"
                value={newPassword} 
                onChange={(e) => setNewPassword(e.target.value)} 
                placeholder="••••••••"
              />
            </div>
            <button type="submit" className="w-full bg-amber-500 hover:bg-amber-600 text-white px-8 py-4 rounded-xl font-bold tracking-wide shadow-lg shadow-amber-500/30 transition-all hover:-translate-y-0.5 mt-2">
              Установить новый пароль
            </button>
          </form>
        </div>
      </div>
    );
  }

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

      {error && (
        <div className="bg-rose-50 text-rose-600 p-4 rounded-xl text-[14px] font-medium border border-rose-100/50">
          ⚠️ {error}
        </div>
      )}
      
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
              <div className="flex flex-col">
                <div className="font-bold text-slate-800 text-lg">@{user.username}</div>
                <div className="text-slate-500 text-sm">{user.fullName || "—"}</div>
              </div>
              <div className="flex gap-2 opacity-0 group-hover:opacity-100 transition-opacity">
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
    </div>
  );
}

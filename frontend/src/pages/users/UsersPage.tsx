import { useEffect, useState } from "react";
import { Users, Plus, Trash2, Key, UserIcon } from "lucide-react";
import { Button } from "../../components/ui/button";
import { Input } from "../../components/ui/input";
import { Card, CardContent } from "../../components/ui/card";

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
      <div className="space-y-6 max-w-xl mx-auto mt-10">
        <div className="flex items-center justify-between">
          <h1 className="text-2xl font-bold tracking-tight">Новый пользователь</h1>
          <Button onClick={() => setIsCreating(false)} className="!bg-transparent text-foreground border border-border hover:bg-muted">
            Отмена
          </Button>
        </div>
        <Card>
          <CardContent className="p-6">
            <form onSubmit={handleCreate} className="space-y-4">
              <div className="space-y-2">
                <label className="text-sm font-medium">Имя пользователя (логин)</label>
                <Input 
                  required 
                  pattern="[a-zA-Z0-9_-]+"
                  value={newUser.username} 
                  onChange={(e) => setNewUser({...newUser, username: e.target.value})} 
                />
                <p className="text-xs text-muted-foreground">Только латиница, цифры и дефис</p>
              </div>
              <div className="space-y-2">
                <label className="text-sm font-medium">Полное имя (комментарий)</label>
                <Input 
                  value={newUser.fullName} 
                  onChange={(e) => setNewUser({...newUser, fullName: e.target.value})} 
                />
              </div>
              <div className="space-y-2">
                <label className="text-sm font-medium">Пароль (Samba)</label>
                <Input 
                  type="password"
                  required 
                  value={newUser.password} 
                  onChange={(e) => setNewUser({...newUser, password: e.target.value})} 
                />
              </div>
              <Button type="submit" className="w-full mt-4">Создать пользователя</Button>
            </form>
          </CardContent>
        </Card>
      </div>
    );
  }

  if (passwordModalOpen) {
    return (
      <div className="space-y-6 max-w-xl mx-auto mt-10">
        <div className="flex items-center justify-between">
          <h1 className="text-2xl font-bold tracking-tight">Смена пароля</h1>
          <Button onClick={() => setPasswordModalOpen(false)} className="!bg-transparent text-foreground border border-border hover:bg-muted">
            Отмена
          </Button>
        </div>
        <Card>
          <CardContent className="p-6">
            <form onSubmit={handleChangePassword} className="space-y-4">
              <p className="text-sm text-muted-foreground mb-4">
                Задайте новый SMB пароль для пользователя <strong>{passwordTarget}</strong>
              </p>
              <div className="space-y-2">
                <label className="text-sm font-medium">Новый пароль</label>
                <Input 
                  type="password"
                  required 
                  value={newPassword} 
                  onChange={(e) => setNewPassword(e.target.value)} 
                />
              </div>
              <Button type="submit" className="w-full mt-4">Изменить пароль</Button>
            </form>
          </CardContent>
        </Card>
      </div>
    );
  }

  return (
    <div className="space-y-6">
      <div className="flex justify-between items-center">
        <div>
          <h1 className="text-3xl font-bold tracking-tight">Пользователи</h1>
          <p className="text-muted-foreground">Управление пользователями Linux и SMB паролями.</p>
        </div>
        <Button onClick={() => setIsCreating(true)} className="flex gap-2">
          <Plus className="w-4 h-4" /> Добавить
        </Button>
      </div>

      {error && <div className="text-red-500 bg-red-50 p-4 rounded-lg">{error}</div>}
      
      {loading ? (
        <div className="text-muted-foreground animate-pulse">Загрузка...</div>
      ) : users.length === 0 ? (
        <div className="border border-dashed border-border rounded-xl p-12 text-center text-muted-foreground flex flex-col items-center">
          <Users className="w-12 h-12 mb-4 opacity-50" />
          В системе не найдено пользователей Samba
        </div>
      ) : (
        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4">
          {users.map((user) => (
            <Card key={user.username} className="flex flex-col">
              <CardContent className="p-5 flex-1 flex items-center gap-4">
                <div className="p-3 bg-blue-500/10 rounded-full">
                  <UserIcon className="w-6 h-6 text-blue-500" />
                </div>
                <div>
                  <h3 className="font-semibold text-lg">{user.username}</h3>
                  <p className="text-sm text-muted-foreground">{user.fullName || "—"}</p>
                </div>
              </CardContent>
              <div className="p-4 border-t border-border flex justify-end gap-2 bg-muted/10 rounded-b-xl">
                <Button onClick={() => openPasswordModal(user.username)} className="!bg-transparent text-foreground border border-border hover:bg-muted text-xs h-8">
                  <Key className="w-3.5 h-3.5 mr-1.5" /> Пароль
                </Button>
                <Button onClick={() => handleDelete(user.username)} className="!bg-transparent border border-red-200 text-red-600 hover:bg-red-50 hover:border-red-300 text-xs h-8">
                  <Trash2 className="w-3.5 h-3.5 mr-1.5" /> Удалить
                </Button>
              </div>
            </Card>
          ))}
        </div>
      )}
    </div>
  );
}

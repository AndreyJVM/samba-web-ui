import { useEffect, useState } from "react";
import { Users, Plus, Trash2, UserPlus, UserMinus } from "lucide-react";
import { Button } from "../../components/ui/button";
import { Input } from "../../components/ui/input";
import { Card, CardContent } from "../../components/ui/card";

interface Group {
  name: string;
  members: string[];
}

export default function GroupsPage() {
  const [groups, setGroups] = useState<Group[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");

  const [isCreating, setIsCreating] = useState(false);
  const [newGroupName, setNewGroupName] = useState("");

  const [isAddingUser, setIsAddingUser] = useState(false);
  const [targetGroup, setTargetGroup] = useState("");
  const [usernameToAdd, setUsernameToAdd] = useState("");

  const fetchGroups = async () => {
    try {
      setLoading(true);
      const res = await fetch("/api/groups");
      const json = await res.json();
      if (json.success) {
        setGroups(json.data);
      } else {
        setError(json.message || "Ошибка загрузки списка групп");
      }
    } catch (err) {
      setError("Ошибка API");
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchGroups();
  }, []);

  const handleDeleteGroup = async (groupName: string) => {
    if (!confirm(`Вы действительно хотите удалить группу ${groupName}?`)) return;
    try {
      const res = await fetch(`/api/groups/${groupName}`, { method: "DELETE" });
      if (res.ok) fetchGroups();
      else alert("Ошибка удаления группы");
    } catch (err) {
      alert("Ошибка сети");
    }
  };

  const handleCreateGroup = async (e: React.FormEvent) => {
    e.preventDefault();
    try {
      const res = await fetch("/api/groups", {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ name: newGroupName })
      });
      if (res.ok) {
        setIsCreating(false);
        setNewGroupName("");
        fetchGroups();
      } else {
        const json = await res.json();
        alert(json.message || "Ошибка создания группы");
      }
    } catch (err) {
      alert("Ошибка сети");
    }
  };

  const handleAddUser = async (e: React.FormEvent) => {
    e.preventDefault();
    try {
      const res = await fetch(`/api/groups/${targetGroup}/users`, {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ username: usernameToAdd })
      });
      if (res.ok) {
        setIsAddingUser(false);
        setUsernameToAdd("");
        fetchGroups();
      } else {
        const json = await res.json();
        alert(json.message || "Ошибка добавления пользователя");
      }
    } catch (err) {
      alert("Ошибка сети");
    }
  };

  const handleRemoveUser = async (groupName: string, username: string) => {
    if (!confirm(`Удалить пользователя ${username} из группы ${groupName}?`)) return;
    try {
      const res = await fetch(`/api/groups/${groupName}/users/${username}`, { method: "DELETE" });
      if (res.ok) fetchGroups();
      else alert("Ошибка удаления пользователя");
    } catch (err) {
      alert("Ошибка сети");
    }
  };

  const openAddUser = (groupName: string) => {
    setTargetGroup(groupName);
    setUsernameToAdd("");
    setIsAddingUser(true);
  };

  if (isCreating) {
    return (
      <div className="space-y-6 max-w-xl mx-auto mt-10">
        <div className="flex items-center justify-between">
          <h1 className="text-2xl font-bold tracking-tight">Новая группа ОС</h1>
          <Button onClick={() => setIsCreating(false)} className="!bg-transparent text-foreground border border-border hover:bg-muted">
            Отмена
          </Button>
        </div>
        <Card>
          <CardContent className="p-6">
            <form onSubmit={handleCreateGroup} className="space-y-4">
              <div className="space-y-2">
                <label className="text-sm font-medium">Название группы</label>
                <Input 
                  required 
                  pattern="[a-zA-Z0-9_-]+"
                  value={newGroupName} 
                  onChange={(e) => setNewGroupName(e.target.value)} 
                />
                <p className="text-xs text-muted-foreground">Только латиница, цифры и дефис</p>
              </div>
              <Button type="submit" className="w-full mt-4">Создать группу</Button>
            </form>
          </CardContent>
        </Card>
      </div>
    );
  }

  if (isAddingUser) {
    return (
      <div className="space-y-6 max-w-xl mx-auto mt-10">
        <div className="flex items-center justify-between">
          <h1 className="text-2xl font-bold tracking-tight">Добавление пользователя</h1>
          <Button onClick={() => setIsAddingUser(false)} className="!bg-transparent text-foreground border border-border hover:bg-muted">
            Отмена
          </Button>
        </div>
        <Card>
          <CardContent className="p-6">
            <form onSubmit={handleAddUser} className="space-y-4">
              <p className="text-sm text-muted-foreground mb-4">
                Добавление существующего системного пользователя в группу <strong>{targetGroup}</strong>
              </p>
              <div className="space-y-2">
                <label className="text-sm font-medium">Имя пользователя</label>
                <Input 
                  required 
                  value={usernameToAdd} 
                  onChange={(e) => setUsernameToAdd(e.target.value)} 
                  placeholder="Например: john_doe"
                />
              </div>
              <Button type="submit" className="w-full mt-4">Добавить в группу</Button>
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
          <h1 className="text-3xl font-bold tracking-tight">Группы пользователей</h1>
          <p className="text-muted-foreground">Управление группами ОС для коллективного доступа к SMB шарам.</p>
        </div>
        <Button onClick={() => setIsCreating(true)} className="flex gap-2">
          <Plus className="w-4 h-4" /> Добавить группу
        </Button>
      </div>

      {error && <div className="text-red-500 bg-red-50 p-4 rounded-lg">{error}</div>}
      
      {loading ? (
        <div className="text-muted-foreground animate-pulse">Загрузка...</div>
      ) : groups.length === 0 ? (
        <div className="border border-dashed border-border rounded-xl p-12 text-center text-muted-foreground flex flex-col items-center">
          <Users className="w-12 h-12 mb-4 opacity-50" />
          Пользовательских групп не найдено (пока отображаются только группы с пользователями Samba).
        </div>
      ) : (
        <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
          {groups.map((group) => (
            <Card key={group.name} className="flex flex-col">
              <CardContent className="p-5 flex-1 space-y-4">
                <div className="flex items-center gap-3 border-b border-border pb-3">
                  <div className="p-2 bg-indigo-500/10 rounded-lg">
                    <Users className="w-5 h-5 text-indigo-500" />
                  </div>
                  <div>
                    <h3 className="font-semibold text-lg">@{group.name}</h3>
                    <p className="text-xs text-muted-foreground mt-0.5">Членов: {group.members ? group.members.length : 0}</p>
                  </div>
                </div>

                <div className="space-y-2">
                  <div className="text-xs font-semibold uppercase tracking-wider text-muted-foreground">Состав группы:</div>
                  {!group.members || group.members.length === 0 ? (
                    <div className="text-sm text-muted-foreground italic">Группа пуста</div>
                  ) : (
                    <div className="flex flex-wrap gap-2">
                      {group.members.map(member => (
                        <div key={member} className="flex items-center gap-1.5 bg-muted px-2 py-1 rounded-md text-sm border border-border">
                          <span>{member}</span>
                          <button 
                            title="Удалить из группы" 
                            onClick={() => handleRemoveUser(group.name, member)}
                            className="text-muted-foreground hover:text-red-500 transition-colors"
                          >
                            <UserMinus className="w-3.5 h-3.5" />
                          </button>
                        </div>
                      ))}
                    </div>
                  )}
                </div>
              </CardContent>
              <div className="p-4 border-t border-border flex justify-end gap-2 bg-muted/10 rounded-b-xl">
                <Button onClick={() => openAddUser(group.name)} className="!bg-transparent text-foreground border border-border hover:bg-muted text-xs h-8">
                  <UserPlus className="w-3.5 h-3.5 mr-1.5" /> Добавить юзера
                </Button>
                <Button onClick={() => handleDeleteGroup(group.name)} className="!bg-transparent border border-red-200 text-red-600 hover:bg-red-50 hover:border-red-300 text-xs h-8">
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

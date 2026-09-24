import { useEffect, useState } from "react";
import { Users, Plus, Trash2, UserPlus, UserMinus } from "lucide-react";

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
        // IMPORTANT FIXED DTO MAPPING (groupName instead of name)
        body: JSON.stringify({ groupName: newGroupName })
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
      <div className="space-y-8 animate-in slide-in-from-bottom-4 duration-500 max-w-2xl mx-auto mt-10">
        <div className="flex items-center justify-between">
          <div>
            <h1 className="text-3xl font-extrabold tracking-tight text-slate-900">Новая группа ОС</h1>
            <p className="text-slate-500 text-[15px] mt-2">Добавление группы для системных прав доступа</p>
          </div>
          <button onClick={() => setIsCreating(false)} className="px-5 py-2.5 bg-white border border-slate-200 hover:border-slate-300 hover:bg-slate-50 text-slate-700 font-medium rounded-xl transition-all shadow-sm">
            Отмена
          </button>
        </div>
        <div className="bg-white rounded-[24px] shadow-sm border border-slate-200/60 p-8 sm:p-10">
          <form onSubmit={handleCreateGroup} className="space-y-6">
            <div className="space-y-3">
              <label className="text-[13px] font-bold text-slate-700 uppercase tracking-wide">Название группы</label>
              <input 
                required 
                autoFocus
                pattern="^[a-z_][a-z0-9_-]*[$]?$"
                className="w-full bg-slate-50 border border-slate-200 focus:border-blue-500 focus:ring-4 focus:ring-blue-500/10 hover:border-slate-300 transition-all py-4 px-4 text-base rounded-xl outline-none"
                placeholder="smb_users"
                value={newGroupName} 
                onChange={(e) => setNewGroupName(e.target.value)} 
              />
              <p className="text-xs text-slate-500">Допускается строчная латиница, цифры, _ и - (начинается с буквы или _).</p>
            </div>
            <button type="submit" className="w-full bg-blue-600 hover:bg-blue-700 text-white px-8 py-4 rounded-xl font-bold tracking-wide shadow-lg shadow-blue-500/30 transition-all hover:-translate-y-0.5">
              Создать группу
            </button>
          </form>
        </div>
      </div>
    );
  }

  if (isAddingUser) {
    return (
      <div className="space-y-8 animate-in slide-in-from-bottom-4 duration-500 max-w-2xl mx-auto mt-10">
        <div className="flex items-center justify-between">
          <div>
            <h1 className="text-3xl font-extrabold tracking-tight text-slate-900">Добавление в группу</h1>
            <p className="text-slate-500 text-[15px] mt-2">Включение пользователя в состав {targetGroup}</p>
          </div>
          <button onClick={() => setIsAddingUser(false)} className="px-5 py-2.5 bg-white border border-slate-200 hover:border-slate-300 hover:bg-slate-50 text-slate-700 font-medium rounded-xl transition-all shadow-sm">
            Отмена
          </button>
        </div>
        <div className="bg-white rounded-[24px] shadow-sm border border-slate-200/60 p-8 sm:p-10">
          <form onSubmit={handleAddUser} className="space-y-6">
            <div className="space-y-3">
              <label className="text-[13px] font-bold text-slate-700 uppercase tracking-wide">Имя пользователя (Логин ОС)</label>
              <input 
                required 
                autoFocus
                className="w-full bg-slate-50 border border-slate-200 focus:border-blue-500 focus:ring-4 focus:ring-blue-500/10 hover:border-slate-300 transition-all py-4 px-4 text-base rounded-xl outline-none"
                value={usernameToAdd} 
                onChange={(e) => setUsernameToAdd(e.target.value)} 
                placeholder="john_doe"
              />
            </div>
            <button type="submit" className="w-full bg-slate-900 hover:bg-slate-800 text-white px-8 py-4 rounded-xl font-bold tracking-wide shadow-lg transition-all hover:-translate-y-0.5 mt-2 flex items-center justify-center gap-2">
              <UserPlus className="w-5 h-5" /> Добавить в <b>{targetGroup}</b>
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
            <div className="bg-indigo-100/50 p-2 rounded-2xl ring-1 ring-indigo-500/10">
              <Users className="w-7 h-7 text-indigo-600" />
            </div>
            Группы
          </h1>
          <p className="text-slate-500 text-[15px] mt-2">Управление группами ОС для прав доступа Samba</p>
        </div>
        <button onClick={() => setIsCreating(true)} className="bg-slate-900 hover:bg-slate-800 text-white px-5 py-2.5 rounded-xl font-medium tracking-wide flex items-center gap-2 shadow-lg shadow-slate-900/20 transition-all hover:shadow-xl hover:-translate-y-0.5">
          <Plus className="w-5 h-5" /> Создать группу
        </button>
      </div>

      {error && (
        <div className="bg-rose-50 text-rose-600 p-4 rounded-xl text-[14px] font-medium border border-rose-100/50">
          ⚠️ {error}
        </div>
      )}
      
      {loading ? (
        <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
          {[1,2,3,4].map(i => (
             <div key={i} className="h-44 bg-white/50 rounded-[20px] border border-slate-100 p-6 animate-pulse flex flex-col justify-between">
                <div className="w-1/3 h-6 bg-slate-200/50 rounded-full mb-4"></div>
                <div className="w-full h-12 bg-slate-100 rounded-xl mb-2"></div>
             </div>
          ))}
        </div>
      ) : groups.length === 0 ? (
        <div className="text-center py-24 bg-white rounded-[24px] border border-dashed border-slate-200/60 shadow-sm">
          <Users className="w-16 h-16 text-indigo-100 mx-auto mb-4" />
          <h3 className="text-xl font-bold text-slate-800 tracking-tight">Групп ОС не найдено</h3>
          <p className="text-slate-500 mt-2">Показаны только группы, участвующие в настройках Samba</p>
        </div>
      ) : (
        <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
          {groups.map((group) => (
            <div key={group.name} className="bg-white rounded-[20px] shadow-sm flex flex-col border border-slate-200/60 overflow-hidden group">
              <div className="p-6 flex-1 flex flex-col">
                <div className="flex items-center justify-between mb-4">
                  <div className="flex items-center gap-3">
                    <div className="p-2 bg-indigo-50 rounded-lg">
                      <Users className="w-5 h-5 text-indigo-500" />
                    </div>
                    <div>
                      <h3 className="font-bold text-lg text-slate-900 tracking-tight">@{group.name}</h3>
                      <p className="text-xs font-semibold text-slate-400 mt-0.5">ЧЕРЕЗ SAMBA</p>
                    </div>
                  </div>
                  
                  <button onClick={() => handleDeleteGroup(group.name)} className="p-2 text-slate-400 hover:text-rose-600 hover:bg-rose-50 rounded-lg transition-colors opacity-0 group-hover:opacity-100" title="Удалить группу">
                    <Trash2 className="w-5 h-5" />
                  </button>
                </div>

                <div className="border border-slate-100 rounded-xl bg-slate-50/50 mt-auto overflow-hidden">
                  <div className="bg-slate-100/80 px-4 py-2 flex items-center justify-between border-b border-slate-100">
                    <span className="text-[11px] font-bold uppercase tracking-wider text-slate-500">Участники ({group.members?.length || 0})</span>
                    <button 
                      onClick={() => openAddUser(group.name)}
                      className="text-indigo-600 hover:text-indigo-700 bg-indigo-100/50 hover:bg-indigo-100 p-1 rounded-md transition-colors"
                      title="Добавить пользователя"
                    >
                      <Plus className="w-3.5 h-3.5" />
                    </button>
                  </div>
                  
                  <div className="p-3">
                    {!group.members || group.members.length === 0 ? (
                      <div className="text-[13px] text-slate-400 italic text-center py-2">Группа пуста</div>
                    ) : (
                      <div className="flex flex-wrap gap-2">
                        {group.members.map(member => (
                          <div key={member} className="flex items-center gap-1.5 bg-white px-2 py-1.5 rounded-lg text-[13px] font-medium border border-slate-200 text-slate-700 shadow-sm">
                            <span className="pb-[1px]">{member}</span>
                            <button 
                              title="Исключить" 
                              onClick={() => handleRemoveUser(group.name, member)}
                              className="text-slate-400 hover:text-rose-500 ml-1 bg-slate-50 hover:bg-rose-50 rounded p-0.5 transition-colors"
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
    </div>
  );
}

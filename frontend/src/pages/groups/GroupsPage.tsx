import { useEffect, useState } from "react";
import { Users, Plus, Trash2, UserPlus, UserMinus } from "lucide-react";
import { api } from "../../lib/api";
import { useToast } from "../../components/ui/toast";
import { useConfirm } from "../../components/ui/confirm";
import { Modal } from "../../components/ui/modal";

interface Group {
  name: string;
  members: string[];
}

export default function GroupsPage() {
  const [groups, setGroups] = useState<Group[]>([]);
  const [loading, setLoading] = useState(true);

  const [isCreating, setIsCreating] = useState(false);
  const [newGroupName, setNewGroupName] = useState("");

  const [isAddingUser, setIsAddingUser] = useState(false);
  const [targetGroup, setTargetGroup] = useState("");
  const [usernameToAdd, setUsernameToAdd] = useState("");

  const { toast, error: toastError, success } = useToast();
  const { confirm } = useConfirm();

  const fetchGroups = async () => {
    try {
      setLoading(true);
      const res = await api.get<Group[]>("/api/groups");
      setGroups(res);
    } catch (err: any) {
      toastError("Ошибка API", err.message);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchGroups();
  }, []);

  const handleDeleteGroup = async (groupName: string) => {
    const ok = await confirm({
      title: "Удаление группы",
      message: `Вы действительно хотите удалить группу ${groupName}?`,
      destructive: true,
      confirmText: "Да, удалить"
    });
    if (!ok) return;

    try {
      await api.delete(`/api/groups/${groupName}`);
      success("Удалено", `Группа ${groupName} успешно удалена`);
      fetchGroups();
    } catch (err: any) {
      toastError("Ошибка удаления", err.message);
    }
  };

  const handleCreateGroup = async (e: React.FormEvent) => {
    e.preventDefault();
    try {
      await api.post("/api/groups", { groupName: newGroupName });
      success("Создано", `Группа ${newGroupName} успешно создана`);
      setIsCreating(false);
      setNewGroupName("");
      fetchGroups();
    } catch (err: any) {
      toastError("Ошибка создания", err.message);
    }
  };

  const handleAddUser = async (e: React.FormEvent) => {
    e.preventDefault();
    try {
      await api.post(`/api/groups/${targetGroup}/users`, { username: usernameToAdd });
      success("Добавлено", `Пользователь ${usernameToAdd} добавлен в группу ${targetGroup}`);
      setIsAddingUser(false);
      setUsernameToAdd("");
      fetchGroups();
    } catch (err: any) {
      toastError("Ошибка добавления", err.message);
    }
  };

  const handleRemoveUser = async (groupName: string, username: string) => {
    const ok = await confirm({
      title: "Исключение пользователя",
      message: `Удалить пользователя ${username} из группы ${groupName}?`,
      destructive: true,
      confirmText: "Исключить"
    });
    if (!ok) return;

    try {
      await api.delete(`/api/groups/${groupName}/users/${username}`);
      success("Исключено", `Пользователь ${username} удален из группы ${groupName}`);
      fetchGroups();
    } catch (err: any) {
      toastError("Ошибка удаления", err.message);
    }
  };

  const openAddUser = (groupName: string) => {
    setTargetGroup(groupName);
    setUsernameToAdd("");
    setIsAddingUser(true);
  };

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
            <div key={group.name} className="bg-white rounded-[20px] shadow-sm flex flex-col border border-slate-200/60 overflow-hidden group hover:shadow-md transition-all">
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
                            <span className="pb-[1px] font-mono font-[500] text-blue-900">{member}</span>
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

      <Modal 
        isOpen={isCreating} 
        onClose={() => setIsCreating(false)}
        title="Новая группа ОС"
        description="Добавление группы для системных прав доступа"
        maxWidth="sm"
      >
        <form onSubmit={handleCreateGroup} className="space-y-4">
          <div className="space-y-2">
            <label className="text-[13px] font-bold text-slate-700 uppercase tracking-wide">Название группы</label>
            <input 
              required 
              autoFocus
              pattern="^[a-z_][a-z0-9_-]*[$]?$"
              className="w-full bg-slate-50 border border-slate-200 focus:border-indigo-500 focus:ring-4 focus:ring-indigo-500/10 hover:border-slate-300 transition-all py-3 px-4 text-base rounded-xl outline-none"
              placeholder="smb_users"
              value={newGroupName} 
              onChange={(e) => setNewGroupName(e.target.value)} 
            />
            <p className="text-xs text-slate-500">Допускается строчная латиница, цифры, _ и - (начинается с буквы или _).</p>
          </div>
          <div className="pt-4 flex justify-end gap-3">
             <button type="button" onClick={() => setIsCreating(false)} className="px-5 py-2.5 bg-white border border-slate-200 hover:bg-slate-50 rounded-xl font-medium text-slate-700 transition-colors">
               Отмена
             </button>
            <button type="submit" className="bg-indigo-600 hover:bg-indigo-700 text-white px-6 py-2.5 rounded-xl font-bold tracking-wide shadow-lg shadow-indigo-500/30 transition-all">
              Создать
            </button>
          </div>
        </form>
      </Modal>

      <Modal 
        isOpen={isAddingUser} 
        onClose={() => setIsAddingUser(false)}
        title="Добавление в группу"
        description={`Включение пользователя в состав ${targetGroup}`}
        maxWidth="sm"
      >
        <form onSubmit={handleAddUser} className="space-y-4">
          <div className="space-y-2">
            <label className="text-[13px] font-bold text-slate-700 uppercase tracking-wide">Имя пользователя (Логин ОС)</label>
            <input 
              required 
              autoFocus
              className="w-full bg-slate-50 border border-slate-200 focus:border-indigo-500 focus:ring-4 focus:ring-indigo-500/10 hover:border-slate-300 transition-all py-3 px-4 text-base rounded-xl outline-none"
              value={usernameToAdd} 
              onChange={(e) => setUsernameToAdd(e.target.value)} 
              placeholder="john_doe"
            />
          </div>
          <div className="pt-4 flex justify-end gap-3">
             <button type="button" onClick={() => setIsAddingUser(false)} className="px-5 py-2.5 bg-white border border-slate-200 hover:bg-slate-50 rounded-xl font-medium text-slate-700 transition-colors">
               Отмена
             </button>
            <button type="submit" className="bg-indigo-600 hover:bg-indigo-700 text-white px-6 py-2.5 rounded-xl font-bold tracking-wide shadow-lg shadow-indigo-500/30 transition-all flex items-center gap-2">
              <UserPlus className="w-4 h-4" /> Добавить
            </button>
          </div>
        </form>
      </Modal>
    </div>
  );
}

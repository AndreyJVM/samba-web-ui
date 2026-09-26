import { useEffect, useState } from "react";
import { FolderKanban, Plus, Trash2, Settings2, ShieldCheck, Users, ChevronDown, ChevronUp } from "lucide-react";
import { api } from "../../lib/api";
import { useToast } from "../../components/ui/toast";
import { useConfirm } from "../../components/ui/confirm";
import { Modal } from "../../components/ui/modal";

interface Share {
  name: string;
  path: string;
  comment?: string;
  readOnly: boolean;
  guestOk: boolean;
  browseable: boolean;
  validUsers?: string;
  writeList?: string;
  createMask?: string;
  directoryMask?: string;
  forceUser?: string;
  forceGroup?: string;
  maxConnections?: string;
  hostsAllow?: string;
  hostsDeny?: string;
  isNew?: boolean;
}

interface User { username: string; }
interface Group { name: string; }

export default function SharesPage() {
  const [shares, setShares] = useState<Share[]>([]);
  const [users, setUsers] = useState<User[]>([]);
  const [groups, setGroups] = useState<Group[]>([]);
  
  const [loading, setLoading] = useState(true);

  const [isEditing, setIsEditing] = useState(false);
  const [currentShare, setCurrentShare] = useState<Partial<Share> | null>(null);
  const [showAdvanced, setShowAdvanced] = useState(false);
  
  const { toast, error: toastError, success } = useToast();
  const { confirm } = useConfirm();

  const fetchAll = async () => {
    try {
      setLoading(true);
      const [shRes, usRes, grRes] = await Promise.all([
        api.get<Share[]>("/api/shares").catch(() => []),
        api.get<User[]>("/api/users").catch(() => []),
        api.get<Group[]>("/api/groups").catch(() => [])
      ]);
      setShares(shRes || []);
      setUsers(usRes || []);
      setGroups(grRes || []);
    } catch (err: any) {
      toastError("Ошибка API", err.message);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchAll();
  }, []);

  const handleDelete = async (name: string) => {
    const ok = await confirm({
      title: "Удаление",
      message: `Точно удалить общую папку [${name}]?`,
      destructive: true,
      confirmText: "Удалить"
    });
    if (!ok) return;

    try {
      await api.delete(`/api/shares/${name}`);
      success("Удалено", `Папка ${name} успешно удалена`);
      fetchAll();
    } catch (err: any) {
      toastError("Ошибка удаления", err.message);
    }
  };

  const handleSave = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!currentShare?.name || !currentShare?.path) return;

    const isNew = currentShare.isNew === true;
    const url = isNew ? "/api/shares" : `/api/shares/${currentShare.name}`;

    try {
      const dto = {
        name: currentShare.name,
        path: currentShare.path,
        comment: currentShare.comment || "",
        readOnly: currentShare.readOnly,
        browseable: currentShare.browseable,
        guestOk: currentShare.guestOk,
        validUsers: currentShare.validUsers || "",
        writeList: currentShare.writeList || "",
        createMask: currentShare.createMask || "",
        directoryMask: currentShare.directoryMask || "",
        forceUser: currentShare.forceUser || "",
        forceGroup: currentShare.forceGroup || "",
        maxConnections: currentShare.maxConnections || "",
        hostsAllow: currentShare.hostsAllow || "",
        hostsDeny: currentShare.hostsDeny || ""
      };

      if (isNew) {
        await api.post(url, dto);
      } else {
        await api.put(url, dto);
      }
      
      success(isNew ? "Создано" : "Обновлено", "Конфигурация успешно сохранена");
      setIsEditing(false);
      fetchAll();
    } catch (err: any) {
      toastError("Ошибка сохранения", err.message);
    }
  };

  const openEditor = (share?: Share) => {
    if (share) {
      setCurrentShare({ ...share, isNew: false });
    } else {
      setCurrentShare({
        name: "",
        path: "",
        browseable: true,
        readOnly: true,
        guestOk: false,
        isNew: true
      });
    }
    setShowAdvanced(false);
    setIsEditing(true);
  };

  const toggleToValidUsers = (token: string) => {
    if (!currentShare) return;
    let current = (currentShare.validUsers || "").split(",").map(s => s.trim()).filter(Boolean);
    if (current.includes(token)) {
      current = current.filter(s => s !== token);
    } else {
      current.push(token);
    }
    setCurrentShare({ ...currentShare, validUsers: current.join(", ") });
  };

  const activeTokens = (currentShare?.validUsers || "").split(",").map(t => t.trim());

  return (
    <div className="space-y-8 animate-in fade-in duration-500 pb-10">
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4">
        <div>
          <h1 className="text-3xl font-extrabold tracking-tight text-slate-900 flex items-center gap-3">
            <div className="bg-blue-100/50 p-2 rounded-2xl ring-1 ring-blue-500/10">
              <FolderKanban className="w-7 h-7 text-blue-600" />
            </div>
            Сетевые ресурсы
          </h1>
          <p className="text-slate-500 text-[15px] mt-2">Папки доступные клиентам Samba из сети</p>
        </div>
        
        <div className="flex flex-wrap items-center gap-3">
          <button onClick={() => openEditor()} className="bg-slate-900 hover:bg-slate-800 text-white px-5 py-2.5 rounded-xl font-medium tracking-wide flex items-center gap-2 shadow-lg shadow-slate-900/20 transition-all hover:shadow-xl hover:-translate-y-0.5">
            <Plus className="w-5 h-5" /> Создать шару
          </button>
        </div>
      </div>

      {loading ? (
        <div className="space-y-4">
          {[1,2,3,4].map(i => (
             <div key={i} className="h-16 bg-white/50 rounded-[20px] border border-slate-100 p-4 animate-pulse flex items-center justify-between">
                <div className="w-1/4 h-5 bg-slate-200/50 rounded-full"></div>
                <div className="w-1/4 h-3 bg-slate-100 rounded-full"></div>
             </div>
          ))}
        </div>
      ) : shares.filter(s => s.name !== 'global').length === 0 ? (
        <div className="text-center py-24 bg-white rounded-[24px] border border-dashed border-slate-200/60 shadow-sm relative overflow-hidden">
          <FolderKanban className="w-16 h-16 text-blue-100 mx-auto mb-4" />
          <h3 className="text-xl font-bold text-slate-800 tracking-tight">Нет доступных директорий</h3>
          <p className="text-slate-500 mt-2 mb-6">Создайте первую папку для доступа по локальной сети.</p>
          <button onClick={() => openEditor()} className="bg-blue-50 text-blue-600 hover:bg-blue-100 px-6 py-2.5 rounded-xl font-semibold transition-colors">
            Новая папка
          </button>
        </div>
      ) : (
        <div className="bg-white rounded-[24px] border border-slate-200/60 shadow-sm overflow-hidden overflow-x-auto">
          <table className="w-full text-left font-medium min-w-[800px]">
             <thead className="bg-slate-50/70 text-slate-500 text-[12px] uppercase tracking-wider border-b border-slate-200/60">
              <tr>
                <th className="py-4 px-6 font-bold w-[20%]">Имя шары</th>
                <th className="py-4 px-5 font-bold w-[25%]">Путь на диске</th>
                <th className="py-4 px-5 font-bold w-[20%]">Описание</th>
                <th className="py-4 px-5 font-bold w-[20%]">Доступ</th>
                <th className="py-4 px-6 font-bold text-right w-[15%]">Действия</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-slate-100">
              {shares.filter(s => s.name !== 'global').map((share) => (
                <tr key={share.name} className="hover:bg-slate-50/70 transition-colors group">
                  <td className="py-4 px-6">
                    <div className="flex items-center gap-3">
                       <div className="bg-blue-50 rounded-lg p-2 group-hover:bg-blue-100 transition-colors">
                         <FolderKanban className="w-4 h-4 text-blue-600" />
                       </div>
                       <span className="text-slate-800 font-bold text-[15px]">{share.name}</span>
                    </div>
                  </td>
                  <td className="py-4 px-5">
                    <span className="text-[13px] font-mono text-slate-500 bg-slate-100/50 border border-slate-200/60 px-2 py-1 rounded truncate block max-w-[200px]" title={share.path}>
                      {share.path || '/'}
                    </span>
                  </td>
                  <td className="py-4 px-5">
                    <span className="text-[13px] text-slate-500 truncate block max-w-[150px]" title={share.comment}>
                      {share.comment || <span className="text-slate-400 italic">—</span>}
                    </span>
                  </td>
                  <td className="py-4 px-5">
                    <div className="flex flex-wrap gap-1.5">
                      {share.readOnly && <span className="bg-rose-50 text-rose-600 text-[10px] font-extrabold px-1.5 py-0.5 rounded uppercase tracking-widest ring-1 ring-inset ring-rose-500/20">Только чтение</span>}
                      {share.guestOk && <span className="bg-emerald-50 text-emerald-600 text-[10px] font-extrabold px-1.5 py-0.5 rounded uppercase tracking-widest ring-1 ring-inset ring-emerald-500/20">Гость</span>}
                      {!share.readOnly && !share.guestOk && <span className="text-slate-400 text-[11px] italic">Приватная</span>}
                    </div>
                  </td>
                  <td className="py-4 px-6 text-right">
                    <div className="flex items-center justify-end gap-1.5 opacity-60 group-hover:opacity-100 transition-opacity">
                      <button onClick={() => openEditor(share)} className="p-2.5 text-slate-600 hover:text-blue-600 hover:bg-blue-50 rounded-xl transition-all shadow-sm border border-transparent hover:border-blue-100 bg-white" title="Настроить">
                        <Settings2 className="w-4 h-4" />
                      </button>
                      <button onClick={() => handleDelete(share.name)} className="p-2.5 text-slate-600 hover:text-rose-600 hover:bg-rose-50 rounded-xl transition-all shadow-sm border border-transparent hover:border-rose-100 bg-white" title="Удалить">
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

      {currentShare && (
        <Modal 
          isOpen={isEditing} 
          onClose={() => setIsEditing(false)}
          title={currentShare.isNew ? "Новая общая папка" : `Настройка: ${currentShare.name}`}
          description="Глобальные и дополнительные настройки SMB директории"
          maxWidth="4xl"
        >
          <form onSubmit={handleSave} className="space-y-6">
            <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
              <div className="space-y-2">
                <label className="text-[13px] font-bold text-slate-700 uppercase tracking-wide">Имя папки (в сети)</label>
                <input 
                  required 
                  value={currentShare.name} 
                  disabled={!currentShare.isNew}
                  onChange={(e) => setCurrentShare({ ...currentShare, name: e.target.value })} 
                  className={`w-full bg-slate-50 border border-slate-200 focus:border-blue-500 focus:ring-4 focus:ring-blue-500/10 hover:border-slate-300 transition-all py-3 px-4 text-sm rounded-xl outline-none ${!currentShare.isNew ? 'opacity-60 cursor-not-allowed' : ''}`}
                />
              </div>
              <div className="space-y-2">
                <label className="text-[13px] font-bold text-slate-700 uppercase tracking-wide">Путь сервера</label>
                <input 
                  required 
                  value={currentShare.path || ""} 
                  onChange={(e) => setCurrentShare({ ...currentShare, path: e.target.value })} 
                  className="w-full bg-slate-50 border border-slate-200 focus:border-blue-500 focus:ring-4 focus:ring-blue-500/10 hover:border-slate-300 transition-all py-3 px-4 text-sm rounded-xl outline-none font-mono"
                  placeholder="/mnt/disk1/data"
                />
              </div>
            </div>

            <div className="space-y-2">
              <label className="text-[13px] font-bold text-slate-700 uppercase tracking-wide">Комментарий</label>
              <input 
                value={currentShare.comment || ""} 
                onChange={(e) => setCurrentShare({ ...currentShare, comment: e.target.value })} 
                className="w-full bg-slate-50 border border-slate-200 focus:border-blue-500 focus:ring-4 focus:ring-blue-500/10 hover:border-slate-300 transition-all py-3 px-4 text-sm rounded-xl outline-none"
                placeholder="Назначение директории"
              />
            </div>

            <div className="grid grid-cols-1 sm:grid-cols-3 gap-4 pt-4 border-t border-slate-100">
              <div className="flex items-center gap-3 bg-slate-50 p-4 rounded-xl border border-slate-100/50">
                <input type="checkbox" id="browseable" 
                   checked={currentShare.browseable} 
                   onChange={(e) => setCurrentShare({ ...currentShare, browseable: e.target.checked })} 
                   className="w-4 h-4 rounded border-slate-300 text-blue-600 focus:ring-blue-500"
                />
                <label htmlFor="browseable" className="text-sm font-semibold text-slate-700 cursor-pointer">Отображать в сети</label>
              </div>
              <div className="flex items-center gap-3 bg-slate-50 p-4 rounded-xl border border-slate-100/50">
                <input type="checkbox" id="ro" 
                   checked={currentShare.readOnly} 
                   onChange={(e) => setCurrentShare({ ...currentShare, readOnly: e.target.checked })} 
                   className="w-4 h-4 rounded border-slate-300 text-blue-600 focus:ring-blue-500"
                />
                <label htmlFor="ro" className="text-sm font-semibold text-slate-700 cursor-pointer">Только чтение</label>
              </div>
              <div className="flex items-center gap-3 bg-slate-50 p-4 rounded-xl border border-slate-100/50">
                <input type="checkbox" id="guest" 
                   checked={currentShare.guestOk} 
                   onChange={(e) => setCurrentShare({ ...currentShare, guestOk: e.target.checked })} 
                   className="w-4 h-4 rounded border-slate-300 text-blue-600 focus:ring-blue-500"
                />
                <label htmlFor="guest" className="text-sm font-semibold text-slate-700 cursor-pointer">Гостевой доступ</label>
              </div>
            </div>

            <div className="space-y-4 pt-4 border-t border-slate-100">
              <div className="flex flex-col gap-1">
                <label className="text-[13px] font-bold text-slate-700 uppercase tracking-wide">Доступ (valid users)</label>
                <p className="text-[12px] text-slate-500">Кому разрешен вход. Группы начинаются с '@' (пусто = всем).</p>
              </div>
              
              <input 
                value={currentShare.validUsers || ""} 
                onChange={(e) => setCurrentShare({ ...currentShare, validUsers: e.target.value })} 
                className="w-full bg-slate-50 border border-slate-200 focus:border-blue-500 focus:ring-4 focus:ring-blue-500/10 hover:border-slate-300 transition-all py-3 px-4 text-sm font-mono rounded-xl outline-none"
                placeholder="user1, user2, @admins"
              />

              <div className="bg-slate-50 p-4 rounded-xl border border-slate-100 space-y-4">
                {groups.length > 0 && (
                  <div>
                    <div className="text-[11px] font-semibold text-slate-400 uppercase tracking-wider mb-2 flex items-center gap-1.5">
                      <Users className="w-3.5 h-3.5" /> Внешние группы ОС:
                    </div>
                    <div className="flex flex-wrap gap-2">
                       {groups.map(g => {
                         const tk = `@${g.name}`;
                         const active = activeTokens.includes(tk);
                         return (
                           <button 
                              key={g.name} type="button"
                              onClick={() => toggleToValidUsers(tk)}
                              className={`px-3 py-1.5 rounded-lg text-xs font-medium transition-all ${active ? 'bg-indigo-500 text-white shadow-sm' : 'bg-white border border-slate-200 text-slate-600 hover:border-indigo-300 hover:text-indigo-600'}`}
                           >
                             {tk}
                           </button>
                         )
                       })}
                    </div>
                  </div>
                )}
                {users.length > 0 && (
                  <div>
                    <div className="text-[11px] font-semibold text-slate-400 uppercase tracking-wider mb-2 flex items-center gap-1.5">
                      <ShieldCheck className="w-3.5 h-3.5" /> Локальные пользователи:
                    </div>
                    <div className="flex flex-wrap gap-2">
                       {users.map(u => {
                         const tk = u.username;
                         const active = activeTokens.includes(tk);
                         return (
                           <button 
                              key={u.username} type="button"
                              onClick={() => toggleToValidUsers(tk)}
                              className={`px-3 py-1.5 rounded-lg text-xs font-medium transition-all ${active ? 'bg-blue-500 text-white shadow-sm' : 'bg-white border border-slate-200 text-slate-600 hover:border-blue-300 hover:text-blue-600'}`}
                           >
                             {tk}
                           </button>
                         )
                       })}
                    </div>
                  </div>
                )}
              </div>
            </div>

            <div className="pt-4 border-t border-slate-100">
              <button 
                type="button"
                onClick={() => setShowAdvanced(!showAdvanced)}
                className="w-full flex items-center justify-between bg-slate-50 hover:bg-slate-100 transition-colors p-4 rounded-xl border border-slate-200 text-slate-700 font-bold text-sm"
              >
                <div className="flex items-center gap-2">
                  <Settings2 className="w-4 h-4 text-slate-500" />
                  Продвинутые опции безопасности и масок
                </div>
                {showAdvanced ? <ChevronUp className="w-5 h-5" /> : <ChevronDown className="w-5 h-5" />}
              </button>
              
              {showAdvanced && (
                 <div className="grid grid-cols-1 md:grid-cols-2 gap-4 mt-4 p-4 bg-slate-50 rounded-xl border border-slate-100 animate-in slide-in-from-top-2 duration-300">
                    {/* Write List */}
                    <div className="space-y-1">
                      <label className="text-[11px] font-bold text-slate-600 uppercase tracking-wide">Write List</label>
                      <input 
                        value={currentShare.writeList || ""} 
                        onChange={(e) => setCurrentShare({ ...currentShare, writeList: e.target.value })} 
                        className="w-full bg-white border border-slate-200 py-2 px-3 text-xs font-mono rounded-lg outline-none focus:border-blue-500 focus:ring-2 focus:ring-blue-500/10"
                        placeholder="@admins, user1"
                      />
                    </div>
                    {/* Max Connections */}
                    <div className="space-y-1">
                      <label className="text-[11px] font-bold text-slate-600 uppercase tracking-wide">Max Connections</label>
                      <input 
                        value={currentShare.maxConnections || ""} 
                        onChange={(e) => setCurrentShare({ ...currentShare, maxConnections: e.target.value })} 
                        className="w-full bg-white border border-slate-200 py-2 px-3 text-xs font-mono rounded-lg outline-none focus:border-blue-500 focus:ring-2 focus:ring-blue-500/10"
                        placeholder="0 (без лимита)"
                      />
                    </div>
                    {/* Create Mask */}
                    <div className="space-y-1">
                      <label className="text-[11px] font-bold text-slate-600 uppercase tracking-wide">Create Mask</label>
                      <input 
                        value={currentShare.createMask || ""} 
                        onChange={(e) => setCurrentShare({ ...currentShare, createMask: e.target.value })} 
                        className="w-full bg-white border border-slate-200 py-2 px-3 text-xs font-mono rounded-lg outline-none focus:border-blue-500 focus:ring-2 focus:ring-blue-500/10"
                        placeholder="0644"
                      />
                    </div>
                    {/* Directory Mask */}
                    <div className="space-y-1">
                      <label className="text-[11px] font-bold text-slate-600 uppercase tracking-wide">Directory Mask</label>
                      <input 
                        value={currentShare.directoryMask || ""} 
                        onChange={(e) => setCurrentShare({ ...currentShare, directoryMask: e.target.value })} 
                        className="w-full bg-white border border-slate-200 py-2 px-3 text-xs font-mono rounded-lg outline-none focus:border-blue-500 focus:ring-2 focus:ring-blue-500/10"
                        placeholder="0755"
                      />
                    </div>
                    {/* Force User */}
                    <div className="space-y-1">
                      <label className="text-[11px] font-bold text-slate-600 uppercase tracking-wide">Force User</label>
                      <input 
                        value={currentShare.forceUser || ""} 
                        onChange={(e) => setCurrentShare({ ...currentShare, forceUser: e.target.value })} 
                        className="w-full bg-white border border-slate-200 py-2 px-3 text-xs font-mono rounded-lg outline-none focus:border-blue-500 focus:ring-2 focus:ring-blue-500/10"
                      />
                    </div>
                    {/* Force Group */}
                    <div className="space-y-1">
                      <label className="text-[11px] font-bold text-slate-600 uppercase tracking-wide">Force Group</label>
                      <input 
                        value={currentShare.forceGroup || ""} 
                        onChange={(e) => setCurrentShare({ ...currentShare, forceGroup: e.target.value })} 
                        className="w-full bg-white border border-slate-200 py-2 px-3 text-xs font-mono rounded-lg outline-none focus:border-blue-500 focus:ring-2 focus:ring-blue-500/10"
                      />
                    </div>
                    {/* Hosts Allow */}
                    <div className="space-y-1">
                      <label className="text-[11px] font-bold text-slate-600 uppercase tracking-wide">Hosts Allow</label>
                      <input 
                        value={currentShare.hostsAllow || ""} 
                        onChange={(e) => setCurrentShare({ ...currentShare, hostsAllow: e.target.value })} 
                        className="w-full bg-white border border-slate-200 py-2 px-3 text-xs font-mono rounded-lg outline-none focus:border-blue-500 focus:ring-2 focus:ring-blue-500/10"
                        placeholder="192.168.1. 127."
                      />
                    </div>
                    {/* Hosts Deny */}
                    <div className="space-y-1">
                      <label className="text-[11px] font-bold text-slate-600 uppercase tracking-wide">Hosts Deny</label>
                      <input 
                        value={currentShare.hostsDeny || ""} 
                        onChange={(e) => setCurrentShare({ ...currentShare, hostsDeny: e.target.value })} 
                        className="w-full bg-white border border-slate-200 py-2 px-3 text-xs font-mono rounded-lg outline-none focus:border-blue-500 focus:ring-2 focus:ring-blue-500/10"
                        placeholder="ALL"
                      />
                    </div>
                 </div>
              )}
            </div>

            <div className="pt-6 border-t border-slate-100 flex justify-end gap-3">
               <button type="button" onClick={() => setIsEditing(false)} className="px-6 py-2.5 bg-white border border-slate-200 hover:bg-slate-50 rounded-xl font-medium text-slate-700 transition-colors">
                 Отмена
               </button>
               <button type="submit" className="bg-blue-600 hover:bg-blue-700 text-white px-8 py-2.5 rounded-xl font-bold tracking-wide shadow-lg shadow-blue-500/30 transition-all">
                 Сохранить
               </button>
            </div>
          </form>
        </Modal>
      )}
    </div>
  );
}

import { useEffect, useState } from "react";
import { FolderKanban, Plus, Trash2, Settings2, ShieldCheck, Users } from "lucide-react";

interface Share {
  name: string;
  path: string;
  comment?: string;
  readOnly: boolean;
  guestOk: boolean;
  browseable: boolean;
  validUsers?: string;
  writeList?: string;
  isNew?: boolean;
}

interface User {
  username: string;
}
interface Group {
  name: string;
}

export default function SharesPage() {
  const [shares, setShares] = useState<Share[]>([]);
  const [users, setUsers] = useState<User[]>([]);
  const [groups, setGroups] = useState<Group[]>([]);
  
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");

  const [isEditing, setIsEditing] = useState(false);
  const [currentShare, setCurrentShare] = useState<Partial<Share> | null>(null);

  const fetchAll = async () => {
    try {
      setLoading(true);
      const [shRes, usRes, grRes] = await Promise.all([
        fetch("/api/shares"),
        fetch("/api/users"),
        fetch("/api/groups")
      ]);
      const shJson = await shRes.json();
      const usJson = await usRes.json();
      const grJson = await grRes.json();

      if (shJson.success) setShares(shJson.data);
      if (usJson.success) setUsers(usJson.data);
      if (grJson.success) setGroups(grJson.data);
      
      if (!shJson.success) setError(shJson.message || "Ошибка загрузки списка папок");
    } catch (err) {
      setError("Ошибка API");
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchAll();
  }, []);

  const handleDelete = async (name: string) => {
    if (!confirm(`Удалить общую папку [${name}]?`)) return;
    try {
      const res = await fetch(`/api/shares/${name}`, { method: "DELETE" });
      if (res.ok) fetchAll();
    } catch (err) {
      alert("Ошибка удаления");
    }
  };

  const handleSave = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!currentShare?.name || !currentShare?.path) return;

    const isNew = currentShare.isNew === true;
    const method = isNew ? "POST" : "PUT";
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
        writeList: currentShare.writeList || ""
      };

      const res = await fetch(url, {
        method,
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify(dto)
      });
      if (res.ok) {
        setIsEditing(false);
        fetchAll();
      } else {
        const json = await res.json();
        alert(json.message || "Ошибка сохранения");
      }
    } catch (err) {
      alert("Ошибка сети");
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

  if (isEditing && currentShare) {
    const activeTokens = (currentShare.validUsers || "").split(",").map(t => t.trim());

    return (
      <div className="space-y-8 animate-in slide-in-from-bottom-4 duration-500 max-w-4xl mx-auto pb-10">
        <div className="flex items-center justify-between">
          <div>
             <h1 className="text-3xl font-extrabold tracking-tight text-slate-900">
              {currentShare.isNew ? "Новая папка" : `Настройка: ${currentShare.name}`}
            </h1>
            <p className="text-slate-500 text-[15px] mt-2">Параметры сетевого доступа и директории</p>
          </div>
          <button onClick={() => setIsEditing(false)} className="px-5 py-2.5 bg-white border border-slate-200 hover:border-slate-300 hover:bg-slate-50 text-slate-700 font-medium rounded-xl transition-all shadow-sm">
            Отмена
          </button>
        </div>

        <div className="bg-white rounded-[24px] shadow-sm border border-slate-200/60 p-8 sm:p-10">
          <form onSubmit={handleSave} className="space-y-8">
            <div className="grid grid-cols-1 md:grid-cols-2 gap-8">
              <div className="space-y-3">
                <label className="text-[13px] font-bold text-slate-700 uppercase tracking-wide">Имя шары (Сетевой путь)</label>
                <input 
                  required 
                  value={currentShare.name} 
                  disabled={!currentShare.isNew}
                  onChange={(e) => setCurrentShare({ ...currentShare, name: e.target.value })} 
                  className={`w-full bg-slate-50 border border-slate-200 focus:border-blue-500 focus:ring-4 focus:ring-blue-500/10 hover:border-slate-300 transition-all py-4 px-4 text-base rounded-xl outline-none ${!currentShare.isNew ? 'opacity-60 cursor-not-allowed' : ''}`}
                />
              </div>
              <div className="space-y-3">
                <label className="text-[13px] font-bold text-slate-700 uppercase tracking-wide">Абсолютный путь на сервере</label>
                <input 
                  required 
                  value={currentShare.path || ""} 
                  onChange={(e) => setCurrentShare({ ...currentShare, path: e.target.value })} 
                  className="w-full bg-slate-50 border border-slate-200 focus:border-blue-500 focus:ring-4 focus:ring-blue-500/10 hover:border-slate-300 transition-all py-4 px-4 text-base rounded-xl outline-none font-mono text-sm"
                  placeholder="/mnt/disk1/data"
                />
              </div>
            </div>

            <div className="space-y-3 whitespace-nowrap overflow-hidden">
              <label className="text-[13px] font-bold text-slate-700 uppercase tracking-wide">Комментарий</label>
              <input 
                value={currentShare.comment || ""} 
                onChange={(e) => setCurrentShare({ ...currentShare, comment: e.target.value })} 
                className="w-full bg-slate-50 border border-slate-200 focus:border-blue-500 focus:ring-4 focus:ring-blue-500/10 hover:border-slate-300 transition-all py-4 px-4 text-base rounded-xl outline-none"
                placeholder="Файлы общего доступа для всех"
              />
            </div>

            <div className="grid grid-cols-1 md:grid-cols-3 gap-4 pt-4 border-t border-slate-100">
              <div className="flex items-center gap-3 bg-slate-50 p-4 rounded-xl border border-slate-100/50">
                <input type="checkbox" id="browseable" 
                   checked={currentShare.browseable} 
                   onChange={(e) => setCurrentShare({ ...currentShare, browseable: e.target.checked })} 
                   className="w-5 h-5 rounded border-slate-300 text-blue-600 focus:ring-blue-500"
                />
                <label htmlFor="browseable" className="text-[14px] font-semibold text-slate-700 cursor-pointer">Видима в сети</label>
              </div>
              <div className="flex items-center gap-3 bg-slate-50 p-4 rounded-xl border border-slate-100/50">
                <input type="checkbox" id="ro" 
                   checked={currentShare.readOnly} 
                   onChange={(e) => setCurrentShare({ ...currentShare, readOnly: e.target.checked })} 
                   className="w-5 h-5 rounded border-slate-300 text-blue-600 focus:ring-blue-500"
                />
                <label htmlFor="ro" className="text-[14px] font-semibold text-slate-700 cursor-pointer">Только чтение</label>
              </div>
              <div className="flex items-center gap-3 bg-slate-50 p-4 rounded-xl border border-slate-100/50">
                <input type="checkbox" id="guest" 
                   checked={currentShare.guestOk} 
                   onChange={(e) => setCurrentShare({ ...currentShare, guestOk: e.target.checked })} 
                   className="w-5 h-5 rounded border-slate-300 text-blue-600 focus:ring-blue-500"
                />
                <label htmlFor="guest" className="text-[14px] font-semibold text-slate-700 cursor-pointer">Гостевой доступ</label>
              </div>
            </div>

            <div className="space-y-4 pt-4 border-t border-slate-100 mb-6">
              <div className="flex flex-col gap-1.5">
                <label className="text-[13px] font-bold text-slate-700 uppercase tracking-wide">Ограничение доступа (valid users)</label>
                <p className="text-[13px] text-slate-500">Если поле пустое, доступ открыт для всех (зависит от Guest Ok).</p>
              </div>
              
              <input 
                value={currentShare.validUsers || ""} 
                onChange={(e) => setCurrentShare({ ...currentShare, validUsers: e.target.value })} 
                 className="w-full bg-slate-50 border border-slate-200 focus:border-blue-500 focus:ring-4 focus:ring-blue-500/10 hover:border-slate-300 transition-all py-3 px-4 text-sm font-mono rounded-xl outline-none"
                placeholder="user1, user2, @groupname"
              />

              {/* Quick selector blocks */}
              <div className="bg-slate-50 p-4 rounded-xl border border-slate-100 space-y-4">
                {groups.length > 0 && (
                  <div>
                    <div className="text-[11px] font-semibold text-slate-400 uppercase tracking-wider mb-2 flex items-center gap-1.5">
                      <Users className="w-3.5 h-3.5" /> ДОБАВИТЬ ГРУППУ:
                    </div>
                    <div className="flex flex-wrap gap-2">
                       {groups.map(g => {
                         const tk = `@${g.name}`;
                         const active = activeTokens.includes(tk);
                         return (
                           <button 
                              key={g.name} type="button"
                              onClick={() => toggleToValidUsers(tk)}
                              className={`px-3 py-1.5 rounded-lg text-sm font-medium transition-all ${active ? 'bg-indigo-500 text-white shadow-sm' : 'bg-white border border-slate-200 text-slate-600 hover:border-indigo-300 hover:text-indigo-600'}`}
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
                      <ShieldCheck className="w-3.5 h-3.5" /> ДОБАВИТЬ ПОЛЬЗОВАТЕЛЯ:
                    </div>
                    <div className="flex flex-wrap gap-2">
                       {users.map(u => {
                         const tk = u.username;
                         const active = activeTokens.includes(tk);
                         return (
                           <button 
                              key={u.username} type="button"
                              onClick={() => toggleToValidUsers(tk)}
                              className={`px-3 py-1.5 rounded-lg text-sm font-medium transition-all ${active ? 'bg-blue-500 text-white shadow-sm' : 'bg-white border border-slate-200 text-slate-600 hover:border-blue-300 hover:text-blue-600'}`}
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
               <button type="submit" className="w-full bg-blue-600 hover:bg-blue-700 text-white px-8 py-4 rounded-xl font-bold tracking-wide shadow-lg shadow-blue-500/30 transition-all hover:-translate-y-0.5">
                 Сохранить настройки
               </button>
            </div>
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
            <div className="bg-blue-100/50 p-2 rounded-2xl ring-1 ring-blue-500/10">
              <FolderKanban className="w-7 h-7 text-blue-600" />
            </div>
            Общие папки
          </h1>
          <p className="text-slate-500 text-[15px] mt-2">Управление доступом к SMB директориям</p>
        </div>
        
        <div className="flex flex-wrap items-center gap-3">
          <button onClick={() => openEditor()} className="bg-slate-900 hover:bg-slate-800 text-white px-5 py-2.5 rounded-xl font-medium tracking-wide flex items-center gap-2 shadow-lg shadow-slate-900/20 transition-all hover:shadow-xl hover:-translate-y-0.5">
            <Plus className="w-5 h-5" /> Создать папку
          </button>
        </div>
      </div>

      {error && (
        <div className="bg-rose-50 text-rose-600 p-4 rounded-xl text-[14px] font-medium border border-rose-100/50">
          ⚠️ {error}
        </div>
      )}
      
      {loading ? (
        <div className="grid grid-cols-1 md:grid-cols-2 xl:grid-cols-3 gap-6">
          {[1,2,3].map(i => (
             <div key={i} className="h-44 bg-white/50 rounded-[20px] border border-slate-100 p-6 animate-pulse flex flex-col justify-between">
                <div className="w-1/3 h-6 bg-slate-200/50 rounded-full mb-4"></div>
                <div className="w-full h-3 bg-slate-100 rounded-full mb-2"></div>
                <div className="w-2/3 h-3 bg-slate-100 rounded-full"></div>
             </div>
          ))}
        </div>
      ) : shares.length === 0 ? (
        <div className="text-center py-24 bg-white rounded-[24px] border border-dashed border-slate-200/60 shadow-sm relative overflow-hidden">
          <FolderKanban className="w-16 h-16 text-blue-100 mx-auto mb-4" />
          <h3 className="text-xl font-bold text-slate-800 tracking-tight">Папок пока нет</h3>
          <p className="text-slate-500 mt-2 mb-6">Создайте первую сетевую директорию для управления</p>
          <button onClick={() => openEditor()} className="bg-blue-50 text-blue-600 hover:bg-blue-100 px-6 py-2.5 rounded-xl font-semibold transition-colors">
            Создать сейчас
          </button>
        </div>
      ) : (
        <div className="grid grid-cols-1 md:grid-cols-2 xl:grid-cols-3 gap-6">
          {shares.filter(s => s.name !== 'global').map((share) => (
            <div key={share.name} className="bg-white rounded-[20px] shadow-sm hover:shadow-md border border-slate-200/60 p-6 transition-all group relative overflow-hidden flex flex-col min-h-[220px]">
              <div className="absolute top-6 right-6 flex gap-2">
                 {share.readOnly && <span className="bg-rose-50 text-rose-600 text-[10px] font-extrabold px-2 py-1 rounded-md uppercase tracking-widest ring-1 ring-inset ring-rose-500/20">Read Only</span>}
                 {share.guestOk && <span className="bg-emerald-50 text-emerald-600 text-[10px] font-extrabold px-2 py-1 rounded-md uppercase tracking-widest ring-1 ring-inset ring-emerald-500/20">Guest OK</span>}
              </div>

              <div className="flex items-center gap-3 mb-1">
                <h3 className="text-xl font-bold text-slate-800 tracking-tight">{share.name}</h3>
              </div>
              
              <div className="text-[13px] font-mono text-slate-500 bg-slate-50 border border-slate-100 px-2 py-1 rounded-md w-fit mb-5 truncate max-w-[80%]" title={share.path}>
                {share.path || '/'}
              </div>

              <p className="text-[14px] text-slate-600 mb-6 flex-1 line-clamp-2">
                {share.comment || <span className="text-slate-400 italic">Описание отсутствует</span>}
              </p>

              <div className="flex items-center justify-between pt-5 border-t border-slate-100 mt-auto">
                <div className="text-[12px] font-semibold text-slate-400 uppercase tracking-wider truncate max-w-[60%]" title={share.validUsers}>
                  {share.validUsers ? `Приват: ${share.validUsers}` : 'Общедоступная'}
                </div>
                
                <div className="flex gap-1.5 opacity-40 group-hover:opacity-100 transition-opacity">
                  <button onClick={() => openEditor(share)} className="p-2 text-slate-500 hover:text-blue-600 hover:bg-blue-50 rounded-lg transition-colors" title="Настроить">
                    <Settings2 className="w-4 h-4" />
                  </button>
                  <button onClick={() => handleDelete(share.name)} className="p-2 text-slate-500 hover:text-rose-600 hover:bg-rose-50 rounded-lg transition-colors" title="Удалить">
                    <Trash2 className="w-4 h-4" />
                  </button>
                </div>
              </div>
            </div>
          ))}
        </div>
      )}
    </div>
  );
}

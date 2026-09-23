import { useEffect, useState } from "react";
import { FolderKanban, Plus, Trash2, Edit3 } from "lucide-react";
import { Button } from "../../components/ui/button";
import { Input } from "../../components/ui/input";
import { Card, CardContent } from "../../components/ui/card";

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

export default function SharesPage() {
  const [shares, setShares] = useState<Share[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");

  const [isEditing, setIsEditing] = useState(false);
  const [currentShare, setCurrentShare] = useState<Partial<Share> | null>(null);

  const fetchShares = async () => {
    try {
      setLoading(true);
      const res = await fetch("/api/shares");
      const json = await res.json();
      if (json.success) {
        setShares(json.data);
      } else {
        setError(json.message || "Ошибка загрузки списка папок");
      }
    } catch (err) {
      setError("Ошибка API");
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchShares();
  }, []);

  const handleDelete = async (name: string) => {
    if (!confirm(`Удалить общую папку [${name}]?`)) return;
    try {
      const res = await fetch(`/api/shares/${name}`, { method: "DELETE" });
      if (res.ok) fetchShares();
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
        fetchShares();
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

  if (isEditing && currentShare) {
    return (
      <div className="space-y-6">
        <div className="flex items-center justify-between">
          <div>
            <h1 className="text-2xl font-bold tracking-tight">
              {currentShare.isNew ? "Новая общая папка" : `Редактирование: ${currentShare.name}`}
            </h1>
          </div>
          <Button onClick={() => setIsEditing(false)} className="!bg-transparent text-foreground border border-border hover:bg-muted">
            Отмена
          </Button>
        </div>

        <Card>
          <CardContent className="p-6">
            <form onSubmit={handleSave} className="space-y-4">
              <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
                <div className="space-y-2">
                  <label className="text-sm font-medium">Имя шары</label>
                  <Input 
                    required 
                    value={currentShare.name} 
                    disabled={!currentShare.isNew}
                    onChange={(e) => setCurrentShare({ ...currentShare, name: e.target.value })} 
                  />
                </div>
                <div className="space-y-2">
                  <label className="text-sm font-medium">Путь на сервере (/path/to/folder)</label>
                  <Input 
                    required 
                    value={currentShare.path || ""} 
                    onChange={(e) => setCurrentShare({ ...currentShare, path: e.target.value })} 
                  />
                </div>
              </div>

              <div className="space-y-2">
                <label className="text-sm font-medium">Комментарий</label>
                <Input 
                  value={currentShare.comment || ""} 
                  onChange={(e) => setCurrentShare({ ...currentShare, comment: e.target.value })} 
                />
              </div>

              <div className="space-y-4 py-4 border-t border-b border-border text-sm">
                <div className="flex items-center gap-2">
                  <input type="checkbox" id="browseable" 
                    checked={currentShare.browseable} 
                    onChange={(e) => setCurrentShare({ ...currentShare, browseable: e.target.checked })} 
                  />
                  <label htmlFor="browseable">Видимая в сети (browseable)</label>
                </div>
                <div className="flex items-center gap-2">
                  <input type="checkbox" id="ro" 
                    checked={currentShare.readOnly} 
                    onChange={(e) => setCurrentShare({ ...currentShare, readOnly: e.target.checked })} 
                  />
                  <label htmlFor="ro">Только для чтения (read only)</label>
                </div>
                <div className="flex items-center gap-2">
                  <input type="checkbox" id="guest" 
                    checked={currentShare.guestOk} 
                    onChange={(e) => setCurrentShare({ ...currentShare, guestOk: e.target.checked })} 
                  />
                  <label htmlFor="guest">Доступ гостям (guest ok)</label>
                </div>
              </div>

              <div className="space-y-2 pb-4">
                <label className="text-sm font-medium">Допустимые пользователи (valid users)</label>
                <Input 
                  placeholder="@smbgroup, user1, user2" 
                  value={currentShare.validUsers || ""} 
                  onChange={(e) => setCurrentShare({ ...currentShare, validUsers: e.target.value })} 
                />
              </div>

              <Button type="submit">Сохранить настройки</Button>
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
          <h1 className="text-3xl font-bold tracking-tight">Общие папки</h1>
          <p className="text-muted-foreground">Управление папками Samba (Shares).</p>
        </div>
        <Button onClick={() => openEditor()} className="flex gap-2">
          <Plus className="w-4 h-4" /> Добавить
        </Button>
      </div>

      {error && <div className="text-red-500 bg-red-50 p-4 rounded-lg">{error}</div>}
      
      {loading ? (
        <div className="text-muted-foreground animate-pulse">Загрузка...</div>
      ) : shares.length === 0 ? (
        <div className="border border-dashed border-border rounded-xl p-12 text-center text-muted-foreground">
          Нет добавленных папок. Трансляция директорий не настроена.
        </div>
      ) : (
        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4">
          {shares.map((share) => (
            <Card key={share.name} className="flex flex-col">
              <CardContent className="p-5 flex-1 space-y-4">
                <div className="flex items-start justify-between">
                  <div className="flex items-center gap-3">
                    <div className="p-2 bg-primary/10 rounded-lg">
                      <FolderKanban className="w-5 h-5 text-primary" />
                    </div>
                    <div>
                      <h3 className="font-semibold text-lg leading-tight">[{share.name}]</h3>
                      <p className="text-xs text-muted-foreground mt-0.5">{share.comment || "Без описания"}</p>
                    </div>
                  </div>
                </div>

                <div className="text-sm text-foreground/80 space-y-1 bg-muted/30 p-3 rounded-md font-mono text-xs overflow-hidden text-ellipsis">
                  <div><span className="text-muted-foreground">Path:</span> {share.path}</div>
                  <div><span className="text-muted-foreground">Read Only:</span> {share.readOnly ? "Да" : "Нет"}</div>
                  <div><span className="text-muted-foreground">Guest Ok:</span> {share.guestOk ? "Да" : "Нет"}</div>
                </div>
              </CardContent>
              <div className="p-4 border-t border-border flex justify-end gap-2 bg-muted/10 rounded-b-xl">
                <Button onClick={() => openEditor(share)} className="!bg-transparent text-foreground border border-border hover:bg-muted text-xs h-8">
                  <Edit3 className="w-3.5 h-3.5 mr-1.5" /> Настроить
                </Button>
                <Button onClick={() => handleDelete(share.name)} className="!bg-transparent border border-red-200 text-red-600 hover:bg-red-50 hover:border-red-300 text-xs h-8">
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

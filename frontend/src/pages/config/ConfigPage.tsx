import { useEffect, useState } from "react";
import { Settings, Save, RotateCcw, AlertTriangle, Fingerprint, Shield, Network, Zap } from "lucide-react";
import { Input } from "../../components/ui/input";
import { Card, CardContent, CardHeader, CardTitle } from "../../components/ui/card";
// Использование нативных чекбоксов и селектов вместо отсутствующих UI компонентов
// для избеания ошибок компиляции TypeScript

interface GlobalConfig {
  workgroup: string;
  serverString: string;
  netbiosName: string;
  security: string;
  mapToGuest: string;
  interfaces: string;
  bindInterfacesOnly: boolean;
  loadPrinters: boolean;
  disableNetbios: boolean;
  serverMinProtocol: string;
  serverMaxProtocol: string;
}

export default function ConfigPage() {
  const [config, setConfig] = useState<GlobalConfig | null>(null);
  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);
  const [message, setMessage] = useState("");
  const [messageType, setMessageType] = useState<"success" | "error">("success");

  const [activeTab, setActiveTab] = useState<"general" | "network" | "security">("general");

  const loadConfig = async () => {
    try {
      setLoading(true);
      const res = await fetch("/api/config/global");
      const json = await res.json();
      if (json.success && json.data) {
        setConfig(json.data);
      }
    } catch (err) {
      setMessage("Ошибка загрузки конфигурации");
      setMessageType("error");
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    loadConfig();
  }, []);

  const handleSave = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!config) return;

    try {
      setSaving(true);
      const res = await fetch("/api/config/global", {
        method: "PUT",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify(config)
      });
      const json = await res.json();
      if (res.ok) {
        setMessage("Конфигурация успешно сохранена! Перезапустите службу Samba во вкладке статуса.");
        setMessageType("success");
      } else {
        setMessage(json.message || "Ошибка сохранения");
        setMessageType("error");
      }
    } catch (err) {
      setMessage("Ошибка сети");
      setMessageType("error");
    } finally {
      setSaving(false);
    }
  };

  const handleRestore = async () => {
    if (!confirm("Вы уверены, что хотите откатиться к конфигурации по умолчанию?")) return;
    try {
      // Имитация бэкенда для отсутствующего эндпоинта 
      // В реальном приложении это должно быть в контроллере, но если его нет:
      setMessage("Откат к заводским настройкам (в разработке)");
      setMessageType("error");
    } catch (err) {
      setMessage("Ошибка сети");
      setMessageType("error");
    }
  };

  if (loading) {
    return <div className="animate-pulse text-muted-foreground p-10">Загрузка конфигурации...</div>;
  }

  if (!config) {
    return <div className="text-red-500 p-10">Не удалось загрузить конфигурацию! Проверьте, запущен ли Samba и работает ли API.</div>;
  }

  return (
    <div className="space-y-6">
      <div>
        <h1 className="text-3xl font-bold tracking-tight text-slate-800">Глобальная конфигурация</h1>
        <p className="text-muted-foreground text-sm mt-1">
          Настройка секции <code className="bg-slate-100 px-1 py-0.5 rounded text-slate-600">[global]</code> в <code className="bg-slate-100 px-1 py-0.5 rounded text-slate-600">/etc/samba/smb.conf</code>.
        </p>
      </div>

      {message && (
        <div className={`p-4 rounded-lg flex items-center gap-3 shadow-sm border ${messageType === 'success' ? 'bg-green-50 text-green-700 border-green-200' : 'bg-red-50 text-red-700 border-red-200'}`}>
          <AlertTriangle className="w-5 h-5 flex-shrink-0" />
          <span className="text-sm font-medium">{message}</span>
        </div>
      )}

      {/* Выбор вкладок */}
      <div className="flex bg-white rounded-lg p-1.5 shadow-sm border border-slate-200 gap-1 w-full max-w-2xl">
        <button 
          onClick={() => setActiveTab('general')} 
          className={`flex-1 py-2 px-3 text-sm font-semibold rounded-md transition-all flex items-center justify-center gap-2 ${activeTab === 'general' ? 'bg-blue-600 text-white shadow' : 'text-slate-600 hover:bg-slate-100'}`}
        >
          <Settings className="w-4 h-4" /> Основное
        </button>
        <button 
          onClick={() => setActiveTab('network')} 
          className={`flex-1 py-2 px-3 text-sm font-semibold rounded-md transition-all flex items-center justify-center gap-2 ${activeTab === 'network' ? 'bg-blue-600 text-white shadow' : 'text-slate-600 hover:bg-slate-100'}`}
        >
          <Network className="w-4 h-4" /> Сеть
        </button>
        <button 
          onClick={() => setActiveTab('security')} 
          className={`flex-1 py-2 px-3 text-sm font-semibold rounded-md transition-all flex items-center justify-center gap-2 ${activeTab === 'security' ? 'bg-blue-600 text-white shadow' : 'text-slate-600 hover:bg-slate-100'}`}
        >
          <Shield className="w-4 h-4" /> Безопасность
        </button>
      </div>

      <form onSubmit={handleSave} className="space-y-6">
        <Card className="border-slate-200 shadow-xl shadow-slate-200/40 rounded-xl overflow-hidden">
          
          {/* ОБЩИЕ НАСТРОЙКИ */}
          {activeTab === 'general' && (
            <>
              <CardHeader className="bg-slate-50/50 border-b border-slate-100 pb-4">
                <CardTitle className="text-lg text-slate-700 flex items-center gap-2">
                  <Fingerprint className="w-5 h-5 text-blue-500" /> Идентификация сервера
                </CardTitle>
              </CardHeader>
              <CardContent className="p-6 space-y-5">
                <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
                  <div className="space-y-2">
                    <label className="text-sm font-semibold text-slate-700">Рабочая группа (Workgroup)</label>
                    <Input 
                      required
                      value={config.workgroup} 
                      onChange={e => setConfig({...config, workgroup: e.target.value})}
                      className="h-11 text-base bg-slate-50 border-slate-200 focus:border-blue-500"
                    />
                    <p className="text-xs text-slate-500">Домен или рабочая группа (обычно WORKGROUP)</p>
                  </div>
                  
                  <div className="space-y-2">
                    <label className="text-sm font-semibold text-slate-700">NetBIOS имя</label>
                    <Input 
                      value={config.netbiosName || ""} 
                      onChange={e => setConfig({...config, netbiosName: e.target.value})}
                      className="h-11 text-base bg-slate-50 border-slate-200 focus:border-blue-500"
                      placeholder="Оставьте пустым для авто"
                    />
                    <p className="text-xs text-slate-500">Имя сервера в сети Windows</p>
                  </div>

                  <div className="space-y-2 md:col-span-2">
                    <label className="text-sm font-semibold text-slate-700">Описание сервера (Server String)</label>
                    <Input 
                      value={config.serverString} 
                      onChange={e => setConfig({...config, serverString: e.target.value})}
                      className="h-11 text-base bg-slate-50 border-slate-200 focus:border-blue-500"
                    />
                  </div>
                </div>

                <div className="pt-6 border-t border-slate-100">
                  <div className="space-y-4">
                    <div className="flex items-center gap-3">
                      <input 
                        type="checkbox"
                        id="loadPrinters" 
                        checked={config.loadPrinters}
                        onChange={(e) => setConfig({...config, loadPrinters: e.target.checked})}
                        className="w-5 h-5 rounded border-slate-300 text-blue-600"
                      />
                      <label htmlFor="loadPrinters" className="text-sm font-medium text-slate-700 leading-none cursor-pointer">
                        Загружать драйверы принтеров
                      </label>
                    </div>
                    <p className="text-xs text-slate-500 ml-8">Отключите, если используете машину только как файловый сервер (ускоряет работу).</p>
                  </div>
                </div>
              </CardContent>
            </>
          )}

          {/* СЕТЕВЫЕ НАСТРОЙКИ */}
          {activeTab === 'network' && (
            <>
              <CardHeader className="bg-slate-50/50 border-b border-slate-100 pb-4">
                <CardTitle className="text-lg text-slate-700 flex items-center gap-2">
                  <Network className="w-5 h-5 text-indigo-500" /> Сеть и привязки
                </CardTitle>
              </CardHeader>
              <CardContent className="p-6 space-y-6">
                <div className="space-y-3">
                  <label className="text-sm font-semibold text-slate-700">Интерфейсы (interfaces)</label>
                  <Input 
                    value={config.interfaces || ""} 
                    onChange={e => setConfig({...config, interfaces: e.target.value})}
                    placeholder="127.0.0.0/8 eth0 (Оставьте пустым для прослушивания всех)"
                    className="h-11 text-base bg-slate-50 border-slate-200 focus:border-blue-500"
                  />
                  <p className="text-xs text-slate-500">Какие сетевые интерфейсы или IP адреса должна прослушивать Samba.</p>
                </div>

                <div className="flex items-center gap-3 bg-slate-50 p-4 rounded-lg border border-slate-100">
                  <input 
                    type="checkbox"
                    id="bindInterfacesOnly" 
                    checked={config.bindInterfacesOnly}
                    onChange={(e) => setConfig({...config, bindInterfacesOnly: e.target.checked})}
                    className="w-5 h-5 rounded border-slate-300 text-blue-600"
                  />
                  <div className="space-y-1">
                    <label htmlFor="bindInterfacesOnly" className="text-sm font-semibold text-slate-700 cursor-pointer">
                      Слушать только указанные интерфейсы (bind interfaces only)
                    </label>
                    <p className="text-xs text-slate-500">Заставляет Samba игнорировать запросы на любых других IP адресах.</p>
                  </div>
                </div>

                <div className="flex items-center gap-3 bg-slate-50 p-4 rounded-lg border border-slate-100">
                  <input 
                    type="checkbox"
                    id="disableNetbios" 
                    checked={config.disableNetbios}
                    onChange={(e) => setConfig({...config, disableNetbios: e.target.checked})}
                    className="w-5 h-5 rounded border-slate-300 text-blue-600"
                  />
                  <div className="space-y-1">
                    <label htmlFor="disableNetbios" className="text-sm font-semibold text-slate-700 cursor-pointer">
                      Отключить NetBIOS
                    </label>
                    <p className="text-xs text-slate-500">Оставьте выключенным для обнаружения в локальной сети Windows (порты 137/138).</p>
                  </div>
                </div>
              </CardContent>
            </>
          )}

          {/* БЕЗОПАСНОСТЬ */}
          {activeTab === 'security' && (
            <>
              <CardHeader className="bg-slate-50/50 border-b border-slate-100 pb-4">
                <CardTitle className="text-lg text-slate-700 flex items-center gap-2">
                  <Shield className="w-5 h-5 text-green-500" /> Контроль доступа
                </CardTitle>
              </CardHeader>
              <CardContent className="p-6 space-y-6">
                <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
                  <div className="space-y-2">
                    <label className="text-sm font-semibold text-slate-700">Модель безопасности</label>
                    <select 
                      value={config.security} 
                      onChange={e => setConfig({...config, security: e.target.value})}
                      className="w-full h-11 text-base bg-slate-50 border border-slate-200 rounded-md px-3 focus:outline-none focus:border-blue-500"
                    >
                      <option value="user">Пользователь (User)</option>
                      <option value="ads">Active Directory (ADS)</option>
                    </select>
                  </div>
                  
                  <div className="space-y-2">
                    <label className="text-sm font-semibold text-slate-700">Отображение гостей</label>
                    <select 
                      value={config.mapToGuest} 
                      onChange={e => setConfig({...config, mapToGuest: e.target.value})}
                      className="w-full h-11 text-base bg-slate-50 border border-slate-200 rounded-md px-3 focus:outline-none focus:border-blue-500"
                    >
                      <option value="Never">Запрещено (Never)</option>
                      <option value="Bad User">Если юзер не найден (Bad User)</option>
                      <option value="Bad Password">Если пароль неверный (Bad Password)</option>
                    </select>
                  </div>
                </div>

                <div className="pt-6 border-t border-slate-100 grid grid-cols-1 md:grid-cols-2 gap-6">
                  <div className="space-y-2">
                    <label className="text-sm font-semibold text-slate-700 flex items-center gap-2">
                      <Zap className="w-4 h-4 text-orange-400" /> Мин. Версия SMB
                    </label>
                    <select 
                      value={config.serverMinProtocol} 
                      onChange={e => setConfig({...config, serverMinProtocol: e.target.value})}
                      className="w-full h-11 text-base bg-slate-50 border border-slate-200 rounded-md px-3 focus:outline-none focus:border-blue-500"
                    >
                      <option value="CORE">CORE (Очень старая)</option>
                      <option value="NT1">NT1 (SMB 1.0)</option>
                      <option value="SMB2">SMB 2.0</option>
                      <option value="SMB3">SMB 3.0</option>
                    </select>
                    <p className="text-xs text-slate-500">NT1 уязвим. Рекомендуется минимум SMB2.</p>
                  </div>

                  <div className="space-y-2">
                    <label className="text-sm font-semibold text-slate-700">Макс. Версия SMB</label>
                    <select 
                      value={config.serverMaxProtocol} 
                      onChange={e => setConfig({...config, serverMaxProtocol: e.target.value})}
                      className="w-full h-11 text-base bg-slate-50 border border-slate-200 rounded-md px-3 focus:outline-none focus:border-blue-500"
                    >
                      <option value="NT1">NT1 (SMB 1.0)</option>
                      <option value="SMB2">SMB 2.0</option>
                      <option value="SMB3">SMB 3.0</option>
                    </select>
                  </div>
                </div>
              </CardContent>
            </>
          )}

          <div className="bg-slate-50/50 p-4 border-t border-slate-100 flex justify-between gap-4">
            <button 
              type="button" 
              onClick={handleRestore}
              className="px-4 py-2 text-slate-700 flex items-center gap-2 hover:bg-slate-200 rounded-md transition-colors text-sm font-medium border border-slate-300"
            >
              <RotateCcw className="w-4 h-4" /> Сбросить по умолчанию
            </button>
            
            <button 
              disabled={saving} 
              type="submit" 
              className="px-6 py-2 bg-blue-600 hover:bg-blue-700 text-white flex items-center gap-2 rounded-md transition-colors text-sm font-bold shadow-sm"
            >
              <Save className="w-4 h-4" /> {saving ? "Сохранение..." : "Сохранить конфигурацию"}
            </button>
          </div>
        </Card>
      </form>
    </div>
  );
}

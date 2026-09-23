import { useState } from "react";
import { Server, Lock, Key, ArrowRight } from "lucide-react";
import { Input } from "../components/ui/input";

export default function LoginPage() {
  const [host, setHost] = useState("");
  const [port, setPort] = useState("22");
  const [username, setUsername] = useState("");
  const [password, setPassword] = useState("");
  const [privateKey, setPrivateKey] = useState("");
  const [useKeyAuth, setUseKeyAuth] = useState(false);
  const [error, setError] = useState("");
  const [loading, setLoading] = useState(false);

  const handleLogin = async (e: React.FormEvent) => {
    e.preventDefault();
    setLoading(true);
    setError("");

    try {
      const res = await fetch("/api/auth/login", {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({
          host,
          port: parseInt(port),
          username,
          password: useKeyAuth ? undefined : password,
          privateKey: useKeyAuth ? privateKey : undefined,
          keyAuth: useKeyAuth
        }),
      });

      const data = await res.json();
      if (res.ok && data.success) {
        window.location.href = "/ui/";
      } else {
        setError(data.message || "Ошибка авторизации");
      }
    } catch (err) {
      setError("Ошибка сети");
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="min-h-screen flex items-center justify-center bg-[#f8fafc] px-4 py-12 relative overflow-hidden font-sans">
      {/* Декоративные фоновые элементы */}
      <div className="absolute top-[-10%] left-[-10%] w-[40%] h-[40%] rounded-full bg-blue-600/5 blur-[120px] pointer-events-none"></div>
      <div className="absolute bottom-[-10%] right-[-10%] w-[40%] h-[40%] rounded-full bg-indigo-600/5 blur-[120px] pointer-events-none"></div>
      
      <div className="w-full max-w-[440px] relative z-10">
        <div className="text-center mb-8">
          <div className="mx-auto w-16 h-16 bg-gradient-to-br from-blue-600 to-indigo-600 rounded-2xl flex items-center justify-center shadow-xl shadow-blue-500/30 mb-6 transform -rotate-3 hover:rotate-0 transition-transform duration-300">
            <Server className="w-8 h-8 text-white" />
          </div>
          <h1 className="text-3xl font-extrabold text-slate-900 tracking-tight">Samba Web UI</h1>
          <p className="text-slate-500 mt-2 text-[15px]">Войдите для управления сервером</p>
        </div>

        <div className="bg-white rounded-[24px] shadow-2xl shadow-slate-200/50 border border-slate-100 p-8 sm:p-10 backdrop-blur-xl">
          <form onSubmit={handleLogin} className="space-y-6 flex flex-col">
            {error && (
              <div className="bg-rose-50 text-rose-600 p-4 rounded-xl text-[14px] font-medium border border-rose-100/50 flex animate-in fade-in slide-in-from-top-2">
                <span className="mr-2">⚠️</span> {error}
              </div>
            )}
            
            <div className="flex gap-4">
              <div className="flex-1 space-y-2">
                <label className="text-[13px] font-bold text-slate-700 uppercase tracking-wide">Хост / IP</label>
                <Input 
                  required 
                  value={host}
                  onChange={e => setHost(e.target.value)}
                  placeholder="192.168.1.100" 
                  className="bg-slate-50 border-slate-200 focus:border-blue-500 hover:border-slate-300 transition-colors text-base py-6 rounded-xl shadow-sm"
                />
              </div>
              <div className="w-24 space-y-2">
                <label className="text-[13px] font-bold text-slate-700 uppercase tracking-wide">Порт</label>
                <Input 
                  required 
                  value={port}
                  onChange={e => setPort(e.target.value)}
                  type="number" 
                  className="bg-slate-50 border-slate-200 focus:border-blue-500 hover:border-slate-300 transition-colors text-base py-6 rounded-xl shadow-sm text-center"
                />
              </div>
            </div>

            <div className="space-y-2">
              <label className="text-[13px] font-bold text-slate-700 uppercase tracking-wide">Пользователь</label>
              <Input 
                required 
                value={username}
                onChange={e => setUsername(e.target.value)}
                placeholder="root" 
                className="bg-slate-50 border-slate-200 focus:border-blue-500 hover:border-slate-300 transition-colors text-base py-6 rounded-xl shadow-sm"
              />
            </div>

            {/* Тип авторизации */}
            <div className="flex bg-slate-100 p-1.5 rounded-xl shadow-inner my-2">
              <button 
                type="button" 
                onClick={() => setUseKeyAuth(false)}
                className={`flex-1 py-2 text-[14px] font-semibold rounded-lg transition-all duration-200 ${!useKeyAuth ? 'bg-white text-blue-600 shadow-sm' : 'text-slate-500 hover:text-slate-700'}`}
              >
                Пароль
              </button>
              <button 
                type="button" 
                onClick={() => setUseKeyAuth(true)}
                className={`flex-1 py-2 text-[14px] font-semibold rounded-lg transition-all duration-200 flex items-center justify-center gap-1.5 ${useKeyAuth ? 'bg-white text-blue-600 shadow-sm' : 'text-slate-500 hover:text-slate-700'}`}
              >
                <Key className="w-4 h-4" /> SSH Ключ
              </button>
            </div>

            {!useKeyAuth ? (
              <div className="space-y-2 animate-in fade-in zoom-in-95 duration-200">
                <label className="text-[13px] font-bold text-slate-700 uppercase tracking-wide flex justify-between">
                  <span>Пароль</span>
                </label>
                <div className="relative">
                  <Input 
                    required 
                    value={password}
                    onChange={e => setPassword(e.target.value)}
                    type="password" 
                    placeholder="••••••••" 
                    className="bg-slate-50 border-slate-200 pl-11 focus:border-blue-500 hover:border-slate-300 transition-colors text-base py-6 rounded-xl shadow-sm"
                  />
                  <Lock className="w-5 h-5 text-slate-400 absolute left-3.5 top-1/2 transform -translate-y-1/2" />
                </div>
              </div>
            ) : (
              <div className="space-y-2 animate-in fade-in zoom-in-95 duration-200">
                <label className="text-[13px] font-bold text-slate-700 uppercase tracking-wide flex items-center justify-between">
                  <span>Приватный ключ</span>
                  <span className="font-normal text-[11px] text-slate-400 lowercase tracking-normal">RSA / Ed25519</span>
                </label>
                <textarea
                  required
                  value={privateKey}
                  onChange={e => setPrivateKey(e.target.value)}
                  placeholder="-----BEGIN OPENSSH PRIVATE KEY-----\n..."
                  className="w-full h-32 text-[13px] font-mono leading-relaxed p-4 bg-slate-50 border border-slate-200 rounded-xl focus:border-blue-500 hover:border-slate-300 focus:outline-none focus:ring-4 focus:ring-blue-500/10 shadow-sm transition-all resize-none"
                />
              </div>
            )}

            <button 
              disabled={loading} 
              type="submit" 
              className="w-full bg-slate-900 hover:bg-slate-800 text-white font-semibold flex items-center justify-center gap-2 h-14 rounded-xl text-[15px] mt-6 transition-all shadow-lg hover:shadow-xl hover:-translate-y-0.5 disabled:opacity-70 disabled:hover:translate-y-0 disabled:hover:shadow-lg group"
            >
              {loading ? "Подключение..." : "Войти в систему"} {!loading && <ArrowRight className="w-4 h-4 opacity-50 group-hover:opacity-100 group-hover:translate-x-1 transition-all" />}
            </button>
          </form>
        </div>
        
        <p className="text-center text-slate-400 text-sm mt-8">
          Samba Web UI © {new Date().getFullYear()}
        </p>
      </div>
    </div>
  );
}

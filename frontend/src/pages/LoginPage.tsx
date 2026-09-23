import { useState } from "react";
import { Server, Lock, Key } from "lucide-react";
import { Button } from "../components/ui/button";
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
    <div className="min-h-screen flex items-center justify-center bg-slate-50 px-4">
      <div className="w-full max-w-md bg-white rounded-2xl shadow-xl overflow-hidden border border-slate-100">
        <div className="bg-blue-600 p-8 text-center text-white">
          <div className="mx-auto bg-white/20 w-16 h-16 rounded-full flex items-center justify-center mb-4">
            <Server className="w-8 h-8 text-white" />
          </div>
          <h1 className="text-2xl font-bold">Samba Web UI</h1>
          <p className="text-blue-100 mt-2 opacity-90 text-sm">Панель управления файловым сервером</p>
        </div>
        
        <div className="p-8">
          <form onSubmit={handleLogin} className="space-y-5 flex flex-col">
            {error && (
              <div className="bg-red-50 text-red-600 p-3 rounded-lg text-sm border border-red-100">
                {error}
              </div>
            )}
            
            <div className="flex gap-3">
              <div className="flex-1 space-y-1">
                <label className="text-sm font-semibold text-slate-700">Хост / IP</label>
                <Input 
                  required 
                  value={host}
                  onChange={e => setHost(e.target.value)}
                  placeholder="192.168.1.100" 
                  className="bg-slate-50 border-slate-200 focus:border-blue-500 text-base py-5"
                />
              </div>
              <div className="w-24 space-y-1">
                <label className="text-sm font-semibold text-slate-700">Порт</label>
                <Input 
                  required 
                  value={port}
                  onChange={e => setPort(e.target.value)}
                  type="number" 
                  className="bg-slate-50 border-slate-200 focus:border-blue-500 text-base py-5"
                />
              </div>
            </div>

            <div className="space-y-1">
              <label className="text-sm font-semibold text-slate-700">Пользователь</label>
              <Input 
                required 
                value={username}
                onChange={e => setUsername(e.target.value)}
                placeholder="root" 
                className="bg-slate-50 border-slate-200 focus:border-blue-500 text-base py-5"
              />
            </div>

            {/* Тип авторизации */}
            <div className="flex bg-slate-100 p-1 rounded-lg">
              <button 
                type="button" 
                onClick={() => setUseKeyAuth(false)}
                className={`flex-1 py-1.5 text-sm font-medium rounded-md transition-colors ${!useKeyAuth ? 'bg-white text-blue-600 shadow-sm' : 'text-slate-500 hover:text-slate-700'}`}
              >
                Пароль
              </button>
              <button 
                type="button" 
                onClick={() => setUseKeyAuth(true)}
                className={`flex-1 py-1.5 text-sm font-medium rounded-md transition-colors flex items-center justify-center gap-1 ${useKeyAuth ? 'bg-white text-blue-600 shadow-sm' : 'text-slate-500 hover:text-slate-700'}`}
              >
                <Key className="w-3.5 h-3.5" /> SSH Ключ
              </button>
            </div>

            {!useKeyAuth ? (
              <div className="space-y-1">
                <label className="text-sm font-semibold text-slate-700 flex justify-between">
                  <span>Пароль</span>
                </label>
                <div className="relative">
                  <Input 
                    required 
                    value={password}
                    onChange={e => setPassword(e.target.value)}
                    type="password" 
                    placeholder="••••••••" 
                    className="bg-slate-50 border-slate-200 pl-10 focus:border-blue-500 text-base py-5"
                  />
                  <Lock className="w-5 h-5 text-slate-400 absolute left-3 top-1/2 transform -translate-y-1/2" />
                </div>
              </div>
            ) : (
              <div className="space-y-1">
                <label className="text-sm font-semibold text-slate-700">Приватный ключ (RSA/Ed25519) <span className="font-normal text-xs text-slate-400 ml-2">Начинается с -----BEGIN...</span></label>
                <textarea
                  required
                  value={privateKey}
                  onChange={e => setPrivateKey(e.target.value)}
                  placeholder="-----BEGIN OPENSSH PRIVATE KEY-----\n..."
                  className="w-full h-32 text-xs font-mono p-3 bg-slate-50 border border-slate-200 rounded-md focus:border-blue-500 focus:outline-none resize-none"
                />
              </div>
            )}

            <Button disabled={loading} type="submit" className="w-full bg-blue-600 hover:bg-blue-700 text-white font-semibold flex items-center justify-center gap-2 h-12 text-base mt-2">
              {loading ? "Подключение..." : "Подключиться"}
            </Button>
          </form>
        </div>
      </div>
    </div>
  );
}

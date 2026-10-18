import { getErrorMessage } from "../lib/errors";
import { useEffect, useState } from "react";
import { Lock, Key, ArrowRight, AlertTriangle } from "lucide-react";
import { Input } from "../components/ui/input";
import { api } from "../lib/api";

interface AuthEnvironment {
  mode: "local" | "remote";
}

export default function LoginPage() {
  const [host, setHost] = useState("");
  const [port, setPort] = useState("22");
  const [username, setUsername] = useState("");
  const [password, setPassword] = useState("");
  const [privateKey, setPrivateKey] = useState("");
  const [useKeyAuth, setUseKeyAuth] = useState(false);
  const [error, setError] = useState("");
  const [loading, setLoading] = useState(false);
  const [executionMode, setExecutionMode] = useState<AuthEnvironment["mode"]>("remote");
  const isLocal = executionMode === "local";

  useEffect(() => {
    api.get<AuthEnvironment>("/api/auth/environment")
      .then((environment) => setExecutionMode(environment.mode))
      .catch(() => setExecutionMode("remote"));
  }, []);

  const handleLogin = async (e: React.FormEvent) => {
    e.preventDefault();
    setLoading(true);
    setError("");

    try {
      await api.post("/api/auth/login", {
        host: isLocal ? undefined : host,
        port: isLocal ? undefined : parseInt(port),
        username,
        authType: !isLocal && useKeyAuth ? "key" : "password",
        password: !isLocal && useKeyAuth ? undefined : password,
        privateKey: !isLocal && useKeyAuth ? privateKey : undefined,
      });
      // Simple redirect since auth state isn't managed globally here
      window.location.href = "/ui/";
    } catch (err) {
      setError(getErrorMessage(err) || "Ошибка авторизации");
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="min-h-screen flex items-center justify-center bg-bg px-4 py-12 relative overflow-hidden font-sans">
      <div className="absolute top-[-10%] left-[-10%] w-[40%] h-[40%] rounded-full bg-[#86efac]/10 blur-[120px] pointer-events-none"></div>
      <div className="absolute bottom-[-10%] right-[-10%] w-[40%] h-[40%] rounded-full bg-[#86efac]/10 blur-[120px] pointer-events-none"></div>
      
      <div className="w-full max-w-[440px] relative z-10">
        <div className="text-center mb-8">
          <div className="flex justify-center mb-6">
            <img 
              src={`${import.meta.env.BASE_URL}shareguard/logo-mark.svg`} 
              alt="ShareGuard" 
              className="h-16 w-auto" 
            />
          </div>
          <h1 className="text-3xl font-extrabold text-foreground tracking-tight">ShareGuard</h1>
          <p className="text-text-muted mt-2 text-[15px]">
            {isLocal ? "Локальное управление сервером" : "Подключение к серверу по SSH"}
          </p>
        </div>

        <div className="bg-surface rounded-[24px] border border-border p-8 sm:p-10 backdrop-blur-xl">
          <form onSubmit={handleLogin} className="space-y-6 flex flex-col">
            {error && (
              /* TODO: token for status-error is not in theme palette, keeping functional status color */
              <div className="bg-surface-2 text-status-error p-4 rounded-xl text-[14px] font-medium border border-status-error/20 flex items-center gap-2 animate-in fade-in slide-in-from-top-2">
                <AlertTriangle className="w-4 h-4 shrink-0" /> {error}
              </div>
            )}
            
            {!isLocal && (
              <div className="flex gap-4">
                <div className="flex-1 space-y-2">
                  <label className="text-[13px] font-bold text-foreground uppercase tracking-wide">Хост / IP</label>
                  <Input
                    required
                    value={host}
                    onChange={e => setHost(e.target.value)}
                    placeholder="192.168.1.100"
                    className="bg-surface-2 border-border-strong focus:border-accent text-foreground hover:border-border-strong transition-colors text-base py-6 rounded-xl"
                  />
                </div>
                <div className="w-24 space-y-2">
                  <label className="text-[13px] font-bold text-foreground uppercase tracking-wide">Порт</label>
                  <Input
                    required
                    value={port}
                    onChange={e => setPort(e.target.value)}
                    type="number"
                    className="bg-surface-2 border-border-strong focus:border-accent text-foreground hover:border-border-strong transition-colors text-base py-6 rounded-xl text-center"
                  />
                </div>
              </div>
            )}

            <div className="space-y-2">
              <label className="text-[13px] font-bold text-foreground uppercase tracking-wide">Пользователь</label>
              <Input 
                required 
                value={username}
                onChange={e => setUsername(e.target.value)}
                placeholder="root" 
                className="bg-surface-2 border-border-strong focus:border-accent text-foreground hover:border-border-strong transition-colors text-base py-6 rounded-xl"
              />
            </div>

            {!isLocal && <div className="flex bg-surface-2 p-1.5 rounded-xl my-2 border border-border">
              <button 
                type="button" 
                onClick={() => setUseKeyAuth(false)}
                className={`flex-1 py-2 text-[14px] font-semibold rounded-lg transition-all duration-200 ${!useKeyAuth ? 'bg-surface text-accent-text border border-border' : 'text-text-muted hover:text-foreground'}`}
              >
                Пароль
              </button>
              <button 
                type="button" 
                onClick={() => setUseKeyAuth(true)}
                className={`flex-1 py-2 text-[14px] font-semibold rounded-lg transition-all duration-200 flex items-center justify-center gap-1.5 ${useKeyAuth ? 'bg-surface text-accent-text border border-border' : 'text-text-muted hover:text-foreground'}`}
              >
                <Key className="w-4 h-4" /> SSH Ключ
              </button>
            </div>}

            {isLocal || !useKeyAuth ? (
              <div className="space-y-2 animate-in fade-in zoom-in-95 duration-200">
                <label className="text-[13px] font-bold text-foreground uppercase tracking-wide flex justify-between">
                  <span>Пароль</span>
                </label>
                <div className="relative">
                  <Input 
                    required 
                    value={password}
                    onChange={e => setPassword(e.target.value)}
                    type="password" 
                    placeholder="••••••••" 
                    className="bg-surface-2 border-border-strong pl-11 focus:border-accent text-foreground hover:border-border-strong transition-colors text-base py-6 rounded-xl"
                  />
                  <Lock className="w-5 h-5 text-text-muted absolute left-3.5 top-1/2 transform -translate-y-1/2" />
                </div>
              </div>
            ) : (
              <div className="space-y-2 animate-in fade-in zoom-in-95 duration-200">
                <label className="text-[13px] font-bold text-foreground uppercase tracking-wide flex items-center justify-between">
                  <span>Приватный ключ</span>
                  <span className="font-normal text-[11px] text-text-muted lowercase tracking-normal">RSA / Ed25519</span>
                </label>
                <textarea
                  required
                  value={privateKey}
                  onChange={e => setPrivateKey(e.target.value)}
                  placeholder="-----BEGIN OPENSSH PRIVATE KEY-----\n..."
                  className="w-full h-32 text-[13px] font-mono leading-relaxed p-4 bg-surface-2 border border-border-strong rounded-xl focus:border-accent hover:border-border-strong focus:outline-none transition-all resize-none text-foreground"
                />
              </div>
            )}

            <button 
              disabled={loading} 
              type="submit" 
              className="w-full bg-[#0a0a0a] hover:bg-black/85 text-white font-semibold flex items-center justify-center gap-2 h-14 rounded-xl text-[15px] mt-6 transition-all disabled:opacity-70 group border border-transparent dark:border-border"
            >
              {loading ? "Подключение..." : "Войти в систему"} {!loading && <ArrowRight className="w-4 h-4 opacity-50 group-hover:opacity-100 group-hover:translate-x-1 transition-all" />}
            </button>
          </form>
        </div>
        
        <p className="text-center text-text-muted text-sm mt-8">
          ShareGuard &copy; {new Date().getFullYear()}
        </p>
      </div>
    </div>
  );
}

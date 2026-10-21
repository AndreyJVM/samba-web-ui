import { getErrorMessage } from "../lib/errors";
import { useEffect, useState } from "react";
import { Lock, Key, ArrowRight, AlertTriangle, SlidersHorizontal } from "lucide-react";
import { Input } from "../components/ui/input";
import { api } from "../lib/api";

interface AuthEnvironment {
  mode: "local" | "remote";
}

export default function LoginPage() {
  const [host, setHost] = useState("");
  const [port, setPort] = useState("22");
  const [showPort, setShowPort] = useState(false);
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
      let finalHost = host.trim();
      let finalPort = parseInt(port, 10);
      if (isNaN(finalPort)) finalPort = 22;

      // Support host:port if user entered it directly into host input
      if (/^[^:]+:\d+$/.test(finalHost)) {
        const [h, p] = finalHost.split(":");
        if (p && !isNaN(parseInt(p, 10))) {
          finalHost = h;
          finalPort = parseInt(p, 10);
        }
      }

      await api.post("/api/auth/login", {
        host: isLocal ? undefined : finalHost,
        port: isLocal ? undefined : finalPort,
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
    <div className="min-h-screen flex flex-col justify-between bg-bg px-4 py-8 sm:py-12 relative font-sans text-foreground selection:bg-accent selection:text-on-accent overflow-y-auto">
      {/* Subtle restrained engineering grid accent */}
      <div
        aria-hidden="true"
        className="fixed inset-0 pointer-events-none opacity-25 [background-image:radial-gradient(var(--border-strong)_1px,transparent_1px)] [background-size:24px_24px] [mask-image:radial-gradient(ellipse_65%_65%_at_50%_50%,#000_40%,transparent_100%)]"
      />

      {/* Top spacer for balanced vertical alignment */}
      <div className="shrink-0" />

      {/* Main card container */}
      <div className="w-full max-w-[400px] mx-auto relative z-10 my-auto">
        {/* Brand identity header */}
        <div className="text-center mb-6">
          <div className="inline-flex items-center justify-center p-2.5 rounded-2xl bg-surface border border-border shadow-xs mb-3">
            <img
              src={`${import.meta.env.BASE_URL}shareguard/logo-mark.svg`}
              alt="ShareGuard"
              className="h-10 w-auto"
            />
          </div>
          <h1 className="text-2xl font-bold text-foreground tracking-tight flex items-center justify-center gap-0.5">
            <span>Share</span>
            <span className="font-extrabold text-foreground">Guard</span>
          </h1>
          <p className="text-text-muted mt-1 text-xs sm:text-sm">
            {isLocal ? "Локальное управление сервером Samba" : "Управление сервером Samba по SSH"}
          </p>
        </div>

        {/* Login form card */}
        <div className="bg-surface rounded-2xl border border-border p-6 sm:p-7 shadow-xs">
          <form onSubmit={handleLogin} className="space-y-4">
            {error && (
              <div
                role="alert"
                className="bg-surface-2 text-status-error p-3 rounded-lg text-xs font-medium border border-status-error/30 flex items-start gap-2.5"
              >
                <AlertTriangle className="w-4 h-4 shrink-0 mt-0.5" />
                <span className="leading-relaxed">{error}</span>
              </div>
            )}

            {/* Main scenario: 1. Server Host with prominent port toggle pill */}
            {!isLocal && (
              <div className="space-y-2">
                <div className="space-y-1.5">
                  <div className="flex items-center justify-between">
                    <label
                      htmlFor="login-host"
                      className="block text-xs font-semibold text-text-muted uppercase tracking-wider"
                    >
                      Хост / IP
                    </label>
                    <button
                      type="button"
                      onClick={() => setShowPort(!showPort)}
                      className={`inline-flex items-center gap-1.5 px-2.5 py-1 rounded-md text-xs font-medium border transition-colors shadow-2xs focus-visible:outline-none focus-visible:ring-1 focus-visible:ring-foreground ${
                        showPort
                          ? "bg-accent/25 border-accent/60 text-accent-text font-semibold"
                          : "bg-surface-2 border-border-strong text-foreground hover:bg-surface hover:border-foreground/30"
                      }`}
                      title={showPort ? "Скрыть настройку порта" : "Изменить порт подключения (по умолчанию 22)"}
                      aria-expanded={showPort}
                    >
                      <SlidersHorizontal className="w-3 h-3 shrink-0 opacity-70" />
                      <span>{showPort ? "Скрыть порт" : `Порт: ${port || "22"}`}</span>
                    </button>
                  </div>
                  <Input
                    id="login-host"
                    name="host"
                    autoComplete="host"
                    required
                    value={host}
                    onChange={e => setHost(e.target.value)}
                    placeholder="192.168.1.100"
                    className="bg-surface-2 border-border-strong text-foreground focus-visible:border-border-strong focus-visible:ring-2 focus-visible:ring-accent-text/30 text-sm h-10 rounded-lg transition-colors"
                  />
                </div>

                {/* Separate Port setting (revealed on click) */}
                {showPort && (
                  <div className="space-y-1.5 animate-in fade-in slide-in-from-top-1 duration-150">
                    <div className="flex items-center justify-between">
                      <label
                        htmlFor="login-port"
                        className="block text-xs font-semibold text-text-muted uppercase tracking-wider"
                      >
                        Порт SSH
                      </label>
                      {port !== "22" && (
                        <button
                          type="button"
                          onClick={() => setPort("22")}
                          className="text-[11px] font-medium text-text-muted hover:text-foreground transition-colors focus-visible:outline-none focus-visible:underline"
                        >
                          Сбросить на 22
                        </button>
                      )}
                    </div>
                    <Input
                      id="login-port"
                      name="port"
                      required
                      value={port}
                      onChange={e => setPort(e.target.value)}
                      type="number"
                      placeholder="22"
                      className="bg-surface-2 border-border-strong text-foreground focus-visible:border-border-strong focus-visible:ring-2 focus-visible:ring-accent-text/30 text-sm h-10 rounded-lg transition-colors"
                    />
                  </div>
                )}
              </div>
            )}

            {/* Main scenario: 2. Login (Username) */}
            <div className="space-y-1.5">
              <label
                htmlFor="login-username"
                className="block text-xs font-semibold text-text-muted uppercase tracking-wider"
              >
                Пользователь
              </label>
              <Input
                id="login-username"
                name="username"
                autoComplete="username"
                required
                value={username}
                onChange={e => setUsername(e.target.value)}
                placeholder="root"
                className="bg-surface-2 border-border-strong text-foreground focus-visible:border-border-strong focus-visible:ring-2 focus-visible:ring-accent-text/30 text-sm h-10 rounded-lg transition-colors"
              />
            </div>

            {/* Main scenario: 3. Password OR Key input */}
            {isLocal || !useKeyAuth ? (
              <div className="space-y-1.5">
                <label
                  htmlFor="login-password"
                  className="block text-xs font-semibold text-text-muted uppercase tracking-wider"
                >
                  Пароль
                </label>
                <div className="relative">
                  <Input
                    id="login-password"
                    name="password"
                    autoComplete="current-password"
                    required
                    value={password}
                    onChange={e => setPassword(e.target.value)}
                    type="password"
                    placeholder="••••••••"
                    className="bg-surface-2 border-border-strong pl-10 text-foreground focus-visible:border-border-strong focus-visible:ring-2 focus-visible:ring-accent-text/30 text-sm h-10 rounded-lg transition-colors"
                  />
                  <Lock className="w-4 h-4 text-text-muted absolute left-3 top-1/2 -translate-y-1/2 pointer-events-none" />
                </div>
              </div>
            ) : (
              <div className="space-y-1.5">
                <div className="flex items-center justify-between">
                  <label
                    htmlFor="login-privatekey"
                    className="block text-xs font-semibold text-text-muted uppercase tracking-wider"
                  >
                    Приватный ключ
                  </label>
                  <span className="text-[11px] font-mono text-text-muted">RSA / Ed25519</span>
                </div>
                <textarea
                  id="login-privatekey"
                  name="privateKey"
                  required
                  value={privateKey}
                  onChange={e => setPrivateKey(e.target.value)}
                  placeholder={"-----BEGIN OPENSSH PRIVATE KEY-----\n..."}
                  className="w-full h-28 text-xs font-mono leading-relaxed p-3 bg-surface-2 border border-border-strong rounded-lg text-foreground placeholder:text-text-muted focus:outline-none focus:border-border-strong focus:ring-2 focus:ring-accent-text/30 transition-colors resize-none"
                />
              </div>
            )}

            {/* Main scenario: 4. Submit button with fixed dimensions to prevent jump */}
            <button
              disabled={loading}
              type="submit"
              className="w-full bg-foreground text-background font-medium flex items-center justify-center gap-2 h-11 rounded-lg text-sm mt-5 transition-all hover:opacity-90 active:scale-[0.99] disabled:opacity-50 disabled:cursor-not-allowed focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-offset-2 focus-visible:ring-foreground group shadow-xs shrink-0"
            >
              {loading ? (
                <>
                  <span className="w-4 h-4 border-2 border-background/30 border-t-background rounded-full animate-spin shrink-0" />
                  <span>Подключение...</span>
                </>
              ) : (
                <>
                  <span>Войти в систему</span>
                  <ArrowRight className="w-4 h-4 opacity-60 group-hover:opacity-100 group-hover:translate-x-0.5 transition-all shrink-0" />
                </>
              )}
            </button>

            {/* Alternate SSH auth method visually separated under the submit button */}
            {!isLocal && (
              <div className="pt-4 border-t border-border mt-5 space-y-2">
                <span className="block text-[11px] font-medium text-text-muted text-center">
                  Способ аутентификации
                </span>
                <div
                  className="flex bg-surface-2 p-1 rounded-lg border border-border"
                  role="tablist"
                  aria-label="Способ аутентификации"
                >
                  <button
                    type="button"
                    role="tab"
                    onClick={() => setUseKeyAuth(false)}
                    aria-selected={!useKeyAuth}
                    className={`flex-1 py-1.5 text-xs font-medium rounded-md transition-all ${
                      !useKeyAuth
                        ? 'bg-surface text-foreground shadow-xs font-semibold'
                        : 'text-text-muted hover:text-foreground'
                    }`}
                  >
                    Пароль
                  </button>
                  <button
                    type="button"
                    role="tab"
                    onClick={() => setUseKeyAuth(true)}
                    aria-selected={useKeyAuth}
                    className={`flex-1 py-1.5 text-xs font-medium rounded-md transition-all flex items-center justify-center gap-1.5 ${
                      useKeyAuth
                        ? 'bg-surface text-foreground shadow-xs font-semibold'
                        : 'text-text-muted hover:text-foreground'
                    }`}
                  >
                    <Key className="w-3.5 h-3.5" />
                    <span>SSH-ключ</span>
                  </button>
                </div>
              </div>
            )}
          </form>
        </div>

        {/* Footer info */}
        <p className="text-center text-text-muted text-xs mt-6">
          ShareGuard &copy; {new Date().getFullYear()}
        </p>
      </div>

      {/* Bottom spacer for balance */}
      <div className="shrink-0" />
    </div>
  );
}

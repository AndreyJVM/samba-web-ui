import { useState } from "react";
import { useNavigate } from "react-router-dom";
import { HardDrive, Lock, User } from "lucide-react";
import { Button } from "../components/ui/button";
import { Input } from "../components/ui/input";

export default function LoginPage() {
  const navigate = useNavigate();
  const [host, setHost] = useState("127.0.0.1");
  const [port, setPort] = useState("2222");
  const [username, setUsername] = useState("admin");
  const [password, setPassword] = useState("admin");
  const [error, setError] = useState("");
  const [isLoading, setIsLoading] = useState(false);

  const handleLogin = async (e: React.FormEvent) => {
    e.preventDefault();
    setIsLoading(true);
    setError("");

    try {
      const response = await fetch("/api/auth/login", {
        method: "POST",
        // Меняем x-www-form-urlencoded на application/json, т.к. бэкенд ждет @RequestBody
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({
          host,
          port,
          username,
          password,
        }),
      });

      const result = await response.json();
      
      if (response.ok && result.success) {
        // Успешный логин! Переходим на дашборд
        navigate("/dashboard");
      } else {
        setError(result.message || "Ошибка авторизации");
      }
    } catch (err) {
      setError("Не удалось подключиться к серверу API");
    } finally {
      setIsLoading(false);
    }
  };

  return (
    <div className="flex bg-muted items-center justify-center min-h-screen p-4">
      <div className="bg-background w-full max-w-sm rounded-[1rem] p-8 border border-border shadow-soft flex flex-col gap-6">
        
        <div className="flex flex-col items-center gap-2 text-center">
          <div className="bg-primary/5 p-3 rounded-full mb-2">
            <HardDrive className="w-8 h-8 text-primary" />
          </div>
          <h1 className="text-2xl font-bold tracking-tight">Samba Web UI</h1>
          <p className="text-muted-foreground text-sm">
            Введите SSH доступы к вашему Linux-серверу для управления Samba
          </p>
        </div>

        {error && (
          <div className="bg-red-50 text-red-600 text-sm p-3 rounded-md border border-red-100 text-center">
            {error}
          </div>
        )}

        <form onSubmit={handleLogin} className="flex flex-col gap-4">
          <div className="flex gap-2">
            <div className="flex-1">
              <label className="text-xs font-semibold mb-1 block text-foreground/80">Сервер (Host)</label>
              <Input
                type="text"
                value={host}
                onChange={(e) => setHost(e.target.value)}
                placeholder="192.168.1.100"
                required
              />
            </div>
            <div className="w-20">
              <label className="text-xs font-semibold mb-1 block text-foreground/80">Порт</label>
              <Input
                type="text"
                value={port}
                onChange={(e) => setPort(e.target.value)}
                placeholder="22"
                required
              />
            </div>
          </div>

          <div>
            <label className="text-xs font-semibold mb-1 block text-foreground/80">Пользователь</label>
            <div className="relative">
              <User className="absolute left-3 top-2.5 h-4 w-4 text-muted-foreground" />
              <Input
                type="text"
                value={username}
                onChange={(e) => setUsername(e.target.value)}
                placeholder="root"
                className="pl-9"
                required
              />
            </div>
          </div>

          <div>
            <label className="text-xs font-semibold mb-1 block text-foreground/80">Пароль</label>
            <div className="relative">
              <Lock className="absolute left-3 top-2.5 h-4 w-4 text-muted-foreground" />
              <Input
                type="password"
                value={password}
                onChange={(e) => setPassword(e.target.value)}
                placeholder="••••••••"
                className="pl-9"
                required
              />
            </div>
          </div>

          <Button type="submit" disabled={isLoading} className="mt-2">
            {isLoading ? "Подключение..." : "Войти"}
          </Button>
        </form>
      </div>
    </div>
  );
}

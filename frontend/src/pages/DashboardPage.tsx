export default function DashboardPage() {
  return (
    <div className="flex bg-muted items-center justify-center min-h-screen p-4">
      <div className="bg-background w-full max-w-lg rounded-[1rem] p-8 border border-border shadow-soft text-center">
        <h1 className="text-2xl font-bold tracking-tight mb-2">🎉 Добро пожаловать!</h1>
        <p className="text-muted-foreground text-sm">
          Авторизация прошла успешно. Вы находитесь в панели управления Samba.
        </p>
      </div>
    </div>
  );
}

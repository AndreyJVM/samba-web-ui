export default function PlaceholderPage({ title, description }: { title: string, description: string }) {
  return (
    <div className="space-y-6">
      <div>
        <h1 className="text-3xl font-bold tracking-tight">{title}</h1>
        <p className="text-muted-foreground">{description}</p>
      </div>
      <div className="flex border border-dashed border-border rounded-xl h-64 items-center justify-center text-muted-foreground bg-muted/10">
        Страница в разработке...
      </div>
    </div>
  );
}

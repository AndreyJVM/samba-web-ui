import React from 'react';

function App() {
  return (
    <div className="flex bg-muted items-center justify-center min-h-screen">
      <div className="bg-background max-w-sm rounded-xl p-8 border border-border shadow-soft text-center flex flex-col gap-4">
        <h1 className="text-2xl font-bold tracking-tight">Samba Web UI</h1>
        <p className="text-muted-foreground text-sm">
          Frontend successfully scaffolded with React, Tailwind CSS, and Vite.
        </p>
        <button className="bg-primary text-primary-foreground font-medium rounded-md px-4 py-2 hover:opacity-90 transition-opacity">
          Get Started
        </button>
      </div>
    </div>
  );
}

export default App;

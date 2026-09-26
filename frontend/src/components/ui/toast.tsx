import React, { createContext, useContext, useState, ReactNode } from "react";
import { AlertCircle, CheckCircle, Info, X } from "lucide-react";

export type ToastType = "success" | "error" | "info";

export interface ToastMessage {
  id: string;
  type: ToastType;
  title: string;
  message?: string;
}

interface ToastContextType {
  toast: (type: ToastType, title: string, message?: string) => void;
  success: (title: string, message?: string) => void;
  error: (title: string, message?: string) => void;
  info: (title: string, message?: string) => void;
}

const ToastContext = createContext<ToastContextType | undefined>(undefined);

export const ToastProvider = ({ children }: { children: ReactNode }) => {
  const [toasts, setToasts] = useState<ToastMessage[]>([]);

  const addToast = (type: ToastType, title: string, message?: string) => {
    const id = Math.random().toString(36).substring(2, 9);
    setToasts((prev) => [...prev, { id, type, title, message }]);
    setTimeout(() => {
      setToasts((prev) => prev.filter((t) => t.id !== id));
    }, 5000); // 5 sec auto dismiss
  };

  const removeToast = (id: string) => {
    setToasts((prev) => prev.filter((t) => t.id !== id));
  };

  const contextValue = {
    toast: addToast,
    success: (title: string, message?: string) => addToast("success", title, message),
    error: (title: string, message?: string) => addToast("error", title, message),
    info: (title: string, message?: string) => addToast("info", title, message),
  };

  return (
    <ToastContext.Provider value={contextValue}>
      {children}
      <div className="fixed bottom-4 right-4 z-50 flex flex-col gap-2 pointer-events-none w-full max-w-sm">
        {toasts.map((t) => (
          <div
            key={t.id}
            className={`pointer-events-auto flex items-start gap-3 p-4 rounded-xl shadow-lg border animate-in slide-in-from-bottom-5 fade-in duration-300 ${
              t.type === "success"
                ? "bg-[#ecfdf5] border-[#a7f3d0] text-[#065f46]"
                : t.type === "error"
                ? "bg-[#fef2f2] border-[#fecaca] text-[#991b1b]"
                : "bg-white border-slate-200 text-slate-800"
            }`}
          >
            {t.type === "success" && <CheckCircle className="w-5 h-5 mt-0.5 shrink-0" />}
            {t.type === "error" && <AlertCircle className="w-5 h-5 mt-0.5 shrink-0" />}
            {t.type === "info" && <Info className="w-5 h-5 mt-0.5 text-blue-500 shrink-0" />}
            
            <div className="flex-1">
              <h4 className="text-[14px] font-bold">{t.title}</h4>
              {t.message && <p className="text-[13px] mt-0.5 opacity-90">{t.message}</p>}
            </div>
            
            <button
              onClick={() => removeToast(t.id)}
              className="p-1 rounded-md opacity-50 hover:opacity-100 transition-opacity"
            >
              <X className="w-4 h-4" />
            </button>
          </div>
        ))}
      </div>
    </ToastContext.Provider>
  );
};

export const useToast = () => {
  const context = useContext(ToastContext);
  if (!context) throw new Error("useToast must be used within ToastProvider");
  return context;
};

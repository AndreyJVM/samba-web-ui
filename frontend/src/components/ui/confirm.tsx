import React, { createContext, useContext, useState, ReactNode, useCallback } from "react";
import { AlertTriangle } from "lucide-react";

interface ConfirmOptions {
  title: string;
  message: string;
  confirmText?: string;
  cancelText?: string;
  destructive?: boolean;
}

interface ConfirmContextType {
  confirm: (options: ConfirmOptions) => Promise<boolean>;
}

const ConfirmContext = createContext<ConfirmContextType | undefined>(undefined);

export const ConfirmProvider = ({ children }: { children: ReactNode }) => {
  const [isOpen, setIsOpen] = useState(false);
  const [options, setOptions] = useState<ConfirmOptions | null>(null);
  const [resolve, setResolve] = useState<(value: boolean) => void>();

  const confirm = useCallback((opts: ConfirmOptions) => {
    setOptions(opts);
    setIsOpen(true);
    return new Promise<boolean>((res) => {
      setResolve(() => res);
    });
  }, []);

  const handleConfirm = () => {
    setIsOpen(false);
    resolve?.(true);
  };

  const handleCancel = () => {
    setIsOpen(false);
    resolve?.(false);
  };

  return (
    <ConfirmContext.Provider value={{ confirm }}>
      {children}
      {isOpen && options && (
        <div className="fixed inset-0 z-[100] flex items-center justify-center bg-slate-900/40 backdrop-blur-sm animate-in fade-in duration-200 p-4">
          <div className="bg-white rounded-2xl shadow-xl border border-slate-100 max-w-md w-full p-6 animate-in zoom-in-95 duration-200">
            <div className="flex gap-4 items-start">
              <div
                className={`p-3 rounded-full shrink-0 ${
                  options.destructive ? "bg-rose-100 text-rose-600" : "bg-amber-100 text-amber-600"
                }`}
              >
                <AlertTriangle className="w-6 h-6" />
              </div>
              <div className="flex-1 pt-1">
                <h3 className="text-lg font-bold text-slate-900 mb-1">{options.title}</h3>
                <p className="text-sm text-slate-500 leading-relaxed">{options.message}</p>
              </div>
            </div>
            
            <div className="mt-6 flex justify-end gap-3">
              <button
                onClick={handleCancel}
                className="px-4 py-2 text-sm font-medium text-slate-600 bg-slate-50 border border-slate-200 rounded-lg hover:bg-slate-100 transition-colors"
              >
                {options.cancelText || "Отмена"}
              </button>
              <button
                onClick={handleConfirm}
                className={`px-4 py-2 text-sm font-medium rounded-lg text-white transition-colors shadow-sm ${
                  options.destructive
                    ? "bg-rose-600 hover:bg-rose-700 shadow-rose-500/20"
                    : "bg-blue-600 hover:bg-blue-700 shadow-blue-500/20"
                }`}
              >
                {options.confirmText || "Подтвердить"}
              </button>
            </div>
          </div>
        </div>
      )}
    </ConfirmContext.Provider>
  );
};

export const useConfirm = () => {
  const context = useContext(ConfirmContext);
  if (!context) throw new Error("useConfirm must be used within ConfirmProvider");
  return context;
};

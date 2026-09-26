import React, { createContext, useContext, useState } from 'react';
import { useTranslation } from '../../lib/i18n';

interface ConfirmOptions {
  title: string;
  message: string;
  destructive?: boolean;
  confirmText?: string;
  cancelText?: string;
}

interface ConfirmContextType {
  confirm: (options: ConfirmOptions) => Promise<boolean>;
}

const ConfirmContext = createContext<ConfirmContextType | undefined>(undefined);

export function ConfirmProvider({ children }: { children: React.ReactNode }) {
  const [isOpen, setIsOpen] = useState(false);
  const [options, setOptions] = useState<ConfirmOptions | null>(null);
  const [resolveFn, setResolveFn] = useState<((value: boolean) => void) | null>(null);

  const { t } = useTranslation();

  const confirm = (opts: ConfirmOptions): Promise<boolean> => {
    setOptions(opts);
    setIsOpen(true);
    return new Promise((resolve) => {
      setResolveFn(() => resolve);
    });
  };

  const handleClose = (value: boolean) => {
    setIsOpen(false);
    if (resolveFn) resolveFn(value);
  };

  return (
    <ConfirmContext.Provider value={{ confirm }}>
      {children}
      {isOpen && options && (
        <div className="fixed inset-0 z-50 flex items-center justify-center p-4">
          <div className="absolute inset-0 bg-background/80 backdrop-blur-sm" onClick={() => handleClose(false)} />
          <div className="relative bg-surface border border-border shadow-subtle rounded-lg max-w-sm w-full p-6 animate-in zoom-in-95 duration-200">
            <h2 className="text-lg font-semibold text-foreground mb-2">{options.title}</h2>
            <p className="text-sm text-status-disabled mb-6">{options.message}</p>
            <div className="flex justify-end gap-3">
              <button
                onClick={() => handleClose(false)}
                className="px-4 py-2 text-[13px] font-medium text-foreground bg-surface border border-border rounded-md hover:bg-surface-hover shadow-sm-subtle transition-colors"
              >
                {options.cancelText || t("common.cancel")}
              </button>
              <button
                onClick={() => handleClose(true)}
                className={`px-4 py-2 text-[13px] font-medium rounded-md shadow-sm transition-colors ${
                  options.destructive
                    ? 'bg-status-error hover:bg-status-error/90 text-white'
                    : 'bg-brand hover:bg-brand-hover text-brand-text'
                }`}
              >
                {options.confirmText || t("common.confirm")}
              </button>
            </div>
          </div>
        </div>
      )}
    </ConfirmContext.Provider>
  );
}

export function useConfirm() {
  const context = useContext(ConfirmContext);
  if (!context) throw new Error('useConfirm must be used within ConfirmProvider');
  return context;
}

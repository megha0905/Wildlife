import { createContext, useCallback, useContext, useState, type ReactNode } from 'react';

type Kind = 'info' | 'error' | 'warn';
interface Toast { id: number; text: string; kind: Kind; }

const ToastContext = createContext<(text: string, kind?: Kind) => void>(() => {});

export function ToastProvider({ children }: { children: ReactNode }) {
  const [toasts, setToasts] = useState<Toast[]>([]);
  const push = useCallback((text: string, kind: Kind = 'info') => {
    const id = Date.now() + Math.random();
    setToasts((t) => [...t, { id, text, kind }]);
    setTimeout(() => setToasts((t) => t.filter((x) => x.id !== id)), kind === 'info' ? 3500 : 7000);
  }, []);
  return (
    <ToastContext.Provider value={push}>
      {children}
      <div className="toasts" role="status" aria-live="polite">
        {toasts.map((t) => <div key={t.id} className={`toast toast-${t.kind}`}>{t.text}</div>)}
      </div>
    </ToastContext.Provider>
  );
}

/** toast("Saved"), toast(msg, "error"); warnings from the API can be shown with kind "warn". */
export function useToast() {
  return useContext(ToastContext);
}

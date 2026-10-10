import { useEffect, useRef, type ReactNode } from 'react';

interface Props {
  title: string;
  open: boolean;
  onClose: () => void;
  children: ReactNode;
  footer?: ReactNode;
  wide?: boolean;
}

/** Native <dialog> modal: focus trapping, Escape to close and backdrop come for free. */
export function Modal({ title, open, onClose, children, footer, wide }: Props) {
  const ref = useRef<HTMLDialogElement>(null);

  useEffect(() => {
    const dlg = ref.current;
    if (!dlg) return;
    if (open && !dlg.open) dlg.showModal();
    if (!open && dlg.open) dlg.close();
  }, [open]);

  return (
    <dialog ref={ref} className={wide ? 'modal wide' : 'modal'} onClose={onClose} onCancel={onClose}>
      {open && (
        <>
          <div className="modal-head">
            <h2>{title}</h2>
            <button className="icon-btn" aria-label="Close" onClick={onClose}>✕</button>
          </div>
          <div className="modal-body">{children}</div>
          {footer && <div className="modal-foot">{footer}</div>}
        </>
      )}
    </dialog>
  );
}

/** Slide-in side panel for record details. */
export function Drawer({ open, onClose, children, accent }: { open: boolean; onClose: () => void; children: ReactNode; accent?: string }) {
  useEffect(() => {
    if (!open) return;
    const onKey = (e: KeyboardEvent) => { if (e.key === 'Escape' && !document.querySelector('dialog[open]')) onClose(); };
    document.addEventListener('keydown', onKey);
    return () => document.removeEventListener('keydown', onKey);
  }, [open, onClose]);
  if (!open) return null;
  return (
    <>
      <div className="drawer-backdrop" onClick={onClose} />
      <aside className="drawer" role="dialog" aria-modal="true">
        {accent && <div className="drawer-accent" style={{ background: accent }} />}
        <button className="icon-btn drawer-close" aria-label="Close" onClick={onClose}>✕</button>
        {children}
      </aside>
    </>
  );
}

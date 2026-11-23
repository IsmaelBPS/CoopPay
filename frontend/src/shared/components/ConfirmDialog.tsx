import { useEffect, useId, useRef } from 'react';

interface ConfirmDialogProps {
  open: boolean;
  title: string;
  description: string;
  confirmLabel?: string;
  cancelLabel?: string;
  isSubmitting?: boolean;
  onConfirm: () => void;
  onCancel: () => void;
}

export function ConfirmDialog({
  open,
  title,
  description,
  confirmLabel = 'Confirmar',
  cancelLabel = 'Cancelar',
  isSubmitting = false,
  onConfirm,
  onCancel,
}: ConfirmDialogProps) {
  const dialogRef = useRef<HTMLDialogElement>(null);
  const titleId = useId();

  useEffect(() => {
    const dialog = dialogRef.current;
    if (!dialog) return;

    if (open && !dialog.open) {
      dialog.showModal();
    } else if (!open && dialog.open) {
      dialog.close();
    }
  }, [open]);

  const handleClose = () => {
    const dialog = dialogRef.current;
    if (!dialog) return;
    const outcome = dialog.returnValue;
    dialog.returnValue = '';

    if (outcome === 'confirm') {
      onConfirm();
    } else {
      onCancel();
    }
  };

  return (
    <dialog
      ref={dialogRef}
      aria-labelledby={titleId}
      className="coop-dialog"
      onClose={handleClose}
    >
      <div className="coop-dialog-body">
        <h3 id={titleId} className="coop-dialog-title">
          {title}
        </h3>
        <p className="coop-dialog-description">{description}</p>
        <form method="dialog" className="coop-dialog-actions">
          <button
            type="submit"
            value="cancel"
            autoFocus
            className="coop-btn coop-btn-secondary"
            disabled={isSubmitting}
          >
            {cancelLabel}
          </button>
          <button
            type="submit"
            value="confirm"
            className="coop-btn coop-btn-danger"
            disabled={isSubmitting}
          >
            {confirmLabel}
          </button>
        </form>
      </div>
    </dialog>
  );
}

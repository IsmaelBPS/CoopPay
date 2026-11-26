import { useEffect, useId, useRef, type FormEvent, type SyntheticEvent } from 'react';
import { closeTopLayer, supportsTopLayerExit } from '../utils/motion';

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

    if (open) {
      dialog.removeAttribute('data-closing');
      if (!dialog.open) {
        dialog.showModal();
      }
    } else if (dialog.open) {
      void closeTopLayer(dialog);
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

  const handleCancel = (event: SyntheticEvent<HTMLDialogElement>) => {
    if (!supportsTopLayerExit()) {
      event.preventDefault();
      void closeTopLayer(event.currentTarget, 'cancel');
    }
  };

  const handleFormSubmit = (event: FormEvent<HTMLFormElement>) => {
    if (!supportsTopLayerExit() && dialogRef.current) {
      event.preventDefault();
      const submitter = (event.nativeEvent as SubmitEvent).submitter as HTMLButtonElement | null;
      void closeTopLayer(dialogRef.current, submitter?.value ?? 'cancel');
    }
  };

  return (
    <dialog
      ref={dialogRef}
      aria-labelledby={titleId}
      className="coop-dialog"
      onCancel={handleCancel}
      onClose={handleClose}
    >
      <div className="coop-dialog-body">
        <h3 id={titleId} className="coop-dialog-title">
          {title}
        </h3>
        <p className="coop-dialog-description">{description}</p>
        <form method="dialog" className="coop-dialog-actions" onSubmit={handleFormSubmit}>
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

let supportsDisplayTransition: boolean | undefined;

/**
 * Probes whether the browser genuinely transitions `display` with
 * `transition-behavior: allow-discrete` (Firefox 129+ parses it but drops exit transitions).
 */
export function canTransitionDisplay(): boolean {
  if (supportsDisplayTransition !== undefined) {
    return supportsDisplayTransition;
  }
  if (
    typeof window === 'undefined' ||
    !window.CSS?.supports?.('transition-behavior', 'allow-discrete') ||
    !document.body
  ) {
    return false;
  }

  const probe = document.createElement('div');
  probe.style.cssText = 'transition: display 1s allow-discrete !important; display: block;';
  document.body.appendChild(probe);
  getComputedStyle(probe).display;
  probe.style.display = 'none';
  supportsDisplayTransition = getComputedStyle(probe).display === 'block';
  probe.remove();
  return supportsDisplayTransition;
}

export function supportsTopLayerExit(): boolean {
  return (
    typeof window !== 'undefined' &&
    Boolean(window.CSS?.supports?.('overlay', 'auto')) &&
    canTransitionDisplay()
  );
}

/**
 * Closes a native `<dialog>` after letting its top-layer exit animation finish
 * on browsers that lack `overlay` or discrete `display` transitions.
 */
export async function closeTopLayer(
  dialog: HTMLDialogElement,
  returnValue?: string,
): Promise<void> {
  if (!dialog.open || dialog.hasAttribute('data-closing')) {
    return;
  }

  if (!supportsTopLayerExit()) {
    dialog.setAttribute('data-closing', '');
    const animations = dialog.getAnimations({ subtree: true });
    if (animations.length > 0) {
      await Promise.race([
        Promise.allSettled(animations.map((a) => a.finished)),
        new Promise((resolve) => setTimeout(resolve, 600)),
      ]);
    }
    if (!dialog.hasAttribute('data-closing')) {
      return;
    }
    dialog.removeAttribute('data-closing');
  }

  if (dialog.open) {
    if (returnValue !== undefined) {
      dialog.close(returnValue);
    } else {
      dialog.close();
    }
  }
}

import { useEffect, type RefObject } from 'react';

let sharedAnnouncer: HTMLDivElement | null = null;
let announceTimer: ReturnType<typeof setTimeout> | undefined;
let announcedField: Element | null = null;

function getAnnouncer(): HTMLDivElement {
  if (sharedAnnouncer && document.body.contains(sharedAnnouncer)) {
    return sharedAnnouncer;
  }
  const el = document.createElement('div');
  el.setAttribute('aria-live', 'polite');
  el.className = 'visually-hidden';
  document.body.append(el);
  sharedAnnouncer = el;
  return el;
}

function spokenText(node: HTMLElement): string {
  const copy = node.cloneNode(true) as HTMLElement;
  copy.querySelectorAll('[aria-hidden="true"]').forEach((n) => n.remove());
  copy.querySelectorAll('[role="img"][aria-label]').forEach((n) => {
    n.replaceWith(`${n.getAttribute('aria-label')}: `);
  });
  return (copy.textContent ?? '').replace(/\s+/g, ' ').trim();
}

function linkError(el: HTMLElement, shown: boolean): void {
  const errorId = el.dataset.error;
  if (!errorId) return;
  const ids = (el.getAttribute('aria-describedby') ?? '')
    .split(/\s+/)
    .filter((id) => id && id !== errorId);
  if (shown) {
    ids.push(errorId);
  }
  if (ids.length > 0) {
    el.setAttribute('aria-describedby', ids.join(' '));
  } else {
    el.removeAttribute('aria-describedby');
  }
}

function announceError(el: HTMLInputElement | HTMLSelectElement | HTMLTextAreaElement): void {
  const errorId = el.dataset.error;
  if (!errorId) return;
  const errorEl = document.getElementById(errorId);
  if (!errorEl) return;

  const firstLabel = el.labels?.[0];
  const labelPrefix = firstLabel ? `${spokenText(firstLabel)}: ` : '';
  const announcer = getAnnouncer();

  clearTimeout(announceTimer);
  announcer.textContent = '';
  announcedField = el;
  announceTimer = setTimeout(() => {
    announcer.textContent = labelPrefix + spokenText(errorEl);
  }, 500);
}

type FormControl = HTMLInputElement | HTMLSelectElement | HTMLTextAreaElement;

function isFormControl(target: EventTarget | null): target is FormControl {
  return (
    target instanceof HTMLInputElement ||
    target instanceof HTMLSelectElement ||
    target instanceof HTMLTextAreaElement
  );
}

export function useAccessibleFormValidation(
  formRef: RefObject<HTMLFormElement | null>,
  rebindKey?: unknown,
): void {
  useEffect(() => {
    const form = formRef.current;
    if (!form) return;

    const supportsUserInvalid =
      typeof CSS !== 'undefined' &&
      typeof CSS.supports === 'function' &&
      CSS.supports('selector(:user-invalid)');

    const flagged = new WeakSet<FormControl>();
    const announced = new WeakSet<FormControl>();
    const dirtyState = new WeakMap<FormControl, { hasInteracted: boolean; hasBlurred: boolean }>();

    const syncAria = (el: FormControl, announce = false, forceInvalid?: boolean) => {
      const invalid =
        forceInvalid !== undefined
          ? forceInvalid
          : supportsUserInvalid
            ? el.matches(':user-invalid')
            : !el.validity.valid;

      if (!supportsUserInvalid) {
        const state = dirtyState.get(el);
        const showFallback = Boolean(state?.hasBlurred || forceInvalid);
        if (showFallback) {
          el.classList.toggle('user-invalid-fallback', invalid);
          el.classList.toggle('user-valid-fallback', !invalid);
        } else {
          el.classList.remove('user-invalid-fallback', 'user-valid-fallback');
        }
      }

      if (invalid) {
        if (announce && !announced.has(el)) {
          announceError(el);
          announced.add(el);
        }
        el.setAttribute('aria-invalid', 'true');
        linkError(el, true);
        flagged.add(el);
      } else if (flagged.has(el) || el.getAttribute('aria-invalid') !== 'true') {
        el.setAttribute('aria-invalid', 'false');
        linkError(el, false);
        flagged.delete(el);
        announced.delete(el);
        if (announcedField === el) {
          clearTimeout(announceTimer);
          getAnnouncer().textContent = '';
        }
      }
    };

    const initializeControls = () => {
      form.querySelectorAll<FormControl>('input, select, textarea').forEach((el) => {
        if (!el.hasAttribute('aria-invalid')) {
          el.setAttribute('aria-invalid', 'false');
        }
      });
    };

    initializeControls();

    const handleBlur = (e: FocusEvent) => {
      if (!isFormControl(e.target)) return;
      if (!supportsUserInvalid) {
        const state = dirtyState.get(e.target) ?? { hasInteracted: false, hasBlurred: false };
        state.hasBlurred = true;
        dirtyState.set(e.target, state);
        if (state.hasInteracted) {
          syncAria(e.target, true, !e.target.validity.valid);
        }
        return;
      }
      syncAria(e.target, true);
    };

    const handleInput = (e: Event) => {
      if (!isFormControl(e.target)) return;
      if (!supportsUserInvalid) {
        const state = dirtyState.get(e.target) ?? { hasInteracted: false, hasBlurred: false };
        state.hasInteracted = true;
        dirtyState.set(e.target, state);
        if (state.hasBlurred) {
          syncAria(e.target, false, !e.target.validity.valid);
        }
        return;
      }
      if (e.target.hasAttribute('aria-invalid')) {
        syncAria(e.target, false);
      }
    };

    const handleChange = (e: Event) => {
      if (!isFormControl(e.target)) return;
      if (!supportsUserInvalid) {
        const state = dirtyState.get(e.target) ?? { hasInteracted: false, hasBlurred: false };
        state.hasInteracted = true;
        state.hasBlurred = true;
        dirtyState.set(e.target, state);
        syncAria(e.target, true, !e.target.validity.valid);
        return;
      }
      syncAria(e.target, true);
    };

    const handleInvalid = (e: Event) => {
      if (!isFormControl(e.target)) return;
      if (!supportsUserInvalid) {
        dirtyState.set(e.target, { hasInteracted: true, hasBlurred: true });
      }
      announced.add(e.target);
      syncAria(e.target, false, !supportsUserInvalid ? true : undefined);
    };

    const handleReset = () => {
      setTimeout(() => {
        form.querySelectorAll<FormControl>('input, select, textarea').forEach((el) => {
          dirtyState.delete(el);
          flagged.delete(el);
          announced.delete(el);
          el.classList.remove('user-invalid-fallback', 'user-valid-fallback');
          el.setAttribute('aria-invalid', 'false');
          linkError(el, false);
        });
      });
    };

    form.addEventListener('blur', handleBlur, true);
    form.addEventListener('input', handleInput);
    form.addEventListener('change', handleChange, true);
    form.addEventListener('invalid', handleInvalid, true);
    form.addEventListener('reset', handleReset, true);

    return () => {
      form.removeEventListener('blur', handleBlur, true);
      form.removeEventListener('input', handleInput);
      form.removeEventListener('change', handleChange, true);
      form.removeEventListener('invalid', handleInvalid, true);
      form.removeEventListener('reset', handleReset, true);
    };
  }, [formRef, rebindKey]);
}

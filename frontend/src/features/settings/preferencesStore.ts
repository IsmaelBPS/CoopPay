import { create } from 'zustand';
import { persist } from 'zustand/middleware';

export type ThemeMode = 'light' | 'dark';

export interface PreferencesState {
  themeMode: ThemeMode;
  setThemeMode: (mode: ThemeMode) => void;
  toggleThemeMode: () => void;
}

export const usePreferencesStore = create<PreferencesState>()(
  persist(
    (set) => ({
      themeMode: 'light',
      setThemeMode: (themeMode) => set({ themeMode }),
      toggleThemeMode: () =>
        set((state) => ({
          themeMode: state.themeMode === 'light' ? 'dark' : 'light',
        })),
    }),
    {
      name: 'cooppay.preferences',
    },
  ),
);

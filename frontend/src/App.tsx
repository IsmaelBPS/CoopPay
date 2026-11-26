import { Theme } from '@astryxdesign/core/theme';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { useEffect } from 'react';
import { BrowserRouter, Navigate, Route, Routes } from 'react-router-dom';
import { cooppayTheme } from './design/cooppayTheme';
import { AccountsPage } from './features/accounts/AccountsPage';
import { AssociatesPage } from './features/associates/AssociatesPage';
import { LoginPage } from './features/auth/LoginPage';
import { RequireAuth } from './features/auth/RequireAuth';
import { LedgerPage } from './features/ledger/LedgerPage';
import { SettingsPage } from './features/settings/SettingsPage';
import { usePreferencesStore } from './features/settings/preferencesStore';
import { AppLayout } from './shared/components/AppLayout';

const queryClient = new QueryClient({
  defaultOptions: {
    queries: {
      refetchOnWindowFocus: false,
    },
  },
});

export function App() {
  const themeMode = usePreferencesStore((state) => state.themeMode);

  useEffect(() => {
    document.documentElement.dataset.theme = themeMode;
  }, [themeMode]);

  return (
    <QueryClientProvider client={queryClient}>
      <Theme theme={cooppayTheme} mode={themeMode}>
        <BrowserRouter>
          <Routes>
            <Route path="/login" element={<LoginPage />} />
            <Route element={<RequireAuth />}>
              <Route element={<AppLayout />}>
                <Route path="/" element={<Navigate to="/associates" replace />} />
                <Route path="/associates" element={<AssociatesPage />} />
                <Route path="/accounts" element={<AccountsPage />} />
                <Route path="/ledger" element={<LedgerPage />} />
                <Route path="/settings" element={<SettingsPage />} />
                <Route path="*" element={<Navigate to="/associates" replace />} />
              </Route>
            </Route>
          </Routes>
        </BrowserRouter>
      </Theme>
    </QueryClientProvider>
  );
}

export default App;

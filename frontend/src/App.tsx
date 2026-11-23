import { Theme } from '@astryxdesign/core/theme';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { BrowserRouter, Navigate, Route, Routes } from 'react-router-dom';
import { cooppayTheme } from './design/cooppayTheme';
import { AccountsPage } from './features/accounts/AccountsPage';
import { AssociatesPage } from './features/associates/AssociatesPage';
import { LedgerPage } from './features/ledger/LedgerPage';
import { AppLayout } from './shared/components/AppLayout';

const queryClient = new QueryClient({
  defaultOptions: {
    queries: {
      refetchOnWindowFocus: false,
    },
  },
});

export function App() {
  return (
    <QueryClientProvider client={queryClient}>
      <Theme theme={cooppayTheme} mode="light">
        <BrowserRouter>
          <Routes>
            <Route element={<AppLayout />}>
              <Route path="/" element={<Navigate to="/associates" replace />} />
              <Route path="/associates" element={<AssociatesPage />} />
              <Route path="/accounts" element={<AccountsPage />} />
              <Route path="/ledger" element={<LedgerPage />} />
              <Route path="*" element={<Navigate to="/associates" replace />} />
            </Route>
          </Routes>
        </BrowserRouter>
      </Theme>
    </QueryClientProvider>
  );
}

export default App;

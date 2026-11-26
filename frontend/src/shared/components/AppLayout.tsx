import { Badge } from '@astryxdesign/core/Badge';
import { Button } from '@astryxdesign/core/Button';
import { Text } from '@astryxdesign/core/Text';
import { NavLink, Outlet, useLocation, useNavigate } from 'react-router-dom';
import { useAuthStore } from '../../features/auth/authStore';

const NAV_ITEMS = [
  { to: '/associates', label: 'Associados', tag: 'API ativa' },
  { to: '/accounts', label: 'Contas & Saldo', tag: 'Preview' },
  { to: '/ledger', label: 'Extrato / Ledger', tag: 'Preview' },
] as const;

const ROUTE_META: Record<string, { title: string; subtitle: string }> = {
  '/associates': {
    title: 'Associados',
    subtitle: 'Cadastro, consulta por ID, atualização e remoção no núcleo CoopPay',
  },
  '/accounts': {
    title: 'Contas & Saldo',
    subtitle: 'Visão de saldo cooperativo e operações financeiras com controle de concorrência',
  },
  '/ledger': {
    title: 'Extrato / Ledger',
    subtitle: 'Rastreabilidade contábil de créditos, débitos e transferências',
  },
};

export function AppLayout() {
  const { pathname } = useLocation();
  const navigate = useNavigate();
  const user = useAuthStore((state) => state.user);
  const authMode = useAuthStore((state) => state.authMode);
  const logout = useAuthStore((state) => state.logout);
  const meta = ROUTE_META[pathname] ?? ROUTE_META['/associates'];

  return (
    <div className="coop-shell">
      <aside className="coop-sidebar" aria-label="Navegação principal">
        <div className="coop-brand">
          <div className="coop-brand-mark" aria-hidden="true">
            C
          </div>
          <div>
            <div className="coop-brand-title">CoopPay</div>
            <span className="coop-brand-sub">Núcleo Financeiro</span>
          </div>
        </div>

        <nav className="coop-nav">
          {NAV_ITEMS.map((item) => (
            <NavLink
              key={item.to}
              to={item.to}
              className={({ isActive }) =>
                `coop-nav-link${isActive ? ' is-active' : ''}`
              }
            >
              <span className="coop-nav-link-inner">
                <span className="sicredi-dot" aria-hidden="true" />
                <span>{item.label}</span>
              </span>
              <span className="coop-nav-tag">{item.tag}</span>
            </NavLink>
          ))}
        </nav>

        <div className="coop-sidebar-footer">
          <strong>Backend Spring Boot</strong>
          <br />
          Proxy ativo em <code>/api/v1</code> → <code>localhost:3499</code>
        </div>
      </aside>

      <div className="coop-main-column">
        <header className="coop-topbar">
          <div>
            <h1 className="coop-page-title">{meta.title}</h1>
            <p className="coop-page-subtitle">{meta.subtitle}</p>
          </div>
          <div style={{ display: 'flex', alignItems: 'center', gap: 10, flexWrap: 'wrap' }}>
            {user && (
              <Text type="supporting" weight="semibold">
                {user.name || user.email}
              </Text>
            )}
            {authMode === 'preview' && <Badge variant="neutral" label="Sessão local" />}
            <Badge variant="green" label="Astryx + Sicredi" />
            <Button
              variant="secondary"
              size="sm"
              label="Sair"
              onClick={() => {
                logout();
                navigate('/login', { replace: true });
              }}
            />
          </div>
        </header>

        <main className="coop-content">
          <Outlet />
        </main>
      </div>
    </div>
  );
}

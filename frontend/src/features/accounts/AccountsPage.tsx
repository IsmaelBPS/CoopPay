import { Badge } from '@astryxdesign/core/Badge';
import { Banner } from '@astryxdesign/core/Banner';
import { Button } from '@astryxdesign/core/Button';
import { Card } from '@astryxdesign/core/Card';
import { formatCurrencyBRL } from '../../shared/utils/format';

const PREVIEW_ACCOUNTS = [
  {
    id: 'cta-001',
    owner: 'Maria Silva',
    type: 'Conta Corrente Cooperativa',
    balance: 14850.75,
    status: 'Ativa',
  },
  {
    id: 'cta-002',
    owner: 'AgroSul Cooperativa Ltda.',
    type: 'Conta Empresarial',
    balance: 92410.0,
    status: 'Ativa',
  },
] as const;

export function AccountsPage() {
  return (
    <>
      <Banner
        status="info"
        title="Modo preview — endpoints de Conta ainda não expostos no backend"
        description="AccountController.java ainda está vazio no Spring Boot. Esta tela já está estruturada com o design system Sicredi para conectar depósito, saque e transferência assim que as rotas REST forem criadas."
      />

      <div className="coop-grid-2">
        {PREVIEW_ACCOUNTS.map((account) => (
          <Card key={account.id} padding={5}>
            <div className="coop-card-header">
              <div>
                <h2 className="coop-card-title">
                  <span className="sicredi-dot" aria-hidden="true" />
                  {account.owner}
                </h2>
                <p className="coop-hint" style={{ marginTop: 4, marginBottom: 0 }}>
                  {account.type} · <code>{account.id}</code>
                </p>
              </div>
              <Badge variant="green" label={account.status} />
            </div>

            <div
              style={{
                padding: '16px',
                borderRadius: 'var(--radius-md)',
                backgroundColor: 'var(--color-brand-tint)',
                border: '1px solid var(--color-border-subtle)',
                marginBottom: 16,
              }}
            >
              <div style={{ fontSize: '0.8125rem', color: 'var(--color-ink-muted)', fontWeight: 700 }}>
                Saldo disponível
              </div>
              <div
                className="tabular-nums"
                style={{
                  fontFamily: 'var(--font-display)',
                  fontSize: '2rem',
                  fontWeight: 600,
                  color: 'var(--color-brand-primary)',
                  marginTop: 4,
                }}
              >
                {formatCurrencyBRL(account.balance)}
              </div>
            </div>

            <div style={{ display: 'flex', gap: 10, flexWrap: 'wrap' }}>
              <Button variant="primary" label="Depositar" isDisabled />
              <Button variant="secondary" label="Transferir" isDisabled />
              <Button variant="ghost" label="Sacar" isDisabled />
            </div>
          </Card>
        ))}
      </div>

      <div className="coop-grid-3">
        <Card padding={4}>
          <h3 className="coop-card-title" style={{ fontSize: '1.05rem', marginBottom: 8 }}>
            Depósito
          </h3>
          <p className="coop-hint" style={{ margin: 0 }}>
            Crédito direto com registro automático de entrada no ledger e atualização concorrente de saldo.
          </p>
        </Card>
        <Card padding={4}>
          <h3 className="coop-card-title" style={{ fontSize: '1.05rem', marginBottom: 8 }}>
            Transferência P2P
          </h3>
          <p className="coop-hint" style={{ margin: 0 }}>
            Movimentação atômica entre contas de associados com bloqueio otimista/pessimista no PostgreSQL.
          </p>
        </Card>
        <Card padding={4}>
          <h3 className="coop-card-title" style={{ fontSize: '1.05rem', marginBottom: 8 }}>
            Eventos Kafka
          </h3>
          <p className="coop-hint" style={{ margin: 0 }}>
            Publicação assíncrona de confirmações e rastreabilidade distribuída após commit transacional.
          </p>
        </Card>
      </div>
    </>
  );
}

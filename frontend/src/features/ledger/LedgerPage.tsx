import { Banner } from '@astryxdesign/core/Banner';
import { Card } from '@astryxdesign/core/Card';
import { formatCurrencyBRL, formatDateTimeBR } from '../../shared/utils/format';

interface PreviewLedgerRow {
  id: string;
  operation: 'DEPOSIT' | 'TRANSFER_IN' | 'TRANSFER_OUT' | 'WITHDRAWAL';
  entryType: 'CREDIT' | 'DEBIT';
  associateName: string;
  occurredAt: string;
  amount: number;
}

const PREVIEW_LEDGER: PreviewLedgerRow[] = [
  {
    id: 'LDG-9041',
    operation: 'DEPOSIT',
    entryType: 'CREDIT',
    associateName: 'Maria Silva',
    occurredAt: '2026-11-22T18:10:00-03:00',
    amount: 2500.0,
  },
  {
    id: 'LDG-9040',
    operation: 'TRANSFER_OUT',
    entryType: 'DEBIT',
    associateName: 'Maria Silva → AgroSul Cooperativa',
    occurredAt: '2026-11-22T16:42:00-03:00',
    amount: 640.5,
  },
  {
    id: 'LDG-9039',
    operation: 'TRANSFER_IN',
    entryType: 'CREDIT',
    associateName: 'AgroSul Cooperativa Ltda.',
    occurredAt: '2026-11-22T14:15:00-03:00',
    amount: 4200.0,
  },
  {
    id: 'LDG-9038',
    operation: 'WITHDRAWAL',
    entryType: 'DEBIT',
    associateName: 'João Pedro Almeida',
    occurredAt: '2026-11-21T11:05:00-03:00',
    amount: 310.0,
  },
];

const OPERATION_LABELS: Record<PreviewLedgerRow['operation'], string> = {
  DEPOSIT: 'Depósito',
  TRANSFER_IN: 'Transferência recebida',
  TRANSFER_OUT: 'Transferência enviada',
  WITHDRAWAL: 'Saque',
};

export function LedgerPage() {
  return (
    <>
      <Banner
        status="info"
        title="Modo preview — aguardando controller de extrato (LedgerEntry)"
        description="O domínio LedgerEntry já existe no backend, mas ainda não possui rota REST. A tabela responsiva abaixo já segue o padrão contábil com números tabulares."
      />

      <Card padding={4}>
        <div
          tabIndex={0}
          role="region"
          aria-labelledby="ledger-table-caption"
          className="table-wrapper"
        >
          <table className="coop-table">
            <caption id="ledger-table-caption">
              Lançamentos recentes do núcleo financeiro
            </caption>
            <thead>
              <tr>
                <th scope="col">Lançamento</th>
                <th scope="col">Operação</th>
                <th scope="col">Associado</th>
                <th scope="col">Data</th>
                <th scope="col" style={{ textAlign: 'right' }}>
                  Valor
                </th>
              </tr>
            </thead>
            <tbody>
              {PREVIEW_LEDGER.map((row) => {
                const isCredit = row.entryType === 'CREDIT';
                return (
                  <tr key={row.id}>
                    <th scope="row" className="tabular-nums">
                      {row.id}
                    </th>
                    <td>
                      <span
                        className={`coop-pill ${isCredit ? 'coop-pill-credit' : 'coop-pill-debit'}`}
                      >
                        {OPERATION_LABELS[row.operation]}
                      </span>
                    </td>
                    <td>{row.associateName}</td>
                    <td className="tabular-nums">{formatDateTimeBR(row.occurredAt)}</td>
                    <td
                      className="tabular-nums"
                      style={{
                        textAlign: 'right',
                        fontWeight: 700,
                        color: isCredit ? 'var(--color-brand-primary)' : 'var(--color-danger)',
                      }}
                    >
                      {isCredit ? '+' : '−'} {formatCurrencyBRL(row.amount)}
                    </td>
                  </tr>
                );
              })}
            </tbody>
          </table>
        </div>
      </Card>
    </>
  );
}

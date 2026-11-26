import { Banner } from '@astryxdesign/core/Banner';
import { Button } from '@astryxdesign/core/Button';
import { Card } from '@astryxdesign/core/Card';
import { HStack } from '@astryxdesign/core/HStack';
import { Selector } from '@astryxdesign/core/Selector';
import { Table } from '@astryxdesign/core/Table';
import { Text } from '@astryxdesign/core/Text';
import { TextInput } from '@astryxdesign/core/TextInput';
import { VStack } from '@astryxdesign/core/VStack';
import { useQuery } from '@tanstack/react-query';
import { useState, type FormEvent } from 'react';
import { formatCurrencyBRL, formatDateTimeBR } from '../../shared/utils/format';
import { getLedgerByAccount, getLedgerByTransaction } from './api';

type SearchMode = 'account' | 'transaction';

const MODE_OPTIONS = [
  { value: 'account', label: 'Por conta (accountId)' },
  { value: 'transaction', label: 'Por transação (transactionId)' },
];

export function LedgerPage() {
  const [mode, setMode] = useState<SearchMode>('account');
  const [input, setInput] = useState('');
  const [search, setSearch] = useState<{ mode: SearchMode; id: string } | null>(null);

  const query = useQuery({
    queryKey: ['ledger', search?.mode, search?.id],
    queryFn: () =>
      search!.mode === 'account' ? getLedgerByAccount(search!.id) : getLedgerByTransaction(search!.id),
    enabled: search !== null,
    retry: false,
  });

  const handleSubmit = (event: FormEvent) => {
    event.preventDefault();
    const id = input.trim();
    if (id) setSearch({ mode, id });
  };

  return (
    <>
      <Card padding={4}>
        <form noValidate onSubmit={handleSubmit}>
          <VStack gap={4}>
            <Selector
              label="Tipo de consulta"
              options={MODE_OPTIONS}
              value={mode}
              onChange={(value) => setMode(value as SearchMode)}
            />
            <TextInput
              label="UUID"
              description={`GET /ledger/${mode === 'account' ? 'accounts' : 'transactions'}/{id}`}
              isRequired
              value={input}
              onChange={setInput}
            />
            <HStack hAlign="end">
              <Button type="submit" variant="primary" label="Consultar extrato" isLoading={query.isFetching} />
            </HStack>
          </VStack>
        </form>
      </Card>

      {query.isError && (
        <Banner status="error" title="Não foi possível carregar o extrato" description={query.error.message} />
      )}

      {query.data && (
        <Card padding={4}>
          <Table
            data={query.data}
            idKey="id"
            columns={[
              {
                key: 'id',
                header: 'Lançamento',
                renderCell: (row) => <Text hasTabularNumbers>{row.id.slice(0, 8)}</Text>,
              },
              { key: 'operationType', header: 'Operação' },
              { key: 'description', header: 'Descrição', renderCell: (row) => row.description ?? '—' },
              {
                key: 'createdAt',
                header: 'Data',
                renderCell: (row) => <Text hasTabularNumbers>{formatDateTimeBR(row.createdAt)}</Text>,
              },
              {
                key: 'amount',
                header: 'Valor',
                align: 'end',
                renderCell: (row) => {
                  const isCredit = row.entryType === 'CREDIT';
                  return (
                    <Text weight="bold" hasTabularNumbers color={isCredit ? 'accent' : 'primary'}>
                      {isCredit ? '+' : '−'} {formatCurrencyBRL(row.amount)}
                    </Text>
                  );
                },
              },
              {
                key: 'balanceAfter',
                header: 'Saldo',
                align: 'end',
                renderCell: (row) => <Text hasTabularNumbers>{formatCurrencyBRL(row.balanceAfter)}</Text>,
              },
            ]}
          />
          {query.data.length === 0 && <Text>Nenhum lançamento encontrado.</Text>}
        </Card>
      )}
    </>
  );
}

import { Banner } from '@astryxdesign/core/Banner';
import { Button } from '@astryxdesign/core/Button';
import { Card } from '@astryxdesign/core/Card';
import { Grid } from '@astryxdesign/core/Grid';
import { Heading } from '@astryxdesign/core/Heading';
import { HStack } from '@astryxdesign/core/HStack';
import { Text } from '@astryxdesign/core/Text';
import { TextInput } from '@astryxdesign/core/TextInput';
import { VStack } from '@astryxdesign/core/VStack';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { useState } from 'react';
import { formatCurrencyBRL } from '../../shared/utils/format';
import { deposit, getAccount, openAccount, transfer, withdraw, type AccountResponse } from './api';

export function AccountsPage() {
  const queryClient = useQueryClient();
  const [accountId, setAccountId] = useState('');
  const [lookupInput, setLookupInput] = useState('');
  const [associateId, setAssociateId] = useState('');
  const [amount, setAmount] = useState('');
  const [targetId, setTargetId] = useState('');
  const [feedback, setFeedback] = useState<{ status: 'success' | 'error'; text: string } | null>(null);

  const accountQuery = useQuery({
    queryKey: ['account', accountId],
    queryFn: () => getAccount(accountId),
    enabled: Boolean(accountId),
    retry: false,
  });

  const done = (text: string) => (account: AccountResponse) => {
    queryClient.setQueryData(['account', account.id], account);
    setAccountId(account.id);
    setLookupInput(account.id);
    setFeedback({ status: 'success', text });
  };
  const failed = (error: Error) => setFeedback({ status: 'error', text: error.message });

  // Nova chave por clique: retry de rede do mesmo clique não duplica; novo clique é nova operação.
  const key = () => crypto.randomUUID();
  const value = () => Number(amount.replace(',', '.'));

  const openMutation = useMutation({
    mutationFn: () => openAccount(associateId.trim()),
    onSuccess: done('Conta aberta.'),
    onError: failed,
  });
  const depositMutation = useMutation({
    mutationFn: () => deposit(accountId, value(), key()),
    onSuccess: done('Depósito realizado.'),
    onError: failed,
  });
  const withdrawMutation = useMutation({
    mutationFn: () => withdraw(accountId, value(), key()),
    onSuccess: done('Saque realizado.'),
    onError: failed,
  });
  const transferMutation = useMutation({
    mutationFn: () => transfer(accountId, targetId.trim(), value(), key()),
    onSuccess: done('Transferência realizada.'),
    onError: failed,
  });

  const account = accountQuery.data;
  const validAmount = value() > 0;
  const busy = depositMutation.isPending || withdrawMutation.isPending || transferMutation.isPending;

  return (
    <>
      {feedback && (
        <Banner
          status={feedback.status}
          title={feedback.text}
          isDismissable
          dismissLabel="Fechar aviso"
          onDismiss={() => setFeedback(null)}
        />
      )}
      {accountQuery.isError && (
        <Banner status="error" title="Conta não encontrada" description={accountQuery.error.message} />
      )}

      <Grid columns={{ minWidth: 400 }} gap={4}>
        <Card padding={5}>
          <VStack gap={4}>
            <Heading level={3}>Abrir conta</Heading>
            <TextInput label="ID do associado" description="POST /accounts" value={associateId} onChange={setAssociateId} />
            <HStack hAlign="end">
              <Button
                variant="primary"
                label="Abrir conta"
                isDisabled={!associateId.trim()}
                isLoading={openMutation.isPending}
                onClick={() => openMutation.mutate()}
              />
            </HStack>
          </VStack>
        </Card>

        <Card padding={5}>
          <VStack gap={4}>
            <Heading level={3}>Consultar conta</Heading>
            <TextInput label="ID da conta" description="GET /accounts/{id}" value={lookupInput} onChange={setLookupInput} />
            <HStack hAlign="end">
              <Button
                variant="secondary"
                label="Consultar"
                isDisabled={!lookupInput.trim()}
                onClick={() => setAccountId(lookupInput.trim())}
              />
            </HStack>
          </VStack>
        </Card>
      </Grid>

      {account && (
        <Card padding={5}>
          <VStack gap={4}>
            <VStack gap={0}>
              <Text type="supporting">
                {account.status} · {account.id}
              </Text>
              <Text weight="bold" hasTabularNumbers>
                Saldo: {formatCurrencyBRL(account.balance)}
              </Text>
            </VStack>
            <TextInput label="Valor (R$)" inputMode="decimal" value={amount} onChange={setAmount} />
            <TextInput label="Conta de destino (transferência)" value={targetId} onChange={setTargetId} />
            <HStack gap={2}>
              <Button variant="primary" label="Depositar" isDisabled={!validAmount || busy} onClick={() => depositMutation.mutate()} />
              <Button variant="secondary" label="Sacar" isDisabled={!validAmount || busy} onClick={() => withdrawMutation.mutate()} />
              <Button
                variant="secondary"
                label="Transferir"
                isDisabled={!validAmount || !targetId.trim() || busy}
                onClick={() => transferMutation.mutate()}
              />
            </HStack>
          </VStack>
        </Card>
      )}
    </>
  );
}

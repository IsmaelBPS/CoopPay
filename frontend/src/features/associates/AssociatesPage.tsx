import { Badge } from '@astryxdesign/core/Badge';
import { Banner } from '@astryxdesign/core/Banner';
import { Button } from '@astryxdesign/core/Button';
import { Card } from '@astryxdesign/core/Card';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { useEffect, useRef, useState, type FormEvent } from 'react';
import { ApiError } from '../../shared/api/http';
import { ConfirmDialog } from '../../shared/components/ConfirmDialog';
import { useAccessibleFormValidation } from '../../shared/hooks/useAccessibleFormValidation';
import {
  createAssociate,
  deleteAssociate,
  getAssociate,
  updateAssociate,
} from './api';
import type { AssociateResponse, AssociateType } from './types';

const RECENT_KEY = 'cooppay.recentAssociates';
const UUID_PATTERN =
  '^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$';

function loadRecentAssociates(): AssociateResponse[] {
  try {
    const raw = sessionStorage.getItem(RECENT_KEY);
    if (!raw) return [];
    const parsed = JSON.parse(raw);
    return Array.isArray(parsed) ? (parsed as AssociateResponse[]) : [];
  } catch {
    return [];
  }
}

function saveRecentAssociates(items: AssociateResponse[]): void {
  try {
    sessionStorage.setItem(RECENT_KEY, JSON.stringify(items.slice(0, 8)));
  } catch {
    // Ignore storage quota errors
  }
}

function formatTypeLabel(type: AssociateType): string {
  return type === 'COMPANY' ? 'Pessoa Jurídica' : 'Pessoa Física';
}

export function AssociatesPage() {
  const queryClient = useQueryClient();

  const createFormRef = useRef<HTMLFormElement>(null);
  const lookupFormRef = useRef<HTMLFormElement>(null);
  const editFormRef = useRef<HTMLFormElement>(null);

  useAccessibleFormValidation(createFormRef);
  useAccessibleFormValidation(lookupFormRef);

  const [recentAssociates, setRecentAssociates] = useState<AssociateResponse[]>(() =>
    loadRecentAssociates(),
  );
  const [selectedId, setSelectedId] = useState<string>(() => recentAssociates[0]?.id ?? '');
  const [lookupInput, setLookupInput] = useState<string>('');
  const [feedback, setFeedback] = useState<{
    status: 'success' | 'error';
    title: string;
    description?: string;
  } | null>(null);
  const [confirmDeleteOpen, setConfirmDeleteOpen] = useState(false);

  // Create form state
  const [createName, setCreateName] = useState('');
  const [createEmail, setCreateEmail] = useState('');
  const [createPassword, setCreatePassword] = useState('');
  const [createType, setCreateType] = useState<AssociateType>('INDIVIDUAL');

  // Edit form state
  const [editName, setEditName] = useState('');
  const [editEmail, setEditEmail] = useState('');
  const [editType, setEditType] = useState<AssociateType>('INDIVIDUAL');

  const upsertRecent = (associate: AssociateResponse) => {
    setRecentAssociates((prev) => {
      const next = [associate, ...prev.filter((item) => item.id !== associate.id)].slice(0, 8);
      saveRecentAssociates(next);
      return next;
    });
  };

  const removeRecent = (id: string) => {
    setRecentAssociates((prev) => {
      const next = prev.filter((item) => item.id !== id);
      saveRecentAssociates(next);
      return next;
    });
  };

  const associateQuery = useQuery({
    queryKey: ['associate', selectedId],
    queryFn: () => getAssociate(selectedId),
    enabled: Boolean(selectedId),
    retry: false,
  });

  useAccessibleFormValidation(editFormRef, associateQuery.data?.id);

  useEffect(() => {
    if (associateQuery.data) {
      setEditName(associateQuery.data.name);
      setEditEmail(associateQuery.data.email);
      setEditType(associateQuery.data.type);
      upsertRecent(associateQuery.data);
    }
  }, [associateQuery.data]);

  const createMutation = useMutation({
    mutationFn: createAssociate,
    onSuccess: (created) => {
      queryClient.setQueryData(['associate', created.id], created);
      upsertRecent(created);
      setSelectedId(created.id);
      setLookupInput(created.id);
      setCreateName('');
      setCreateEmail('');
      setCreatePassword('');
      setCreateType('INDIVIDUAL');
      createFormRef.current?.reset();
      setFeedback({
        status: 'success',
        title: 'Associado cadastrado com sucesso',
        description: `${created.name} (${created.id})`,
      });
    },
    onError: (err) => {
      const msg = err instanceof ApiError ? err.message : 'Não foi possível cadastrar o associado.';
      setFeedback({
        status: 'error',
        title: 'Falha ao cadastrar associado',
        description: msg,
      });
    },
  });

  const updateMutation = useMutation({
    mutationFn: ({ id, name, email, type }: { id: string; name: string; email: string; type: AssociateType }) =>
      updateAssociate(id, { name, email, type }),
    onSuccess: (updated) => {
      queryClient.setQueryData(['associate', updated.id], updated);
      upsertRecent(updated);
      setFeedback({
        status: 'success',
        title: 'Dados do associado atualizados',
        description: `${updated.name} — ${updated.email}`,
      });
    },
    onError: (err) => {
      const msg = err instanceof ApiError ? err.message : 'Não foi possível atualizar o associado.';
      setFeedback({
        status: 'error',
        title: 'Falha ao atualizar associado',
        description: msg,
      });
    },
  });

  const deleteMutation = useMutation({
    mutationFn: deleteAssociate,
    onSuccess: (_data, deletedId) => {
      queryClient.removeQueries({ queryKey: ['associate', deletedId] });
      removeRecent(deletedId);
      setSelectedId('');
      setConfirmDeleteOpen(false);
      setFeedback({
        status: 'success',
        title: 'Associado removido',
        description: `Registro ${deletedId} excluído do núcleo CoopPay.`,
      });
    },
    onError: (err) => {
      setConfirmDeleteOpen(false);
      const msg = err instanceof ApiError ? err.message : 'Não foi possível remover o associado.';
      setFeedback({
        status: 'error',
        title: 'Falha ao remover associado',
        description: msg,
      });
    },
  });

  const handleCreateSubmit = (e: FormEvent<HTMLFormElement>) => {
    e.preventDefault();
    if (!e.currentTarget.checkValidity()) {
      return;
    }
    setFeedback(null);
    createMutation.mutate({
      name: createName.trim(),
      email: createEmail.trim(),
      password: createPassword,
      type: createType,
    });
  };

  const handleLookupSubmit = (e: FormEvent<HTMLFormElement>) => {
    e.preventDefault();
    if (!e.currentTarget.checkValidity()) {
      return;
    }
    setFeedback(null);
    const trimmed = lookupInput.trim();
    setSelectedId(trimmed);
    void queryClient.invalidateQueries({ queryKey: ['associate', trimmed] });
  };

  const handleEditSubmit = (e: FormEvent<HTMLFormElement>) => {
    e.preventDefault();
    if (!selectedId || !e.currentTarget.checkValidity()) {
      return;
    }
    setFeedback(null);
    updateMutation.mutate({
      id: selectedId,
      name: editName.trim(),
      email: editEmail.trim(),
      type: editType,
    });
  };

  const activeAssociate = associateQuery.data;

  return (
    <>
      {feedback && (
        <Banner
          status={feedback.status}
          title={feedback.title}
          description={feedback.description}
          isDismissable
          dismissLabel="Fechar aviso"
          onDismiss={() => setFeedback(null)}
        />
      )}

      <div className="coop-grid-2">
        <Card padding={5}>
          <div className="coop-card-header">
            <h2 className="coop-card-title">
              <span className="sicredi-dot" aria-hidden="true" />
              Cadastrar associado
            </h2>
            <Badge variant="green" label="POST /associates" />
          </div>

          <form ref={createFormRef} className="coop-form" onSubmit={handleCreateSubmit}>
            <div className="coop-field">
              <label htmlFor="create-name" className="coop-label">
                Nome completo
              </label>
              <span id="create-name-hint" className="coop-hint">
                Nome civil ou razão social do associado
              </span>
              <input
                id="create-name"
                name="name"
                type="text"
                required
                minLength={2}
                autoComplete="name"
                className="coop-input"
                aria-describedby="create-name-hint"
                data-error="create-name-error"
                value={createName}
                onChange={(e) => setCreateName(e.target.value)}
              />
              <div id="create-name-error" className="error-msg">
                <span role="img" aria-label="Erro">
                  ❌
                </span>{' '}
                Informe o nome completo (mínimo 2 caracteres).
              </div>
            </div>

            <div className="coop-field">
              <label htmlFor="create-email" className="coop-label">
                E-mail
              </label>
              <span id="create-email-hint" className="coop-hint">
                Formato esperado: nome@cooppay.com
              </span>
              <input
                id="create-email"
                name="email"
                type="email"
                required
                autoComplete="email"
                className="coop-input"
                aria-describedby="create-email-hint"
                data-error="create-email-error"
                value={createEmail}
                onChange={(e) => setCreateEmail(e.target.value)}
              />
              <div id="create-email-error" className="error-msg">
                <span role="img" aria-label="Erro">
                  ❌
                </span>{' '}
                Digite um endereço de e-mail válido, como nome@cooppay.com.
              </div>
            </div>

            <div className="coop-field">
              <label htmlFor="create-password" className="coop-label">
                Senha de acesso
              </label>
              <span id="create-password-hint" className="coop-hint">
                Mínimo de 6 caracteres para cadastro inicial
              </span>
              <input
                id="create-password"
                name="password"
                type="password"
                required
                minLength={6}
                autoComplete="new-password"
                className="coop-input"
                aria-describedby="create-password-hint"
                data-error="create-password-error"
                value={createPassword}
                onChange={(e) => setCreatePassword(e.target.value)}
              />
              <div id="create-password-error" className="error-msg">
                <span role="img" aria-label="Erro">
                  ❌
                </span>{' '}
                A senha precisa ter pelo menos 6 caracteres.
              </div>
            </div>

            <div className="coop-field">
              <label htmlFor="create-type" className="coop-label">
                Tipo de associado
              </label>
              <span id="create-type-hint" className="coop-hint">
                Selecione Pessoa Física (INDIVIDUAL) ou Jurídica (COMPANY)
              </span>
              <select
                id="create-type"
                name="type"
                required
                className="coop-select"
                aria-describedby="create-type-hint"
                data-error="create-type-error"
                value={createType}
                onChange={(e) => setCreateType(e.target.value as AssociateType)}
              >
                <option value="INDIVIDUAL">Pessoa Física (INDIVIDUAL)</option>
                <option value="COMPANY">Pessoa Jurídica (COMPANY)</option>
              </select>
              <div id="create-type-error" className="error-msg">
                <span role="img" aria-label="Erro">
                  ❌
                </span>{' '}
                Selecione o tipo do associado.
              </div>
            </div>

            <div style={{ display: 'flex', justifyContent: 'flex-end', marginTop: 4 }}>
              <Button
                type="submit"
                variant="primary"
                label="Cadastrar associado"
                isLoading={createMutation.isPending}
              />
            </div>
          </form>
        </Card>

        <div style={{ display: 'flex', flexDirection: 'column', gap: 24 }}>
          <Card padding={5}>
            <div className="coop-card-header">
              <h2 className="coop-card-title">
                <span className="sicredi-dot" aria-hidden="true" />
                Consultar associado por ID
              </h2>
              <Badge variant="neutral" label="GET /associates/{id}" />
            </div>

            <form ref={lookupFormRef} className="coop-form" onSubmit={handleLookupSubmit}>
              <div className="coop-field">
                <label htmlFor="lookup-id" className="coop-label">
                  UUID do associado
                </label>
                <span id="lookup-id-hint" className="coop-hint">
                  O backend atual expõe consulta por UUID (ex.: 550e8400-e29b-41d4-a716-446655440000)
                </span>
                <input
                  id="lookup-id"
                  name="associateId"
                  type="text"
                  required
                  pattern={UUID_PATTERN}
                  className="coop-input tabular-nums"
                  aria-describedby="lookup-id-hint"
                  data-error="lookup-id-error"
                  placeholder="xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx"
                  value={lookupInput}
                  onChange={(e) => setLookupInput(e.target.value)}
                />
                <div id="lookup-id-error" className="error-msg">
                  <span role="img" aria-label="Erro">
                    ❌
                  </span>{' '}
                  Informe um UUID válido com 36 caracteres no formato padrão.
                </div>
              </div>

              <div style={{ display: 'flex', justifyContent: 'flex-end' }}>
                <Button
                  type="submit"
                  variant="secondary"
                  label="Buscar associado"
                  isLoading={associateQuery.isFetching}
                />
              </div>
            </form>

            {recentAssociates.length > 0 && (
              <div style={{ marginTop: 20, borderTop: '1px solid var(--color-border-subtle)', paddingTop: 14 }}>
                <div className="coop-label" style={{ marginBottom: 8 }}>
                  Recentes nesta sessão
                </div>
                <div style={{ display: 'flex', flexDirection: 'column', gap: 8 }}>
                  {recentAssociates.map((item) => {
                    const isSelected = item.id === selectedId;
                    return (
                      <button
                        key={item.id}
                        type="button"
                        onClick={() => {
                          setSelectedId(item.id);
                          setLookupInput(item.id);
                          setFeedback(null);
                        }}
                        style={{
                          textAlign: 'left',
                          padding: '10px 12px',
                          borderRadius: 'var(--radius-sm)',
                          border: `1px solid ${isSelected ? 'var(--color-brand-primary)' : 'var(--color-border-subtle)'}`,
                          backgroundColor: isSelected ? 'var(--color-brand-tint)' : '#ffffff',
                          transition:
                            'border-color var(--motion-base) var(--ease-in-out), background-color var(--motion-base) var(--ease-in-out)',
                          cursor: 'pointer',
                          display: 'flex',
                          justifyContent: 'space-between',
                          alignItems: 'center',
                          gap: 10,
                        }}
                      >
                        <div>
                          <div style={{ fontWeight: 700, color: 'var(--color-ink-strong)' }}>
                            {item.name}
                          </div>
                          <div style={{ fontSize: '0.75rem', color: 'var(--color-ink-muted)' }}>
                            {item.email}
                          </div>
                        </div>
                        <Badge
                          variant={item.type === 'COMPANY' ? 'info' : 'green'}
                          label={formatTypeLabel(item.type)}
                        />
                      </button>
                    );
                  })}
                </div>
              </div>
            )}
          </Card>

          {selectedId && associateQuery.isError && (
            <Banner
              status="error"
              title="Associado não encontrado"
              description={
                associateQuery.error instanceof ApiError
                  ? associateQuery.error.message
                  : 'Verifique se o backend Spring Boot está rodando na porta 3499 e se o UUID existe.'
              }
            />
          )}

          {activeAssociate && (
            <Card padding={5}>
              <div className="coop-card-header">
                <h2 className="coop-card-title">
                  <span className="sicredi-dot" aria-hidden="true" />
                  Editar ou remover associado
                </h2>
                <Badge
                  variant={activeAssociate.type === 'COMPANY' ? 'info' : 'green'}
                  label={formatTypeLabel(activeAssociate.type)}
                />
              </div>

              <p
                className="coop-hint tabular-nums"
                style={{ marginBottom: 14, wordBreak: 'break-all' }}
              >
                ID: <code>{activeAssociate.id}</code>
              </p>

              <form ref={editFormRef} className="coop-form" onSubmit={handleEditSubmit}>
                <div className="coop-field">
                  <label htmlFor="edit-name" className="coop-label">
                    Nome
                  </label>
                  <input
                    id="edit-name"
                    name="editName"
                    type="text"
                    required
                    minLength={2}
                    className="coop-input"
                    data-error="edit-name-error"
                    value={editName}
                    onChange={(e) => setEditName(e.target.value)}
                  />
                  <div id="edit-name-error" className="error-msg">
                    <span role="img" aria-label="Erro">
                      ❌
                    </span>{' '}
                    Informe um nome válido (mínimo 2 caracteres).
                  </div>
                </div>

                <div className="coop-field">
                  <label htmlFor="edit-email" className="coop-label">
                    E-mail
                  </label>
                  <input
                    id="edit-email"
                    name="editEmail"
                    type="email"
                    required
                    className="coop-input"
                    data-error="edit-email-error"
                    value={editEmail}
                    onChange={(e) => setEditEmail(e.target.value)}
                  />
                  <div id="edit-email-error" className="error-msg">
                    <span role="img" aria-label="Erro">
                      ❌
                    </span>{' '}
                    Informe um e-mail válido.
                  </div>
                </div>

                <div className="coop-field">
                  <label htmlFor="edit-type" className="coop-label">
                    Tipo
                  </label>
                  <select
                    id="edit-type"
                    name="editType"
                    required
                    className="coop-select"
                    data-error="edit-type-error"
                    value={editType}
                    onChange={(e) => setEditType(e.target.value as AssociateType)}
                  >
                    <option value="INDIVIDUAL">Pessoa Física (INDIVIDUAL)</option>
                    <option value="COMPANY">Pessoa Jurídica (COMPANY)</option>
                  </select>
                  <div id="edit-type-error" className="error-msg">
                    <span role="img" aria-label="Erro">
                      ❌
                    </span>{' '}
                    Selecione o tipo do associado.
                  </div>
                </div>

                <div
                  style={{
                    display: 'flex',
                    justifyContent: 'space-between',
                    gap: 12,
                    marginTop: 6,
                    flexWrap: 'wrap',
                  }}
                >
                  <Button
                    type="button"
                    variant="destructive"
                    label="Remover associado"
                    onClick={() => setConfirmDeleteOpen(true)}
                  />
                  <Button
                    type="submit"
                    variant="primary"
                    label="Salvar alterações"
                    isLoading={updateMutation.isPending}
                  />
                </div>
              </form>
            </Card>
          )}
        </div>
      </div>

      <ConfirmDialog
        open={confirmDeleteOpen}
        title="Remover associado?"
        description={
          activeAssociate
            ? `Esta ação excluirá permanentemente o registro de ${activeAssociate.name} (${activeAssociate.email}).`
            : 'Confirma a remoção deste associado?'
        }
        confirmLabel="Sim, remover"
        cancelLabel="Cancelar"
        isSubmitting={deleteMutation.isPending}
        onCancel={() => setConfirmDeleteOpen(false)}
        onConfirm={() => {
          if (activeAssociate) {
            deleteMutation.mutate(activeAssociate.id);
          }
        }}
      />
    </>
  );
}

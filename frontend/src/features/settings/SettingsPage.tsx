import { Avatar, AvatarStatusDot } from '@astryxdesign/core/Avatar';
import { Badge } from '@astryxdesign/core/Badge';
import { Banner } from '@astryxdesign/core/Banner';
import { Button } from '@astryxdesign/core/Button';
import { Card } from '@astryxdesign/core/Card';
import { Divider } from '@astryxdesign/core/Divider';
import { Heading } from '@astryxdesign/core/Heading';
import { HStack } from '@astryxdesign/core/HStack';
import { Switch } from '@astryxdesign/core/Switch';
import { Text } from '@astryxdesign/core/Text';
import { VStack } from '@astryxdesign/core/VStack';
import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { useAuthStore } from '../auth/authStore';
import { usePreferencesStore } from './preferencesStore';

export function SettingsPage() {
  const navigate = useNavigate();
  const user = useAuthStore((state) => state.user);
  const authMode = useAuthStore((state) => state.authMode);
  const rememberMe = useAuthStore((state) => state.rememberMe);
  const logout = useAuthStore((state) => state.logout);

  const themeMode = usePreferencesStore((state) => state.themeMode);
  const setThemeMode = usePreferencesStore((state) => state.setThemeMode);

  const [copiedBanner, setCopiedBanner] = useState(false);

  const displayName = user?.name || user?.email || 'Associado';
  const statusLabel = authMode === 'api' ? 'API conectada' : 'Sessão local';

  const handleCopyEmail = async () => {
    if (!user?.email) return;
    try {
      await navigator.clipboard?.writeText(user.email);
      setCopiedBanner(true);
    } catch {
      // Ignore clipboard permission errors in restricted contexts
    }
  };

  return (
    <VStack gap={5}>
      {copiedBanner && (
        <Banner
          status="success"
          title="E-mail copiado para a área de transferência"
          description={user?.email}
          isDismissable
          dismissLabel="Fechar aviso"
          onDismiss={() => setCopiedBanner(false)}
          container="card"
        />
      )}

      <Card padding={5} elevation="none">
        <VStack gap={4}>
          <HStack justify="between" vAlign="center" wrap="wrap" gap={3}>
            <Heading level={2}>
              Perfil do associado
            </Heading>
            <HStack gap={2} vAlign="center">
              <Badge variant={authMode === 'api' ? 'green' : 'neutral'} label={statusLabel} />
              <Badge variant="green" label="Astryx + Sicredi" />
            </HStack>
          </HStack>

          <Divider />

          <HStack justify="between" vAlign="center" wrap="wrap" gap={4}>
            <HStack gap={4} vAlign="center">
              <Avatar
                name={displayName}
                size="lg"
                tooltip={false}
                status={
                  <AvatarStatusDot
                    variant={authMode === 'api' ? 'success' : 'neutral'}
                    label={statusLabel}
                  />
                }
              />
              <VStack gap={1}>
                <Text type="large" weight="semibold">
                  {displayName}
                </Text>
                {user?.email && (
                  <Text type="body" color="secondary">
                    {user.email}
                  </Text>
                )}
                {user?.id && (
                  <Text type="supporting" color="secondary" hasTabularNumbers>
                    ID da sessão: {user.id}
                  </Text>
                )}
              </VStack>
            </HStack>

            {user?.email && (
              <Button
                variant="secondary"
                size="sm"
                icon="copy"
                label="Copiar e-mail"
                onClick={handleCopyEmail}
              />
            )}
          </HStack>
        </VStack>
      </Card>

      <Card padding={5} elevation="none">
        <VStack gap={4}>
          <Heading level={2}>
            Aparência
          </Heading>
          <Divider />
          <Switch
            label="Modo escuro"
            description="Alterna a interface do CoopPay entre tema claro e escuro."
            value={themeMode === 'dark'}
            onChange={(checked) => setThemeMode(checked ? 'dark' : 'light')}
            labelSpacing="spread"
          />
        </VStack>
      </Card>

      <Card padding={5} elevation="none">
        <VStack gap={4}>
          <Heading level={2}>
            Sessão e segurança
          </Heading>
          <Divider />
          <HStack justify="between" vAlign="center" wrap="wrap" gap={4}>
            <VStack gap={1}>
              <Text weight="semibold">Persistência da sessão</Text>
              <Text type="supporting" color="secondary">
                {rememberMe
                  ? 'Sessão persistida neste navegador (localStorage)'
                  : 'Sessão temporária nesta aba (sessionStorage)'}
              </Text>
            </VStack>
            <Button
              variant="destructive"
              size="md"
              label="Sair da conta"
              onClick={() => {
                logout();
                navigate('/login', { replace: true });
              }}
            />
          </HStack>
        </VStack>
      </Card>
    </VStack>
  );
}

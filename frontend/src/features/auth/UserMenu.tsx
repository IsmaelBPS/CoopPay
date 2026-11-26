import { Avatar, AvatarStatusDot } from '@astryxdesign/core/Avatar';
import { Badge } from '@astryxdesign/core/Badge';
import { Divider } from '@astryxdesign/core/Divider';
import {
  DropdownMenu,
  type DropdownMenuItemData,
  type DropdownMenuOption,
} from '@astryxdesign/core/DropdownMenu';
import { HStack } from '@astryxdesign/core/HStack';
import { Text } from '@astryxdesign/core/Text';
import { VStack } from '@astryxdesign/core/VStack';
import { useEffect, useRef, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { usePreferencesStore } from '../settings/preferencesStore';
import { useAuthStore } from './authStore';

export function UserMenu() {
  const navigate = useNavigate();
  const user = useAuthStore((state) => state.user);
  const authMode = useAuthStore((state) => state.authMode);
  const logout = useAuthStore((state) => state.logout);

  const themeMode = usePreferencesStore((state) => state.themeMode);
  const toggleThemeMode = usePreferencesStore((state) => state.toggleThemeMode);

  const [copiedEmail, setCopiedEmail] = useState(false);
  const copyTimerRef = useRef<ReturnType<typeof setTimeout> | undefined>(undefined);

  useEffect(() => {
    return () => {
      clearTimeout(copyTimerRef.current);
    };
  }, []);

  const displayName = user?.name || user?.email || 'Associado';
  const statusLabel = authMode === 'api' ? 'API conectada' : 'Sessão local';

  const handleCopyEmail = async () => {
    if (!user?.email) return;
    try {
      await navigator.clipboard?.writeText(user.email);
      setCopiedEmail(true);
      clearTimeout(copyTimerRef.current);
      copyTimerRef.current = setTimeout(() => setCopiedEmail(false), 1500);
    } catch {
      // Ignore clipboard permission errors in restricted contexts
    }
  };

  const preferenceItems: DropdownMenuItemData[] = [
    {
      id: 'theme',
      icon: 'viewColumns',
      label: 'Alternar tema',
      description: themeMode === 'dark' ? 'Mudar para tema claro' : 'Mudar para tema escuro',
      endContent: <Badge variant="neutral" label={themeMode === 'dark' ? 'Escuro' : 'Claro'} />,
      hasCloseOnSelect: false,
      onClick: toggleThemeMode,
    },
  ];

  if (user?.email) {
    preferenceItems.push({
      id: 'copy-email',
      icon: copiedEmail ? 'checkDouble' : 'copy',
      label: copiedEmail ? 'E-mail copiado' : 'Copiar e-mail',
      description: user.email,
      hasCloseOnSelect: false,
      onClick: handleCopyEmail,
    });
  }

  const items: DropdownMenuOption[] = [
    {
      type: 'section',
      id: 'account',
      title: 'Conta',
      items: [
        {
          id: 'profile',
          icon: 'info',
          label: 'Meu perfil',
          description: 'Dados do associado e sessão ativa',
          onClick: () => navigate('/settings'),
        },
        {
          id: 'settings',
          icon: 'wrench',
          label: 'Configurações',
          description: 'Aparência, preferências e segurança',
          onClick: () => navigate('/settings'),
        },
        {
          id: 'ledger',
          icon: 'clock',
          label: 'Extrato / Ledger',
          description: 'Consultar movimentações da conta',
          onClick: () => navigate('/ledger'),
        },
      ],
    },
    {
      type: 'section',
      id: 'preferences',
      title: 'Preferências',
      items: preferenceItems,
    },
    { type: 'divider' },
    {
      id: 'logout',
      icon: 'close',
      label: 'Sair da conta',
      description: 'Encerrar sessão neste dispositivo',
      variant: 'destructive',
      onClick: () => {
        logout();
        navigate('/login', { replace: true });
      },
    },
  ];

  return (
    <DropdownMenu
      presentation="adaptive"
      placement="below"
      alignment="end"
      menuWidth={272}
      hasChevron
      button={{
        label: displayName,
        variant: 'ghost',
        size: 'md',
        icon: (
          <Avatar
            name={displayName}
            size="sm"
            tooltip={false}
            status={
              <AvatarStatusDot
                variant={authMode === 'api' ? 'success' : 'neutral'}
                label={statusLabel}
              />
            }
          />
        ),
      }}
      header={
        <VStack gap={2} paddingInline={3} paddingBlock={2}>
          <HStack gap={3} vAlign="center">
            <Avatar name={displayName} size="md" tooltip={false} />
            <VStack gap={0}>
              <Text weight="semibold">{displayName}</Text>
              {user?.email && (
                <Text type="supporting" color="secondary">
                  {user.email}
                </Text>
              )}
            </VStack>
          </HStack>
          <HStack gap={2} vAlign="center">
            <Badge variant={authMode === 'api' ? 'green' : 'neutral'} label={statusLabel} />
            <Badge variant="neutral" label={themeMode === 'dark' ? 'Tema escuro' : 'Tema claro'} />
          </HStack>
          <Divider />
        </VStack>
      }
      items={items}
    />
  );
}

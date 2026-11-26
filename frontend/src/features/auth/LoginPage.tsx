import { Badge } from '@astryxdesign/core/Badge';
import { Banner } from '@astryxdesign/core/Banner';
import { Button } from '@astryxdesign/core/Button';
import { Card } from '@astryxdesign/core/Card';
import { Center } from '@astryxdesign/core/Center';
import { CheckboxInput } from '@astryxdesign/core/CheckboxInput';
import { Divider } from '@astryxdesign/core/Divider';
import { Heading } from '@astryxdesign/core/Heading';
import { HStack } from '@astryxdesign/core/HStack';
import { InputGroup } from '@astryxdesign/core/InputGroup';
import { Link } from '@astryxdesign/core/Link';
import { Text } from '@astryxdesign/core/Text';
import { TextInput } from '@astryxdesign/core/TextInput';
import { VStack } from '@astryxdesign/core/VStack';
import { useRef, useState, type CSSProperties, type FormEvent } from 'react';
import { Navigate, useLocation, useNavigate } from 'react-router-dom';
import { useAuthStore } from './authStore';

const EMAIL_PATTERN = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;

const pageStyle: CSSProperties = {
  backgroundColor: 'var(--color-background-body)',
};

const brandMarkStyle: CSSProperties = {
  backgroundColor: 'var(--color-brand-primary)',
  borderRadius: 'var(--radius-md)',
  color: 'var(--color-on-accent)',
  fontFamily: 'var(--font-display)',
  fontStyle: 'italic',
  fontWeight: 600,
  fontSize: '1.375rem',
};

const italicHeadingStyle: CSSProperties = {
  fontStyle: 'italic',
  color: 'var(--color-ink-heading)',
};

const fullWidthStyle: CSSProperties = {
  width: '100%',
};

function getEmailError(value: string): string | undefined {
  const trimmed = value.trim();
  if (!trimmed) {
    return 'Informe seu e-mail de associado.';
  }
  if (!EMAIL_PATTERN.test(trimmed)) {
    return 'Informe um e-mail válido no formato nome@cooppay.com.';
  }
  return undefined;
}

function getPasswordError(value: string): string | undefined {
  if (!value) {
    return 'Informe sua senha de acesso.';
  }
  if (value.length < 6) {
    return 'A senha deve ter pelo menos 6 caracteres.';
  }
  return undefined;
}

export function LoginPage() {
  const navigate = useNavigate();
  const location = useLocation();

  const isAuthenticated = useAuthStore((state) => state.isAuthenticated);
  const rememberedEmail = useAuthStore((state) => state.rememberedEmail);
  const storedRememberMe = useAuthStore((state) => state.rememberMe);
  const isLoading = useAuthStore((state) => state.isLoading);
  const error = useAuthStore((state) => state.error);
  const login = useAuthStore((state) => state.login);
  const clearError = useAuthStore((state) => state.clearError);

  const [email, setEmail] = useState(() => rememberedEmail);
  const [password, setPassword] = useState('');
  const [rememberMe, setRememberMe] = useState(() => storedRememberMe);
  const [showPassword, setShowPassword] = useState(false);
  const [showRecoveryInfo, setShowRecoveryInfo] = useState(false);
  const [touched, setTouched] = useState({ email: false, password: false });
  const [submitted, setSubmitted] = useState(false);

  const emailInputRef = useRef<HTMLInputElement>(null);
  const passwordInputRef = useRef<HTMLInputElement>(null);

  const from =
    (location.state as { from?: { pathname?: string } } | null)?.from?.pathname ?? '/associates';

  if (isAuthenticated) {
    return <Navigate to={from} replace />;
  }

  const emailError = touched.email || submitted ? getEmailError(email) : undefined;
  const passwordError = touched.password || submitted ? getPasswordError(password) : undefined;

  const handleSubmit = async (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault();
    setSubmitted(true);

    const nextEmailError = getEmailError(email);
    const nextPasswordError = getPasswordError(password);

    if (nextEmailError) {
      emailInputRef.current?.focus();
      return;
    }

    if (nextPasswordError) {
      passwordInputRef.current?.focus();
      return;
    }

    const ok = await login({
      email: email.trim(),
      password,
      rememberMe,
    });

    if (ok) {
      navigate(from, { replace: true });
    }
  };

  return (
    <Center role="main" axis="both" padding={6} minHeight="100dvh" style={pageStyle}>
      <VStack gap={5} hAlign="center" width="100%" maxWidth={420}>
        <HStack gap={3} vAlign="center">
          <Center width={44} height={44} aria-hidden="true" style={brandMarkStyle}>
            C
          </Center>
          <VStack gap={0}>
            <Heading level={3} weight="semibold" style={italicHeadingStyle}>
              CoopPay
            </Heading>
            <Text type="supporting" color="secondary">
              Núcleo Financeiro Cooperativo
            </Text>
          </VStack>
        </HStack>

        <Card padding={8} width="100%" elevation="none">
          <VStack gap={5} hAlign="stretch">
            <VStack gap={1}>
              <HStack gap={2} vAlign="center">
                <svg width="8" height="8" viewBox="0 0 8 8" aria-hidden="true">
                  <circle cx="4" cy="4" r="4" fill="var(--color-brand-primary)" />
                </svg>
                <Heading level={2} accessibilityLevel={1} weight="medium" style={italicHeadingStyle}>
                  Acessar sua conta
                </Heading>
              </HStack>
              <Text type="body" color="secondary" size="sm">
                Informe suas credenciais de associado para continuar.
              </Text>
            </VStack>

            {error && (
              <Banner
                status="error"
                title="Não foi possível entrar"
                description={error}
                isDismissable
                dismissLabel="Fechar aviso"
                onDismiss={clearError}
                container="card"
              />
            )}

            {showRecoveryInfo && (
              <Banner
                status="info"
                title="Recuperação de acesso"
                description="A redefinição de senha será integrada ao serviço de autenticação da cooperativa. Em modo local, informe um e-mail válido e uma senha com 6 ou mais caracteres."
                isDismissable
                dismissLabel="Fechar aviso"
                onDismiss={() => setShowRecoveryInfo(false)}
                container="card"
              />
            )}

            <form noValidate onSubmit={handleSubmit}>
              <VStack gap={4} hAlign="stretch">
                <TextInput
                  ref={emailInputRef}
                  id="login-email"
                  htmlName="email"
                  label="E-mail"
                  description="Formato: associado@cooppay.com"
                  type="email"
                  size="lg"
                  width="100%"
                  isRequired
                  autoComplete="username"
                  hasSpellCheck={false}
                  hasAutoCorrect={false}
                  autoCapitalize="off"
                  enterKeyHint="next"
                  placeholder="nome@cooppay.com"
                  value={email}
                  onChange={(val) => {
                    setEmail(val);
                    if (error) clearError();
                  }}
                  onBlur={() => setTouched((prev) => ({ ...prev, email: true }))}
                  status={emailError ? { type: 'error', message: emailError } : undefined}
                />

                <InputGroup
                  label="Senha"
                  description="Mínimo de 6 caracteres"
                  size="lg"
                  isRequired
                  style={fullWidthStyle}
                  status={passwordError ? { type: 'error', message: passwordError } : undefined}
                >
                  <TextInput
                    ref={passwordInputRef}
                    id="login-password"
                    htmlName="password"
                    label="Senha de acesso"
                    isLabelHidden
                    type={showPassword ? 'text' : 'password'}
                    autoComplete="current-password"
                    enterKeyHint="done"
                    placeholder="Digite sua senha"
                    value={password}
                    onChange={(val) => {
                      setPassword(val);
                      if (error) clearError();
                    }}
                    onBlur={() => setTouched((prev) => ({ ...prev, password: true }))}
                    status={passwordError ? { type: 'error' } : undefined}
                  />
                  <Button
                    type="button"
                    variant="secondary"
                    label={showPassword ? 'Ocultar' : 'Mostrar'}
                    aria-pressed={showPassword}
                    aria-controls="login-password"
                    onClick={() => setShowPassword((prev) => !prev)}
                  />
                </InputGroup>

                <HStack justify="between" vAlign="center" wrap="wrap" gap={2}>
                  <CheckboxInput
                    label="Manter conectado"
                    htmlName="rememberMe"
                    size="sm"
                    value={rememberMe}
                    onChange={setRememberMe}
                  />
                  <Link
                    href="#recuperar-senha"
                    type="supporting"
                    size="sm"
                    onClick={(event) => {
                      event.preventDefault();
                      setShowRecoveryInfo(true);
                    }}
                  >
                    Esqueci minha senha
                  </Link>
                </HStack>

                <Button
                  type="submit"
                  variant="primary"
                  size="lg"
                  width="100%"
                  label="Entrar"
                  isLoading={isLoading}
                />
              </VStack>
            </form>

            <Divider />

            <HStack justify="between" vAlign="center" wrap="wrap" gap={2}>
              <Text type="supporting" color="secondary">
                Acesso exclusivo para associados
              </Text>
              <Badge variant="green" label="Ambiente seguro" />
            </HStack>
          </VStack>
        </Card>

        <Text type="supporting" color="secondary" justify="center">
          CoopPay · Cooperativismo financeiro com rastreabilidade em tempo real
        </Text>
      </VStack>
    </Center>
  );
}

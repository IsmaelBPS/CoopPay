# Animações — CoopPay

Complementa o `design.md`. Princípio: movimento discreto e funcional (confiança e clareza financeira). Animação serve para dar feedback ou orientar, nunca para decorar.

---

## 1. Tokens de Movimento

```css
:root {
  --motion-fast: 120ms;   /* hover, focus, toggles */
  --motion-base: 200ms;   /* botões, inputs, cores, bordas */
  --motion-slow: 320ms;   /* dialogs, painéis, troca de página */

  --ease-out: cubic-bezier(0.2, 0, 0, 1);      /* entradas */
  --ease-in: cubic-bezier(0.4, 0, 1, 1);       /* saídas */
  --ease-in-out: cubic-bezier(0.4, 0, 0.2, 1); /* mudanças de estado */
}
```

> Declarar em `frontend/src/design/tokens.css`. Transições existentes (`0.2s ease`, `0.25s ease`) devem migrar para estes tokens ao serem tocadas; não é necessário refatorar tudo de uma vez.

---

## 2. Padrões

| Situação | Propriedade | Duração | Easing |
| --- | --- | --- | --- |
| Hover/focus de botão, link, linha de tabela | `background-color`, `color`, `border-color` | `--motion-base` | `--ease-in-out` |
| Dialog / modal (`ConfirmDialog`) entrando | `opacity` + `transform: translateY(8px) → 0` | `--motion-slow` | `--ease-out` |
| Dialog / modal saindo | `opacity` | `--motion-base` | `--ease-in` |
| Toast / feedback de sucesso ou erro | `opacity` + `transform` | `--motion-base` | `--ease-out` |
| Troca de tema (claro/escuro) | `background-color`, `color`, `border-color` | `--motion-base` | `--ease-in-out` |
| Loading (skeleton / spinner) | `opacity` pulsando ou rotação | contínuo, só enquanto carrega | `linear` |

---

## 3. Regras

1. **Só `transform` e `opacity`** para movimento. Cores podem transicionar. Nunca animar `width`, `height`, `top`, `left` (causa reflow).
2. **Duração máxima: 400ms.** Acima disso a interface parece lenta.
3. **`prefers-reduced-motion` é obrigatório:** com a preferência ativa, remover `transform` e deixar apenas fade curto ou nenhum movimento.
   ```css
   @media (prefers-reduced-motion: reduce) {
     *, *::before, *::after {
       animation-duration: 0.01ms !important;
       transition-duration: 0.01ms !important;
     }
   }
   ```
4. **Valores monetários não animam.** Saldo, valores de extrato e totais mudam instantaneamente (sem contador rolando, sem fade): o usuário precisa ler o número exato.
5. **Sem loops infinitos** fora de indicadores de loading.
6. **Não bloquear interação:** a animação nunca deve impedir clique ou foco; elementos saindo devem deixar de receber eventos (`pointer-events: none`).
7. **Foco visível não é animação opcional:** o outline de foco aparece imediatamente (sem delay).

---

## 4. Implementação

- Preferir **CSS** (`transition` / `@keyframes`) com os tokens acima.
- Não adicionar biblioteca de animação (ex.: Framer Motion) sem necessidade comprovada.
- Componentes Astryx: usar as transições nativas do componente; sobrescrever apenas via tokens, não via CSS ad hoc.

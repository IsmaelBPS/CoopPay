# Design System — CoopPay (Inspiração Visual: Sicredi)

Referência extraída diretamente de `https://www.sicredi.com.br/home/` para manter consistência visual cooperativa (confiança, clareza e foco financeiro) na interface do CoopPay.

---

## 1. Identidade e Direção Visual

- **Personalidade:** Institucional cooperativa, limpa, acolhedora e objetiva.
- **Contraste base:** Fundo predominantemente claro (`#ffffff` / `#fafafa`), textos em cinza-esverdeado profundo (`#323c32` / `#0c0e0c`) e verde Sicredi (`#33820d`) reservado para ações principais, links e destaques semânticos.
- **Assinatura visual:** Títulos em **Exo 2** (frequentemente em *itálico* nos níveis principais `h1`–`h4`) combinados com corpo de texto e formulários em **Nunito**, além de neutros com leve subtom verde-acinzentado (`#cdd3cd`, `#e1e6e1`, `#5a645a`).

---

## 2. Tipografia

### Famílias
- **Display / Títulos:** `'Exo 2', sans-serif`
- **Corpo / UI / Botões:** `'Nunito', sans-serif`

> Google Fonts equivalentes para uso no frontend:
> `https://fonts.googleapis.com/css2?family=Exo+2:ital,wght@0,300;0,400;0,500;0,600;1,300;1,500&family=Nunito:ital,wght@0,400;0,600;0,700;1,400&display=swap`

### Escala Tipográfica (Desktop → Mobile)
| Token | Uso | Família | Estilo / Peso | Tamanho (Desktop) | Line Height |
| --- | --- | --- | --- | --- | --- |
| `--font-h1` | Hero / Título principal | `Exo 2` | `italic`, `500` | `4.375rem` (70px) / `1.375rem` mobile | `4.5rem` |
| `--font-h2` | Título de seção | `Exo 2` | `italic`, `500` | `3.75rem` (60px) / `2.25rem` mobile | `4.25rem` |
| `--font-h3` | Subtítulo de seção | `Exo 2` | `italic`, `300` | `3.75rem` (60px) / `2.25rem` mobile | `4.25rem` |
| `--font-h4` | Destaque médio | `Exo 2` | `italic`, `300` | `3.25rem` (52px) / `2rem` mobile | `3.5rem` |
| `--font-h5` | Título de bloco / card | `Exo 2` | `normal`, `400` | `1.5rem`–`3.25rem` | `1.3`–`3.5rem` |
| `--font-h6` | Subtítulo de painel | `Exo 2` | `normal`, `500` | `1.25rem`–`2.25rem` | `1.3`–`2.75rem` |
| `--font-label` | Label de card / notificação | `Exo 2` | `normal`, `400` | `1.125rem` (18px) | `1.5rem` (24px) |
| `--font-body` | Texto padrão | `Nunito` | `normal`, `400` | `1rem` (16px) | `1.5rem` (24px) |
| `--font-small` | Metadados / legendas | `Nunito` | `normal`, `400` | `0.875rem` (14px) / `0.75rem` (12px) | `1.25rem` |

> **Nota para Dashboard (CoopPay):** Em telas operacionais (tabelas de associados, contas e extrato), manter `Exo 2` nos títulos de página/seção (`1.5rem`–`2rem`) e `Nunito` (`14px`–`16px`) em tabelas, inputs e badges; usar `font-variant-numeric: tabular-nums` em valores monetários (`R$`).

---

## 3. Tokens de Cores

### Marca (Verde Sicredi)
| Token | Hex | Uso |
| --- | --- | --- |
| `--color-brand-primary` | `#33820d` | Botão primário, links, ícones de destaque, indicador ativo, scrollbar |
| `--color-brand-hover` | `#26610a` | Estado `:hover` / `:active` de ações primárias |
| `--color-brand-deep` | `#0a4b1e` / `#0b4a1f` | Fundos escuros institucionais, cabeçalhos/banners de destaque |
| `--color-brand-tint` | `#ecf6e7` | Fundo suave de destaque, badges positivos, hover sutil de linhas |

### Neutros (Subtom Verde-Cinza)
| Token | Hex | Uso |
| --- | --- | --- |
| `--color-ink-strong` | `#0c0e0c` | Texto de alto contraste / valores financeiros |
| `--color-ink-heading` | `#323c32` | Cor padrão de `h1`–`h6` e títulos |
| `--color-ink-body` | `#333033` | Texto corrido alternativo |
| `--color-ink-muted` | `#5a645a` / `#5a635a` | Subtítulos, labels secundários, metadados |
| `--color-border-strong` | `#cdd3cd` | Bordas de cards, divisores, inputs |
| `--color-border-subtle` | `#e1e6e1` | Divisores internos de tabela e separadores leves |
| `--color-surface-muted` | `#f5f5f5` | Fundo de seções alternadas / cabeçalho de tabela |
| `--color-surface-page` | `#fafafa` | Fundo geral da aplicação |
| `--color-surface-card` | `#ffffff` | Fundo de cards, modais e painéis |

### Semânticas (Base Bootstrap / Feedback)
| Token | Hex | Uso no CoopPay |
| --- | --- | --- |
| `--color-success` | `#28a745` | Créditos, depósitos, operações confirmadas (ou `#33820d`) |
| `--color-warning` | `#ffc107` | Operações pendentes / em processamento |
| `--color-danger` | `#dc3545` | Débitos, estornos, erros de validação/saldo insuficiente |
| `--color-info` | `#17a2b8` | Informativos neutros e status assíncronos |

---

## 4. Espaçamento, Bordas e Breakpoints

### Raios de Borda (`border-radius`)
- **Botões e inputs padrão:** `4px` (`.a-link-principal a`, Bootstrap base `.25rem`)
- **Cards e painéis:** `6px` a `8px` (`.card-padding-border`: `8px`)
- **Containers de destaque:** `10px`
- **Pills / badges / indicadores:** `100px` ou `50%` (`.sicredi-ponto-verde`: `8px × 8px`, `border-radius: 10px`)

### Espaçamento
- **Grid de cards destaque:** `gap: 32px`, `margin-top: 40px` (3 colunas no desktop)
- **Padding interno de card:** `16px` (`1rem`) a `20px` (`1.25rem`); cards compactos `12px`
- **Botão primário CTA:** `padding: 8px 40px` (ou `8px 20px` em ações de tabela/formulário)

### Breakpoints
- `sm`: `576px`
- `md`: `768px`
- `lg`: `992px`
- `xl`: `1200px`

---

## 5. Detalhes Visuais e Componentes-Chave

1. **Botão Primário:** Fundo `#33820d`, texto `#ffffff` em `Nunito` `16px` `bold`, `border-radius: 4px`, transição suave de `background-color .3s` para `#26610a` no hover.
2. **Botão Secundário (Outline):** Fundo `#ffffff`, texto `#33820d`, borda `1px solid #33820d`. Em fundos escuros (`#0a4b1e` / `#33820d`), usar fundo transparente com borda e texto `#ffffff`.
3. **Links:** Cor `#33820d`, sem sublinhado por padrão; no hover mantém `#33820d` com `text-underline-position: under`.
4. **Ponto Verde Sicredi (`.sicredi-ponto-verde`):** Círculo de `8px × 8px` em `#33820d` usado antes de itens de lista, status ativo ou marcadores de seção.
5. **Cards:** Fundo `#ffffff`, borda `1px solid #cdd3cd` (ou `rgba(0,0,0,.125)`), `border-radius: 8px`, sem sombras pesadas (visual limpo e flat com hierarquia por borda e contraste de fundo `#fafafa` vs `#ffffff`).
6. **Scrollbars customizadas:** Thumb em `#33820d` com `border-radius: 10px`.

---

## 6. CSS Tokens Prontos para Uso

```css
:root {
  /* Tipografia */
  --font-display: 'Exo 2', sans-serif;
  --font-sans: 'Nunito', sans-serif;

  /* Cores da marca */
  --color-brand-primary: #33820d;
  --color-brand-hover: #26610a;
  --color-brand-deep: #0a4b1e;
  --color-brand-tint: #ecf6e7;

  /* Neutros */
  --color-ink-strong: #0c0e0c;
  --color-ink-heading: #323c32;
  --color-ink-body: #333033;
  --color-ink-muted: #5a645a;
  --color-border: #cdd3cd;
  --color-border-subtle: #e1e6e1;
  --color-surface-muted: #f5f5f5;
  --color-surface-page: #fafafa;
  --color-surface-card: #ffffff;

  /* Semânticas */
  --color-success: #28a745;
  --color-warning: #ffc107;
  --color-danger: #dc3545;
  --color-info: #17a2b8;

  /* Raios */
  --radius-sm: 4px;
  --radius-md: 8px;
  --radius-lg: 10px;
  --radius-pill: 100px;
}
```

### Aplicação no CoopPay (com Astryx)
- Use `@astryxdesign/theme-neutral` como base e estenda via `defineTheme({ extends: neutralTheme, ... })` mapeando `--color-brand-primary` (`#33820d`), `--color-ink-heading` (`#323c32`), `--color-surface-page` (`#fafafa`) e as fontes `Exo 2` / `Nunito`.
- Para o extrato (`LedgerEntry`), destaque créditos com `#33820d` / fundo `#ecf6e7` e débitos/estornos com `#dc3545`, mantendo alinhamento numérico tabular (`tabular-nums`).

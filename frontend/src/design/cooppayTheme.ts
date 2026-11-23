import { defineTheme } from '@astryxdesign/core/theme';
import { defaultThemeSlug, themes } from '../astryx-themes';

export const cooppayTheme = defineTheme({
  name: 'cooppay',
  extends: themes[defaultThemeSlug],
  tokens: {
    '--color-accent': ['#33820d', '#4ea822'],
    '--color-on-accent': ['#ffffff', '#ffffff'],
    '--color-text-accent': ['#33820d', '#7ad14d'],
    '--color-text-primary': ['#0c0e0c', '#e6ebe6'],
    '--color-text-secondary': ['#5a645a', '#b3bdb3'],
    '--color-background-body': ['#fafafa', '#121512'],
    '--color-background-surface': ['#ffffff', '#1a1e1a'],
    '--color-background-card': ['#ffffff', '#1a1e1a'],
    '--color-border': ['#cdd3cd', '#2d352d'],
    '--color-border-emphasized': ['#5a645a', '#485448'],
    '--color-success': ['#28a745', '#28a745'],
    '--color-warning': ['#ffc107', '#ffc107'],
    '--color-error': ['#dc3545', '#e45865'],
    '--color-info': ['#17a2b8', '#17a2b8'],
    '--font-family-heading': "'Exo 2', sans-serif",
    '--font-family-body': "'Nunito', sans-serif",
    '--radius-inner': '4px',
    '--radius-element': '4px',
    '--radius-container': '8px',
  },
});

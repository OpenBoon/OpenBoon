import { Global, css } from '@emotion/react'

import { colors, constants, typography, spacing } from '.'

const StylesReset = () => (
  <>
    <Global
      styles={{
        'html, body, #__next': {
          margin: 0,
          padding: 0,
          minHeight: '100%',
        },
        '*, :after, :before': {
          boxSizing: 'border-box',
        },
        '*': {
          WebkitFontSmoothing: 'antialiased',
          MozOsxFontSmoothing: 'grayscale',
        },
        body: {
          fontFamily: typography.family.regular,
          fontSize: typography.size.regular,
          lineHeight: typography.height.regular,
          fontWeight: typography.weight.regular,
          color: colors.structure.white,
          backgroundColor: colors.structure.soot,
        },
        'input, textarea, select, button': {
          margin: 0,
          fontFamily: 'inherit',
        },
        'h1, h2, h3, h4': { margin: 0, padding: 0 },
        h3: {
          fontSize: typography.size.medium,
          lineHeight: typography.height.medium,
        },
        a: {
          color: colors.structure.white,
          textDecoration: 'none',
          '&:hover': {
            textDecoration: 'underline',
          },
        },
        input: {
          border: 0,
          padding: spacing.normal,
          fontSize: typography.size.regular,
          lineHeight: typography.height.regular,
        },
        'input[type="search"]': {
          WebkitAppearance: 'textfield',
        },
        'input[type="search"]::-webkit-search-cancel-button': {
          MozAppearance: 'none',
          WebkitAppearance: 'none',
          height: constants.icons.regular,
          width: constants.icons.regular,
          backgroundImage: `url('data:image/svg+xml;base64,PHN2ZyB4bWxucz0iaHR0cDovL3d3dy53My5vcmcvMjAwMC9zdmciIHZpZXdCb3g9IjAgMCAyMCAyMCI+CiAgPHBhdGggZmlsbD0iY3VycmVudENvbG9yIiBkPSJNMTMuOSA0bDEuNCAxLjQtNC4yIDQuMiA0LjIgNC4yLTEuNCAxLjRMOS43IDExbC00LjIgNC4yTDQgMTMuOWw0LjItNC4yTDQgNS40IDUuNCA0bDQuMiA0LjJMMTMuOSA0eiIgLz4KPC9zdmc+Cg==')`,
          backgroundRepeat: `no-repeat, repeat`,
          backgroundPosition: `right top 50%`,
          backgroundColor: colors.structure.transparent,
          color: colors.structure.coal,
          cursor: 'pointer',
        },
        table: {
          width: '100%',
          borderSpacing: 0,
        },
        '.js-focus-visible :focus:not(.focus-visible)': {
          outline: 'none',
        },
        '.hidden': {
          border: '0',
          clip: 'rect(0 0 0 0)',
          height: '1px',
          margin: '-1px',
          overflow: 'hidden',
          padding: '0',
          position: 'absolute',
          width: '1px',
        },
        '.react-grid-item.react-grid-placeholder': {
          backgroundColor: colors.key.one,
        },
        '[data-reach-combobox-popover]': {
          fontSize: '100%',
        },
      }}
    />
    <Global
      styles={css`
        @font-face {
          font-family: 'Roboto';
          font-style: normal;
          font-weight: 400;
          src: url('/fonts/roboto-latin-400.woff2');
          src: local('Roboto'), local('Roboto-Regular'),
            url('/fonts/roboto-latin-400.woff2') format('woff2'),
            url('/fonts/roboto-latin-400.woff') format('woff'),
            url('/fonts/roboto-latin-400.ttf') format('truetype');
        }
        @font-face {
          font-family: 'Roboto';
          font-style: normal;
          font-weight: 500;
          src: url('/fonts/roboto-latin-500.woff2');
          src: local('Roboto Medium'), local('Roboto-Medium'),
            url('/fonts/roboto-latin-500.woff2') format('woff2'),
            url('/fonts/roboto-latin-500.woff') format('woff'),
            url('/fonts/roboto-latin-500.ttf') format('truetype');
        }
        @font-face {
          font-family: 'Roboto';
          font-style: normal;
          font-weight: 700;
          src: url('/fonts/roboto-latin-700.woff2');
          src: local('Roboto Bold'), local('Roboto-Bold'),
            url('/fonts/roboto-latin-700.woff2') format('woff2'),
            url('/fonts/roboto-latin-700.woff') format('woff'),
            url('/fonts/roboto-latin-700.ttf') format('truetype');
        }
        @font-face {
          font-family: 'Roboto Condensed';
          font-style: normal;
          font-weight: 400;
          src: url('/fonts/roboto-condensed-v18-latin-400.woff2');
          src: local('Roboto Condensed'), local('RobotoCondensed-Regular'),
            url('/fonts/roboto-condensed-v18-latin-400.woff2') format('woff2'),
            url('/fonts/roboto-condensed-v18-latin-400.woff') format('woff'),
            url('/fonts/roboto-condensed-v18-latin-400.ttf') format('truetype');
        }
        @font-face {
          font-family: 'Roboto Mono';
          font-style: normal;
          font-weight: 400;
          src: url('/fonts/roboto-mono-v7-latin-400.woff2');
          src: local('Roboto Mono'), local('RobotoMono-Regular'),
            url('/fonts/roboto-mono-v7-latin-400.woff2') format('woff2'),
            url('/fonts/roboto-mono-v7-latin-400.woff') format('woff'),
            url('/fonts/roboto-mono-v7-latin-400.ttf') format('truetype');
        }
      `}
    />
  </>
)

export default StylesReset

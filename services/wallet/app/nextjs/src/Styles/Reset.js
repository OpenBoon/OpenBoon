import React from 'react'
import { Global, css } from '@emotion/core'

import { colors, typography, constants, spacing } from '.'

const StylesReset = () => (
  <>
    <Global
      styles={{
        'html, body, #__next': {
          margin: 0,
          padding: 0,
          height: '100%',
        },
        '*, :after, :before': {
          boxSizing: 'border-box',
        },
        '*': {
          WebkitFontSmoothing: 'antialiased',
          MozOsxFontSmoothing: 'grayscale',
        },
        body: {
          fontFamily: `Roboto, Avenir, "sans-serif"`,
          fontSize: typography.size.hecto,
          lineHeight: typography.height.hecto,
          fontWeight: typography.weight.regular,
          color: colors.primaryFont,
          backgroundColor: colors.primaryBackground,
        },
        'input, textarea, select, button': {
          fontFamily: 'inherit',
        },
        h3: {
          fontSize: typography.size.kilo,
          lineHeight: typography.height.kilo,
          fontWeight: typography.weight.regular,
        },
        small: {
          fontSize: typography.size.uno,
          lineHeight: typography.height.uno,
        },
        a: {
          color: colors.primaryFont,
          opacity: constants.opacity.half,
          textDecoration: 'none',
          '&:hover': {
            textDecoration: 'underline',
          },
        },
        input: {
          border: 0,
          padding: spacing.normal,
          fontSize: typography.size.hecto,
        },
      }}
    />
    <Global
      styles={css`
        @font-face {
          font-family: 'Roboto';
          font-style: normal;
          font-weight: 100;
          src: url('/fonts/roboto/roboto-latin-100.woff2');
          src: local('Roboto Thin'), local('Roboto-Thin'),
            url('/fonts/roboto/roboto-latin-100.woff2') format('woff2'),
            url('/fonts/roboto/roboto-latin-100.woff') format('woff'),
            url('/fonts/roboto/roboto-latin-100.ttf') format('truetype');
        }
        @font-face {
          font-family: 'Roboto';
          font-style: normal;
          font-weight: 300;
          src: url('/fonts/roboto/roboto-latin-300.woff2');
          src: local('Roboto Light'), local('Roboto-Light'),
            url('/fonts/roboto/roboto-latin-300.woff2') format('woff2'),
            url('/fonts/roboto/roboto-latin-300.woff') format('woff'),
            url('/fonts/roboto/roboto-latin-300.ttf') format('truetype');
        }
        @font-face {
          font-family: 'Roboto';
          font-style: normal;
          font-weight: 400;
          src: url('/fonts/roboto/roboto-latin-regular.woff2');
          src: local('Roboto'), local('Roboto-Regular'),
            url('/fonts/roboto/roboto-latin-regular.woff2') format('woff2'),
            url('/fonts/roboto/roboto-latin-regular.woff') format('woff'),
            url('/fonts/roboto/roboto-latin-regular.ttf') format('truetype');
        }
        @font-face {
          font-family: 'Roboto';
          font-style: normal;
          font-weight: 500;
          src: url('/fonts/roboto/roboto-latin-500.woff2');
          src: local('Roboto Medium'), local('Roboto-Medium'),
            url('/fonts/roboto/roboto-latin-500.woff2') format('woff2'),
            url('/fonts/roboto/roboto-latin-500.woff') format('woff'),
            url('/fonts/roboto/roboto-latin-500.ttf') format('truetype');
        }
        @font-face {
          font-family: 'Roboto';
          font-style: normal;
          font-weight: 700;
          src: url('/fonts/roboto/roboto-latin-700.woff2');
          src: local('Roboto Bold'), local('Roboto-Bold'),
            url('/fonts/roboto/roboto-latin-700.woff2') format('woff2'),
            url('/fonts/roboto/roboto-latin-700.woff') format('woff'),
            url('/fonts/roboto/roboto-latin-700.ttf') format('truetype');
        }
        @font-face {
          font-family: 'Roboto';
          font-style: normal;
          font-weight: 900;
          src: url('/fonts/roboto/roboto-latin-900.woff2');
          src: local('Roboto Black'), local('Roboto-Black'),
            url('/fonts/roboto/roboto-latin-900.woff2') format('woff2'),
            url('/fonts/roboto/roboto-latin-900.woff') format('woff'),
            url('/fonts/roboto/roboto-latin-900.ttf') format('truetype');
        }
      `}
    />
  </>
)

export default StylesReset

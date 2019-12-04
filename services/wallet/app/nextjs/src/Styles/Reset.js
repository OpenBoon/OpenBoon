import React from 'react'
import { Global } from '@emotion/core'

import { colors, typography, constants, spacing } from '.'

const StylesReset = () => (
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
      '@font-face': [
        {
          fontFamily: 'Roboto',
          fontStyle: 'normal',
          fontWeight: '100',
          src: 'url(../Fonts/Roboto/Roboto-Thin.ttf) format(truetype)',
        },
        {
          fontFamily: 'Roboto',
          fontStyle: 'normal',
          fontWeight: '300',
          src: 'url(../Fonts/Roboto/Roboto-Light.ttf) format(truetype)',
        },
        {
          fontFamily: 'Roboto',
          fontStyle: 'normal',
          fontWeight: '400',
          src: 'url(../Fonts/Roboto/Roboto-Regular.ttf) format(truetype)',
        },
        {
          fontFamily: 'Roboto',
          fontStyle: 'normal',
          fontWeight: '500',
          src: 'url(../Fonts/Roboto/Roboto-Medium.ttf) format(truetype)',
        },
        {
          fontFamily: 'Roboto',
          fontStyle: 'normal',
          fontWeight: '700',
          src: 'url(../Fonts/Roboto/Roboto-Bold.ttf) format(truetype)',
        },
        {
          fontFamily: 'Roboto',
          fontStyle: 'normal',
          fontWeight: '900',
          src: 'url(./.RFonts/Roboto/oboto-Black.ttf) format(truetype)',
        },
      ],
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
)

export default StylesReset

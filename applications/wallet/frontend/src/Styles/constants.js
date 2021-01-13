import { keyframes } from '@emotion/react'

import colors from './colors'

const rotate = keyframes`
  from { transform:rotate(0deg); }
  to { transform:rotate(360deg); }
`

const borderRadius = {
  none: 0,
  small: 2,
  medium: 4,
  large: 14,
  round: 32,
}

const borderWidths = { regular: 1, medium: 2, large: 4 }

const newBorders = Object.entries(borderWidths).reduce((acc, [name, size]) => {
  return {
    ...acc,
    [name]: Object.entries(colors.structure).reduce(
      (acc2, [colorName, colorHex]) => {
        return {
          ...acc2,
          [colorName]: `${size}px solid ${colorHex}`,
        }
      },
      {},
    ),
  }
}, {})

const borders = {
  keyOneRegular: `1px solid ${colors.key.one}`,
  keyOneMedium: `2px solid ${colors.key.one}`,
  keyOneLarge: `4px solid ${colors.key.one}`,
  keyTwoLarge: `4px solid ${colors.key.two}`,
  error: `1px solid ${colors.signal.warning.base}`,
  assetSelected: `4px solid ${colors.signal.sky.base}`,
  ...newBorders,
}

const opacity = {
  background: 0.15,
  hex22Pct: '38',
  third: 0.3,
  half: 0.5,
  // https://gist.github.com/lopspower/03fb1cc0ac9f32ef38f4
  hexHalf: '80',
  eighth: 0.8,
  full: 1,
}

const boxShadows = {
  default: `0 2px 4px 0 rgba(0, 0, 0, ${opacity.half})`,
  input: `inset 0 1px 3px 0 transparent`,
  menu: `0 4px 7px 0 ${colors.structure.black}`,
  navBar: `0 0 4px 0 rgba(0, 0, 0, ${opacity.half})`,
  dropdown: `0 2px 6px 0 ${colors.structure.black}`,
  tableRow: `0 0 5px 0 rgba(0, 0, 0, ${opacity.half})`,
  panel: `3px 0 3px 0px rgba(0, 0, 0, ${opacity.third})`,
  infoBar: `0px 3px 3px 0 rgba(0, 0, 0, ${opacity.third})`,
  inset: `inset 0 0 4px 0 rgba(0, 0, 0, 0.6)`,
}

const constants = {
  borderRadius,
  borderWidths,
  borders,
  opacity,
  boxShadows,
  icons: {
    mini: 16,
    small: 18,
    regular: 20,
    moderate: 22,
    comfy: 24,
    large: 32,
  },
  overlay: `${colors.structure.black}e6`,
  navbar: {
    height: 44,
  },
  form: {
    maxWidth: 470,
  },
  paragraph: {
    maxWidth: 600,
  },
  animations: {
    slide: 'all .15s ease',
    infiniteRotation: `${rotate} 2s linear infinite`,
    dualRotation: `${rotate} 1s linear 2`,
  },
  timeline: {
    rulerRowHeight: 44,
  },
}

export default constants

import { keyframes } from '@emotion/core'

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

const borderWidths = { regular: '1px', medium: '2px', large: '4px' }

const newBorders = Object.entries(borderWidths).reduce((acc, [name, size]) => {
  return {
    ...acc,
    [name]: Object.entries(colors.structure).reduce(
      (acc2, [colorName, colorHex]) => {
        return {
          ...acc2,
          [colorName]: `${size} solid ${colorHex}`,
        }
      },
      {},
    ),
  }
}, {})

console.log(newBorders)

const borders = {
  default: `1px solid ${colors.structure.mattGrey}`,
  transparent: `1px solid transparent`,
  transparentMedium: `2px solid transparent`,
  separator: `1px solid ${colors.structure.zinc}`,
  spacer: `1px solid ${colors.structure.coal}`,
  tabs: `1px solid ${colors.structure.iron}`,
  error: `2px solid ${colors.signal.warning.base}`,
  success: `1px solid ${colors.signal.grass.base}`,
  tableRow: `1px solid ${colors.structure.steel}`,
  radio: `1px solid ${colors.structure.white}`,
  radioMedium: `2px solid ${colors.structure.white}`,
  inputSmall: `1px solid ${colors.key.one}`,
  input: `2px solid ${colors.key.one}`,
  pill: `2px solid ${colors.structure.steel}`,
  assetInactive: `4px solid ${colors.transparent}`,
  assetHover: `4px solid ${colors.structure.white}`,
  assetSelected: `4px solid ${colors.signal.electricBlue.base}`,
  prettyMetadata: `4px solid ${colors.structure.iron}`,
  facet: `4px solid ${colors.key.one}`,
  unselectedFacet: `4px solid ${colors.structure.steel}`,
  metrics: `2px solid ${colors.structure.white}`,
  outline: `thin solid transparent`,
  ...newBorders,
}

const opacity = {
  background: 0.15,
  third: 0.3,
  half: 0.5,
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
  assets: `inset 0 0 4px 0 rgba(0, 0, 0, 0.6)`,
}

const constants = {
  borderRadius,
  borders,
  opacity,
  boxShadows,
  overlay: `${colors.structure.black}e6`,
  navbar: {
    height: 44,
  },
  pageTitle: {
    height: 61,
  },
  tableHeader: {
    height: 45,
  },
  form: {
    maxWidth: 470,
  },
  paragraph: {
    maxWidth: 600,
  },
  animations: {
    infiniteRotation: `${rotate} 2s linear infinite`,
    dualRotation: `${rotate} 1s linear 2`,
  },
}

export default constants

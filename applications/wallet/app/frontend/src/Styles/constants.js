import colors from './colors'

const borderRadius = {
  small: 2,
}

const borders = {
  default: `1px solid ${colors.grey5}`,
  transparent: `1px solid transparent`,
  separator: `1px solid ${colors.rocks.pewter}`,
}

const opacity = {
  half: 0.59,
}

const boxShadows = {
  default: `0 2px 4px 0 rgba(0, 0, 0, ${opacity.half})`,
  input: `inset 0 1px 3px 0 transparent`,
  menu: `0 4px 7px 0 ${colors.rocks.black}`,
  navBar: `0 0 4px 0 rgba(0, 0, 0, ${opacity.half})`,
  dropdown: `0 2px 6px 0 ${colors.rocks.black}`,
}

const constants = {
  borderRadius,
  borders,
  opacity,
  boxShadows,
  navbar: {
    height: 40,
  },
}

export default constants

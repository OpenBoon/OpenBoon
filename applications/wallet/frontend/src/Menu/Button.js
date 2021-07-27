import PropTypes from 'prop-types'

import stylesShape from '../Styles/shape'

import { colors, constants, spacing } from '../Styles'

import ChevronSvg from '../Icons/chevron.svg'

import Button, { VARIANTS } from '../Button'

const WIDTH = 200

const MenuButton = ({ onBlur, onClick, legend, style }) => (
  <Button
    aria-label="Toggle Dropdown Menu"
    variant={VARIANTS.NEUTRAL}
    css={{
      '&,&:hover,&:visited': {
        backgroundColor: colors.structure.steel,
      },
      '&:hover': {
        backgroundColor: colors.structure.zinc,
      },
      '&[aria-disabled=true]': {
        backgroundColor: colors.structure.steel,
      },
      width: WIDTH,
      paddingTop: spacing.base,
      paddingBottom: spacing.base,
      paddingLeft: spacing.normal,
      paddingRight: spacing.moderate,
      flexDirection: 'row',
      justifyContent: 'space-between',
      marginBottom: spacing.small,
      color: colors.structure.white,
      ...style,
    }}
    onBlur={onBlur}
    onClick={onClick}
  >
    {legend}
    <ChevronSvg height={constants.icons.regular} />
  </Button>
)

MenuButton.defaultProps = {
  style: {},
}

MenuButton.propTypes = {
  onBlur: PropTypes.func.isRequired,
  onClick: PropTypes.func.isRequired,
  legend: PropTypes.node.isRequired,
  style: stylesShape,
}

export default MenuButton

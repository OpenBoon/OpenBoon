import PropTypes from 'prop-types'

import ChevronSvg from '../Icons/chevron.svg'

import Button, { VARIANTS } from '../Button'
import { spacing, colors } from '../Styles'

const WIDTH = 200
const ICON_SIZE = 20

const MenuButton = ({ onBlur, onClick, legend }) => (
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
    }}
    onBlur={onBlur}
    onClick={onClick}
    isDisabled={false}
  >
    {legend}
    <ChevronSvg height={ICON_SIZE} />
  </Button>
)

MenuButton.propTypes = {
  onBlur: PropTypes.func.isRequired,
  onClick: PropTypes.func.isRequired,
  legend: PropTypes.string.isRequired,
}

export default MenuButton

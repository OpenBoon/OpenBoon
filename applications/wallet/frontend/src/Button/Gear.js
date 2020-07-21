import PropTypes from 'prop-types'

import { colors, spacing, constants } from '../Styles'

import Button, { VARIANTS } from '.'

import GearSvg from '../Icons/gear.svg'

const ButtonGear = ({ onBlur, onClick }) => (
  <Button
    aria-label="Toggle Actions Menu"
    className="gear"
    variant={VARIANTS.NEUTRAL}
    style={{
      color: colors.structure.coal,
      padding: spacing.moderate / 2,
      margin: -spacing.moderate / 2,
      borderRadius: constants.borderRadius.round,
      ':hover': {
        color: `${colors.structure.white} !important`,
        backgroundColor: colors.structure.steel,
      },
    }}
    onBlur={onBlur}
    onClick={onClick}
    isDisabled={false}
  >
    <GearSvg height={constants.icons.regular} />
  </Button>
)

ButtonGear.propTypes = {
  onBlur: PropTypes.func.isRequired,
  onClick: PropTypes.func.isRequired,
}

export default ButtonGear

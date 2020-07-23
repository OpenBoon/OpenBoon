import PropTypes from 'prop-types'

import { colors, spacing, constants } from '../Styles'

import Button, { VARIANTS } from '.'

import KebabSvg from '../Icons/kebab.svg'

const ButtonActions = ({ onBlur, onClick }) => (
  <Button
    aria-label="Toggle Actions Menu"
    className="actions"
    variant={VARIANTS.NEUTRAL}
    style={{
      color: colors.structure.coal,
      padding: spacing.moderate / 2,
      margin: -spacing.moderate / 2,
      ':hover, &.focus-visible:focus': {
        color: `${colors.structure.white} !important`,
      },
    }}
    onBlur={onBlur}
    onClick={onClick}
    isDisabled={false}
  >
    <KebabSvg height={constants.icons.regular} />
  </Button>
)

ButtonActions.propTypes = {
  onBlur: PropTypes.func.isRequired,
  onClick: PropTypes.func.isRequired,
}

export default ButtonActions

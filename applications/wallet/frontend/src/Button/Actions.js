import PropTypes from 'prop-types'

import stylesShape from '../Styles/shape'

import { colors, spacing, constants } from '../Styles'

import Button, { VARIANTS } from '.'

import KebabSvg from '../Icons/kebab.svg'

const ButtonActions = ({ onBlur, onClick, style }) => {
  return (
    <Button
      aria-label="Toggle Actions Menu"
      className="actions"
      variant={VARIANTS.NEUTRAL}
      style={{
        color: colors.structure.steel,
        padding: spacing.moderate / 2,
        margin: -spacing.moderate / 2,
        ':hover, &.focus-visible:focus': {
          color: `${colors.structure.white} !important`,
        },
        ...style,
      }}
      onBlur={onBlur}
      onClick={onClick}
    >
      <KebabSvg height={constants.icons.regular} />
    </Button>
  )
}

ButtonActions.defaultProps = {
  style: {},
}

ButtonActions.propTypes = {
  onBlur: PropTypes.func.isRequired,
  onClick: PropTypes.func.isRequired,
  style: stylesShape,
}

export default ButtonActions

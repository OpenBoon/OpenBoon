import PropTypes from 'prop-types'

import { constants, spacing, colors } from '../Styles'

export const noop = () => () => {}

const INPUT_WIDTH = 52

const BASE = {
  textAlign: 'center',
  borderRadius: constants.borderRadius.small,
  backgroundColor: colors.structure.lead,
  color: colors.structure.white,
  ':focus': {
    outline: constants.borders.regular.transparent,
    border: constants.borders.keyOneRegular,
    color: colors.structure.coal,
    backgroundColor: colors.structure.white,
  },
}

const STYLES = {
  PRIMARY: {
    paddingLeft: spacing.moderate,
    paddingRight: spacing.moderate,
    paddingTop: spacing.normal,
    paddingBottom: spacing.normal,
    border: constants.borders.regular.transparent,
    width: '60%',
    ':hover': {
      border: constants.borders.regular.steel,
    },
  },
  SECONDARY: {
    padding: spacing.small,
    border: constants.borders.regular.iron,
    width: INPUT_WIDTH,
    ':hover': {
      border: constants.borders.regular.white,
    },
  },
}

export const VARIANTS = Object.keys(STYLES).reduce(
  (accumulator, style) => ({ ...accumulator, [style]: style }),
  {},
)

const InputRange = ({
  label,
  value,
  onChange,
  onKeyPress,
  onBlur,
  variant,
}) => {
  return (
    <label css={{ display: 'flex', alignItems: 'center' }}>
      {label} &nbsp;
      <input
        type="text"
        css={{ ...BASE, ...STYLES[variant] }}
        value={value}
        onChange={onChange}
        onKeyPress={onKeyPress}
        onBlur={onBlur}
      />
    </label>
  )
}

InputRange.defaultProps = {
  onKeyPress: noop,
  onBlur: noop,
}

InputRange.propTypes = {
  label: PropTypes.string.isRequired,
  value: PropTypes.oneOfType([PropTypes.number, PropTypes.string]).isRequired,
  onChange: PropTypes.func.isRequired,
  onKeyPress: PropTypes.func,
  onBlur: PropTypes.func,
  variant: PropTypes.oneOf(Object.keys(VARIANTS)).isRequired,
}

export default InputRange

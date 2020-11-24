import PropTypes from 'prop-types'

import { constants, spacing, colors, typography } from '../Styles'

const PADDING = spacing.base
const BORDER_WIDTH = 2

const BASE = {
  outlineOffset: 0,
  padding: `${spacing.moderate - BORDER_WIDTH}px ${spacing.moderate}px`,
  borderRadius: constants.borderRadius.small,
  boxShadow: constants.boxShadows.input,
  width: '100%',
  fontSize: typography.size.regular,
  lineHeight: typography.height.regular,
}

const STYLES = {
  PRIMARY: {
    color: colors.structure.black,
    backgroundColor: colors.structure.white,
  },
  SECONDARY: {
    color: colors.structure.white,
    backgroundColor: colors.structure.smoke,
    '&:focus, .noop': {
      border: constants.borders.keyOneMedium,
      backgroundColor: colors.structure.white,
      outline: colors.key.one,
      color: colors.structure.black,
    },
  },
}

const BORDER_STYLES = ({ hasError }) => ({
  border: hasError
    ? constants.borders.error
    : constants.borders.regular.transparent,
  borderWidth: BORDER_WIDTH,
})

export const VARIANTS = Object.keys(STYLES).reduce(
  (accumulator, style) => ({ ...accumulator, [style]: style }),
  {},
)

const Textarea = ({
  id,
  variant,
  label,
  value,
  onChange,
  hasError,
  errorMessage,
  ...props
}) => (
  <div css={{ paddingTop: PADDING }}>
    <label
      htmlFor={id}
      css={{
        display: 'block',
        paddingBottom: PADDING,
        color: colors.structure.zinc,
      }}
    >
      {label}
    </label>
    <div css={{ position: 'relative' }}>
      <textarea
        id={id}
        rows="5"
        name={id}
        value={value}
        onChange={onChange}
        css={{ ...BASE, ...STYLES[variant], ...BORDER_STYLES({ hasError }) }}
        // eslint-disable-next-line react/jsx-props-no-spreading
        {...props}
      />
    </div>
    <div css={{ paddingTop: PADDING }}>
      {hasError && (
        <span
          css={{
            color: colors.signal.warning.base,
            fontStyle: typography.style.italic,
          }}
        >
          {errorMessage}
        </span>
      )}
      &nbsp;
    </div>
  </div>
)

Textarea.defaultProps = {
  errorMessage: '',
}

Textarea.propTypes = {
  id: PropTypes.string.isRequired,
  variant: PropTypes.oneOf(Object.keys(VARIANTS)).isRequired,
  label: PropTypes.string.isRequired,
  value: PropTypes.string.isRequired,
  onChange: PropTypes.func.isRequired,
  hasError: PropTypes.bool.isRequired,
  errorMessage: PropTypes.string,
}

export default Textarea

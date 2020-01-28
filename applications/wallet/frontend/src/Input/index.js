import PropTypes from 'prop-types'

import { constants, spacing, colors, typography } from '../Styles'

const PADDING = spacing.base
const BORDER_WIDTH = 2

const BASE = {
  outlineOffset: 0,
  padding: spacing.moderate,
  borderRadius: constants.borderRadius.small,
  boxShadow: constants.boxShadows.input,
  width: '100%',
}

const STYLES = {
  PRIMARY: {
    color: colors.structure.black,
    backgroundColor: colors.structure.white,
  },
  SECONDARY: {
    color: colors.structure.white,
    backgroundColor: colors.structure.mattGrey,
    '&:focus': {
      border: constants.borders.input,
      backgroundColor: colors.structure.white,
      outline: colors.key.one,
      color: colors.structure.black,
    },
  },
}

const BORDER_STYLES = ({ hasError }) => ({
  border: hasError ? constants.borders.error : constants.borders.transparent,
  borderWidth: BORDER_WIDTH,
})

export const VARIANTS = Object.keys(STYLES).reduce(
  (accumulator, style) => ({ ...accumulator, [style]: style }),
  {},
)

const Input = ({
  id,
  variant,
  type,
  label,
  value,
  onChange,
  hasError,
  errorMessage,
  after,
  ...props
}) => (
  <div css={{ paddingTop: PADDING, paddingBottom: PADDING }}>
    <label htmlFor={id} css={{ display: 'block', paddingBottom: PADDING }}>
      {label}
    </label>
    <div css={{ position: 'relative' }}>
      <input
        id={id}
        type={type}
        name={id}
        value={value}
        onChange={onChange}
        css={[BASE, STYLES[variant], BORDER_STYLES({ hasError })]}
        // eslint-disable-next-line react/jsx-props-no-spreading
        {...props}
      />
      {!!after && (
        <div
          css={{
            position: 'absolute',
            right: 0,
            top: 0,
            bottom: 0,
            display: 'flex',
            justifyContent: 'center',
          }}>
          {after}
        </div>
      )}
      <div css={{ paddingTop: PADDING }}>
        &nbsp;
        {hasError && (
          <span
            css={{
              color: colors.signal.warning.base,
              fontStyle: typography.style.italic,
            }}>
            {errorMessage}
          </span>
        )}
      </div>
    </div>
  </div>
)

Input.defaultProps = {
  after: false,
  errorMessage: '',
}

Input.propTypes = {
  id: PropTypes.string.isRequired,
  variant: PropTypes.oneOf(Object.keys(VARIANTS)).isRequired,
  type: PropTypes.oneOf(['text', 'password']).isRequired,
  label: PropTypes.string.isRequired,
  value: PropTypes.string.isRequired,
  onChange: PropTypes.func.isRequired,
  hasError: PropTypes.bool.isRequired,
  errorMessage: PropTypes.string,
  after: PropTypes.node,
}

export default Input

import PropTypes from 'prop-types'

import { constants, spacing } from '../Styles'

const PADDING = spacing.base

const Input = ({
  id,
  type,
  label,
  value,
  onChange,
  hasError,
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
        css={{
          outlineOffset: 0,
          padding: spacing.moderate,
          borderRadius: constants.borderRadius.small,
          boxShadow: constants.boxShadows.input,
          width: '100%',
          border: hasError
            ? constants.borders.error
            : constants.borders.transparent,
        }}
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
    </div>
  </div>
)

Input.defaultProps = {
  after: false,
}

Input.propTypes = {
  id: PropTypes.string.isRequired,
  type: PropTypes.oneOf(['text', 'password']).isRequired,
  label: PropTypes.string.isRequired,
  value: PropTypes.string.isRequired,
  onChange: PropTypes.func.isRequired,
  hasError: PropTypes.bool.isRequired,
  after: PropTypes.node,
}

export default Input

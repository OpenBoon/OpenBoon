import PropTypes from 'prop-types'

import { constants, spacing } from '../Styles'

const Input = ({ id, type, label, value, onChange, ...props }) => (
  <div css={{ paddingTop: spacing.moderate, paddingBottom: spacing.moderate }}>
    <label
      htmlFor={id}
      css={{ display: 'block', paddingBottom: spacing.moderate }}>
      {label}
    </label>
    <input
      id={id}
      type={type}
      name={id}
      value={value}
      onChange={onChange}
      css={{
        padding: spacing.moderate,
        borderRadius: constants.borderRadius.small,
        boxShadow: constants.boxShadows.input,
        width: '100%',
      }}
      // eslint-disable-next-line react/jsx-props-no-spreading
      {...props}
    />
  </div>
)

Input.propTypes = {
  id: PropTypes.string.isRequired,
  type: PropTypes.oneOf(['text', 'password']).isRequired,
  label: PropTypes.string.isRequired,
  value: PropTypes.string.isRequired,
  onChange: PropTypes.func.isRequired,
}

export default Input

import PropTypes from 'prop-types'

import { constants, spacing } from '../Styles'

const HEIGHT = 39

const Input = ({ id, type, label, value, onChange }) => (
  <div css={{ paddingTop: spacing.normal, paddingBottom: spacing.moderate }}>
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
        height: HEIGHT,
        borderRadius: constants.borderRadius.small,
        boxShadow: constants.boxShadows.input,
        width: '100%',
      }}
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

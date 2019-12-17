import { useState } from 'react'
import PropTypes from 'prop-types'

import { colors, spacing } from '../Styles'

import CheckboxIcon from './Icon'

const Checkbox = ({ label, onClick, initialValue }) => {
  const [isChecked, setIsChecked] = useState(initialValue)

  return (
    <label
      css={{
        display: 'flex',
        alignItems: 'center',
        color: colors.white,
        cursor: 'pointer',
      }}>
      <CheckboxIcon
        isChecked={isChecked}
        onClick={() => {
          setIsChecked(!isChecked)
          onClick(!isChecked)
        }}
      />
      <span css={{ paddingLeft: spacing.moderate }}>{label}</span>
    </label>
  )
}

Checkbox.propTypes = {
  label: PropTypes.string.isRequired,
  onClick: PropTypes.func.isRequired,
  initialValue: PropTypes.bool.isRequired,
}

export default Checkbox

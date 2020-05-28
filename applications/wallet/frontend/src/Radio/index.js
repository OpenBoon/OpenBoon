import PropTypes from 'prop-types'
import { useState } from 'react'

import { spacing, typography } from '../Styles'

import RadioIcon from './Icon'

const Radio = ({
  option: { value, label, initialValue, isDisabled },
  onClick,
}) => {
  const [isChecked, setIsChecked] = useState(initialValue)

  return (
    <label css={{ display: 'flex' }}>
      <RadioIcon
        value={value}
        isChecked={isChecked}
        onClick={() => {
          if (isDisabled) return
          setIsChecked(!isChecked)
          onClick(!isChecked)
        }}
      />

      <div
        css={{
          paddingLeft: spacing.base,
          fontWeight: typography.weight.bold,
        }}
      >
        {label}
      </div>
    </label>
  )
}

Radio.propTypes = {
  option: PropTypes.shape({
    value: PropTypes.string.isRequired,
    label: PropTypes.string.isRequired,
    initialValue: PropTypes.bool.isRequired,
    isDisabled: PropTypes.bool.isRequired,
  }).isRequired,
  onClick: PropTypes.func.isRequired,
}

export default Radio

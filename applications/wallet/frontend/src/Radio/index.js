import PropTypes from 'prop-types'
import { useState } from 'react'

import { spacing, typography } from '../Styles'

import RadioIcon from './Icon'

const Radio = ({
  option: { value, label, legend, initialValue, isDisabled },
  onClick,
}) => {
  const [isChecked, setIsChecked] = useState(initialValue)

  return (
    <div>
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
      <div css={{ paddingLeft: spacing.comfy }}>{legend}</div>
    </div>
  )
}

Radio.propTypes = {
  option: PropTypes.shape({
    value: PropTypes.string.isRequired,
    label: PropTypes.string.isRequired,
    legend: PropTypes.string.isRequired,
    initialValue: PropTypes.bool.isRequired,
    isDisabled: PropTypes.bool.isRequired,
  }).isRequired,
  onClick: PropTypes.func.isRequired,
}

export default Radio

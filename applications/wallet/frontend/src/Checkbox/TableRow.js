/* eslint-disable jsx-a11y/label-has-associated-control */
import { useState } from 'react'
import PropTypes from 'prop-types'

import checkboxOptionShape from './optionShape'

import CheckboxIcon from './Icon'

const CheckboxTableRow = ({
  option: { value, label, initialValue, isDisabled },
  onClick,
}) => {
  const [isChecked, setIsChecked] = useState(initialValue)

  const toggleValue = (event) => {
    event.preventDefault()
    if (isDisabled) return
    setIsChecked(!isChecked)
    onClick(!isChecked)
  }

  return (
    <tr
      css={{ cursor: isDisabled ? 'not-allowed' : 'pointer' }}
      onClick={toggleValue}
    >
      <td>
        <label
          css={{
            // prevents text highlight on double click
            userSelect: 'none', // Chrome
            WebkitUserSelect: 'none', // Safari
            MozUserSelect: 'none', // Firefox
          }}
        >
          <CheckboxIcon
            value={value}
            isChecked={isChecked}
            isDisabled={isDisabled}
            onClick={toggleValue}
          />
          <div className="hidden">
            {value}: {label}
          </div>
        </label>
      </td>
      <td>{value}</td>
      <td>{label}</td>
    </tr>
  )
}

CheckboxTableRow.propTypes = {
  option: PropTypes.shape(checkboxOptionShape).isRequired,
  onClick: PropTypes.func.isRequired,
}

export default CheckboxTableRow

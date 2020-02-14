/* eslint-disable jsx-a11y/label-has-associated-control */
import { useState } from 'react'
import PropTypes from 'prop-types'

import CheckboxIcon from './Icon'

const CheckboxTableRow = ({ value, label, legend, initialValue, onClick }) => {
  const [isChecked, setIsChecked] = useState(initialValue)

  const toggleValue = event => {
    event.preventDefault()
    setIsChecked(!isChecked)
    onClick(!isChecked)
  }

  return (
    <tr css={{ cursor: 'pointer' }} onClick={toggleValue}>
      <td>
        <label
          css={{
            // prevents text highlight on double click
            userSelect: 'none', // Chrome
            WebkitUserSelect: 'none', // Safari
            MozUserSelect: 'none', // Firefox
          }}>
          <CheckboxIcon
            value={value}
            isChecked={isChecked}
            onClick={toggleValue}
          />
          <div className="hidden">
            {value}, {label}: {legend}
          </div>
        </label>
      </td>
      <td>{value}</td>
      <td>
        <strong>{label}:&nbsp;</strong>
        {legend}
      </td>
    </tr>
  )
}

CheckboxTableRow.propTypes = {
  value: PropTypes.string.isRequired,
  label: PropTypes.string.isRequired,
  legend: PropTypes.string.isRequired,
  onClick: PropTypes.func.isRequired,
  initialValue: PropTypes.bool.isRequired,
}

export default CheckboxTableRow

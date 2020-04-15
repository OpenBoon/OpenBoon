import React from 'react'
import PropTypes from 'prop-types'

import { colors, constants } from '../Styles'

import CheckmarkSvg from '../Icons/checkmark.svg'

const SIZE = 20

const getBorder = ({ isChecked, isDisabled, disabledChecked }) => {
  if (disabledChecked) return 'none'

  if (isChecked) return `2px solid ${colors.key.one}`

  if (isDisabled) return `2px solid ${colors.structure.mattGrey}`

  return `2px solid ${colors.structure.steel}`
}

const CheckboxIcon = ({ value, isChecked, isDisabled, onClick }) => {
  const disabledChecked = isDisabled && isChecked

  return (
    <div css={{ display: 'flex', position: 'relative' }}>
      <input
        type="checkbox"
        value={value}
        defaultChecked={isChecked}
        onClick={onClick}
        css={{
          margin: 0,
          padding: 0,
          width: SIZE,
          height: SIZE,
          WebkitAppearance: 'none',
          backgroundColor:
            isChecked && !disabledChecked ? colors.key.one : colors.transparent,
          border: getBorder({ isChecked, isDisabled, disabledChecked }),
          borderRadius: constants.borderRadius.small,
          cursor: isDisabled ? 'not-allowed' : 'pointer',
        }}
      />
      <div
        css={{
          position: 'absolute',
          top: 0,
          left: 0,
          bottom: 0,
          width: SIZE,
          alignItems: 'center',
          justifyContent: 'center',
          display: 'flex',
          cursor: isDisabled || disabledChecked ? 'not-allowed' : 'pointer',
        }}
      >
        <CheckmarkSvg
          width={20}
          css={{
            path: {
              transition: 'all .3s ease',
              opacity: isChecked ? 100 : 0,
              fill: disabledChecked ? colors.key.one : colors.white,
            },
          }}
        />
      </div>
    </div>
  )
}

CheckboxIcon.propTypes = {
  value: PropTypes.string.isRequired,
  isChecked: PropTypes.bool.isRequired,
  isDisabled: PropTypes.bool.isRequired,
  onClick: PropTypes.func.isRequired,
}

export default CheckboxIcon

import React from 'react'
import PropTypes from 'prop-types'

import { colors, constants } from '../Styles'

import CheckmarkSvg from '../Icons/checkmark.svg'

const SIZE = 20

const CheckboxIcon = ({ value, isChecked, onClick }) => (
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
        backgroundColor: isChecked ? colors.key.one : colors.transparent,
        border: isChecked
          ? `2px solid ${colors.key.one}`
          : `2px solid ${colors.structure.steel}`,
        borderRadius: constants.borderRadius.small,
        cursor: 'pointer',
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
      }}>
      <CheckmarkSvg
        width={14}
        css={{
          path: {
            transition: 'all .3s ease',
            opacity: isChecked ? 100 : 0,
            fill: colors.white,
          },
        }}
      />
    </div>
  </div>
)

CheckboxIcon.propTypes = {
  value: PropTypes.string.isRequired,
  onClick: PropTypes.func.isRequired,
  isChecked: PropTypes.bool.isRequired,
}

export default CheckboxIcon

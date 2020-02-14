import React from 'react'
import PropTypes from 'prop-types'

import { colors, constants } from '../Styles'

import CheckmarkSvg from '../Icons/checkmark.svg'
import CheckboxPartialSvg from '../Icons/checkboxpartial.svg'
import { CHECKMARK_WIDTH } from '../Accordion'

const SIZE = 20

const STYLES = {
  PRIMARY: {
    size: SIZE,
  },
  SECONDARY: {
    size: CHECKMARK_WIDTH,
  },
}

export const VARIANTS = Object.keys(STYLES).reduce(
  (accumulator, style) => ({ ...accumulator, [style]: style }),
  {},
)

const CheckboxIcon = ({ variant, value, isChecked, isPartial, onClick }) => {
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
          width: STYLES[variant].size,
          height: STYLES[variant].size,
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
          width: STYLES[variant].size,
          alignItems: 'center',
          justifyContent: 'center',
          display: 'flex',
        }}>
        {isPartial ? (
          <CheckboxPartialSvg
            width={STYLES[variant].size}
            css={{
              path: {
                transition: 'all .3s ease',
              },
              color: colors.key.one,
            }}
          />
        ) : (
          <CheckmarkSvg
            width={STYLES[variant].size}
            css={{
              path: {
                transition: 'all .3s ease',
                opacity: isChecked ? 100 : 0,
                fill: colors.white,
              },
            }}
          />
        )}
      </div>
    </div>
  )
}

CheckboxIcon.propTypes = {
  variant: PropTypes.oneOf(Object.keys(VARIANTS)).isRequired,
  value: PropTypes.string.isRequired,
  onClick: PropTypes.func.isRequired,
  isChecked: PropTypes.bool.isRequired,
  isPartial: PropTypes.bool.isRequired,
}

export default CheckboxIcon

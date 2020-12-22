import { useState } from 'react'
import PropTypes from 'prop-types'

import checkboxOptionShape from './optionShape'

import { colors, spacing, typography, constants } from '../Styles'

const WIDTH = 30
const HEIGHT = 16

const CheckboxSwitch = ({
  option: { value, label, initialValue },
  onClick,
}) => {
  const [hasFocus, setHasFocus] = useState(false)
  const [isChecked, setIsChecked] = useState(initialValue)

  return (
    <label
      css={{
        display: 'flex',
        alignItems: 'center',
        cursor: 'pointer',
        padding: spacing.small,
        ':hover': {
          backgroundColor: colors.structure.mattGrey,
          div: {
            color: isChecked ? colors.key.one : colors.structure.white,
          },
          rect: {
            stroke: isChecked ? colors.structure.smoke : colors.structure.white,
          },
          circle: {
            fill: isChecked ? colors.key.one : colors.structure.white,
          },
        },
        ...(hasFocus
          ? {
              backgroundColor: colors.structure.mattGrey,
              outline: 'auto 5px -webkit-focus-ring-color',
              div: {
                color: isChecked ? colors.key.one : colors.structure.white,
              },
              rect: {
                stroke: isChecked
                  ? colors.structure.smoke
                  : colors.structure.white,
              },
              circle: {
                fill: isChecked ? colors.key.one : colors.structure.white,
              },
            }
          : {}),
      }}
    >
      <input
        className="hidden"
        type="checkbox"
        value={value}
        defaultChecked={isChecked}
        onFocus={({ target }) => {
          setHasFocus(target.className.includes('focus-visible'))
        }}
        onBlur={() => setHasFocus(false)}
        onClick={() => {
          onClick(!isChecked)
          setIsChecked(!isChecked)
        }}
      />

      <svg width={WIDTH} height={HEIGHT}>
        <rect
          css={{ transition: constants.animations.slide }}
          stroke={colors.structure.smoke}
          fill={colors.structure.coal}
          x={0}
          y={HEIGHT / 4}
          width={WIDTH}
          height={HEIGHT / 2}
          rx={HEIGHT / 4}
        />
        <circle
          css={{
            transition: constants.animations.slide,
            fill: isChecked ? colors.key.one : colors.structure.iron,
          }}
          cx={isChecked ? WIDTH - HEIGHT / 2 : HEIGHT / 2}
          cy={HEIGHT / 2}
          r={HEIGHT / 2}
        />
      </svg>

      <div
        css={{
          display: 'flex',
          alignItems: 'center',
          whiteSpace: 'nowrap',
          marginLeft: spacing.base,
          textTransform: 'uppercase',
          transition: constants.animations.slide,
          color: isChecked ? colors.key.one : colors.structure.steel,
          userSelect: 'none',
          fontFamily: typography.family.condensed,
          marginBottom: -spacing.hairline,
        }}
      >
        {label}
      </div>
    </label>
  )
}

CheckboxSwitch.propTypes = {
  option: PropTypes.shape(checkboxOptionShape).isRequired,
  onClick: PropTypes.func.isRequired,
}

export default CheckboxSwitch

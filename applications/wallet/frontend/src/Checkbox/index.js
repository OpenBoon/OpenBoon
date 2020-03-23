/* eslint-disable jsx-a11y/label-has-associated-control */
import { useState } from 'react'
import PropTypes from 'prop-types'

import checkboxOptionShape from './optionShape'

import { colors, spacing, typography } from '../Styles'

import CheckboxIcon from './Icon'

const STYLES = {
  PRIMARY: {
    main: {
      display: 'flex',
      flexDirection: 'column',
    },
    label: {
      alignItems: 'flex-start',
      paddingLeft: 0,
    },
    legend: {
      paddingLeft: 0,
    },
  },
  SECONDARY: {
    main: {
      width: 'max-content',
      paddingLeft: spacing.normal,
    },
    label: {
      paddingLeft: spacing.normal,
      alignItems: 'center',
    },
    legend: {
      paddingLeft: spacing.moderate,
    },
  },
}

export const VARIANTS = Object.keys(STYLES).reduce(
  (accumulator, style) => ({ ...accumulator, [style]: style }),
  {},
)

const Checkbox = ({
  variant,
  option: { value, label, icon, legend, initialValue, isDisabled },
  onClick,
}) => {
  const [isChecked, setIsChecked] = useState(initialValue)

  return (
    <label
      css={{
        display: 'flex',
        alignItems: legend ? STYLES[variant].label.alignItems : 'center',
        color: colors.white,
        cursor: isDisabled ? 'not-allowed' : 'pointer',
        paddingBottom: spacing.normal,
        paddingLeft: STYLES[variant].label.paddingLeft,
      }}
    >
      <CheckboxIcon
        value={value}
        isChecked={isChecked}
        isDisabled={isDisabled}
        onClick={() => {
          if (isDisabled) return
          setIsChecked(!isChecked)
          onClick(!isChecked)
        }}
      />
      {!!icon && (
        <div
          css={{
            paddingLeft: spacing.comfy,
            display: 'flex',
            alignItems: 'center',
          }}
        >
          {icon}
        </div>
      )}
      <div css={[{ paddingLeft: spacing.moderate }, STYLES[variant].main]}>
        <span
          css={{
            color: colors.structure.white,
            fontSize: typography.size.regular,
            lineHeight: typography.height.regular,
            fontWeight: typography.weight.bold,
          }}
        >
          {label}
        </span>
        {!!legend && (
          <span
            css={[
              {
                color: colors.structure.zinc,
              },
              STYLES[variant].legend,
            ]}
          >
            {legend}
          </span>
        )}
      </div>
    </label>
  )
}

Checkbox.propTypes = {
  variant: PropTypes.oneOf(Object.keys(VARIANTS)).isRequired,
  option: PropTypes.shape(checkboxOptionShape).isRequired,
  onClick: PropTypes.func.isRequired,
}

export default Checkbox

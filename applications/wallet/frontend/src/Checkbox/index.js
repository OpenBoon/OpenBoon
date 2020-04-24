/* eslint-disable jsx-a11y/label-has-associated-control */
import { useState } from 'react'
import PropTypes from 'prop-types'

import checkboxOptionShape from './optionShape'

import { colors, spacing, typography } from '../Styles'

import CheckboxIcon from './Icon'

const STYLES = {
  MULTILINE: {
    label: {
      alignItems: 'flex-start',
      paddingLeft: 0,
      paddingBottom: spacing.normal,
    },
    icon: { size: 20 },
    main: {
      display: 'flex',
      flexDirection: 'column',
      paddingLeft: spacing.moderate,
    },
    value: {
      color: colors.structure.white,
      fontSize: typography.size.regular,
      lineHeight: typography.height.regular,
      fontWeight: typography.weight.bold,
    },
    legend: {
      paddingLeft: 0,
      color: colors.structure.zinc,
    },
  },
  INLINE: {
    label: {
      alignItems: 'center',
      paddingLeft: spacing.normal,
      paddingBottom: spacing.normal,
    },
    icon: { size: 20 },
    main: {
      width: 'max-content',
      paddingLeft: spacing.normal,
    },
    value: {
      color: colors.structure.white,
      fontSize: typography.size.regular,
      lineHeight: typography.height.regular,
      fontWeight: typography.weight.bold,
    },
    legend: {
      paddingLeft: spacing.moderate,
      color: colors.structure.zinc,
    },
  },
  SMALL: {
    label: {
      alignItems: 'center',
      paddingLeft: 0,
      paddingBottom: 0,
    },
    icon: { size: 16 },
    main: {
      width: 'max-content',
      paddingLeft: spacing.base,
    },
    value: {
      color: colors.structure.zinc,
      fontSize: typography.size.regular,
      lineHeight: typography.height.regular,
    },
    legend: {
      paddingLeft: spacing.moderate,
      color: colors.structure.zinc,
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
        color: colors.white,
        cursor: isDisabled ? 'not-allowed' : 'pointer',
        paddingBottom: STYLES[variant].label.paddingBottom,
        paddingLeft: STYLES[variant].label.paddingLeft,
        alignItems: legend ? STYLES[variant].label.alignItems : 'center',
      }}
    >
      <CheckboxIcon
        size={STYLES[variant].icon.size}
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
      <div css={STYLES[variant].main}>
        <span css={STYLES[variant].value}>{label}</span>
        {!!legend && <span css={STYLES[variant].legend}>{legend}</span>}
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

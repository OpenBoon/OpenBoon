/* eslint-disable jsx-a11y/label-has-associated-control */
import { useState } from 'react'
import PropTypes from 'prop-types'

import checkboxOptionShape from './optionShape'

import { colors, spacing, typography, constants } from '../Styles'

import CheckboxIcon from './Icon'

const STYLES = {
  PRIMARY: {
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
  SECONDARY: {
    label: {
      alignItems: 'center',
      paddingLeft: spacing.normal,
      paddingBottom: spacing.normal,
    },
    icon: { size: 20 },
    main: {
      display: 'flex',
      flexDirection: 'column',
      paddingLeft: spacing.normal,
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
      fontFamily: typography.family.mono,
      fontSize: typography.size.small,
      lineHeight: typography.height.small,
    },
    legend: {
      paddingLeft: spacing.moderate,
    },
  },
  MENU: {
    label: {
      alignItems: 'center',
      padding: spacing.base,
      borderTop: constants.borders.medium.steel,
    },
    icon: { size: 16 },
    main: {
      display: 'flex',
      overflow: 'hidden',
      paddingLeft: spacing.base,
    },
    value: {
      overflow: 'hidden',
      textOverflow: 'ellipsis',
      whiteSpace: 'nowrap',
    },
    legend: {
      paddingLeft: spacing.mini,
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
  onBlur,
}) => {
  const [isChecked, setIsChecked] = useState(initialValue)

  return (
    <label
      css={{
        display: 'flex',
        color: colors.white,
        cursor: isDisabled ? 'not-allowed' : 'pointer',
        ...STYLES[variant].label,
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
        onBlur={onBlur}
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

Checkbox.defaultProps = {
  onBlur: undefined,
}

Checkbox.propTypes = {
  variant: PropTypes.oneOf(Object.keys(VARIANTS)).isRequired,
  option: PropTypes.shape(checkboxOptionShape).isRequired,
  onClick: PropTypes.func.isRequired,
  onBlur: PropTypes.func,
}

export default Checkbox

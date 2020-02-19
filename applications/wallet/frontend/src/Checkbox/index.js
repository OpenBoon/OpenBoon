/* eslint-disable jsx-a11y/label-has-associated-control */
import { useState } from 'react'
import PropTypes from 'prop-types'

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
  value,
  label,
  icon,
  legend,
  initialValue,
  onClick,
}) => {
  const [isChecked, setIsChecked] = useState(initialValue)

  return (
    <label
      css={{
        display: 'flex',
        alignItems: legend ? STYLES[variant].label.alignItems : 'center',
        color: colors.white,
        cursor: 'pointer',
        paddingBottom: spacing.normal,
      }}>
      <CheckboxIcon
        value={value}
        isChecked={isChecked}
        onClick={() => {
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
          }}>
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
          }}>
          {label}
        </span>
        {!!legend && (
          <span
            css={[
              {
                color: colors.structure.zinc,
              },
              STYLES[variant].legend,
            ]}>
            {legend}
          </span>
        )}
      </div>
    </label>
  )
}

Checkbox.propTypes = {
  variant: PropTypes.oneOf(Object.keys(VARIANTS)).isRequired,
  value: PropTypes.string.isRequired,
  label: PropTypes.string.isRequired,
  icon: PropTypes.node.isRequired,
  legend: PropTypes.string.isRequired,
  onClick: PropTypes.func.isRequired,
  initialValue: PropTypes.bool.isRequired,
}

export default Checkbox

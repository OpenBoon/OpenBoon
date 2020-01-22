/* eslint-disable jsx-a11y/label-has-associated-control */

import { useState } from 'react'
import PropTypes from 'prop-types'

import { colors, spacing, typography } from '../Styles'

import CheckboxIcon from './Icon'

const Checkbox = ({ label, legend, initialValue, onClick }) => {
  const [isChecked, setIsChecked] = useState(initialValue)

  return (
    <label
      css={{
        display: 'flex',
        alignItems: legend ? 'flex-start' : 'center',
        color: colors.white,
        cursor: 'pointer',
        paddingBottom: spacing.moderate,
      }}>
      <CheckboxIcon
        isChecked={isChecked}
        onClick={() => {
          setIsChecked(!isChecked)
          onClick(!isChecked)
        }}
      />
      <div
        css={{
          paddingLeft: spacing.moderate,
          display: 'flex',
          flexDirection: 'column',
        }}>
        <span
          css={{
            color: colors.structure.zinc,
            fontSize: typography.size.regular,
            lineHeight: typography.height.regular,
            fontWeight: typography.weight.bold,
          }}>
          {label}
        </span>
        {!!legend && (
          <span css={{ color: colors.structure.steel }}>{legend}</span>
        )}
      </div>
    </label>
  )
}

Checkbox.propTypes = {
  label: PropTypes.string.isRequired,
  legend: PropTypes.string.isRequired,
  onClick: PropTypes.func.isRequired,
  initialValue: PropTypes.bool.isRequired,
}

export default Checkbox

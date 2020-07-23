import PropTypes from 'prop-types'

import checkboxOptionShape from './optionShape'

import { colors, spacing, typography } from '../Styles'

import Checkbox, { VARIANTS } from '.'

const CheckboxGroup = ({ legend, description, variant, options, onClick }) => {
  return (
    <fieldset
      css={{
        border: 'none',
        padding: 0,
        margin: 0,
        paddingTop: spacing.moderate,
      }}
    >
      <legend
        css={{
          float: 'left',
          padding: 0,
          paddingBottom: spacing.moderate,
          fontSize: typography.size.medium,
          lineHeight: typography.height.medium,
          fontWeight: typography.weight.medium,
          display: 'flex',
        }}
      >
        {legend}
      </legend>
      {description && (
        <div
          css={{
            clear: 'both',
            paddingBottom: spacing.normal,
            color: colors.structure.zinc,
          }}
        >
          {description}
        </div>
      )}
      <div css={{ clear: 'both' }} />
      {options.map((option) => (
        <Checkbox
          key={option.value}
          variant={variant}
          option={option}
          onClick={(value) => onClick({ [option.value]: value })}
        />
      ))}
    </fieldset>
  )
}

CheckboxGroup.propTypes = {
  legend: PropTypes.node.isRequired,
  description: PropTypes.node.isRequired,
  variant: PropTypes.oneOf(Object.keys(VARIANTS)).isRequired,
  options: PropTypes.arrayOf(PropTypes.shape(checkboxOptionShape)).isRequired,
  onClick: PropTypes.func.isRequired,
}

export default CheckboxGroup

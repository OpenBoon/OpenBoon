import PropTypes from 'prop-types'

import checkboxOptionShape from './optionShape'

import { colors, spacing, typography } from '../Styles'

import Checkbox, { VARIANTS } from '.'

const CheckboxGroup = ({
  legend,
  subHeader,
  description,
  variant,
  options,
  onClick,
}) => {
  return (
    <fieldset
      css={{
        border: 'none',
        padding: 0,
        margin: 0,
        paddingTop: spacing.moderate,
      }}
    >
      <div css={{ display: 'flex' }}>
        <legend
          css={{
            padding: 0,
            paddingBottom: spacing.moderate,
            fontSize: typography.size.medium,
            lineHeight: typography.height.medium,
            fontWeight: typography.weight.medium,
          }}
        >
          {legend}
        </legend>
        {subHeader}
      </div>
      {description && (
        <div
          css={{
            paddingBottom: spacing.normal,
            color: colors.structure.zinc,
          }}
        >
          {description}
        </div>
      )}
      <div />
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

CheckboxGroup.defaultProps = {
  subHeader: '',
}

CheckboxGroup.propTypes = {
  legend: PropTypes.node.isRequired,
  subHeader: PropTypes.node,
  description: PropTypes.node.isRequired,
  variant: PropTypes.oneOf(Object.keys(VARIANTS)).isRequired,
  options: PropTypes.arrayOf(PropTypes.shape(checkboxOptionShape)).isRequired,
  onClick: PropTypes.func.isRequired,
}

export default CheckboxGroup

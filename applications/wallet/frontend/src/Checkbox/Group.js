import PropTypes from 'prop-types'

import checkboxOptionShape from './optionShape'

import { spacing, typography } from '../Styles'

import Checkbox, { VARIANTS } from '.'

const CheckboxGroup = ({ legend, variant, options, onClick }) => {
  return (
    <fieldset
      css={{
        border: 'none',
        padding: 0,
        margin: 0,
        paddingTop: spacing.moderate,
      }}>
      <legend
        css={{
          float: 'left',
          padding: 0,
          paddingBottom: spacing.moderate,
          fontSize: typography.size.medium,
          lineHeight: typography.height.medium,
          fontWeight: typography.weight.medium,
        }}>
        {legend}
      </legend>
      <div css={{ clear: 'both' }} />
      {options.map(option => (
        <Checkbox
          key={option.value}
          variant={variant}
          option={option}
          onClick={value => onClick({ [option.value]: value })}
        />
      ))}
    </fieldset>
  )
}

CheckboxGroup.propTypes = {
  legend: PropTypes.string.isRequired,
  variant: PropTypes.oneOf(Object.keys(VARIANTS)).isRequired,
  options: PropTypes.arrayOf(PropTypes.shape(checkboxOptionShape)).isRequired,
  onClick: PropTypes.func.isRequired,
}

export default CheckboxGroup

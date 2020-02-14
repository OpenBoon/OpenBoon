import PropTypes from 'prop-types'

import { spacing, typography } from '../Styles'

import Checkbox, { VARIANTS as CHECKBOX_VARIANTS } from '.'
import { VARIANTS as CHECKBOX_ICON_VARIANTS } from './Icon'

const CheckboxGroup = ({ variant, iconVariant, legend, options, onClick }) => {
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
          key={option.key}
          value={option.key}
          label={option.label}
          icon={option.icon}
          legend={option.legend}
          initialValue={option.initialValue}
          onClick={value => onClick({ [option.key]: value })}
          variant={variant}
          iconVariant={iconVariant}
        />
      ))}
    </fieldset>
  )
}

CheckboxGroup.propTypes = {
  variant: PropTypes.oneOf(Object.keys(CHECKBOX_VARIANTS)).isRequired,
  iconVariant: PropTypes.oneOf(Object.keys(CHECKBOX_ICON_VARIANTS)).isRequired,
  legend: PropTypes.string.isRequired,
  options: PropTypes.arrayOf(
    PropTypes.shape({
      key: PropTypes.string.isRequired,
      label: PropTypes.string.isRequired,
      icon: PropTypes.node.isRequired,
      legend: PropTypes.string.isRequired,
      initialValue: PropTypes.bool.isRequired,
    }),
  ).isRequired,
  onClick: PropTypes.func.isRequired,
}

export default CheckboxGroup

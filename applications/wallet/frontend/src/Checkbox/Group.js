import PropTypes from 'prop-types'

import { spacing, typography } from '../Styles'

import Checkbox from '.'

const CheckboxGroup = ({ legend, options, onClick }) => {
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
          legend={option.legend}
          initialValue={option.initialValue}
          onClick={value => onClick({ [option.key]: value })}
        />
      ))}
    </fieldset>
  )
}

CheckboxGroup.propTypes = {
  legend: PropTypes.string.isRequired,
  options: PropTypes.arrayOf(
    PropTypes.shape({
      key: PropTypes.string.isRequired,
      label: PropTypes.string.isRequired,
      legend: PropTypes.string.isRequired,
      initialValue: PropTypes.bool.isRequired,
    }),
  ).isRequired,
  onClick: PropTypes.func.isRequired,
}

export default CheckboxGroup

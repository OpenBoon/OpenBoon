import PropTypes from 'prop-types'

import { spacing, typography } from '../Styles'

import Radio from '.'

const RadioGroup = ({ legend, options, onClick }) => {
  return (
    <fieldset
      css={{
        display: 'flex',
        alignItems: 'center',
        border: 'none',
        padding: 0,
        margin: 0,
      }}
    >
      <legend
        css={{
          display: 'flex',
          padding: 0,
          paddingRight: spacing.normal,
          float: 'left',
          fontSize: typography.size.medium,
          lineHeight: typography.height.medium,
          fontWeight: typography.weight.medium,
        }}
      >
        {`${legend}:`}
      </legend>

      {options.map((option) => (
        <div key={option.value} css={{ paddingRight: spacing.comfy }}>
          <Radio name={legend} option={option} onClick={onClick} />
        </div>
      ))}
    </fieldset>
  )
}

RadioGroup.propTypes = {
  legend: PropTypes.string.isRequired,
  options: PropTypes.arrayOf(
    PropTypes.shape({
      value: PropTypes.string.isRequired,
      label: PropTypes.string.isRequired,
      legend: PropTypes.string.isRequired,
      initialValue: PropTypes.bool.isRequired,
    }).isRequired,
  ).isRequired,
  onClick: PropTypes.func.isRequired,
}

export default RadioGroup

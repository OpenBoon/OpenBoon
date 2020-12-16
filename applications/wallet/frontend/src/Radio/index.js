import PropTypes from 'prop-types'

import { colors, spacing, typography } from '../Styles'

import RadioIcon from './Icon'

const Radio = ({ option: { value, label, legend, initialValue }, onClick }) => {
  return (
    <div>
      <label css={{ display: 'flex' }}>
        <RadioIcon value={value} isChecked={initialValue} onClick={onClick} />
        <div
          css={{
            paddingLeft: spacing.base,
            fontWeight: legend
              ? typography.weight.bold
              : typography.weight.regular,
            color: initialValue
              ? colors.structure.white
              : colors.structure.steel,
          }}
        >
          {label}
        </div>
      </label>
      <div css={{ paddingLeft: spacing.comfy }}>{legend}</div>
    </div>
  )
}

Radio.propTypes = {
  option: PropTypes.shape({
    value: PropTypes.string.isRequired,
    label: PropTypes.string.isRequired,
    legend: PropTypes.string.isRequired,
    initialValue: PropTypes.bool.isRequired,
  }).isRequired,
  onClick: PropTypes.func.isRequired,
}

export default Radio

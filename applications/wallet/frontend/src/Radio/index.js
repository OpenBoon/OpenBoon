import PropTypes from 'prop-types'

import { spacing, typography } from '../Styles'

import RadioIcon from './Icon'

const Radio = ({ option: { value, label, legend, initialValue } }) => {
  return (
    <div>
      <label css={{ display: 'flex' }}>
        <RadioIcon value={value} isChecked={initialValue} />
        <div
          css={{
            paddingLeft: spacing.base,
            fontWeight: typography.weight.bold,
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
}

export default Radio

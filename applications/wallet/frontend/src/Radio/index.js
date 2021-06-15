import PropTypes from 'prop-types'

import { colors, spacing, typography } from '../Styles'

import RadioIcon from './Icon'

const Radio = ({ option: { value, label, legend, initialValue }, onClick }) => {
  return (
    <div>
      <label
        css={{ display: 'flex', alignItems: legend ? 'flex-start' : 'center' }}
      >
        <RadioIcon value={value} isChecked={initialValue} onClick={onClick} />

        <div css={{ paddingLeft: spacing.moderate }}>
          <div
            css={{
              fontWeight: legend
                ? typography.weight.bold
                : typography.weight.regular,
              color: colors.structure.white,
            }}
          >
            {label}
          </div>

          <div css={{ color: colors.structure.zinc }}>{legend}</div>
        </div>
      </label>
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

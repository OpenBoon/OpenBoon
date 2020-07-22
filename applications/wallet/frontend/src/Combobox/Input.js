import PropTypes from 'prop-types'
import { ComboboxInput as ReachComboboxInput } from '@reach/combobox'

import { constants, spacing, colors, typography } from '../Styles'

const ComboboxInput = ({ id, value, hasError, onChange, onBlur }) => {
  return (
    <ReachComboboxInput
      id={id}
      selectOnClick
      css={{
        width: '100%',
        padding: spacing.base,
        outline: constants.borders.regular.transparent,
        backgroundColor: colors.structure.smoke,
        color: colors.structure.white,
        fontWeight: typography.weight.medium,
        border: hasError
          ? constants.borders.error
          : constants.borders.regular.transparent,
        borderRadius: constants.borderRadius.small,
        ':hover': {
          border: constants.borders.regular.steel,
        },
        ':focus': {
          backgroundColor: colors.structure.white,
          color: colors.structure.black,
        },
        marginBottom: spacing.hairline,
      }}
      value={value}
      onChange={onChange}
      onBlur={onBlur}
    />
  )
}

ComboboxInput.propTypes = {
  id: PropTypes.string.isRequired,
  value: PropTypes.string.isRequired,
  hasError: PropTypes.bool.isRequired,
  onChange: PropTypes.func.isRequired,
  onBlur: PropTypes.func.isRequired,
}

export default ComboboxInput

import PropTypes from 'prop-types'
import { ComboboxInput as ReachComboboxInput } from '@reach/combobox'

import { constants, spacing, colors, typography } from '../Styles'

const ComboboxInput = ({ value, hasError, onChange }) => {
  return (
    <ReachComboboxInput
      selectOnClick
      autoComplete="off"
      css={{
        width: '100%',
        padding: `${spacing.moderate - constants.borderWidths.medium}px ${
          spacing.base
        }px`,
        outline: constants.borders.regular.transparent,
        backgroundColor: colors.structure.smoke,
        color: colors.structure.white,
        lineHeight: typography.height.medium,
        fontWeight: typography.weight.medium,
        border: hasError
          ? constants.borders.error
          : constants.borders.medium.transparent,
        borderRadius: constants.borderRadius.small,
        borderWidth: constants.borderWidths.medium,
        ':hover': {
          border: constants.borders.medium.white,
        },
        ':focus': {
          backgroundColor: colors.structure.white,
          color: colors.structure.black,
        },
        marginBottom: spacing.hairline,
      }}
      value={value}
      onChange={onChange}
    />
  )
}

ComboboxInput.propTypes = {
  value: PropTypes.string.isRequired,
  hasError: PropTypes.bool.isRequired,
  onChange: PropTypes.func.isRequired,
}

export default ComboboxInput

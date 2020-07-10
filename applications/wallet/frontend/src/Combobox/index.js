import PropTypes from 'prop-types'
import { useState } from 'react'

import ComboboxContainer from './Container'
import ComboboxInput from './Input'
import ComboboxOptions from './Options'

import { spacing, colors, typography } from '../Styles'

const Combobox = ({
  id,
  inputLabel,
  options,
  originalValue,
  currentValue,
  onChange,
  hasError,
  errorMessage,
}) => {
  const [showAllOptions, setShowAllOptions] = useState(true)
  const filteredOptions = options.filter(
    (option) => showAllOptions || option.label.includes(currentValue),
  )

  const handleOnChange = ({ value, showAll = true }) => {
    setShowAllOptions(showAll)
    onChange({ value })
  }

  return (
    <div css={{ flex: 1 }}>
      <div
        css={{
          paddingBottom: spacing.base,
          color: colors.structure.steel,
        }}
      >
        {inputLabel}
      </div>
      <ComboboxContainer onSelect={(value) => handleOnChange({ value })}>
        <ComboboxInput
          id={id}
          value={currentValue}
          hasError={hasError}
          onChange={(event) =>
            handleOnChange({ value: event.target.value, showAll: false })
          }
          onBlur={(event) => {
            if (!event.target.value) {
              handleOnChange({ value: originalValue })
            }
          }}
        />
        <ComboboxOptions options={filteredOptions} />
      </ComboboxContainer>

      {hasError && errorMessage && (
        <div
          css={{
            fontStyle: typography.style.italic,
            color: colors.signal.warning.base,
            paddingTop: spacing.base,
          }}
        >
          {errorMessage}
        </div>
      )}
    </div>
  )
}

Combobox.defaultProps = {
  errorMessage: '',
}

Combobox.propTypes = {
  id: PropTypes.string.isRequired,
  inputLabel: PropTypes.string.isRequired,
  options: PropTypes.arrayOf(
    PropTypes.shape({
      label: PropTypes.string.isRequired,
      count: PropTypes.number.isRequired,
    }),
  ).isRequired,
  originalValue: PropTypes.string.isRequired,
  currentValue: PropTypes.string.isRequired,
  onChange: PropTypes.func.isRequired,
  hasError: PropTypes.bool.isRequired,
  errorMessage: PropTypes.string,
}

export default Combobox

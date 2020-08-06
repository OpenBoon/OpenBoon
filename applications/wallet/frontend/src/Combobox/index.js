import PropTypes from 'prop-types'
import { useState, useEffect } from 'react'

import ComboboxContainer from './Container'
import ComboboxInput from './Input'
import ComboboxOptions from './Options'

import { spacing, colors, typography } from '../Styles'

const Combobox = ({
  label,
  options,
  value,
  onChange,
  hasError,
  errorMessage,
}) => {
  const [isLoading, setIsLoading] = useState(false)
  const [currentValue, setCurrentValue] = useState(value)
  const [showAllOptions, setShowAllOptions] = useState(true)
  const [fetchedOptions, setFetchedOptions] = useState([])

  useEffect(() => {
    const fetchOptions = async () => {
      setIsLoading(true)

      const data = await options()

      setFetchedOptions(data)
      setIsLoading(false)
    }

    if (typeof options === 'function') {
      fetchOptions()
    } else {
      setFetchedOptions(options)
    }
  }, [options])

  return (
    <div css={{ flex: 1 }}>
      <div
        css={{
          paddingBottom: spacing.base,
          color: colors.structure.steel,
        }}
      >
        {label}
      </div>
      <ComboboxContainer
        onSelect={(newValue) => {
          setShowAllOptions(true)

          setCurrentValue(newValue)

          onChange({ value: newValue })
        }}
      >
        <ComboboxInput
          value={currentValue}
          hasError={hasError}
          onChange={({ target }) => {
            setShowAllOptions(false)

            setCurrentValue(target.value)
          }}
          onBlur={(event) => {
            if (!event.target.value) {
              setShowAllOptions(true)

              setCurrentValue(value)
            }
          }}
        />
        <ComboboxOptions
          options={fetchedOptions}
          isLoading={isLoading}
          showAllOptions={showAllOptions}
          currentValue={currentValue}
        />
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
  label: PropTypes.string.isRequired,
  options: PropTypes.oneOfType([
    PropTypes.func,
    PropTypes.arrayOf(
      PropTypes.shape({
        label: PropTypes.string.isRequired,
        count: PropTypes.number,
      }),
    ),
  ]).isRequired,
  value: PropTypes.string.isRequired,
  onChange: PropTypes.func.isRequired,
  hasError: PropTypes.bool.isRequired,
  errorMessage: PropTypes.string,
}

export default Combobox

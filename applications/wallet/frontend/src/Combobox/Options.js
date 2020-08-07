import PropTypes from 'prop-types'
import {
  ComboboxPopover as ReachComboboxPopover,
  ComboboxList as ReachComboboxList,
  ComboboxOption as ReachComboboxOption,
  ComboboxOptionText as ReachComboboxOptionText,
} from '@reach/combobox'

import Loading from '../Loading'

import { colors, constants, spacing, typography, zIndex } from '../Styles'

const MAX_HEIGHT = 350

const ComboboxOptions = ({ options, isLoading, showAllOptions, value }) => {
  const filteredOptions = options.filter(
    (option) =>
      showAllOptions ||
      option.label.toLowerCase().includes(value.toLowerCase()),
  )

  if (!isLoading && filteredOptions.length === 0) return null

  return (
    <ReachComboboxPopover
      css={{
        position: 'absolute',
        zIndex: zIndex.layout.interactive,
        border: 'none',
        background: 'none',
        marginTop: spacing.hairline,
      }}
    >
      {isLoading ? (
        <Loading />
      ) : (
        <ReachComboboxList
          css={{
            width: '100%',
            maxHeight: MAX_HEIGHT,
            overflowY: 'scroll',
            backgroundColor: colors.structure.white,
            borderRadius: constants.borderRadius.small,
            color: colors.structure.black,
            fontWeight: typography.weight.medium,
            paddingTop: spacing.base,
            paddingBottom: spacing.base,
          }}
        >
          {filteredOptions.map(({ label, count }) => {
            return (
              <ReachComboboxOption
                key={label}
                css={{
                  padding: `${spacing.base}px ${spacing.moderate}px`,
                  '[data-highlighted]': {
                    backgroundColor: colors.structure.pebble,
                  },
                }}
                value={label}
              >
                <ReachComboboxOptionText
                  css={{
                    '[data-user-value]': {
                      fontWeight: typography.weight.medium,
                    },
                    '[data-suggested-value]': {
                      fontWeight: typography.weight.medium,
                    },
                  }}
                />
                {count && (
                  <span
                    css={{ color: colors.structure.steel }}
                  >{` (${count})`}</span>
                )}
              </ReachComboboxOption>
            )
          })}
        </ReachComboboxList>
      )}
    </ReachComboboxPopover>
  )
}

ComboboxOptions.propTypes = {
  options: PropTypes.arrayOf(
    PropTypes.shape({
      label: PropTypes.string.isRequired,
      count: PropTypes.number,
    }).isRequired,
  ).isRequired,
  isLoading: PropTypes.bool.isRequired,
  showAllOptions: PropTypes.bool.isRequired,
  value: PropTypes.string.isRequired,
}

export default ComboboxOptions

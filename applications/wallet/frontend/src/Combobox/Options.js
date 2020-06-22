import PropTypes from 'prop-types'
import {
  ComboboxPopover as ReachComboboxPopover,
  ComboboxList as ReachComboboxList,
  ComboboxOption as ReachComboboxOption,
  ComboboxOptionText as ReachComboboxOptionText,
} from '@reach/combobox'

import { constants, spacing, colors, typography } from '../Styles'

const ComboboxOptions = ({ options }) => {
  if (options.length === 0) return null
  return (
    <ReachComboboxPopover
      css={{
        border: 'none',
        background: 'none',
        marginTop: spacing.hairline,
      }}
    >
      <ReachComboboxList
        css={{
          width: '100%',
          position: 'absolute',
          backgroundColor: colors.structure.white,
          borderRadius: constants.borderRadius.small,
          color: colors.structure.black,
          fontWeight: typography.weight.medium,
          paddingTop: spacing.base,
          paddingBottom: spacing.base,
        }}
      >
        {options.map(({ label, count }) => {
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
    </ReachComboboxPopover>
  )
}

ComboboxOptions.propTypes = {
  options: PropTypes.arrayOf(
    PropTypes.shape({
      label: PropTypes.string.isRequired,
      count: PropTypes.number.isRequired,
    }).isRequired,
  ).isRequired,
}

export default ComboboxOptions

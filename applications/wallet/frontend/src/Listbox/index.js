import { useState } from 'react'
import {
  ListboxInput,
  ListboxButton,
  ListboxList,
  ListboxPopover,
} from '@reach/listbox'
import PropTypes from 'prop-types'

import listboxShape from './shape'

import ChevronSvg from '../Icons/chevron.svg'

import { constants, spacing, colors, typography } from '../Styles'

import ListboxOptions from './Options'
import { getFilteredOptions } from './helpers'

const ICON_SIZE = 20
const MAX_HEIGHT = 350

const Listbox = ({ label, options, onChange, value, placeholder }) => {
  const [searchString, setSearchString] = useState('')

  const filteredOptions = getFilteredOptions({ options, searchString })

  const hasResults = Object.keys(filteredOptions).length !== 0

  return (
    <label
      css={{
        '[data-reach-listbox-input][data-state="expanded"]': {
          borderRadius: `${constants.borderRadius.small}px ${constants.borderRadius.small}px 0 0`,
        },
      }}
    >
      <div
        css={{
          paddingBottom: spacing.base,
          color: colors.structure.white,
          fontSize: typography.size.regular,
          lineHeight: typography.height.regular,
          fontWeight: typography.weight.medium,
        }}
      >
        {label}
      </div>
      <ListboxInput
        defaultValue={value}
        css={{
          width: '100%',
          display: 'flex',
          backgroundColor: colors.structure.steel,
          fontSize: typography.size.regular,
          lineHeight: typography.height.regular,
          fontWeight: typography.weight.medium,
          borderRadius: constants.borderRadius.small,
        }}
        onChange={(v) => onChange({ value: v })}
      >
        <ListboxButton
          css={{
            flex: 1,
            border: 'none',
            display: 'inline-flex',
            alignItems: 'center',
            justifyContent: 'space-between',
            padding: spacing.base,
          }}
          arrow={
            <ChevronSvg height={ICON_SIZE} color={colors.structure.white} />
          }
        >
          {placeholder}
        </ListboxButton>
        <ListboxPopover
          css={{
            ':focus-within': {
              outline: 'none',
              boxShadow: 'none',
            },
            backgroundColor: colors.structure.steel,
            border: 'none',
            padding: 0,
            borderBottomRightRadius: constants.borderRadius.small,
            borderBottomLeftRadius: constants.borderRadius.small,
            overflow: 'hidden',
            boxShadow: constants.boxShadows.dropdown,
          }}
        >
          <div
            css={{
              padding: spacing.small,
            }}
          >
            <input
              aria-label="Search metadata type"
              type="search"
              value={searchString}
              onChange={({ target: { value: searchValue } }) =>
                setSearchString(searchValue)
              }
              css={{
                width: '100%',
                padding: `${spacing.moderate}px ${spacing.base}px`,
                borderRadius: constants.borderRadius.small,
                boxShadow: constants.boxShadows.input,
                border: constants.borders.medium.transparent,
                '&:focus': {
                  border: constants.borders.keyOneMedium,
                  outline: colors.key.one,
                },
              }}
            />
          </div>

          {hasResults && (
            <ListboxList
              css={{
                margin: 0,
                overflow: 'auto',
                maxHeight: MAX_HEIGHT,
                backgroundColor: colors.structure.steel,
                color: colors.structure.white,
                fontWeight: typography.weight.medium,
                paddingTop: spacing.base,
                paddingBottom: spacing.base,
              }}
            >
              <ListboxOptions options={filteredOptions} nestedCount={0} />
            </ListboxList>
          )}
          {!hasResults && <div css={{ padding: spacing.base }}>No Results</div>}
        </ListboxPopover>
      </ListboxInput>
    </label>
  )
}

Listbox.propTypes = {
  label: PropTypes.string.isRequired,
  options: listboxShape.isRequired,
  onChange: PropTypes.func.isRequired,
  value: PropTypes.string.isRequired,
  placeholder: PropTypes.string.isRequired,
}

export default Listbox

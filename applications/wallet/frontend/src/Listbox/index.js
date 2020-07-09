import { useState } from 'react'
import {
  ListboxInput,
  ListboxButton,
  ListboxList,
  ListboxPopover,
} from '@reach/listbox'
import PropTypes from 'prop-types'
import deepfilter from 'deep-filter'

import listboxShape from './shape'

import ChevronSvg from '../Icons/chevron.svg'

import { constants, spacing, colors, typography } from '../Styles'

import ListboxOptions from './Options'

const ICON_SIZE = 20
const MAX_HEIGHT = 350

const Listbox = ({ label, options, onChange, value, placeholder }) => {
  const [searchString, setSearchString] = useState('')

  const filteredOptions = deepfilter(options, (option, prop) => {
    if (!searchString) return true

    if (typeof prop === 'string' && typeof option === 'string') {
      return prop.toLowerCase().includes(searchString)
    }

    if (typeof option === 'object' && Object.keys(option).length === 0) {
      return false
    }

    return true
  })

  const hasResults = Object.keys(filteredOptions).length !== 0

  return (
    <label>
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
            marginTop: -2,
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
              type="search"
              value={searchString}
              onChange={({ target: { value: searchValue } }) =>
                setSearchString(searchValue)
              }
              css={{
                outline: 'none',
                width: '100%',
                padding: `${spacing.moderate}px ${spacing.base}px`,
                borderRadius: constants.borderRadius.small,
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

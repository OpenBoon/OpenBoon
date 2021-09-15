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

import InputSearch, { VARIANTS as INPUT_SEARCH_VARIANTS } from '../Input/Search'

import ListboxOptions from './Options'
import { getFilteredOptions } from './helpers'

const MAX_HEIGHT = 350

const Listbox = ({
  label,
  inputLabel,
  options,
  onChange,
  value,
  placeholder,
}) => {
  const [searchString, setSearchString] = useState('')

  const filteredOptions = getFilteredOptions({ options, searchString })

  const hasResults = Object.keys(filteredOptions).length !== 0

  return (
    <label
      css={{
        '[data-reach-listbox-input][data-state="expanded"]': {
          boxShadow: constants.boxShadows.dropdown,
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
            <ChevronSvg
              height={constants.icons.regular}
              color={colors.structure.white}
            />
          }
        >
          <span>{placeholder}</span>
        </ListboxButton>

        <ListboxPopover
          css={{
            ':focus-within': {
              outline: 'none',
              boxShadow: 'none',
            },
            backgroundColor: colors.structure.transparent,
            border: 'none',
            padding: 0,
          }}
        >
          <div
            css={{
              overflowY: 'hidden',
              padding: spacing.base,
              paddingTop: 0,
              margin: -spacing.base,
              marginBottom: 0,
            }}
          >
            <div
              css={{
                borderBottomRightRadius: constants.borderRadius.small,
                borderBottomLeftRadius: constants.borderRadius.small,
                backgroundColor: colors.structure.steel,
                boxShadow: constants.boxShadows.dropdown,
              }}
            >
              <div css={{ padding: spacing.small }}>
                <InputSearch
                  aria-label={inputLabel}
                  placeholder={inputLabel}
                  value={searchString}
                  onChange={({ value: v }) => setSearchString(v)}
                  variant={INPUT_SEARCH_VARIANTS.LIGHT}
                />
              </div>

              {hasResults && (
                <ListboxList
                  css={{
                    borderBottomRightRadius: constants.borderRadius.small,
                    borderBottomLeftRadius: constants.borderRadius.small,
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

              {!hasResults && (
                <div css={{ padding: spacing.base }}>No Results</div>
              )}
            </div>
          </div>
        </ListboxPopover>
      </ListboxInput>
    </label>
  )
}

Listbox.propTypes = {
  label: PropTypes.string.isRequired,
  inputLabel: PropTypes.string.isRequired,
  options: listboxShape.isRequired,
  onChange: PropTypes.func.isRequired,
  value: PropTypes.string.isRequired,
  placeholder: PropTypes.node.isRequired,
}

export default Listbox

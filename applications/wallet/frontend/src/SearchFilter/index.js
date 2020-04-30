import { useState } from 'react'
import PropTypes from 'prop-types'

import { colors, spacing, constants } from '../Styles'

import { dispatch, ACTIONS } from '../Filters/helpers'

const BUTTON_SIZE = 42

const SearchFilter = ({ projectId, assetId, filters }) => {
  const [searchString, setSearchString] = useState('')

  const hasSearch = searchString !== ''

  return (
    <form action="" method="post" onSubmit={(event) => event.preventDefault()}>
      <div css={{ display: 'flex' }}>
        <input
          type="text"
          placeholder="Create text filter (search name or field value)"
          value={searchString}
          onChange={({ target: { value } }) => setSearchString(value)}
          css={{
            flex: 1,
            border: 'none',
            padding: spacing.moderate,
            borderTopLeftRadius: constants.borderRadius.small,
            borderBottomLeftRadius: constants.borderRadius.small,
            color: colors.structure.pebble,
            backgroundColor: colors.structure.coal,
            '&:focus': {
              color: colors.structure.coal,
              backgroundColor: colors.structure.white,
            },
          }}
        />
        <button
          type="submit"
          aria-disabled={!searchString}
          aria-label="Search"
          onClick={() => {
            if (searchString === '') return

            setSearchString('')

            dispatch({
              action: ACTIONS.ADD_FILTERS,
              payload: {
                projectId,
                assetId,
                filters,
                newFilters: [{ type: 'search', value: searchString }],
              },
            })
          }}
          css={{
            width: BUTTON_SIZE,
            borderTopRightRadius: constants.borderRadius.small,
            borderBottomRightRadius: constants.borderRadius.small,
            color: hasSearch ? colors.structure.white : colors.structure.black,
            backgroundColor: hasSearch
              ? colors.key.one
              : colors.structure.steel,
            margin: 0,
            padding: 0,
            border: 0,
            cursor: searchString === '' ? 'not-allowed' : 'pointer',
          }}
        >
          +
        </button>
      </div>
    </form>
  )
}

SearchFilter.propTypes = {
  projectId: PropTypes.string.isRequired,
  assetId: PropTypes.string.isRequired,
  filters: PropTypes.arrayOf(PropTypes.shape({}).isRequired).isRequired,
}

export default SearchFilter

import { useState } from 'react'
import PropTypes from 'prop-types'

import { colors, spacing, constants } from '../Styles'
import PlusSvg from '../Icons/plus.svg'

import { dispatch, ACTIONS } from '../Filters/helpers'

import InputSearch, { VARIANTS as INPUT_SEARCH_VARIANTS } from '../Input/Search'

const BUTTON_SIZE = 42

const SearchFilter = ({ pathname, projectId, assetId, filters }) => {
  const [searchString, setSearchString] = useState('')

  const hasSearch = searchString !== ''

  return (
    <form action="" method="post" onSubmit={(event) => event.preventDefault()}>
      <div css={{ display: 'flex' }}>
        <InputSearch
          aria-label="Add Simple Text Filter"
          placeholder="Type here to create a simple text filter"
          value={searchString}
          onChange={({ value }) => setSearchString(value)}
          variant={INPUT_SEARCH_VARIANTS.DARK}
          style={{ backgroundColor: colors.structure.coal }}
        />
        <button
          type="submit"
          aria-disabled={!searchString}
          aria-label="Search"
          onClick={() => {
            if (searchString === '') return

            setSearchString('')

            dispatch({
              type: ACTIONS.ADD_FILTERS,
              payload: {
                pathname,
                projectId,
                assetId,
                filters,
                newFilters: [
                  {
                    type: 'textContent',
                    attribute: '',
                    values: { query: searchString },
                  },
                ],
              },
            })
          }}
          css={{
            display: 'flex',
            justifyContent: 'center',
            alignItems: 'center',
            width: BUTTON_SIZE,
            marginLeft: spacing.small,
            borderRadius: constants.borderRadius.small,
            backgroundColor: hasSearch
              ? colors.key.one
              : colors.structure.steel,
            padding: 0,
            border: 0,
            cursor: searchString === '' ? 'not-allowed' : 'pointer',
          }}
        >
          <PlusSvg
            height={constants.icons.regular}
            css={{
              color: hasSearch
                ? colors.structure.white
                : colors.structure.smoke,
            }}
          />
        </button>
      </div>
    </form>
  )
}

SearchFilter.propTypes = {
  pathname: PropTypes.string.isRequired,
  projectId: PropTypes.string.isRequired,
  assetId: PropTypes.string.isRequired,
  filters: PropTypes.arrayOf(PropTypes.shape({}).isRequired).isRequired,
}

export default SearchFilter

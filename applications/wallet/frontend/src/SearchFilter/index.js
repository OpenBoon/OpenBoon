import { useState } from 'react'
import PropTypes from 'prop-types'

import { colors, spacing, constants, typography } from '../Styles'
import PlusSvg from '../Icons/plus.svg'

import { dispatch, ACTIONS } from '../Filters/helpers'

const BUTTON_SIZE = 42

const SearchFilter = ({ pathname, projectId, assetId, filters }) => {
  const [searchString, setSearchString] = useState('')

  const hasSearch = searchString !== ''

  return (
    <form action="" method="post" onSubmit={(event) => event.preventDefault()}>
      <div css={{ display: 'flex' }}>
        <input
          type="search"
          placeholder="Type here to create simple text filter"
          value={searchString}
          onChange={({ target: { value } }) => setSearchString(value)}
          css={{
            flex: 1,
            border: constants.borders.regular.transparent,
            padding: spacing.moderate,
            paddingLeft: spacing.spacious,
            borderTopLeftRadius: constants.borderRadius.small,
            borderBottomLeftRadius: constants.borderRadius.small,
            color: colors.structure.pebble,
            backgroundColor: colors.structure.coal,
            ':hover': {
              border: constants.borders.regular.steel,
            },
            ':focus': {
              outline: constants.borders.regular.transparent,
              border: constants.borders.keyOneRegular,
              color: colors.structure.coal,
              backgroundColor: colors.structure.white,
              backgroundImage: `url('data:image/svg+xml;base64,PHN2ZyB4bWxucz0iaHR0cDovL3d3dy53My5vcmcvMjAwMC9zdmciIHZpZXdCb3g9IjAgMCAyMCAyMCI+CiAgICA8cGF0aCBmaWxsPSIjYjNiM2IzIiBkPSJNMTMuODU3IDEyLjMxNGgtLjgyM2wtLjMwOC0uMzA4YTYuNDM4IDYuNDM4IDAgMDAxLjY0NS00LjMyQTYuNjcyIDYuNjcyIDAgMDA3LjY4NiAxIDYuNjcyIDYuNjcyIDAgMDAxIDcuNjg2YTYuNjcyIDYuNjcyIDAgMDA2LjY4NiA2LjY4NSA2LjQzOCA2LjQzOCAwIDAwNC4zMi0xLjY0NWwuMzA4LjMwOHYuODIzTDE3LjQ1NyAxOSAxOSAxNy40NTdsLTUuMTQzLTUuMTQzem0tNi4xNzEgMGE0LjYxIDQuNjEgMCAwMS00LjYyOS00LjYyOCA0LjYxIDQuNjEgMCAwMTQuNjI5LTQuNjI5IDQuNjEgNC42MSAwIDAxNC42MjggNC42MjkgNC42MSA0LjYxIDAgMDEtNC42MjggNC42Mjh6Ii8+Cjwvc3ZnPg==')`,
            },
            '::placeholder': {
              fontStyle: typography.style.italic,
            },
            backgroundImage: `url('data:image/svg+xml;base64,PHN2ZyB4bWxucz0iaHR0cDovL3d3dy53My5vcmcvMjAwMC9zdmciIHZpZXdCb3g9IjAgMCAyMCAyMCI+CiAgICA8cGF0aCBmaWxsPSIjNGE0YTRhIiBkPSJNMTMuODU3IDEyLjMxNGgtLjgyM2wtLjMwOC0uMzA4YTYuNDM4IDYuNDM4IDAgMDAxLjY0NS00LjMyQTYuNjcyIDYuNjcyIDAgMDA3LjY4NiAxIDYuNjcyIDYuNjcyIDAgMDAxIDcuNjg2YTYuNjcyIDYuNjcyIDAgMDA2LjY4NiA2LjY4NSA2LjQzOCA2LjQzOCAwIDAwNC4zMi0xLjY0NWwuMzA4LjMwOHYuODIzTDE3LjQ1NyAxOSAxOSAxNy40NTdsLTUuMTQzLTUuMTQzem0tNi4xNzEgMGE0LjYxIDQuNjEgMCAwMS00LjYyOS00LjYyOCA0LjYxIDQuNjEgMCAwMTQuNjI5LTQuNjI5IDQuNjEgNC42MSAwIDAxNC42MjggNC42MjkgNC42MSA0LjYxIDAgMDEtNC42MjggNC42Mjh6Ii8+Cjwvc3ZnPg==')`,
            backgroundRepeat: `no-repeat, repeat`,
            backgroundPosition: `left ${spacing.base}px top 50%`,
            backgroundSize: constants.iconSize,
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
            width: BUTTON_SIZE,
            borderTopRightRadius: constants.borderRadius.small,
            borderBottomRightRadius: constants.borderRadius.small,
            backgroundColor: hasSearch
              ? colors.key.one
              : colors.structure.steel,
            margin: 0,
            padding: 0,
            border: 0,
            cursor: searchString === '' ? 'not-allowed' : 'pointer',
          }}
        >
          <PlusSvg
            height={constants.iconSize}
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

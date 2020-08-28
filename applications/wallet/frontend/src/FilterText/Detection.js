import PropTypes from 'prop-types'
import { useState } from 'react'

import { colors, constants, spacing } from '../Styles'
import PlusSvg from '../Icons/plus.svg'
import CrossSvg from '../Icons/cross.svg'

import filterShape from '../Filter/shape'
import { dispatch, ACTIONS } from '../Filters/helpers'

import Button, { VARIANTS } from '../Button'

import InputSearch, { VARIANTS as INPUT_SEARCH_VARIANTS } from '../Input/Search'

const BUTTON_SIZE = 42

const FilterTextDetection = ({
  pathname,
  projectId,
  assetId,
  filters,
  filter: {
    type,
    attribute,
    values: { query },
  },
  filterIndex,
}) => {
  const [searchString, setSearchString] = useState('')
  const [isEditing, setEditing] = useState(false)

  const hasSearch = searchString !== ''

  if (query && !isEditing) {
    return (
      <div css={{ padding: spacing.normal }}>
        <div
          css={{
            display: 'flex',
            height: BUTTON_SIZE,
          }}
        >
          <Button
            aria-label="Edit Text Detection"
            variant={VARIANTS.MENU}
            style={{
              padding: 0,
              paddingLeft: spacing.moderate,
              color: colors.structure.pebble,
              backgroundColor: colors.structure.mattGrey,
              alignItems: 'flex-start',
            }}
            onClick={() => {
              setEditing(true)
              setSearchString(query)
            }}
          >
            <span
              css={{
                paddingLeft: spacing.hairline,
              }}
            >
              {query}
            </span>
          </Button>

          <Button
            title="Clear"
            aria-label="Clear Text Detection"
            style={{
              width: BUTTON_SIZE,
              padding: spacing.moderate,
              backgroundColor: colors.structure.coal,
            }}
            variant={VARIANTS.ICON}
            onClick={() => {
              setSearchString('')
              dispatch({
                type: ACTIONS.UPDATE_FILTER,
                payload: {
                  pathname,
                  projectId,
                  assetId,
                  filters,
                  updatedFilter: {
                    type,
                    attribute,
                    values: {},
                  },
                  filterIndex,
                },
              })
            }}
          >
            <CrossSvg height={constants.icons.regular} />
          </Button>
        </div>
      </div>
    )
  }

  return (
    <form action="" method="post" onSubmit={(event) => event.preventDefault()}>
      <div css={{ padding: spacing.normal }}>
        <div
          css={{
            display: 'flex',
            height: BUTTON_SIZE,
            position: 'relative',
          }}
        >
          <InputSearch
            autoFocus
            aria-label="Add Text Detection Filter"
            placeholder="Filter by detected text"
            value={searchString}
            onChange={({ value }) => setSearchString(value)}
            variant={INPUT_SEARCH_VARIANTS.DARK}
          />

          <button
            type="submit"
            aria-disabled={!searchString}
            aria-label="Search"
            onClick={() => {
              if (searchString === '') return

              dispatch({
                type: ACTIONS.UPDATE_FILTER,
                payload: {
                  pathname,
                  projectId,
                  assetId,
                  filters,
                  updatedFilter: {
                    type,
                    attribute,
                    values: { query: searchString },
                  },
                  filterIndex,
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
              color: hasSearch
                ? colors.structure.white
                : colors.structure.black,
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
              color={
                searchString ? colors.structure.white : colors.structure.smoke
              }
            />
          </button>
        </div>
      </div>
    </form>
  )
}

FilterTextDetection.propTypes = {
  pathname: PropTypes.string.isRequired,
  projectId: PropTypes.string.isRequired,
  assetId: PropTypes.string.isRequired,
  filters: PropTypes.arrayOf(PropTypes.shape(filterShape)).isRequired,
  filter: PropTypes.shape(filterShape).isRequired,
  filterIndex: PropTypes.number.isRequired,
}

export default FilterTextDetection

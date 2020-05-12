import { useState } from 'react'
import PropTypes from 'prop-types'

import { colors, constants, spacing, typography } from '../Styles'
import SearchSvg from '../Icons/search.svg'
import CrossSvg from '../Icons/cross.svg'

import Button, { VARIANTS } from '../Button'

import filterShape from '../Filter/shape'

import { dispatch, ACTIONS } from '../Filters/helpers'

const BUTTON_SIZE = 42
const ICON_SIZE = 20

const FilterTextDetectionContent = ({
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

  const hasSearch = searchString !== ''

  return (
    <>
      {!query && (
        <form
          action=""
          method="post"
          onSubmit={(event) => event.preventDefault()}
        >
          <div css={{ display: 'flex', position: 'relative' }}>
            <input
              type="search"
              placeholder="Search text"
              value={searchString}
              onChange={({ target: { value } }) => setSearchString(value)}
              css={{
                flex: 1,
                border: constants.borders.transparent,
                padding: spacing.moderate,
                borderTopLeftRadius: constants.borderRadius.small,
                borderBottomLeftRadius: constants.borderRadius.small,
                color: colors.structure.pebble,
                backgroundColor: colors.structure.mattGrey,
                '&:focus': {
                  color: colors.structure.coal,
                  backgroundColor: colors.structure.white,
                },
                ':hover': {
                  border: constants.borders.tableRow,
                },
                paddingLeft: spacing.moderate,
                '::placeholder': {
                  fontStyle: typography.style.italic,
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
                  action: ACTIONS.UPDATE_FILTER,
                  payload: {
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
                width: BUTTON_SIZE,
                borderTopRightRadius: constants.borderRadius.small,
                borderBottomRightRadius: constants.borderRadius.small,
                color: hasSearch
                  ? colors.structure.white
                  : colors.structure.black,
                backgroundColor: hasSearch
                  ? colors.key.one
                  : colors.structure.steel,
                margin: 0,
                padding: 0,
                border: 0,
                cursor: searchString === '' ? 'not-allowed' : 'pointer',
              }}
            >
              <SearchSvg
                width={ICON_SIZE}
                css={{ color: colors.structure.white }}
              />
            </button>
          </div>
        </form>
      )}

      {query && (
        <div css={{ display: 'flex' }}>
          <input
            readOnly
            type="text"
            placeholder="Search text"
            value={query}
            css={{
              flex: 1,
              border: constants.borders.transparent,
              padding: spacing.moderate,
              borderTopLeftRadius: constants.borderRadius.small,
              borderBottomLeftRadius: constants.borderRadius.small,
              color: colors.structure.pebble,
              backgroundColor: colors.structure.mattGrey,
              paddingLeft: spacing.moderate,
            }}
          />
          <Button
            title="Clear"
            aria-label="Clear Text Detection"
            style={{
              width: BUTTON_SIZE,
              padding: spacing.moderate,
              backgroundColor: colors.structure.coal,
              ':hover': {
                svg: {
                  color: colors.structure.white,
                },
              },
            }}
            variant={VARIANTS.NEUTRAL}
            onClick={() => {
              dispatch({
                action: ACTIONS.UPDATE_FILTER,
                payload: {
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
            <CrossSvg
              width={ICON_SIZE}
              css={{ color: colors.structure.steel }}
            />
          </Button>
        </div>
      )}
    </>
  )
}

FilterTextDetectionContent.propTypes = {
  projectId: PropTypes.string.isRequired,
  assetId: PropTypes.string.isRequired,
  filters: PropTypes.arrayOf(PropTypes.shape(filterShape)).isRequired,
  filter: PropTypes.shape(filterShape).isRequired,
  filterIndex: PropTypes.number.isRequired,
}

export default FilterTextDetectionContent

import PropTypes from 'prop-types'
import { useState } from 'react'

import { colors, constants, spacing, typography } from '../Styles'
import SearchSvg from '../Icons/search.svg'
import CrossSvg from '../Icons/cross.svg'

import filterShape from '../Filter/shape'
import { dispatch, ACTIONS } from '../Filters/helpers'

import Accordion, { VARIANTS as ACCORDION_VARIANTS } from '../Accordion'
import Button, { VARIANTS } from '../Button'
import FiltersTitle from '../Filters/Title'

const BUTTON_SIZE = 42
const ICON_SIZE = 20

const FilterTextDetection = ({
  projectId,
  assetId,
  filters,
  filter,
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
    <Accordion
      cacheKey={`FilterTextDetection.${filter.attribute}.${filterIndex}`}
      variant={ACCORDION_VARIANTS.FILTER}
      title={
        <FiltersTitle
          projectId={projectId}
          assetId={assetId}
          filters={filters}
          filter={filter}
          filterIndex={filterIndex}
        />
      }
      isInitiallyOpen
    >
      <div
        css={{
          padding: `${spacing.normal}px ${spacing.moderate}px`,
          '.ErrorBoundary > div': {
            backgroundColor: 'transparent',
            boxShadow: 'none',
          },
          '.Loading': {
            backgroundColor: 'transparent',
            boxShadow: 'none',
          },
        }}
      >
        {query && !hasSearch ? (
          <div css={{ display: 'flex', height: BUTTON_SIZE }}>
            <Button
              aria-label="Edit Text Detection"
              variant={VARIANTS.NEUTRAL}
              style={{
                flex: 1,
                paddingLeft: spacing.moderate,
                color: colors.structure.pebble,
                backgroundColor: colors.structure.mattGrey,
                fontWeight: typography.weight.regular,
                alignItems: 'flex-start',
                ':hover': {
                  backgroundColor: colors.structure.smoke,
                },
              }}
              onClick={() => setSearchString(query)}
            >
              <span
                css={{
                  paddingTop: spacing.hairline,
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
        ) : (
          <form
            action=""
            method="post"
            onSubmit={(event) => event.preventDefault()}
          >
            <div
              css={{
                display: 'flex',
                height: BUTTON_SIZE,
                position: 'relative',
              }}
            >
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
      </div>
    </Accordion>
  )
}

FilterTextDetection.propTypes = {
  projectId: PropTypes.string.isRequired,
  assetId: PropTypes.string.isRequired,
  filters: PropTypes.arrayOf(PropTypes.shape(filterShape)).isRequired,
  filter: PropTypes.shape(filterShape).isRequired,
  filterIndex: PropTypes.number.isRequired,
}

export default FilterTextDetection

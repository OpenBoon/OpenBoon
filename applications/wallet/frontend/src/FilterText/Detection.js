import PropTypes from 'prop-types'
import { useState } from 'react'

import { colors, constants, spacing, typography } from '../Styles'
import SearchSvg from '../Icons/search.svg'
import CrossSvg from '../Icons/cross.svg'

import filterShape from '../Filter/shape'
import { dispatch, ACTIONS } from '../Filters/helpers'

import Button, { VARIANTS } from '../Button'

const BUTTON_SIZE = 42
const ICON_SIZE = 20

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
              ':hover': { svg: { color: colors.structure.white } },
            }}
            variant={VARIANTS.NEUTRAL}
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
            <CrossSvg
              height={ICON_SIZE}
              css={{ color: colors.structure.steel }}
            />
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
          <input
            // eslint-disable-next-line jsx-a11y/no-autofocus
            autoFocus
            type="search"
            placeholder="Search text"
            value={searchString}
            onChange={({ target: { value } }) => setSearchString(value)}
            css={{
              flex: 1,
              border: constants.borders.regular.transparent,
              padding: spacing.moderate,
              borderTopLeftRadius: constants.borderRadius.small,
              borderBottomLeftRadius: constants.borderRadius.small,
              color: colors.structure.pebble,
              backgroundColor: colors.structure.mattGrey,
              ':focus': {
                outline: constants.borders.regular.transparent,
                border: constants.borders.keyOneRegular,
                ':hover': {
                  border: constants.borders.keyOneRegular,
                },
                color: colors.structure.coal,
                backgroundColor: colors.structure.white,
              },
              ':hover': {
                border: constants.borders.regular.steel,
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
              height={ICON_SIZE}
              css={{ color: colors.structure.white }}
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

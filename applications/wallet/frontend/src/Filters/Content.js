import PropTypes from 'prop-types'

import { spacing, constants } from '../Styles'

import SearchFilter from '../SearchFilter'
import Button, { VARIANTS } from '../Button'
import FilterExists from '../FilterExists'

import { dispatch, ACTIONS } from './helpers'

const FiltersContent = ({ projectId, assetId, filters, setIsMenuOpen }) => {
  return (
    <div>
      <div
        css={{ padding: spacing.small, borderBottom: constants.borders.spacer }}
      >
        <div css={{ display: 'flex' }}>
          <Button
            variant={VARIANTS.PRIMARY}
            style={{ flex: 1 }}
            onClick={() => setIsMenuOpen((isMenuOpen) => !isMenuOpen)}
          >
            + Add Metadata Filters
          </Button>

          <div css={{ width: spacing.base }} />

          <Button
            variant={VARIANTS.SECONDARY}
            style={{ flex: 1 }}
            isDisabled={filters.length === 0}
            onClick={() => {
              dispatch({
                action: ACTIONS.CLEAR_FILTERS,
                payload: { projectId, assetId },
              })
            }}
          >
            Clear All Filters
          </Button>
        </div>

        <div css={{ height: spacing.small }} />

        <SearchFilter
          projectId={projectId}
          assetId={assetId}
          filters={filters}
        />
      </div>

      {filters.map((filter, index) => {
        if (filter.type === 'exists') {
          return (
            <FilterExists
              // eslint-disable-next-line react/no-array-index-key
              key={`${filter.type}-${index}`}
              projectId={projectId}
              assetId={assetId}
              filters={filters}
              filter={filter}
              filterIndex={index}
            />
          )
        }

        return (
          <li
            // eslint-disable-next-line react/no-array-index-key
            key={`${filter.type}-${index}`}
            css={{
              display: 'flex',
              justifyContent: 'space-between',
            }}
          >
            • {filter.type}: {filter.attribute || filter.value}
            <button
              type="button"
              onClick={() =>
                dispatch({
                  action: ACTIONS.DELETE_FILTER,
                  payload: {
                    projectId,
                    assetId,
                    filters,
                    filterIndex: index,
                  },
                })
              }
            >
              delete
            </button>
          </li>
        )
      })}
    </div>
  )
}

FiltersContent.propTypes = {
  projectId: PropTypes.string.isRequired,
  assetId: PropTypes.string.isRequired,
  filters: PropTypes.arrayOf(
    PropTypes.shape({
      type: PropTypes.oneOf(['search', 'facet', 'range', 'exists']).isRequired,
      attribute: PropTypes.string,
      values: PropTypes.shape({}),
    }).isRequired,
  ).isRequired,
  setIsMenuOpen: PropTypes.func.isRequired,
}

export default FiltersContent

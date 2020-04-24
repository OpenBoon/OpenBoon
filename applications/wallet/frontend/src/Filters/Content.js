import PropTypes from 'prop-types'

import { spacing } from '../Styles'

import SearchFilter from '../SearchFilter'
import Button, { VARIANTS } from '../Button'

import { dispatch, ACTIONS } from './helpers'

const FiltersContent = ({ projectId, assetId, filters, setIsMenuOpen }) => {
  return (
    <div css={{ padding: spacing.small }}>
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
          onClick={() => {
            dispatch({
              action: ACTIONS.CLEAR_FILTERS,
              payload: {
                projectId,
                assetId,
              },
            })
          }}
        >
          Clear All Filters
        </Button>
      </div>

      <div css={{ height: spacing.small }} />

      <SearchFilter projectId={projectId} assetId={assetId} filters={filters} />

      <ul css={{ padding: 0 }}>
        {filters.map((filter, index) => (
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
        ))}
      </ul>
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
      value: PropTypes.oneOfType([PropTypes.string, PropTypes.object]),
    }).isRequired,
  ).isRequired,
  setIsMenuOpen: PropTypes.func.isRequired,
}

export default FiltersContent

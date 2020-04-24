import PropTypes from 'prop-types'

import { spacing } from '../Styles'

import SearchFilter from '../SearchFilter'
import Button, { VARIANTS } from '../Button'

import { dispatch, ACTIONS } from './helpers'

const FiltersContent = ({ projectId, assetId, filters, setIsMenuOpen }) => {
  return (
    <div css={{ padding: spacing.small }}>
      <Button
        variant={VARIANTS.PRIMARY}
        onClick={() => setIsMenuOpen((isMenuOpen) => !isMenuOpen)}
      >
        + Add Metadata Filters
      </Button>

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
            • {filter.value}
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
      value: PropTypes.oneOfType([PropTypes.string, PropTypes.object])
        .isRequired,
    }).isRequired,
  ).isRequired,
  setIsMenuOpen: PropTypes.func.isRequired,
}

export default FiltersContent

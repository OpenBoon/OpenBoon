import PropTypes from 'prop-types'

import filterShape from '../Filter/shape'

import SearchSvg from '../Icons/search.svg'

import { spacing, constants, colors } from '../Styles'

import FilterTextContentQuery from './Query'

import { dispatch, ACTIONS } from '../Filters/helpers'

const FilterTextContent = ({
  projectId,
  assetId,
  filters,
  filter,
  filter: { type, attribute },
  filterIndex,
}) => {
  if (attribute === '') {
    return (
      <FilterTextContentQuery // eslint-disable-next-line react/no-array-index-key
        key={`${type}-${filterIndex}`}
        projectId={projectId}
        assetId={assetId}
        filters={filters}
        filter={filter}
        filterIndex={filterIndex}
      />
    )
  }
  return (
    <li
      // eslint-disable-next-line react/no-array-index-key
      key={`${type}-${filterIndex}`}
      css={{
        display: 'flex',
        justifyContent: 'space-between',
      }}
    >
      <div
        css={{
          display: 'flex',
          alignItems: 'center',
          color: 'white',
          height: spacing.large,
          width: '100%',
          borderBottom: constants.borders.divider,
          paddingLeft: spacing.comfy,
          paddingRight: spacing.comfy,
        }}
      >
        <div css={{ display: 'flex', paddingRight: spacing.normal }}>
          <SearchSvg css={{ width: 14, color: colors.key.one }} />
        </div>
        <div
          title={filter.attribute}
          css={{
            overflow: 'hidden',
            textOverflow: 'ellipsis',
            whiteSpace: 'nowrap',
          }}
        >
          {filter.attribute}
        </div>
      </div>
      <button
        type="button"
        onClick={() =>
          dispatch({
            action: ACTIONS.DELETE_FILTER,
            payload: {
              projectId,
              assetId,
              filters,
              filterIndex,
            },
          })
        }
      >
        delete
      </button>
    </li>
  )
}

FilterTextContent.propTypes = {
  projectId: PropTypes.string.isRequired,
  assetId: PropTypes.string.isRequired,
  filters: PropTypes.arrayOf(PropTypes.shape(filterShape)).isRequired,
  filter: PropTypes.shape(filterShape).isRequired,
  filterIndex: PropTypes.number.isRequired,
}

export default FilterTextContent

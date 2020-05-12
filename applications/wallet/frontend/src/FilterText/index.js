import PropTypes from 'prop-types'

import filterShape from '../Filter/shape'

import FilterTextSearch from './Search'
import FilterTextDetection from './Detection'

const FilterText = ({
  projectId,
  assetId,
  filters,
  filter,
  filter: { attribute },
  filterIndex,
}) => {
  if (attribute === '') {
    return (
      <FilterTextSearch
        projectId={projectId}
        assetId={assetId}
        filters={filters}
        filter={filter}
        filterIndex={filterIndex}
      />
    )
  }

  return (
    <FilterTextDetection
      projectId={projectId}
      assetId={assetId}
      filters={filters}
      filter={filter}
      filterIndex={filterIndex}
    />
  )
}

FilterText.propTypes = {
  projectId: PropTypes.string.isRequired,
  assetId: PropTypes.string.isRequired,
  filters: PropTypes.arrayOf(PropTypes.shape(filterShape)).isRequired,
  filter: PropTypes.shape(filterShape).isRequired,
  filterIndex: PropTypes.number.isRequired,
}

export default FilterText

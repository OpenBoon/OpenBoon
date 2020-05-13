import PropTypes from 'prop-types'

import filterShape from '../Filter/shape'

import { spacing } from '../Styles'

import Accordion, { VARIANTS as ACCORDION_VARIANTS } from '../Accordion'
import FiltersTitle from '../Filters/Title'

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
        <FilterTextDetection
          projectId={projectId}
          assetId={assetId}
          filters={filters}
          filter={filter}
          filterIndex={filterIndex}
        />
      </div>
    </Accordion>
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

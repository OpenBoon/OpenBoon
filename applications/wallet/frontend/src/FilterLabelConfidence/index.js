import PropTypes from 'prop-types'

import filterShape from '../Filter/shape'

import { spacing } from '../Styles'

import Accordion, { VARIANTS as ACCORDION_VARIANTS } from '../Accordion'
import FiltersTitle from '../Filters/Title'
import SuspenseBoundary from '../SuspenseBoundary'

import FilterLabelConfidenceContent from './Content'

const FilterLabelConfidence = ({
  projectId,
  assetId,
  filters,
  filter,
  filterIndex,
}) => {
  return (
    <Accordion
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
      cacheKey={`FilterLabelConfidence.${filter.attribute}.${filterIndex}`}
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
        <SuspenseBoundary>
          <FilterLabelConfidenceContent
            projectId={projectId}
            assetId={assetId}
            filters={filters}
            filter={filter}
            filterIndex={filterIndex}
          />
        </SuspenseBoundary>
      </div>
    </Accordion>
  )
}

FilterLabelConfidence.propTypes = {
  projectId: PropTypes.string.isRequired,
  assetId: PropTypes.string.isRequired,
  filters: PropTypes.arrayOf(PropTypes.shape(filterShape)).isRequired,
  filter: PropTypes.shape(filterShape).isRequired,
  filterIndex: PropTypes.number.isRequired,
}

export default FilterLabelConfidence

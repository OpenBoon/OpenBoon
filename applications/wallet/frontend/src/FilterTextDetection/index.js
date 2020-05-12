import PropTypes from 'prop-types'

import filterShape from '../Filter/shape'

import { spacing } from '../Styles'

import Accordion, { VARIANTS as ACCORDION_VARIANTS } from '../Accordion'
import FiltersTitle from '../Filters/Title'
import SuspenseBoundary from '../SuspenseBoundary'

import FilterTextDetectionContent from './Content'

const FilterTextDetection = ({
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
          <FilterTextDetectionContent
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

FilterTextDetection.propTypes = {
  projectId: PropTypes.string.isRequired,
  assetId: PropTypes.string.isRequired,
  filters: PropTypes.arrayOf(PropTypes.shape(filterShape)).isRequired,
  filter: PropTypes.shape(filterShape).isRequired,
  filterIndex: PropTypes.number.isRequired,
}

export default FilterTextDetection

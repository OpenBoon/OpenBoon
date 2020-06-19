import PropTypes from 'prop-types'

import filterShape from '../Filter/shape'

import { spacing } from '../Styles'

import Accordion, { VARIANTS as ACCORDION_VARIANTS } from '../Accordion'
import FilterTitle from '../Filter/Title'
import FilterActions from '../Filter/Actions'
import SuspenseBoundary from '../SuspenseBoundary'

import FilterLabelConfidenceContent from './Content'

const FilterLabelConfidence = ({
  pathname,
  projectId,
  assetId,
  filters,
  filter,
  filterIndex,
}) => {
  return (
    <Accordion
      variant={ACCORDION_VARIANTS.FILTER}
      title={<FilterTitle filter={filter} />}
      actions={
        <FilterActions
          pathname={pathname}
          projectId={projectId}
          assetId={assetId}
          filters={filters}
          filter={filter}
          filterIndex={filterIndex}
        />
      }
      cacheKey={`FilterLabelConfidence.${filter.attribute}.${filterIndex}`}
      isInitiallyOpen
      isResizeable
    >
      <div
        css={{
          padding: spacing.normal,
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
            pathname={pathname}
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
  pathname: PropTypes.string.isRequired,
  projectId: PropTypes.string.isRequired,
  assetId: PropTypes.string.isRequired,
  filters: PropTypes.arrayOf(PropTypes.shape(filterShape)).isRequired,
  filter: PropTypes.shape(filterShape).isRequired,
  filterIndex: PropTypes.number.isRequired,
}

export default FilterLabelConfidence

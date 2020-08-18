import PropTypes from 'prop-types'

import filterShape from '../Filter/shape'

import { spacing } from '../Styles'

import Accordion, { VARIANTS as ACCORDION_VARIANTS } from '../Accordion'
import FilterIcon from '../Filter/Icon'
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
      icon={<FilterIcon filter={filter} />}
      title={filter.attribute}
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
      cacheKey={`FilterLabelConfidence.${filter.attribute}`}
      isInitiallyOpen
      isResizeable
    >
      <div css={{ padding: spacing.normal }}>
        <SuspenseBoundary isTransparent>
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

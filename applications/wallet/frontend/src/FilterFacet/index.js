import PropTypes from 'prop-types'

import filterShape from '../Filter/shape'

import { spacing } from '../Styles'

import Accordion, { VARIANTS as ACCORDION_VARIANTS } from '../Accordion'
import SuspenseBoundary from '../SuspenseBoundary'

import FilterFacetContent from './Content'

const FilterFacet = ({ projectId, assetId, filters, filter, filterIndex }) => {
  return (
    <Accordion
      variant={ACCORDION_VARIANTS.PANEL}
      title={filter.attribute}
      isInitiallyOpen
    >
      <div
        css={{
          padding: spacing.normal,
          paddingLeft: spacing.moderate,
          paddingRight: spacing.moderate,
          '> div': {
            backgroundColor: 'transparent',
            boxShadow: 'none',
          },
        }}
      >
        <SuspenseBoundary>
          <FilterFacetContent
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

FilterFacet.propTypes = {
  projectId: PropTypes.string.isRequired,
  assetId: PropTypes.string.isRequired,
  filters: PropTypes.arrayOf(PropTypes.shape(filterShape)).isRequired,
  filter: PropTypes.shape(filterShape).isRequired,
  filterIndex: PropTypes.number.isRequired,
}

export default FilterFacet

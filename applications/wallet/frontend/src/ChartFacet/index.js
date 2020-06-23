import PropTypes from 'prop-types'

import chartShape from '../Chart/shape'

import Card, { VARIANTS as CARD_VARIANTS } from '../Card'
import SuspenseBoundary from '../SuspenseBoundary'

import ChartForm from '../ChartForm'

import ChartFacetContent from './Content'

const ChartFacet = ({ chart }) => {
  if (!chart.attribute) {
    return <ChartForm chart={chart} />
  }

  return (
    <Card
      variant={CARD_VARIANTS.DARK}
      header={chart.attribute}
      content={
        <SuspenseBoundary>
          <ChartFacetContent chart={chart} />
        </SuspenseBoundary>
      }
    />
  )
}

ChartFacet.propTypes = {
  chart: PropTypes.shape(chartShape).isRequired,
}

export default ChartFacet

import PropTypes from 'prop-types'
import SuspenseBoundary from '../SuspenseBoundary'

import chartShape from '../Chart/shape'

import Card, { VARIANTS as CARD_VARIANTS } from '../Card'

import ChartForm from '../ChartForm'

import ChartFacetContent from './Content'

const ChartFacet = ({ chart, chartIndex, dispatch }) => {
  if (!chart.attribute) {
    return (
      <Card
        variant={CARD_VARIANTS.DARK}
        header=""
        content={
          <ChartForm
            chart={chart}
            chartIndex={chartIndex}
            dispatch={dispatch}
          />
        }
      />
    )
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
  chartIndex: PropTypes.number.isRequired,
  dispatch: PropTypes.func.isRequired,
}

export default ChartFacet

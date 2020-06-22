import PropTypes from 'prop-types'

import chartShape from '../Chart/shape'

import Card, { VARIANTS as CARD_VARIANTS } from '../Card'
import SuspenseBoundary from '../SuspenseBoundary'

import ChartRangeContent from './Content'

const ChartRange = ({ chart }) => {
  if (!chart.attribute) return null

  return (
    <Card
      variant={CARD_VARIANTS.DARK}
      header={chart.attribute}
      content={
        <SuspenseBoundary>
          <ChartRangeContent chart={chart} />
        </SuspenseBoundary>
      }
    />
  )
}

ChartRange.propTypes = {
  chart: PropTypes.shape(chartShape).isRequired,
}

export default ChartRange

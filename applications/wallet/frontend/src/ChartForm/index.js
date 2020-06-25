import PropTypes from 'prop-types'
import SuspenseBoundary from '../SuspenseBoundary'

import chartShape from '../Chart/shape'

import Card, { VARIANTS as CARD_VARIANTS } from '../Card'

import ChartFormContent from './Content'

const ChartForm = ({ chart, chartIndex, dispatch }) => {
  return (
    <Card
      variant={CARD_VARIANTS.DARK}
      header=""
      content={
        <SuspenseBoundary>
          <ChartFormContent
            chart={chart}
            chartIndex={chartIndex}
            dispatch={dispatch}
          />
        </SuspenseBoundary>
      }
    />
  )
}

ChartForm.propTypes = {
  chart: PropTypes.shape(chartShape).isRequired,
  chartIndex: PropTypes.number.isRequired,
  dispatch: PropTypes.func.isRequired,
}

export default ChartForm

import PropTypes from 'prop-types'

import chartShape from '../Chart/shape'

import SuspenseBoundary from '../SuspenseBoundary'

import ChartFormContent from './Content'

const ChartForm = ({ chart, chartIndex, dispatch }) => {
  return (
    <SuspenseBoundary>
      <ChartFormContent
        chart={chart}
        chartIndex={chartIndex}
        dispatch={dispatch}
      />
    </SuspenseBoundary>
  )
}

ChartForm.propTypes = {
  chart: PropTypes.shape(chartShape).isRequired,
  chartIndex: PropTypes.number.isRequired,
  dispatch: PropTypes.func.isRequired,
}

export default ChartForm

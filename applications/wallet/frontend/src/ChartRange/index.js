import PropTypes from 'prop-types'

import chartShape from '../Chart/shape'

import Card, { VARIANTS as CARD_VARIANTS } from '../Card'
import SuspenseBoundary from '../SuspenseBoundary'

import ChartForm from '../ChartForm'
import ChartsHeader from '../Charts/Header'

import ChartRangeContent from './Content'

const ChartRange = ({ chart, chartIndex, dispatch }) => {
  const { attribute } = chart

  if (!attribute) {
    return (
      <ChartForm chart={chart} chartIndex={chartIndex} dispatch={dispatch} />
    )
  }

  return (
    <Card
      variant={CARD_VARIANTS.DARK}
      header={
        <ChartsHeader
          attribute={attribute}
          chartIndex={chartIndex}
          dispatch={dispatch}
        />
      }
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
  chartIndex: PropTypes.number.isRequired,
  dispatch: PropTypes.func.isRequired,
}

export default ChartRange

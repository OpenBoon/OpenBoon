import { useState } from 'react'
import PropTypes from 'prop-types'

import chartShape from '../Chart/shape'

import Card, { VARIANTS as CARD_VARIANTS } from '../Card'
import SuspenseBoundary from '../SuspenseBoundary'

import ChartForm from '../ChartForm'
import ChartHeader from '../Chart/Header'

import ChartRangeContent from './Content'

const ChartRange = ({ chart, chartIndex, dispatch }) => {
  const [isEditing, setIsEditing] = useState(false)

  const { attribute } = chart

  if (!attribute || isEditing) {
    return (
      <ChartForm
        chart={chart}
        chartIndex={chartIndex}
        dispatch={dispatch}
        isEditing={isEditing}
        setIsEditing={setIsEditing}
      />
    )
  }

  return (
    <Card
      variant={CARD_VARIANTS.DARK}
      header={
        <ChartHeader
          attribute={attribute}
          chartIndex={chartIndex}
          dispatch={dispatch}
          setIsEditing={setIsEditing}
        />
      }
      content={
        <div css={{ height: '100%', '.Loading': { minHeight: 'auto' } }}>
          <SuspenseBoundary isTransparent>
            <ChartRangeContent chart={chart} />
          </SuspenseBoundary>
        </div>
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

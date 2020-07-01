import { useState } from 'react'
import PropTypes from 'prop-types'
import SuspenseBoundary from '../SuspenseBoundary'

import chartShape from '../Chart/shape'

import Card, { VARIANTS as CARD_VARIANTS } from '../Card'

import ChartForm from '../ChartForm'
import ChartHeader from '../Chart/Header'

import ChartFacetContent from './Content'

const ChartFacet = ({ chart, chartIndex, dispatch }) => {
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

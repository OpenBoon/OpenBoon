import PropTypes from 'prop-types'
import SuspenseBoundary from '../SuspenseBoundary'

import chartShape from '../Chart/shape'

import Card, { VARIANTS as CARD_VARIANTS } from '../Card'

import ChartFormContent from './Content'

const ChartForm = ({
  chart,
  chartIndex,
  dispatch,
  isEditing,
  setIsEditing,
}) => {
  return (
    <Card
      variant={CARD_VARIANTS.DARK}
      header=""
      content={
        <SuspenseBoundary isTransparent>
          <ChartFormContent
            chart={chart}
            chartIndex={chartIndex}
            dispatch={dispatch}
            isEditing={isEditing}
            setIsEditing={setIsEditing}
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
  isEditing: PropTypes.bool.isRequired,
  setIsEditing: PropTypes.func.isRequired,
}

export default ChartForm

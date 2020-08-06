import PropTypes from 'prop-types'

import { constants } from '../Styles'

import SuspenseBoundary from '../SuspenseBoundary'

import MetadataPrettyNoResults from './NoResults'
import MetadataPrettyPredictionsQuery from './PredictionsQuery'
import MetadataPrettyPredictionsContent from './PredictionsContent'

const MetadataPrettyPredictions = ({ name, value: { predictions }, path }) => {
  if (predictions.length === 0) {
    return <MetadataPrettyNoResults name={name} />
  }

  if (Object.keys(predictions[0]).includes('bbox')) {
    return (
      <div
        css={{
          '&:not(:first-of-type)': {
            borderTop: constants.borders.large.smoke,
          },
        }}
      >
        <SuspenseBoundary isTransparent>
          <MetadataPrettyPredictionsQuery name={name} path={path} />
        </SuspenseBoundary>
      </div>
    )
  }

  return (
    <MetadataPrettyPredictionsContent name={name} predictions={predictions} />
  )
}

MetadataPrettyPredictions.propTypes = {
  name: PropTypes.string.isRequired,
  value: PropTypes.shape({
    predictions: PropTypes.arrayOf(PropTypes.shape({})).isRequired,
  }).isRequired,
  path: PropTypes.string.isRequired,
}

export default MetadataPrettyPredictions

import PropTypes from 'prop-types'
import { useRouter } from 'next/router'

import { constants } from '../Styles'

import SuspenseBoundary from '../SuspenseBoundary'
import Button, { VARIANTS } from '../Button'

import {
  ACTIONS as FILTER_ACTIONS,
  dispatch as filterDispatch,
} from '../Filters/helpers'

import MetadataPrettyNoResults from './NoResults'
import MetadataPrettyPredictionsQuery from './PredictionsQuery'
import MetadataPrettyPredictionsContent, {
  FILTER_TYPES,
} from './PredictionsContent'

const MetadataPrettyPredictions = ({
  path,
  name,
  value: { type, predictions },
}) => {
  const {
    pathname,
    query: { projectId, assetId, query },
  } = useRouter()

  if (predictions.length === 0) {
    return (
      <MetadataPrettyNoResults
        name={
          <Button
            aria-label="Add Filter"
            variant={VARIANTS.NEUTRAL}
            style={{
              fontSize: 'inherit',
              lineHeight: 'inherit',
            }}
            onClick={() => {
              filterDispatch({
                type: FILTER_ACTIONS.ADD_VALUE,
                payload: {
                  pathname,
                  projectId,
                  assetId,
                  filter: {
                    type: FILTER_TYPES[type],
                    attribute: `${path}.${name}`,
                    values: {},
                  },
                  query,
                },
              })
            }}
          >
            {name}
          </Button>
        }
      />
    )
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
          <MetadataPrettyPredictionsQuery path={path} name={name} type={type} />
        </SuspenseBoundary>
      </div>
    )
  }

  return (
    <MetadataPrettyPredictionsContent
      path={path}
      name={name}
      type={type}
      predictions={predictions}
    />
  )
}

MetadataPrettyPredictions.propTypes = {
  path: PropTypes.string.isRequired,
  name: PropTypes.string.isRequired,
  value: PropTypes.shape({
    type: PropTypes.oneOf(['labels', 'text']).isRequired,
    predictions: PropTypes.arrayOf(PropTypes.shape({})).isRequired,
  }).isRequired,
}

export default MetadataPrettyPredictions

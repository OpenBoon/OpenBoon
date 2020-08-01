import PropTypes from 'prop-types'

import { constants, spacing, colors } from '../Styles'

import SuspenseBoundary from '../SuspenseBoundary'

import MetadataPrettyNoResults from './NoResults'
import MetadataPrettyLabelsQuery from './LabelsQuery'
import MetadataPrettyLabelsContent from './LabelsContent'

const MetadataPrettyLabels = ({ name, value: { predictions }, path }) => {
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
          '> div.ErrorBoundary': {
            padding: `${spacing.normal}px ${spacing.moderate}px`,
            div: {
              backgroundColor: colors.structure.transparent,
              boxShadow: 'none',
            },
          },
          '.Loading': {
            backgroundColor: colors.structure.transparent,
            boxShadow: 'none',
          },
        }}
      >
        <SuspenseBoundary>
          <MetadataPrettyLabelsQuery name={name} path={path} />
        </SuspenseBoundary>
      </div>
    )
  }

  return <MetadataPrettyLabelsContent name={name} value={{ predictions }} />
}

MetadataPrettyLabels.propTypes = {
  name: PropTypes.string.isRequired,
  value: PropTypes.shape({
    predictions: PropTypes.arrayOf(PropTypes.shape({})).isRequired,
  }).isRequired,
  path: PropTypes.string.isRequired,
}

export default MetadataPrettyLabels

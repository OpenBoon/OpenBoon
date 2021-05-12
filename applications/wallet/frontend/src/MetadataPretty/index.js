import PropTypes from 'prop-types'

import SuspenseBoundary from '../SuspenseBoundary'

import MetadataPrettyMetrics from './Metrics'
import MetadataPrettyLabels from './Labels'
import MetadataPrettySwitch from './Switch'

const MetadataPretty = ({ metadata, section }) => {
  if (section === 'metrics') {
    return <MetadataPrettyMetrics pipeline={metadata.metrics.pipeline} />
  }

  if (section === 'labels') {
    return (
      <SuspenseBoundary isTransparent>
        <MetadataPrettyLabels />
      </SuspenseBoundary>
    )
  }

  return (
    <MetadataPrettySwitch path={section} name="" value={metadata[section]} />
  )
}

MetadataPretty.propTypes = {
  metadata: PropTypes.shape({
    source: PropTypes.shape({
      path: PropTypes.string,
      filename: PropTypes.string,
      extension: PropTypes.string,
      mimetype: PropTypes.string,
    }),
    system: PropTypes.shape({
      projectId: PropTypes.string,
      dataSourceId: PropTypes.string,
      jobId: PropTypes.string,
      taskId: PropTypes.string,
      timeCreated: PropTypes.string,
      state: PropTypes.string,
    }),
    files: PropTypes.arrayOf(
      PropTypes.shape({
        id: PropTypes.string,
        size: PropTypes.number,
        name: PropTypes.string,
        mimetype: PropTypes.string,
        category: PropTypes.string,
        attrs: PropTypes.shape({
          width: PropTypes.number,
          height: PropTypes.number,
        }),
      }),
    ),
    metrics: PropTypes.shape({
      pipeline: PropTypes.arrayOf(PropTypes.shape({})),
    }),
  }).isRequired,
  section: PropTypes.string.isRequired,
}

export default MetadataPretty

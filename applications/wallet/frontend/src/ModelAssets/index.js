import PropTypes from 'prop-types'
import useSWR from 'swr'

import { cleanup, encode } from '../Filters/helpers'

import ModelAssetsContent from './Content'

const ModelAssets = ({ projectId, modelId, moduleName }) => {
  const {
    data: { results: labels },
  } = useSWR(`/api/v1/projects/${projectId}/models/${modelId}/get_labels/`)

  const labelsAggregate = labels.reduce((acc, { label }) => {
    return [...acc, label]
  }, [])

  const encodedFilter = encode({
    filters: [
      {
        type: 'label',
        attribute: `labels.${moduleName}`,
        modelId,
        values: {
          scope: 'all',
          labels: labelsAggregate,
        },
      },
    ],
  })

  const query = cleanup({ query: encodedFilter })

  return <ModelAssetsContent projectId={projectId} query={query} />
}

ModelAssets.propTypes = {
  projectId: PropTypes.string.isRequired,
  modelId: PropTypes.string.isRequired,
  moduleName: PropTypes.string.isRequired,
}

export default ModelAssets

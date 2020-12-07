import PropTypes from 'prop-types'
import { useRouter } from 'next/router'
import useSWR from 'swr'

import { cleanup, decode, encode } from '../Filters/helpers'
import { SCOPE_OPTIONS } from '../AssetLabeling/helpers'

import ModelAssetsContent from './Content'

const ModelAssets = ({ moduleName }) => {
  const {
    query: { projectId, modelId, query: q },
  } = useRouter()

  const { scope, label } = decode({ query: q })

  const {
    data: { results: labels },
  } = useSWR(`/api/v1/projects/${projectId}/models/${modelId}/get_labels/`)

  const encodedFilter = encode({
    filters: [
      {
        type: 'label',
        attribute: `labels.${moduleName}`,
        modelId,
        values: {
          scope: scope || SCOPE_OPTIONS[0].label,
          labels: [label || labels[0].label],
        },
      },
    ],
  })

  const query = cleanup({ query: encodedFilter })

  return <ModelAssetsContent projectId={projectId} query={query} />
}

ModelAssets.propTypes = {
  moduleName: PropTypes.string.isRequired,
}

export default ModelAssets

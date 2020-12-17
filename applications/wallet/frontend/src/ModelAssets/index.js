import PropTypes from 'prop-types'
import { useRouter } from 'next/router'
import useSWR from 'swr'

import { decode, encode } from '../Filters/helpers'
import { SCOPE_OPTIONS } from '../AssetLabeling/helpers'

import ModelAssetsContent from './Content'

const ModelAssets = ({ moduleName }) => {
  const {
    query: { projectId, modelId, query: q },
  } = useRouter()

  const { scope, label } = decode({ query: q })

  const {
    data: { results: labels = [] },
  } = useSWR(`/api/v1/projects/${projectId}/models/${modelId}/get_labels/`)

  const labelValue = label || (labels[0] && labels[0].label) || ''

  const filter = encode({
    filters: [
      {
        type: 'label',
        attribute: `labels.${moduleName}`,
        modelId,
        values: {
          scope: scope || SCOPE_OPTIONS[0].label,
          labels: [labelValue],
        },
      },
    ],
  })

  return <ModelAssetsContent filter={filter} label={labelValue} />
}

ModelAssets.propTypes = {
  moduleName: PropTypes.string.isRequired,
}

export default ModelAssets

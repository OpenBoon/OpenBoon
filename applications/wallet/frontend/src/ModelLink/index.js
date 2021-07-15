import { useRouter } from 'next/router'
import useSWR from 'swr'

import { constants, colors } from '../Styles'

import ModelLinkForm from './Form'

const Model = () => {
  const {
    query: { projectId, modelId },
  } = useRouter()

  const { data: model } = useSWR(
    `/api/v1/projects/${projectId}/models/${modelId}/`,
  )

  const { datasetType } = model

  return (
    <div>
      <div
        css={{
          maxWidth: constants.paragraph.maxWidth,
          color: colors.structure.zinc,
        }}
      >
        Datasets are groups of labels added to assets that are used by the model
        for training. When using an uploaded, pre-trained model, only the
        testing labels in the dataset are used.
      </div>

      <ModelLinkForm datasetType={datasetType} />
    </div>
  )
}

export default Model

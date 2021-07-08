import { useRouter } from 'next/router'
import useSWR from 'swr'

import { spacing } from '../Styles'

import ItemTitle from '../Item/Title'
import ItemList from '../Item/List'
import ItemSeparator from '../Item/Separator'

import ModelLinkForm from './Form'

const ModelLinkContent = () => {
  const {
    query: { projectId, modelId },
  } = useRouter()

  const {
    data: { results: modelTypes },
  } = useSWR(`/api/v1/projects/${projectId}/models/model_types/`)

  const { data: model } = useSWR(
    `/api/v1/projects/${projectId}/models/${modelId}/`,
  )

  const { name, type, description, datasetType } = model

  const { label } = modelTypes.find(({ name: n }) => n === type) || {
    label: type,
  }

  return (
    <div>
      <ItemTitle type="Model" name={name} />

      <ItemList
        attributes={[
          ['Model Type', label],
          ['Description', description],
        ]}
      />

      <div css={{ height: spacing.normal }} />

      <ItemSeparator />

      <ModelLinkForm datasetType={datasetType} />
    </div>
  )
}

export default ModelLinkContent

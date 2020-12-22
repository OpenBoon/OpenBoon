import { mutate } from 'swr'

import { fetcher, revalidate, parseResponse } from '../Fetch/helpers'

export const SCOPE_OPTIONS = [
  { value: 'TRAIN', label: 'Train' },
  { value: 'TEST', label: 'Test' },
]

export const getOptions = async ({ projectId, modelId }) => {
  if (!modelId) {
    return []
  }

  const { results } = await revalidate({
    key: `/api/v1/projects/${projectId}/models/${modelId}/get_labels`,
  })

  return results
}

export const getSubmitText = ({ localState, existingLabel }) => {
  const { success, isLoading } = localState

  if ((success && !isLoading) || existingLabel) {
    return 'Label Saved'
  }

  if (!success && isLoading) {
    return 'Saving...'
  }

  return 'Save Label'
}

const getLabelAction = ({ body }) => {
  if (body.addLabels && body.removeLabels) {
    return 'update'
  }

  if (body.removeLabels) {
    return 'delete'
  }

  return 'add'
}

export const onSubmit = async ({
  dispatch,
  localDispatch,
  localState: { modelId, label, scope, reloadKey },
  labels,
  projectId,
  assetId,
}) => {
  localDispatch({ isLoading: true, errors: {} })

  const existingModel = labels.find(
    ({ modelId: labelModel }) => labelModel === modelId,
  )

  const body = {}

  if (existingModel) {
    body.removeLabels = [{ assetId, label: existingModel.label }]
  }

  if (label !== '') {
    body.addLabels = [{ assetId, label, scope }]
  }

  const labelAction = getLabelAction({ body })

  try {
    await fetcher(
      `/api/v1/projects/${projectId}/models/${modelId}/${labelAction}_labels/`,
      {
        method: labelAction === 'delete' ? 'DELETE' : 'POST',
        body: JSON.stringify(body),
      },
    )

    mutate(`/api/v1/projects/${projectId}/assets/${assetId}/`)

    localDispatch({
      reloadKey: reloadKey + 1,
      success: true,
      isLoading: false,
      errors: {},
    })

    dispatch({ modelId, label, scope, assetId })
  } catch (response) {
    const errors = await parseResponse({ response })

    localDispatch({ success: false, isLoading: false, errors })
  }
}

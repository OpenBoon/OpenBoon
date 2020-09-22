import { mutate } from 'swr'

import { fetcher, revalidate, parseResponse } from '../Fetch/helpers'

export const getOptions = async ({ projectId, modelId, isFirstLabel }) => {
  if (!modelId || isFirstLabel) {
    return []
  }

  const { results } = await revalidate({
    key: `/api/v1/projects/${projectId}/models/${modelId}/get_labels`,
  })

  return results
}

export const getSubmitText = ({ state, existingLabel }) => {
  const { success, isLoading } = state

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
  state: { modelId, label, reloadKey },
  labels,
  projectId,
  assetId,
}) => {
  dispatch({ isLoading: true, errors: {} })

  const existingModel = labels.find(
    ({ modelId: labelModel }) => labelModel === modelId,
  )

  const body = {}

  if (existingModel) {
    body.removeLabels = [{ assetId, label: existingModel.label }]
  }

  if (label !== '') {
    body.addLabels = [{ assetId, label }]
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

    dispatch({
      reloadKey: reloadKey + 1,
      success: true,
      isLoading: false,
      errors: {},
    })
  } catch (response) {
    const errors = await parseResponse({ response })

    dispatch({ success: false, isLoading: false, errors })
  }
}

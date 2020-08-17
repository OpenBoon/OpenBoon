import { mutate } from 'swr'

import { fetcher, revalidate } from '../Fetch/helpers'

export const getOptions = async ({ modelId, projectId }) => {
  if (!modelId) {
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
  setLocalModelId,
  setLocalLabel,
}) => {
  dispatch({ isLoading: true })

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

    setLocalModelId({ value: modelId })

    setLocalLabel({ value: label })
  } catch (response) {
    try {
      const errors = await response.json()

      const parsedErrors = Object.keys(errors).reduce((acc, errorKey) => {
        acc[errorKey] = errors[errorKey].join(' ')
        return acc
      }, {})

      dispatch({ success: false, isLoading: false, errors: parsedErrors })
    } catch (error) {
      dispatch({
        success: false,
        isLoading: false,
        errors: { global: 'Something went wrong. Please try again.' },
      })
    }
  }
}

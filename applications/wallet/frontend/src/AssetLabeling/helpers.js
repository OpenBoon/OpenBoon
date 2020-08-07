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
  if (body.add_labels && body.remove_labels) {
    return 'update'
  }

  if (body.remove_labels) {
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
    body.remove_labels = [{ assetId, label: existingModel.label }]
  }

  if (label !== '') {
    body.add_labels = [{ assetId, label }]
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

export const onDelete = async ({
  modelId,
  label,
  setError,
  projectId,
  assetId,
}) => {
  try {
    await fetcher(
      `/api/v1/projects/${projectId}/models/${modelId}/delete_labels/`,
      {
        method: 'DELETE',
        body: JSON.stringify({
          remove_labels: [
            {
              assetId,
              label,
            },
          ],
        }),
      },
    )

    mutate(`/api/v1/projects/${projectId}/assets/${assetId}/`)

    setError('')
  } catch (response) {
    setError('Something went wrong. Please try again.')
  }
}

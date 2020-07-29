import { mutate } from 'swr'

import { fetcher } from '../Fetch/helpers'

export const getSubmitText = ({ state }) => {
  const { success, isLoading } = state

  if (success && !isLoading) {
    return 'Label Saved'
  }

  if (!success && isLoading) {
    return 'Saving...'
  }

  return 'Save Label'
}

export const onSubmit = async ({
  dispatch,
  state: { model, label },
  projectId,
  assetId,
  setLocalModel,
  setLocalLabel,
}) => {
  dispatch({ isLoading: true })

  try {
    await fetcher(`/api/v1/projects/${projectId}/models/${model}/add_labels/`, {
      method: 'POST',
      body: JSON.stringify({
        add_labels: [
          {
            assetId,
            label,
          },
        ],
      }),
    })

    mutate(`/api/v1/projects/${projectId}/assets/${assetId}/`)

    dispatch({
      success: true,
      isLoading: false,
      errors: {},
    })

    setLocalModel({ value: model })
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

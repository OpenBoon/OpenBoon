import { mutate } from 'swr'

import { fetcher } from '../Fetch/helpers'

export const onSave = async ({
  projectId,
  assetId,
  labels,
  predictions,
  errors,
  dispatch,
}) => {
  const newLabels = predictions.map(({ bbox, simhash }) => {
    return {
      bbox,
      simhash,
      label: labels[simhash],
    }
  })

  try {
    dispatch({ isLoading: true })

    await fetcher(`/api/v1/projects/${projectId}/faces/${assetId}/save/`, {
      method: 'POST',
      body: JSON.stringify({
        labels: newLabels,
      }),
    })

    mutate(`/api/v1/projects/${projectId}/faces/${assetId}/`)
    mutate(`/api/v1/projects/${projectId}/faces/labels`)

    dispatch({ isLoading: false })
  } catch (response) {
    try {
      const errorObject = await response.json()
      const errorMessage = errorObject.labels[0].nonFieldErrors[0]

      dispatch({
        isLoading: false,
        errors: { ...errors, global: errorMessage },
      })
    } catch (error) {
      dispatch({
        isLoading: false,
        errors: {
          ...errors,
          global: 'Something went wrong. Please try again.',
        },
      })
    }
  }
}

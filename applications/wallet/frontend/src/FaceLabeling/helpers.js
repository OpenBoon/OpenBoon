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
  const newLabels = predictions
    .filter(
      // filter out labels that are the same as the predicted label
      ({ simhash, label }) => label !== labels[simhash],
    )
    .map(({ bbox, simhash }) => {
      return {
        bbox,
        simhash,
        label: labels[simhash],
      }
    })

  try {
    dispatch({ isLoading: true, errors: { ...errors, global: '' } })

    await fetcher(`/api/v1/projects/${projectId}/faces/${assetId}/save/`, {
      method: 'POST',
      body: JSON.stringify({
        labels: newLabels,
      }),
    })

    await mutate(`/api/v1/projects/${projectId}/faces/${assetId}/`)
    await mutate(`/api/v1/projects/${projectId}/faces/labels/`)
    await mutate(`/api/v1/projects/${projectId}/faces/status/`)

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

export const onTrain = async ({ projectId, setError }) => {
  try {
    setError('')

    const { jobId } = await fetcher(
      `/api/v1/projects/${projectId}/faces/train/`,
      {
        method: 'POST',
      },
    )

    mutate(
      `/api/v1/projects/${projectId}/faces/status/`,
      {
        unappliedChanges: false,
        jobId,
      },
      false,
    )
  } catch (error) {
    setError('Something went wrong. Please try again.')
  }
}

export const getSaveButtonCopy = ({ isChanged, isLoading }) => {
  if (isLoading) {
    return 'Saving...'
  }

  if (isChanged) {
    return 'Save'
  }

  return 'Saved'
}

export const getHelpInfoCopy = ({ jobId, unappliedChanges }) => {
  if (jobId && unappliedChanges) {
    return 'There are newly saved labels that have not been used for training. Stop current training and restart with the latest labels.'
  }

  if (jobId && !unappliedChanges) {
    return 'Training in progress. No action can be taken.'
  }

  return 'Train model with saved labels and apply new predictions.'
}

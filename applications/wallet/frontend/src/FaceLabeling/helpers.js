import { mutate } from 'swr'

import { fetcher, parseResponse } from '../Fetch/helpers'

export const onSave = async ({
  projectId,
  assetId,
  labels,
  predictions,
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
    dispatch({ isLoading: true, errors: { labels: {} } })

    await fetcher(`/api/v1/projects/${projectId}/faces/${assetId}/save/`, {
      method: 'POST',
      body: JSON.stringify({
        labels: newLabels,
      }),
    })

    await mutate(`/api/v1/projects/${projectId}/faces/${assetId}/`)
    await mutate(`/api/v1/projects/${projectId}/faces/labels/`)
    await mutate(`/api/v1/projects/${projectId}/faces/status/`)

    dispatch({ isChanged: false, isLoading: false })
  } catch (response) {
    const errors = await parseResponse({ response })

    dispatch({ isLoading: false, errors: { ...errors, labels: {} } })
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
  } catch (response) {
    const { global } = await parseResponse({ response })

    setError(global)
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
    return 'There are new labels that have not been used for training. Restart training with the latest labels.'
  }

  if (jobId && !unappliedChanges) {
    return 'There are no new labels to train.'
  }

  return 'There are new labels. Click to train and apply.'
}

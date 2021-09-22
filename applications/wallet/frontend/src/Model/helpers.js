import { mutate } from 'swr'

import { fetcher, parseResponse } from '../Fetch/helpers'

export const onTrainAndTest = async ({
  model,
  test,
  apply,
  projectId,
  modelId,
  setError,
}) => {
  try {
    setError('')

    const { jobId } = await fetcher(
      `/api/v1/projects/${projectId}/models/${modelId}/train/`,
      {
        body: JSON.stringify({ test, apply }),
        method: 'POST',
      },
    )

    mutate(
      `/api/v1/projects/${projectId}/models/${modelId}/`,
      {
        ...model,
        ready: true,
        runningJobId: jobId,
      },
      false,
    )
  } catch (response) {
    const { global } = await parseResponse({ response })

    setError(global)
  }
}

export const onTest = async ({ model, projectId, modelId, setError }) => {
  try {
    setError('')

    const { jobId } = await fetcher(
      `/api/v1/projects/${projectId}/models/${modelId}/test/`,
      {
        method: 'POST',
      },
    )

    mutate(
      `/api/v1/projects/${projectId}/models/${modelId}/`,
      {
        ...model,
        ready: true,
        runningJobId: jobId,
      },
      false,
    )
  } catch (response) {
    const { global } = await parseResponse({ response })

    setError(global)
  }
}

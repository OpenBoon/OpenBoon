import { mutate } from 'swr'

import { fetcher } from '../Fetch/helpers'

export const onTrain = async ({
  model,
  deploy,
  projectId,
  modelId,
  setError,
}) => {
  try {
    setError('')

    const { jobId } = await fetcher(
      `/api/v1/projects/${projectId}/models/${modelId}/train/`,
      {
        body: JSON.stringify({ deploy }),
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
  } catch (error) {
    setError('Something went wrong. Please try again.')
  }
}

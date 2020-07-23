import { mutate } from 'swr'
import Router from 'next/router'

import { fetcher } from '../Fetch/helpers'

export const onTrain = async ({ apply, projectId, setError }) => {
  try {
    setError('')

    const { jobId } = await fetcher(
      `/api/v1/projects/${projectId}/faces/train/`,
      {
        body: JSON.stringify({ apply }),
        method: 'POST',
      },
    )

    mutate(
      `/api/v1/projects/${projectId}/faces/status/`,
      {
        ready: true,
        runningJobId: jobId,
      },
      false,
    )
  } catch (error) {
    setError('Something went wrong. Please try again.')
  }
}

export const onDelete = ({
  setDeleteModalOpen,
  projectId,
  modelId,
}) => async () => {
  setDeleteModalOpen(false)

  await fetcher(`/api/v1/projects/${projectId}/models/${modelId}/`, {
    method: 'DELETE',
  })

  await mutate(`/api/v1/projects/${projectId}/models/`)

  Router.push(
    '/[projectId]/models?action=delete-model-success',
    `/${projectId}/models?action=delete-model-success`,
  )
}

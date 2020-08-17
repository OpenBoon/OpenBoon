import { mutate } from 'swr'
import Router from 'next/router'

import { fetcher, revalidate } from '../Fetch/helpers'

export const onTrain = async ({
  model,
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
        body: JSON.stringify({ apply }),
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

export const onDelete = ({
  setDeleteModalOpen,
  projectId,
  modelId,
}) => async () => {
  setDeleteModalOpen(false)

  await fetcher(`/api/v1/projects/${projectId}/models/${modelId}/`, {
    method: 'DELETE',
  })

  await revalidate({
    key: `/api/v1/projects/${projectId}/models/`,
    paginated: true,
  })

  Router.push(
    '/[projectId]/models?action=delete-model-success',
    `/${projectId}/models`,
  )
}

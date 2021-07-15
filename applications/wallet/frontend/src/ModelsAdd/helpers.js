import Router from 'next/router'

import {
  fetcher,
  revalidate,
  getQueryString,
  parseResponse,
} from '../Fetch/helpers'

export const slugify = ({ value }) => {
  // https://gist.github.com/codeguy/6684588#gistcomment-3361909
  return value
    .toString()
    .normalize('NFD')
    .replace(/[\u0300-\u036f]/g, '')
    .toLowerCase()
    .replace(/\s+/g, '-')
    .replace(/[^a-z0-9-]/g, '')
    .replace(/--+/g, '-')
    .trim()
}

export const onSubmit = async ({
  dispatch,
  projectId,
  state: { name, description, type },
}) => {
  dispatch({ isLoading: true, errors: {} })

  try {
    const { id: modelId } = await fetcher(
      `/api/v1/projects/${projectId}/models/`,
      {
        method: 'POST',
        body: JSON.stringify({ name, description, type }),
      },
    )

    await revalidate({
      key: `/api/v1/projects/${projectId}/models/`,
    })

    await revalidate({
      key: `/api/v1/projects/${projectId}/models/all/`,
    })

    const queryString = getQueryString({
      action: 'add-model-success',
      modelId,
    })

    Router.push(`/[projectId]/models${queryString}`, `/${projectId}/models`)
  } catch (response) {
    const errors = await parseResponse({ response })

    dispatch({ isLoading: false, errors })
  }
}

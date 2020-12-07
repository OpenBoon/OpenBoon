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
    .trim()
    .replace(/\s+/g, '-')
    .replace(/[^\w-]+/g, '')
    .replace(/--+/g, '-')
}

export const onSubmit = async ({
  dispatch,
  projectId,
  state: { type, name, moduleName },
}) => {
  dispatch({ isLoading: true, errors: {} })

  try {
    const {
      results: { id: modelId },
    } = await fetcher(`/api/v1/projects/${projectId}/models/`, {
      method: 'POST',
      body: JSON.stringify({ type, name, moduleName }),
    })

    await revalidate({
      key: `/api/v1/projects/${projectId}/models/`,
      paginated: true,
    })

    await revalidate({
      key: `/api/v1/projects/${projectId}/models/all/`,
      paginated: false,
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

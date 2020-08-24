import Router from 'next/router'

import { fetcher, revalidate, getQueryString } from '../Fetch/helpers'

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
  dispatch({ isLoading: true })

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

    const queryString = getQueryString({
      action: 'add-model-success',
      modelId,
    })

    Router.push(`/[projectId]/models${queryString}`, `/${projectId}/models`)
  } catch (response) {
    try {
      const errors = await response.json()

      const parsedErrors = Object.keys(errors).reduce((acc, errorKey) => {
        acc[errorKey] = errors[errorKey].join(' ')
        return acc
      }, {})

      dispatch({ isLoading: false, errors: parsedErrors })
    } catch (error) {
      dispatch({
        isLoading: false,
        errors: { global: 'Something went wrong. Please try again.' },
      })
    }
  }
}
